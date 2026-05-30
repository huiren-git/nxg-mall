package com.nxg.mall.api.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nxg.mall.common.dto.payload.LoginEventDTO;
import com.nxg.mall.common.dto.payload.UserRegisterDTO;
import com.nxg.mall.common.enums.CaptchaScene;
import com.nxg.mall.common.enums.EventType;
import com.nxg.mall.common.enums.UserRoleEnum;
import com.nxg.mall.common.enums.UserStatusEnum;
import com.nxg.mall.common.dto.*;
import com.nxg.mall.common.exception.BusinessException;
import com.nxg.mall.api.service.MailService;
import com.nxg.mall.common.vo.*;
import com.nxg.mall.common.entity.User;
import com.nxg.mall.common.entity.UserStatus;
import com.nxg.mall.api.service.AuthService;
import com.nxg.mall.api.service.IUserService;
import com.nxg.mall.api.service.IUserStatusService;
import com.nxg.mall.common.utils.EmailUtil;
import com.nxg.mall.common.utils.JwtUtil;
import com.nxg.mall.common.utils.RedisUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.nxg.mall.common.constant.RedisConstants.*;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    // 前端项目的访问地址，建议配置在 application.yml 中 (如: app.frontend.url: http://localhost:3000)
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private MailService mailService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IUserStatusService userStatusService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    @Qualifier("ipV4Searcher")
    private Searcher ipV4Searcher;

    @Autowired
    @Qualifier("ipV6Searcher")
    private Searcher ipV6Searcher;

    private final SecureRandom random = new SecureRandom();

    @Override
    public void sendCode(@Valid @RequestBody SendCodeRequest request) {

        log.info("============== 收到验证码请求，开始执行防刷锁逻辑！==============");
        String email = request.getEmail();
        String sceneCode = request.getScene();

        // 1. 先验证并获取枚举对象
        CaptchaScene sceneEnum = CaptchaScene.getByCode(sceneCode);
        if (sceneEnum == null) {
            throw new BusinessException("非法业务类型");
        }

        // 2. 统一使用枚举的 getCode() 拼接 Key
        String redisKey = CAPTCHA_PREFIX + sceneEnum.getCode() + ":" + email;

        // ================== 🛠️ 新增：60秒防刷原子限流机制 ==================
        String lockKey = "lock:code:email:" + email;
        // setIfAbsent 是原子操作：如果 key 不存在则写入并返回 true；如果已存在则返回 false
        Boolean isLockSuccess = redisUtil.setIfAbsent(lockKey, "1", 60, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(isLockSuccess)) {
            // 谁要是 60 秒内敢再来，直接乱棍打走
            throw new BusinessException("操作过于频繁，请60秒后再试");
        }
        // ==================================================================

        // 3. 验证码生成与 5 分钟有效期缓存不变
        String code = String.format("%06d", random.nextInt(1000000));
        redisUtil.set(redisKey, code, 5, TimeUnit.MINUTES);

        // 4. 发送邮件
        mailService.sendCodeEmail(email, code, sceneEnum.getDescription());

        log.info("验证码请求成功 - Key: {}, 验证码: {}", redisKey, code);
    }

    @Override
    @Transactional
    public void register(RegisterRequest request, String clientIp) {
        // 1. IP限制检查
        String ipKey = REG_FAIL_PREFIX + clientIp;
        Object failTimesObj = redisUtil.get(ipKey);
        if (failTimesObj != null) {
            int failTimes = Integer.parseInt(failTimesObj.toString());
            if (failTimes >= MAX_FAIL_TIMES) {
                throw new RuntimeException("注册失败，请检查用户名邮箱和验证码后重试");
            }
        }

        try {
            // 2. 参数校验 -- 由Spring Validation自动完成

            // 3. 唯一性校验 -- 判断用户名或邮箱是否存在
            if (userService.existsByUsername(request.getUsername()) || userService.existsByEmail(request.getEmail())) {
                throw new RuntimeException("注册失败，请检查用户名邮箱和验证码后重试");
            }

            // 4. 验证码校验
            String captchaKey = CAPTCHA_REGISTER_PREFIX + request.getEmail();

            Object storedCodeObj = redisUtil.get(captchaKey);
            if (storedCodeObj == null || !storedCodeObj.toString().equals(request.getCode())) {
                // 验证码错误，增加失败计数
                incrementFailCount(ipKey);
                throw new RuntimeException("注册失败，请检查用户名邮箱和验证码后重试");
            }
            // 验证通过，删除验证码（防止重复使用）
            redisUtil.delete(captchaKey);

            // 5. 密码加密
            String encodedPassword = passwordEncoder.encode(request.getPassword());

            // 6. 插入用户
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(encodedPassword);
            user.setRole(0);   // 默认注册为 Customer

            boolean saved = userService.save(user);
            if (!saved) {
                throw new RuntimeException("注册失败，请检查用户名邮箱和验证码后重试");
            }

            // 7. 插入用户状态表
            userStatusService.initUserStatus(user.getId());

            // ======= 新增：异步发送注册消息到 Kafka =======
            try {
                UserRegisterDTO event = UserRegisterDTO.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .registerIp(clientIp)
                        .terminal("Web")
                        .timestamp(System.currentTimeMillis())
                        .build();

                // 包装成 GenericEventDTO
                GenericEventDTO<UserRegisterDTO> message = new GenericEventDTO<>();
                message.setType(EventType.REGISTER); // 确保你的枚举中有这个类型
                message.setData(event);
                message.setTimestamp(System.currentTimeMillis());

                // 发送到业务事件 Topic
                kafkaTemplate.send("nxg_business_event", JSON.toJSONString(message));

                log.debug("已发送注册埋点消息，用户ID: {}", user.getId());
            } catch (Exception e) {
                // 埋点失败不应导致业务回滚
                log.error("发送注册埋点消息失败", e);
            }

            // 注册成功，清除该IP失败计数
            redisUtil.delete(ipKey);
            log.info("用户注册成功，用户名：{}，邮箱：{}", request.getUsername(), request.getEmail());

        } catch (RuntimeException e) {
            // 如果是我们主动抛出的统一错误，直接抛出；否则记录并抛出统一错误
            if (e.getMessage().equals("注册失败，请检查用户名邮箱和验证码后重试")) {
                throw e;
            } else {
                log.error("注册异常", e);
                incrementFailCount(ipKey);
                throw new RuntimeException("注册失败，请检查用户名邮箱和验证码后重试");
            }
        }
    }

    @Override
    public void updatePassword(Long userId, PasswordUpdateDTO dto) {
        User user = userService.getById(userId);

        // 复用校验逻辑
        validateCaptcha(user.getEmail(), dto.getCode(),
                CaptchaScene.UPDATE_PASSWORD, "密码修改失败，验证码错误或已过期");

        userService.lambdaUpdate()
                .set(User::getPassword, passwordEncoder.encode(dto.getNewPassword()))
                .eq(User::getId, userId)
                .update();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmail(Long userId, EmailUpdateDTO dto) {
        User user = userService.getById(userId);

        // 校验旧邮箱
        validateCaptcha(user.getEmail(), dto.getOldCode(),
                CaptchaScene.UPDATE_EMAIL_OLD, "原邮箱验证码错误");

        // 校验新邮箱
        validateCaptcha(dto.getNewEmail(), dto.getNewCode(),
                CaptchaScene.UPDATE_EMAIL_NEW, "新邮箱验证码错误");

        // 2. 检查新邮箱是否被占用 (使用 lambdaQuery)
        boolean exists = userService.lambdaQuery()
                .eq(User::getEmail, dto.getNewEmail())
                .ne(User::getId, userId) // 排除掉用户自己
                .exists();

        if (exists) {
            throw new BusinessException("该邮箱已被其他账号占用");
        }

        // 3. 执行更新
        boolean success = userService.lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getEmail, dto.getNewEmail())
                .update();

        if (!success) {
            throw new BusinessException("邮箱更新失败");
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordDTO dto) {
        String email = dto.getEmail();

        // 1. 查询用户是否存在
        User user = userService.lambdaQuery().eq(User::getEmail, email).one();
        if (user == null) {
            throw new BusinessException("该邮箱尚未注册");
        }

        // 2. 【核心拦截】检查角色是否为普通客户 (比较数字)
        // 假设 user.getRole() 返回的是 Integer 类型
        if (!Objects.equals(user.getRole(), UserRoleEnum.CUSTOMER.getCode())) {
            log.warn("非Customer用户试图通过邮箱重置密码。用户: {}, 角色代码: {}", email, user.getRole());
            throw new BusinessException("非普通用户不支持自助重置密码，请联系系统管理员处理");
        }

        // 3. 生成唯一的 UUID 作为 Token
        String token = UUID.randomUUID().toString().replace("-", "");

        // 4. 将 Token 存入 Redis，Key 为 Token，Value 为邮箱。有效期 30 分钟。
        String redisKey = RESET_TOKEN_PREFIX + token;
        redisUtil.set(redisKey, email, 30, TimeUnit.MINUTES);

        // 5. 拼接前端重置界面的完整 URL
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        // 6. 异步发送邮件
        mailService.sendResetLinkEmail(email, resetLink);

        log.info("重置密码邮件已发送至: {}, 角色确认: {}", email, UserRoleEnum.CUSTOMER.getDescription());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordDTO dto) {
        String token = dto.getToken();
        String redisKey = RESET_TOKEN_PREFIX + token;

        // 1. 从 Redis 通过 Token 反查出邮箱（使用 Optional 优雅处理 Null 和 异常抛出）
        String email = Optional.ofNullable(redisUtil.get(redisKey))
                .map(Object::toString)
                .orElseThrow(() -> new BusinessException("重置链接无效或已过期，请重新申请"));

        // 2. 加密新密码
        String encodedPassword = passwordEncoder.encode(dto.getNewPassword());

        // 3. 根据查到的邮箱更新密码
        boolean success = userService.lambdaUpdate()
                .set(User::getPassword, encodedPassword)
                // .set(User::getUpdateTime, LocalDateTime.now()) // 建议：密码变更时更新时间戳
                .eq(User::getEmail, email)
                .update();

        if (!success) {
            throw new BusinessException("密码重置失败，系统异常");
        }

        // 4. 更新成功后，立刻废弃该 Token，防止回放攻击
        redisUtil.delete(redisKey);

        log.info("邮箱 {} 成功通过验证链接重置了密码", email);
    }

    /**
     * 通用验证码校验逻辑
     * @param email 邮箱
     * @param code 用户输入的验证码
     * @param scene 业务场景
     * @param errorMsg 失败时的提示文案
     */
    private void validateCaptcha(String email, String code, CaptchaScene scene, String errorMsg) {
        // 修正点：显式调用 .getCode() 保持与发送时一致
        String captchaKey = CAPTCHA_PREFIX + scene.getCode() + ":" + email;

        Object storedCodeObj = redisUtil.get(captchaKey);
        log.info("从Redis获取Key: {}, 结果: {}", captchaKey, storedCodeObj); // 建议加一行日志排查

        if (storedCodeObj == null || !storedCodeObj.toString().equals(code)) {
            throw new BusinessException(errorMsg);
        }

        redisUtil.delete(captchaKey);
    }

    private void incrementFailCount(String ipKey) {
        // 获取当前失败次数
        Object failTimesObj = redisUtil.get(ipKey);
        int failTimes = failTimesObj == null ? 0 : Integer.parseInt(failTimesObj.toString());

        if (failTimes >= MAX_FAIL_TIMES) {
            return; // 已锁定
        }

        failTimes++;
        if (failTimes >= MAX_FAIL_TIMES) {
            // 达到上限，锁定
            redisUtil.set(ipKey, failTimes, LOCK_EXPIRE_HOURS, TimeUnit.HOURS);
        } else {
            // 未达到上限：如果 key 不存在，则设置过期时间为固定窗口长度；否则只更新值
            if (failTimesObj == null) {
                // 第一次失败，设置固定窗口过期时间
                redisUtil.set(ipKey, failTimes, FAIL_COUNT_EXPIRE_DAYS, TimeUnit.DAYS);
            } else {
                // 已存在，只更新值，不改变过期时间
                redisUtil.increment(ipKey, failTimes);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponseVO login(LoginRequest request, String clientIp) {
        String account = request.getAccount();

        // ================== 🛠️ 新增安全防御：步骤 0. 熔断拦截检查 ==================
        // 只要账号被锁，直接在最前端乱棍打走，不给它任何触碰数据库和事务的机会
        String lockKey = "login:lock:" + account;
        if (redisUtil.hasKey(lockKey)) {
            throw new RuntimeException("账号因密码错误次数过多已被锁定，请15分钟后再试");
        }
        // =========================================================================

        // 1. 根据用户名或者邮箱查询用户
        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, account)
                .or()
                .eq(User::getEmail, account));

        // 2. 校验用户是否存在及密码
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            // ================== 🛠️ 新增安全防御：步骤 2.5 失败计数与熔断触发 ==================
            String failKey = "login:fail:count:" + account;
            // 利用 Redis 的原子自增命令
            Long failCount = redisUtil.increment(failKey, 1);

            // 如果是当天第一次输错，设置计数器的有效期为 1 小时，防止无限累加
            if (failCount == 1) {
                redisUtil.set(failKey, failCount, 1, TimeUnit.HOURS);
            }

            // 核心熔断阈值判定：达到 5 次，直接下发物理熔断锁
            if (failCount >= 5) {
                redisUtil.set(lockKey, "LOCKED", 15, TimeUnit.MINUTES); // 锁定 15 分钟
                redisUtil.delete(failKey); // 触发熔断后，清空失败计数器
                throw new RuntimeException("密码错误次数过多，账号已被锁定 15 分钟！");
            }
            // =========================================================================

            throw new RuntimeException("登录失败，请检查用户名和密码。还剩 " + (5 - failCount) + " 次机会");
        }

        // 3. 校验并更新用户状态
        UserStatus userStatus = userStatusService.getById(user.getId());
        if (userStatus == null) {
            throw new RuntimeException("账号状态异常，请联系管理员");
        }

        // --- 自动解冻检查 ---
        if (!UserStatusEnum.ACTIVE.getCode().equals(userStatus.getStatus())) {
            if (userStatus.getFreezeExpireTime() != null &&
                    userStatus.getFreezeExpireTime().isBefore(LocalDateTime.now())) {
                userStatus.setStatus(UserStatusEnum.ACTIVE.getCode());
                userStatus.setFreezeReason(null);
                userStatus.setFreezeExpireTime(null);
            } else {
                throw new RuntimeException("账号已被冻结，原因码：" + userStatus.getFreezeReason());
            }
        }

        // ================== 🛠️ 新增安全防御：登录成功，清空所有失败痕迹 ==================
        redisUtil.delete("login:fail:count:" + account);
        // =========================================================================

        // 4. 获取枚举信息
        UserRoleEnum roleEnum = UserRoleEnum.getByCode(user.getRole());
        UserStatusEnum statusEnum = UserStatusEnum.getByCode(userStatus.getStatus());

        // 5. 构建用户信息 VO
        UserCenterInfoVO userInfo = new UserCenterInfoVO();
        userInfo.setUserId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRole(roleEnum.getRoleName());
        userInfo.setStatus(statusEnum != null ? statusEnum.getValue() : "正常");

        // 6. 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleEnum.getRoleName());

        // --- 7. 更新最后登录信息 ---
        userStatus.setLastLoginTime(LocalDateTime.now());
        userStatus.setLastLoginIp(clientIp);

        boolean updateSuccess = userStatusService.updateById(userStatus);
        if (!updateSuccess) {
            throw new RuntimeException("系统繁忙，登录记录失败");
        }

        // 判定是否为普通用户
        if (UserRoleEnum.CUSTOMER.getCode().equals(user.getRole())) {
            String[] regionResult = parseIpRegion(clientIp);
            String province = regionResult[0];
            String city = regionResult[1];

            GenericEventDTO<LoginEventDTO> loginEvent = GenericEventDTO.<LoginEventDTO>builder()
                    .eventId(UUID.randomUUID().toString())
                    .type(EventType.LOGIN)
                    .timestamp(System.currentTimeMillis())
                    .userId(user.getId())
                    .ip(clientIp)
                    .data(LoginEventDTO.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .role(roleEnum.getRoleName())
                            .ip(clientIp)
                            .province(province)
                            .city(city)
                            .platform("Web")
                            .build())
                    .build();

            String jsonPayload = JSON.toJSONString(loginEvent);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    kafkaTemplate.send("nxg_business_event", String.valueOf(user.getId()), jsonPayload);
                    log.info("用户 {} 登录事件(地域: {}-{}) 已发送至 Kafka", user.getUsername(), province, city);
                }
            });
        }

        // 8. 返回响应
        LoginResponseVO response = new LoginResponseVO();
        response.setToken(token);
        response.setUserInfo(userInfo);
        return response;
    }

    @Override
    public UserCenterInfoVO getUserProfile(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            io.jsonwebtoken.Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.parseLong(claims.getSubject());

            User user = userService.getById(userId);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }

            UserStatus userStatus = userStatusService.getById(userId);
            if (userStatus == null || userStatus.getStatus() == 0) {
                throw new RuntimeException("用户已被冻结");
            }

            UserCenterInfoVO userInfo = new UserCenterInfoVO();
            userInfo.setUserId(user.getId());
            userInfo.setUsername(user.getUsername());
            userInfo.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
            userInfo.setAvatar(user.getAvatar());

            String roleStr;
            switch (user.getRole()) {
                case 1: roleStr = "ROLE_SALES"; break;
                case 2: roleStr = "ROLE_ADMIN"; break;
                default: roleStr = "ROLE_CUSTOMER";
            }
            userInfo.setRole(roleStr);

            String statusStr;
            switch (userStatus.getStatus()) {
                case 0: statusStr = "FROZEN"; break;
                case 1: statusStr = "ACTIVE"; break;
                default: statusStr = "OTHER_STATUS";
            }
            userInfo.setStatus(statusStr);

            return userInfo;
        } catch (Exception e) {
            log.error("Token解析失败", e);
            throw new RuntimeException("无效的token，请重新登录");
        }
    }

    private String[] parseIpRegion(String ip) {
        String province = "";
        String city = "";

        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            return new String[]{province, city};
        }

        try {
            String regionStr;
            // 判断是否为 IPv6 地址
            if (ip.contains(":")) {
                regionStr = ipV6Searcher.search(ip);
            } else {
                regionStr = ipV4Searcher.search(ip);
            }

            // ip2region 返回格式：国家|区域|省份|城市|ISP  (例如: 中国|0|广东省|深圳市|电信)
            if (regionStr != null) {
                String[] parts = regionStr.split("\\|");
                if (parts.length >= 4) {
                    // 过滤掉 ip2region 默认的占位符 "0"
                    province = "0".equals(parts[2]) ? "" : parts[2];
                    city = "0".equals(parts[3]) ? "" : parts[3];
                }
            }
        } catch (Exception e) {
            log.error("⚡ Ip2region 解析位置异常, IP: {}", ip, e);
        }

        return new String[]{province, city};
    }
}
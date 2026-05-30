package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.api.vo.UserBaseInfoVO;
import com.nxg.mall.common.dto.CustomerQueryDTO;
import com.nxg.mall.common.dto.SalesAddDTO;
import com.nxg.mall.common.dto.SalesUpdateDTO;
import com.nxg.mall.common.entity.User;
import com.nxg.mall.common.entity.UserStatus;
import com.nxg.mall.common.exception.BusinessException;
import com.nxg.mall.api.mapper.UserMapper;
import com.nxg.mall.api.mapper.UserStatusMapper;
import com.nxg.mall.api.service.IUserService;
import com.nxg.mall.common.utils.SystemConstants;
import com.nxg.mall.common.vo.CustomerVO;
import com.nxg.mall.common.vo.ManageVO;
import com.nxg.mall.common.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserStatusMapper userStatusMapper; // 注入状态表的 Mapper

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean existsByUsername(String username) {
        return this.count(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return this.count(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务
    public boolean removeById(Serializable id) {
        // 1. 检查该用户是否存在
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException("用户不存在"); // 自定义异常
        }

        // 2. 检查权限 (例如：禁止删除超级管理员)
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException("禁止删除管理员账号");
        }

        // 3. 执行物理删除或逻辑删除 (取决于你在实体类上的配置)
        return super.removeById(id);
    }

    @Override
    public void saveSales(SalesAddDTO dto) {
        // 1. 唯一性校验
        // 检查邮箱是否存在
        if (this.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())) != null) {
            throw new BusinessException("该邮箱已被注册");
        }
        // 检查用户名是否存在
        if (this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())) != null) {
            throw new BusinessException("用户名已存在");
        }

        // 2. 转换并保存
        User user = new User();
        BeanUtils.copyProperties(dto, user);

        // 强制设置角色和默认状态
        user.setRole(SystemConstants.SALES);
        // 设置余额
        user.setBalance(new BigDecimal("0.00"));
        // 密码加密
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        this.save(user);

        // 3. 初始化用户状态信息到 t_user_status
        UserStatus userStatus = new UserStatus();
        userStatus.setUserId(user.getId()); // 获取刚刚生成的 User ID
        userStatus.setStatus(SystemConstants.Normal);            // 1-正常

        // 如果你的数据库字段没有设置默认值，建议在这里显式初始化
        userStatus.setLastLoginTime(null);

        userStatusMapper.insert(userStatus);
    }

    @Override
    public void updateSales(Long id, SalesUpdateDTO dto) {
        // 1. 存在性校验
        User user = this.getById(id);
        if (user == null) throw new BusinessException("用户不存在");

        // 2. 更新允许修改的字段
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());

        // 注意：通常不建议在编辑资料时直接修改 username，如果允许修改，需重新校验唯一性
        this.updateById(user);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        // 1. 存在性校验
        User user = this.getById(id);
        if (user == null) throw new BusinessException("用户不存在");

        // 使用共用的加密逻辑
        user.setPassword(passwordEncoder.encode(newPassword));
        this.updateById(user);
    }

    @Override
    public Page<CustomerVO> getCustomerPage(CustomerQueryDTO queryDTO) {
        // 构造 MyBatis-Plus 的 Page 对象
        Page<CustomerVO> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());

        // 调用自定义的 Mapper 方法，将 page 和 查询条件 传进去
        return userMapper.selectCustomerPage(page, queryDTO);
    }

    @Override
    public IPage<ManageVO> getManagePage(Integer page, Integer size, String name, Integer role) {
        Page<ManageVO> pageParam = new Page<>(page, size);
        return userMapper.selectManagePage(pageParam, name, role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetCustomerPassword(Long id) {
        // 1. 查询用户是否存在且为顾客角色 (假设 role=0 为顾客)
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, id)
                .eq(User::getRole, 0));

        if (user == null) {
            throw new BusinessException("用户不存在或角色错误");
        }

        // 2. 加密默认密码 (123456)
        String encodedPassword = passwordEncoder.encode("123456");

        // 3. 执行更新
        User updateEntity = new User();
        updateEntity.setId(id);
        updateEntity.setPassword(encodedPassword);

        return this.updateById(updateEntity);
    }

    @Override
    public UserVO getUserVOById(Long id) {
        User user = this.getById(id);
        if (user == null) return null;

        UserVO vo = new UserVO();
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    @Override
    public UserBaseInfoVO getCustomerBaseInfo(Long userId) {
        // 1. 基础业务校验
        if (userId == null || userId <= 0) {
            log.warn("查询用户基本信息失败：传入的 userId 异常 [{}]", userId);
            return null;
        }

        // 2. 调用自定义的多表联查 Mapper 方法
        UserBaseInfoVO baseInfoVO = baseMapper.selectCustomerBaseInfo(userId);

        // 3. 业务兜底处理（如果用户刚注册，基本信息存在但轨迹表全为空，进行日志审计或默认值填充）
        if (baseInfoVO == null) {
            log.info("未找到 ID 为 {} 的顾客信息", userId);
            return null;
        }

        if (baseInfoVO.getLastLoginTime() == null) {
            log.debug("该顾客 [ID: {}] 属于新注册用户，暂无登录轨迹数据", userId);
        }

        return baseInfoVO;
    }
}
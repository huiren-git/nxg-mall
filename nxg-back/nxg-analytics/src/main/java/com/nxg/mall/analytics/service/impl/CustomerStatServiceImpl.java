package com.nxg.mall.analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.analytics.dto.UserPersonaDTO;
import com.nxg.mall.analytics.entity.CustomerStat;
import com.nxg.mall.analytics.mapper.CustomerStatDailyMapper;
import com.nxg.mall.analytics.service.CategorySyncService;
import com.nxg.mall.analytics.vo.TrendDataVO;
import com.nxg.mall.analytics.vo.UserPersonaVO;
import com.nxg.mall.analytics.vo.UserSummaryVO;
import com.nxg.mall.common.entity.log.LogMessage;
import com.nxg.mall.analytics.entity.UserPersona;
import com.nxg.mall.analytics.mapper.CustomerStatMapper;
import com.nxg.mall.analytics.mapper.UserPersonaMapper;
import com.nxg.mall.analytics.model.dto.CustomerQueryDTO;
import com.nxg.mall.analytics.service.ICustomerStatService;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CustomerStatServiceImpl extends ServiceImpl<CustomerStatMapper, CustomerStat> implements ICustomerStatService {

    @Autowired
    private UserPersonaMapper userPersonaMapper;

    @Autowired
    @Qualifier("ipV4Searcher")
    private Searcher v4Searcher;

    @Autowired
    @Qualifier("ipV6Searcher")
    private Searcher v6Searcher;

    @Autowired
    private CategorySyncService categorySyncService;

    @Autowired
    private CustomerStatDailyMapper customerStatDailyMapper;

    @Override
    public IPage<CustomerStat> getCustomerPage(CustomerQueryDTO query) {
        Page<CustomerStat> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<CustomerStat> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getNickname())) {
            wrapper.like(CustomerStat::getNickname, query.getNickname());
        }

        // 动态排序逻辑
        if (StringUtils.hasText(query.getSortField())) {
            boolean isAsc = "ascend".equals(query.getSortOrder());
            if ("balance".equals(query.getSortField())) {
                wrapper.orderBy(true, isAsc, CustomerStat::getBalance);
            } else if ("orderCount".equals(query.getSortField())) {
                wrapper.orderBy(true, isAsc, CustomerStat::getOrderCount);
            } else if ("totalSpend".equals(query.getSortField())) {
                wrapper.orderBy(true, isAsc, CustomerStat::getTotalSpend);
            }
        } else {
            wrapper.orderByDesc(CustomerStat::getTotalSpend);
        }

        return this.page(page, wrapper);
    }

    @Override
    @Transactional
    public void processUserLog(LogMessage logMessage) {
        Long userId = logMessage.getUserId();
        UserPersona persona = userPersonaMapper.selectById(userId);
        if (persona == null) {
            persona = new UserPersona();
            persona.setUserId(userId);
            persona.setPurchasingPower("MEDIUM"); // 初始默认值
        }

        // 1. 处理 IP -> 地域 (Region & Area)
        try {
            // search 结果格式: 国家|区域|省份|城市|网络运营商
            String ip = logMessage.getClientIp();
            String regionInfo = "未知|0|0|0|0";

            // 根据 IP 格式选择搜索器
            if (ip.contains(":")) {
                // IPv6 格式通常包含冒号
                regionInfo = v6Searcher.search(ip);
            } else {
                // 否则视为 IPv4
                regionInfo = v4Searcher.search(ip);
            }

            String[] parts = regionInfo.split("\\|");

            // 直接存入解析出的中文名
            String province = parts[2]; // 例如: 广东省
            String city = parts[3];     // 例如: 广州市

            // 过滤掉 '0' (ip2region 对于未知字段会返回 0)
            persona.setRegion("0".equals(province) ? "未知省份" : province);
            persona.setLastLoginArea("0".equals(city) ? "未知城市" : city);
        } catch (Exception e) {
            log.warn("IP 解析失败: {}", logMessage.getClientIp());
        }

        // 2. 处理标签 (Tags/Preferences - 存储 ID 集合)
        String categoryIdStr = String.valueOf(logMessage.getCategoryId());
        String currentTags = persona.getTags(); // 假设存的是 "1,2,3"

        if (!StringUtils.hasText(currentTags)) {
            persona.setTags(categoryIdStr);
        } else {
            // 检查 ID 是否已存在，不存在则追加
            List<String> tagList = new ArrayList<>(Arrays.asList(currentTags.split(",")));
            if (!tagList.contains(categoryIdStr)) {
                tagList.add(categoryIdStr);
                persona.setTags(String.join(",", tagList));
            }
        }
        userPersonaMapper.insertOrUpdate(persona);
    }

    // 定期任务：统计订单金额更新购买力
    @Override
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    public void calculatePurchasingPower() {
        // 伪代码：从订单聚合表中读取 SUM(total_amount) 并 update t_user_persona
        // UPDATE t_user_persona SET purchasing_power = 'HIGH' WHERE ...
    }

    @Override
    public UserPersonaVO getPersonaByUserId(Long userId) {
        UserPersona persona = userPersonaMapper.selectById(userId);
        UserPersonaVO vo = new UserPersonaVO();
        BeanUtils.copyProperties(persona, vo);

        if (StringUtils.hasText(persona.getTags())) {
            List<String> names = Arrays.stream(persona.getTags().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(idStr -> {
                        try {
                            // 安全转化为 Long
                            Long categoryId = Long.parseLong(idStr);
                            // 1. 优先从 Redis 缓存拿 2. 缓存没有则查数据库并补入缓存
                            String name = categorySyncService.getNameById(categoryId);
                            return name != null ? name : "未知标签";
                        } catch (NumberFormatException e) {
                            // 如果不小心混入了非数字，兜底处理，不让整个接口挂掉
                            return idStr;
                        }
                    })
                    .collect(Collectors.toList());
            vo.setPreferences(names);
        }
        return vo;
    }

    @Override
    public UserSummaryVO getSummary(LocalDate start, LocalDate end) {
        // 聚合指定日期范围内的各项总和
        return customerStatDailyMapper.selectSummary(start, end);
    }

    @Override
    public TrendDataVO getTrend(String type, LocalDate start, LocalDate end) {
        // 1. 生成日期序列（防止某天没数据导致图表断裂）
        List<String> days = getDayRange(start, end);

        // 2. 从数据库查询
        List<Map<String, Object>> rawData = customerStatDailyMapper.selectTrend(type, start, end);

        // 3. 数据影视（将数据库查出的 list 转为与 days 对应的 list）
        Map<String, Long> dataMap = rawData.stream().collect(
                Collectors.toMap(m -> m.get("day").toString(), m -> (Long)m.get("val"))
        );

        List<Long> values = days.stream()
                .map(d -> dataMap.getOrDefault(d, 0L))
                .collect(Collectors.toList());

        return TrendDataVO.builder().days(days).data(values).type(type).build();
    }

    private List<String> getDayRange(LocalDate start, LocalDate end) {
        return start.datesUntil(end.plusDays(1))
                .map(d -> d.format(DateTimeFormatter.ofPattern("MM-dd")))
                .collect(Collectors.toList());
    }
}

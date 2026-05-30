package com.nxg.mall.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.analytics.entity.CustomerStatDaily;
import com.nxg.mall.analytics.vo.UserSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface CustomerStatDailyMapper extends BaseMapper<CustomerStatDaily> {

    // 修改之前的 SQL 语句中的表名
    @Select("SELECT SUM(uv_count) as visitors, SUM(pv_count) as pageViews, " +
            "SUM(new_user_count) as newUsers, SUM(trade_user_count) as tradeUsers, " +
            "SUM(recharge_user_count) as rechargeUsers " +
            "FROM t_customer_stat_daily " +
            "WHERE stat_date BETWEEN #{start} AND #{end}")
    UserSummaryVO selectSummary(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<Map<String, Object>> selectTrend(@Param("type") String type,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end);
}

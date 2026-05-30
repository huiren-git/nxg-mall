package com.nxg.mall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nxg.mall.api.vo.UserBaseInfoVO;
import com.nxg.mall.common.dto.CustomerQueryDTO;
import com.nxg.mall.common.entity.User;
import com.nxg.mall.common.vo.CustomerVO;
import com.nxg.mall.common.vo.ManageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    /**
     * 扣减用户余额（原子操作）
     * @param userId 用户ID
     * @param amount 扣减金额
     * @return 更新受影响的行数。如果余额不足导致扣减失败，返回 0
     */
    @Update("UPDATE t_user SET balance = balance - #{amount} " +
            "WHERE id = #{userId} AND balance >= #{amount}")
    int reduceBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * TODO 获取admin后台的customer列表
     * @param page
     * @param query
     * @return
     */
    Page<CustomerVO> selectCustomerPage(@Param("page") Page<CustomerVO> page, @Param("query") CustomerQueryDTO query);

    /**
     * 充值用户余额
     * @param userId
     * @param amount
     * @return
     */
    @Update("UPDATE t_user SET balance = balance + #{amount} WHERE id = #{userId}")
    boolean updateBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    // 自定义分页查询
    IPage<ManageVO> selectManagePage(Page<ManageVO> page,
                                     @Param("name") String name,
                                     @Param("role") Integer role);

    @Select("SELECT " +
            "  u.id AS userId, " +
            "  u.username, " +
            "  u.nickname, " +
            "  u.avatar, " +
            "  u.email, " +
            "  u.balance, " +
            "  u.create_time AS registerTime, " +
            "  s.last_login_time AS lastLoginTime, " +
            "  s.last_login_ip AS lastLoginIp " +
            "FROM t_user u " +
            "LEFT JOIN t_user_status s ON u.id = s.user_id " +
            "WHERE u.id = #{userId}")
    UserBaseInfoVO selectCustomerBaseInfo(@Param("userId") Long userId);
}
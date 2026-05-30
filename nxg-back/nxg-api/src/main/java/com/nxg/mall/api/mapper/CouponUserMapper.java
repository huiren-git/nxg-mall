package com.nxg.mall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.common.entity.CouponUser;
import com.nxg.mall.common.vo.CouponUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface CouponUserMapper extends BaseMapper<CouponUser> {
    // 继承 BaseMapper 即可满足基础查询与插入
    /**
     * 关联查询用户优惠券详情
     */
    List<CouponUserVO> selectUserCouponList(@Param("userId") Long userId,
                                            @Param("status") Integer status);
}

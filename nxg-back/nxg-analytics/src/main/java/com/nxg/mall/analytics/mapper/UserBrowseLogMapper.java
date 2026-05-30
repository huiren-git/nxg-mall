package com.nxg.mall.analytics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nxg.mall.common.entity.log.UserBrowseLog;
import com.nxg.mall.common.vo.BrowseLogSalesVO;
import com.nxg.mall.common.vo.BrowseLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

@Mapper
public interface UserBrowseLogMapper extends BaseMapper<UserBrowseLog> {
    /**
     * 分页查询用户浏览记录（关联商品信息）
     * @param page 分页参数
     * @param userId 用户ID
     * @return 分页后的VO结果集
     */

    /**
     * 分页查询用户浏览记录（关联商品信息）
     * @param page 明确指定泛型为 BrowseLogVO
     */
    IPage<BrowseLogSalesVO> selectBrowseLogPage(Page<BrowseLogSalesVO> page, @Param("userId") Long userId);

    // 查询给 C 端用户中心使用
    IPage<BrowseLogVO> selectUserBrowseLogPage(Page<BrowseLogVO> page, @Param("userId") Long userId);
}

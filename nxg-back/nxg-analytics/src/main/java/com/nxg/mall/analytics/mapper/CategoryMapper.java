package com.nxg.mall.analytics.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.common.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 从业务库 (nxg) 读取所有分类
     * 注意：这里的 @DS("nxg") 必须和你 application.yml 中配置的名称一致
     */
    @DS("biz")
    @Select("SELECT id, name FROM t_category")
    List<Category> selectFromApi();

    /**
     * 写入统计库 (analytics)
     * 虽然类上可能有默认 DS，但建议显式指定确保安全
     */
    @DS("analytics")
    default void insertOrUpdateBatch(List<Category> list) {
        // 这里可以使用 MyBatis-Plus 的内置方法，或者自定义 REPLACE INTO SQL
    }
}

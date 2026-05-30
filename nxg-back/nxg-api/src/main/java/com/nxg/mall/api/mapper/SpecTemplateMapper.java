package com.nxg.mall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.common.entity.SpecTemplate;
import com.nxg.mall.common.vo.SpecTemplateVO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface SpecTemplateMapper extends BaseMapper<SpecTemplate> {
    /**
     * 使用 XML 级联查询获取复合模板列表
     */
    List<SpecTemplateVO> selectComplexList(@Param("name") String name);
}

package com.nxg.mall.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nxg.mall.common.entity.AttributeKey;
import com.nxg.mall.common.vo.ProductSpecVO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface AttributeKeyMapper extends BaseMapper<AttributeKey> {

    List<ProductSpecVO.SpecKeyVO> selectSpecMetadataByProductId(Long productId);

    // 批量插入并返回主键 ID
    int insertBatch(@Param("list") List<AttributeKey> list);

    /**
     * 这里的逻辑对应你之前在 XML 中写的 collection 嵌套查询
     */
    List<ProductSpecVO.SpecKeyVO> selectSpecMetadataByCategoryId(@Param("categoryId") Long categoryId);
}

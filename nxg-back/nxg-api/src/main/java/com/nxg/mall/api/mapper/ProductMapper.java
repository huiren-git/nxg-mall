package com.nxg.mall.api.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nxg.mall.common.dto.ProductManageQueryDTO;
import com.nxg.mall.common.entity.Product;
import com.nxg.mall.common.vo.ProductListVO;
import com.nxg.mall.common.vo.ProductManageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
@DS("biz")
public interface ProductMapper extends BaseMapper<Product> {
    /**
     * 自定义分页查询已删除的商品
     * @param page 分页参数
     * @param query 查询条件（关键字等）
     */
    IPage<ProductManageVO> selectDeletedPage(IPage<ProductManageVO> page, @Param("query") ProductManageQueryDTO query);

    /**
     * 查询sales/admin后台的商品列表
     * @param page
     * @param query
     * @return
     */
    IPage<ProductManageVO> selectProductManagePage(IPage<ProductManageVO> page, @Param("query") ProductManageQueryDTO query);

    /**
     * 分页查询适用商品的 VO 集合
     */
    Page<ProductListVO> selectProductVOByCouponScope(
            Page<ProductListVO> page,
            @Param("couponId") Long couponId,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder
    );

    /**
     * 手动将逻辑删除字段置为 0 (未删除状态)
     */
    @Update("UPDATE t_product SET is_deleted = 0 WHERE id = #{id} AND is_deleted = 1")
    int restoreById(@Param("id") Long id);

    /**
     * 根据订单购买历史，推荐共同购买的商品
     * @param productId 当前商品ID
     * @param limit 获取数量
     */
    List<ProductListVO> selectRelatedByOrderHistory(@Param("productId") Long productId, @Param("limit") int limit);
}

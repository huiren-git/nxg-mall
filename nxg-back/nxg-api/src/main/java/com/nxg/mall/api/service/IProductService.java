package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.api.vo.ProductDetailManageVO;
import com.nxg.mall.common.dto.*;
import com.nxg.mall.common.vo.*;
import com.nxg.mall.common.entity.Product;
import java.util.List;

public interface IProductService extends IService<Product> {

    /**
     * 创建商品及其SKU
     */
    void createProduct(ProductCreateDTO createDTO, Long operatorId);

    /**
     * 获取分类商品列表页的聚合数据（包含面包屑和分页商品）
     */
    CategoryPageVO getCategoryPageData(Long categoryId, Integer pageNum, Integer pageSize);

    /**
     * 原有的分页查询商品方法（保持原子性，供内部调用）
     */
    IPage<Product> getProductsByCategoryId(Long categoryId, Page<Product> page);

    /**
     * 获取指定优惠券适用商品的列表
     * @param couponId
     * @param queryDTO
     * @return
     */
    Page<ProductListVO> getProductsByCouponScope(Long couponId, ProductQueryDTO queryDTO);

    /**
     * 根据ID获取商品详情（包含完整的路径处理）
     */
    ProductDetailVO getProductDetail(Long id);

    ProductDetailManageVO getProductDetailManage(Long id);

    IPage<ProductListVO> getProductPage(ProductCustomerQueryDTO queryDTO, Long userId);

    IPage<ProductManageVO> getManageProductPage(ProductManageQueryDTO queryDTO);

    void updateShelfStatus(Long id, Boolean isOnShelf);

    void restoreProduct(Long id);

    void updateProductInfo(Long id, ProductUpdateDTO dto);

    /**
     * 获取商品关联推荐列表
     */
    List<ProductListVO> getRelatedProducts(Long productId);

    /**
     * 获取首页商品推荐列表
     */
    List<ProductListVO> getHomeRecommendProducts(Long userId);

}

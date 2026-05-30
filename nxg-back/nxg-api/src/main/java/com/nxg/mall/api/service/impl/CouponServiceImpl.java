package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.common.dto.*;
import com.nxg.mall.common.entity.Category;
import com.nxg.mall.common.entity.Coupon;
import com.nxg.mall.common.entity.CouponScope;
import com.nxg.mall.common.entity.Product;
import com.nxg.mall.common.exception.BusinessException;
import com.nxg.mall.api.mapper.CategoryMapper;
import com.nxg.mall.api.mapper.CouponMapper;
import com.nxg.mall.api.mapper.ProductMapper;
import com.nxg.mall.api.service.ICouponScopeService;
import com.nxg.mall.api.service.ICouponService;
import com.nxg.mall.api.service.bo.UserCouponDetailBO;
import com.nxg.mall.common.vo.CouponAvailableVO;
import com.nxg.mall.common.vo.CouponManageVO;
import com.nxg.mall.common.vo.CouponVO;
import com.nxg.mall.common.vo.ProductListVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {

    @Autowired
    private ICouponScopeService couponScopeService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public IPage<CouponVO> getCouponCenterPage(Long userId, CouponQueryDTO queryDTO) {
        // 2. 构建分页对象
        Page<Coupon> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 3. 执行关联查询
        return baseMapper.selectCouponCenterPage(page, userId, queryDTO.getCategory());
    }

    @Override
    public IPage<ProductListVO> getProductsByCouponId(Long couponId, Integer current,
                                                         Integer size, String sortField, String sortOrder) {
        // 1. 获取优惠券详情
        Coupon coupon = this.getById(couponId);
        if (coupon == null || coupon.getIsDeleted() == 1) {
            throw new BusinessException("优惠券不存在或已失效");
        }

        // 2. 准备分页和 LambdaQueryWrapper
        Page<Product> page = new Page<>(current, size);
        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();

        // 【C端过滤条件】
        productWrapper.eq(Product::getStatus, 1)
                .eq(Product::getIsDeleted, 0);

        // 3. 根据优惠券类型匹配商品范围
        if (coupon.getCategory() != 1) {
            List<Long> targetIds = couponScopeService.list(
                    new LambdaQueryWrapper<CouponScope>().eq(CouponScope::getCouponId, couponId)
            ).stream().map(CouponScope::getTargetId).collect(Collectors.toList());

            if (CollectionUtils.isEmpty(targetIds)) {
                return new Page<>();
            }

            if (coupon.getCategory() == 2) {
                // 【优化：合并查询】避免循环查库
                LambdaQueryWrapper<Category> categoryWrapper = new LambdaQueryWrapper<>();
                categoryWrapper.select(Category::getId); // 只查ID，提高效率

                // 使用 AND ( (id = x OR path LIKE 'x/%') OR (id = y OR path LIKE 'y/%') )
                categoryWrapper.and(wrapper -> {
                    for (int i = 0; i < targetIds.size(); i++) {
                        Long id = targetIds.get(i);
                        // 匹配逻辑：本身 ID 或者 路径以 "ID/" 开头（即子类目）
                        wrapper.eq(Category::getId, id)
                                .or().likeRight(Category::getPath, id + "/")
                                .or().like(Category::getPath, "/" + id + "/");

                        if (i < targetIds.size() - 1) wrapper.or();
                    }
                });

                List<Long> allCategoryIds = categoryMapper.selectList(categoryWrapper)
                        .stream().map(Category::getId).collect(Collectors.toList());

                productWrapper.in(Product::getCategoryId, allCategoryIds);

            } else if (coupon.getCategory() == 3) {
                // 单品券
                productWrapper.in(Product::getId, targetIds);
            }
        }

        // 4. 处理排序 (使用 Lambda 映射)
        if ("asc".equalsIgnoreCase(sortOrder)) {
            productWrapper.orderByAsc(getSortColumn(sortField));
        } else {
            productWrapper.orderByDesc(getSortColumn(sortField));
        }

        // 5. 执行查询并转换
        IPage<Product> productPage = productMapper.selectPage(page, productWrapper);

        return productPage.convert(product -> {
            ProductListVO vo = new ProductListVO();
            BeanUtils.copyProperties(product, vo);
            return vo;
        });
    }

    /**
     * 辅助方法：将字符串映射为 Lambda 方法引用，彻底消除硬编码字符串
     */
    private SFunction<Product, ?> getSortColumn(String sortField) {
        switch (sortField) {
            case "sales": return Product::getSalesCount;
            case "price": return Product::getPrice;
            case "time":  return Product::getCreateTime;
            default:      return Product::getSortOrder;
        }
    }

    @Override
    public List<CouponAvailableVO> findOrderAvailableCoupons(Long userId, OrderCouponCheckDTO checkDTO) {
        // 1. 获取该用户所有【未使用】且【在有效期内】的优惠券及范围
        // 建议在 Mapper 中通过 JOIN 一次性查出 coupon 和 scope 列表

        System.out.println("Checking for user: " +  userId);

        List<UserCouponDetailBO> myCoupons = baseMapper.selectUserAvailableCoupons(userId);

        System.out.println("Checking couponssssssssssssss for user: " +  myCoupons);

        return myCoupons.stream().map(coupon -> {
            CouponAvailableVO vo = new CouponAvailableVO();
            BeanUtils.copyProperties(coupon, vo);

            System.out.println("Checking coupon for user: " +  coupon);

            // 2. 筛选出当前订单中，符合该优惠券适用范围的商品
            BigDecimal matchAmount = calculateMatchAmount(coupon, checkDTO.getItems());

            // 3. 逻辑判断
            BigDecimal condition = coupon.getConditionAmount();
            if (matchAmount.compareTo(condition) >= 0) {
                vo.setIsUsable(true);
                vo.setReason("");
            } else {
                vo.setIsUsable(false);
                BigDecimal diff = condition.subtract(matchAmount);
                vo.setReason("还差 ¥" + diff.setScale(2, RoundingMode.HALF_UP) + " 可用");
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 计算符合券范围的商品总金额
     */
    private BigDecimal calculateMatchAmount(UserCouponDetailBO coupon, List<OrderItemDTO> items) {
        // 1. 基础非空校验：如果没有商品，金额自然是 0
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }



        // 2. 获取范围类型：使用 Integer.valueOf(1).equals 避开 NPE
        Integer couponCategory = coupon.getCategory();
        // 严谨判空：如果 category 丢失，保守处理返回 0 或按全场处理
        if (couponCategory == null) return BigDecimal.ZERO;

        // 3. 获取目标 ID 列表：确保它至少是个空数组，而不是 null
        List<Long> targetIds = coupon.getScopeTargetIds() != null ?
                coupon.getScopeTargetIds() : new ArrayList<>();

        return items.stream()
                .filter(item -> {
                    // category: 1-全场通用, 2-指定分类, 3-指定商品
                    if (couponCategory == 1) {
                        return true;
                    }

                    // 对于非全场券，必须有 targetIds 才有意义
                    if (targetIds.isEmpty()) {
                        return false;
                    }

                    if (couponCategory == 2) {
                        return item.getCategoryId() != null && targetIds.contains(item.getCategoryId());
                    }
                    if (couponCategory == 3) {
                        return item.getProductId() != null && targetIds.contains(item.getProductId());
                    }
                    return false;
                })
                .map(item -> {
                    // 确保单价和数量不为 null，防止计算时崩溃
                    BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                    int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                    return price.multiply(new BigDecimal(qty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务，异常自动回滚
    public void saveCouponWithScope(CouponSaveDTO saveDTO) {
        // 1. 转换并保存优惠券主表
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(saveDTO, coupon);
        coupon.setTakeCount(0); // 初始化领取数量
        coupon.setStatus(1);    // 默认启用
        this.save(coupon);      // 保存后 MyBatis-Plus 会自动将自增ID回填到 coupon 对象

        // 2. 处理适用范围 (如果不是全场通用)
        if (saveDTO.getCategory() != 1 && !CollectionUtils.isEmpty(saveDTO.getRangeIds())) {
            List<CouponScope> scopeList = saveDTO.getRangeIds().stream().map(targetId -> {
                CouponScope scope = new CouponScope();
                scope.setCouponId(coupon.getId()); // 使用主表回填的ID
                scope.setTargetId(targetId);
                return scope;
            }).collect(Collectors.toList());

            // 批量插入关联表
            couponScopeService.saveBatch(scopeList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCoupon(CouponUpdateDTO updateDTO) {
        // 1. 更新主表基本信息
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(updateDTO, coupon);
        // updateById 是 IService 提供的单条更新
        if (!this.updateById(coupon)) {
            throw new BusinessException("更新优惠券失败，记录不存在");
        }

        // 2. 更新范围表（先删后增）
        // 物理删除旧关联
        couponScopeService.remove(new LambdaQueryWrapper<CouponScope>()
                .eq(CouponScope::getCouponId, updateDTO.getId()));

        // 如果是全场券（Category=1），则不需要插入范围表
        if (updateDTO.getCategory() != 1 && !CollectionUtils.isEmpty(updateDTO.getRangeIds())) {
            List<CouponScope> scopeList = updateDTO.getRangeIds().stream().map(targetId -> {
                CouponScope scope = new CouponScope();
                scope.setCouponId(updateDTO.getId());
                scope.setTargetId(targetId);
                return scope;
            }).collect(Collectors.toList());

            // --- 使用 IService 提供的批量插入方法 ---
            couponScopeService.saveBatch(scopeList);
        }
    }

    @Override
    public Page<CouponManageVO> getCouponManagePage(CouponManageQueryDTO queryDTO) {
        // 1. 构建 MyBatis-Plus 分页对象
        Page<Coupon> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        // 2. 判断是否是查询回收站 (isDeleted == 1)
        if (queryDTO.getIsDeleted() != null && queryDTO.getIsDeleted() == 1) {
            // 调用你自定义的 Mapper 方法（由于你要在 Service 统一转 VO，建议把 Mapper 返回值改回 Page<Coupon>，详见下方说明）
            Page<Coupon> deletedCouponPage = baseMapper.selectDeletedPage(page, queryDTO);

            // 转换并封装为 VO
            return (Page<CouponManageVO>) deletedCouponPage.convert(coupon -> {
                CouponManageVO vo = new CouponManageVO();
                BeanUtils.copyProperties(coupon, vo);
                return vo;
            });
        }

        // 3. 常规优惠券列表查询 (isDeleted == 0)
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(queryDTO.getTitle()), Coupon::getTitle, queryDTO.getTitle())
                .eq(queryDTO.getCategory() != null, Coupon::getCategory, queryDTO.getCategory())
                // 建议显式加上状态过滤，因为常规列表通常要看具体状态
                .eq(queryDTO.getStatus() != null, Coupon::getStatus, queryDTO.getStatus())
                .orderByDesc(Coupon::getCreateTime);

        // 执行常规查询
        Page<Coupon> couponPage = this.page(page, wrapper);

        // 4. 转换并封装常规查询的 VO
        return (Page<CouponManageVO>) couponPage.convert(coupon -> {
            CouponManageVO vo = new CouponManageVO();
            BeanUtils.copyProperties(coupon, vo);
            return vo;
        });
    }

    @Override
    public boolean toggleStatus(Long id, Integer status) {
        Coupon coupon = new Coupon();
        coupon.setId(id);
        coupon.setStatus(status);
        return this.updateById(coupon);
    }

    @Override
    public boolean restoreCoupon(Long id) {
        return this.baseMapper.restoreCouponById(id) > 0;
    }
}
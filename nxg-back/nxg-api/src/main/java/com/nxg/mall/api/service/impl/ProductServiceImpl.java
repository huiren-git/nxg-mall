package com.nxg.mall.api.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.api.vo.ProductDetailManageVO;
import com.nxg.mall.common.dto.payload.TrafficLogMessageDTO;
import com.nxg.mall.common.enums.EventType;
import com.nxg.mall.common.dto.*;
import com.nxg.mall.common.entity.Sku;
import com.nxg.mall.common.exception.BusinessException;
import com.nxg.mall.api.service.ISkuService;
import com.nxg.mall.api.service.UserBrowseLogService;
import com.nxg.mall.common.utils.ImageSecurityUtils;
import com.nxg.mall.common.vo.*;
import com.nxg.mall.common.entity.Category;
import com.nxg.mall.common.entity.Product;
import com.nxg.mall.api.mapper.ProductMapper;
import com.nxg.mall.api.service.ICategoryService;
import com.nxg.mall.api.service.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements IProductService {

    @Value("${file.domain}")
    private String domain;

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private ISkuService skuService; // 注入 SKU 业务层

    @Autowired
    private UserBrowseLogService userBrowseLogService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProduct(ProductCreateDTO createDTO, Long operatorId) {
        // 1. 插入商品主表 (SPU)
        Product product = new Product();
        BeanUtils.copyProperties(createDTO, product);
        product.setOperatorId(operatorId);
        product.setSalesCount(0);

        // 计算总库存
        int totalStock = createDTO.getSkuList().stream().mapToInt(SkuDTO::getStock).sum();
        product.setStock(totalStock);

        productMapper.insert(product);
        Long productId = product.getId();

        // 2. 调用 SkuService 进行批量保存
        // 我们将 DTO 转换逻辑或简单的参数传递给 SkuService
        skuService.saveSkuList(productId, createDTO.getSkuList(), operatorId);

        log.info("商品创建成功，SPU ID: {}, SKU 数量: {}", productId, createDTO.getSkuList().size());
    }

    @Override
    public CategoryPageVO getCategoryPageData(Long categoryId, Integer pageNum, Integer pageSize) {
        CategoryPageVO dto = new CategoryPageVO();

        // 1. 获取面包屑导航 (调用 CategoryService 的原子方法)
        dto.setBreadcrumbs(categoryService.getCategoryPath(categoryId));

        // 2. 分页查询商品数据
        Page<Product> pageParam = new Page<>(pageNum, pageSize);
        IPage<Product> productPage = this.getProductsByCategoryId(categoryId, pageParam);
        dto.setProductPage(productPage);

        return dto;
    }

    @Override
    public IPage<Product> getProductsByCategoryId(Long categoryId, Page<Product> page) {
        // 1. 获取当前分类及其路径信息
        Category current = categoryService.getById(categoryId);
        if (current == null) return page;

        // 2. 利用 path 字段进行前缀匹配 (包含当前类及其所有子孙类)
        String pathPrefix = current.getPath() + "/";

        // 找到所有相关的分类 ID 集合
        List<Long> allCategoryIds = categoryService.list(new LambdaQueryWrapper<Category>()
                        .select(Category::getId)
                        .apply("path LIKE {0} OR id = {1}", pathPrefix + "%", categoryId))
                .stream()
                .map(Category::getId)
                .collect(Collectors.toList());

        // 3. 执行商品分页查询
        return this.page(page, new LambdaQueryWrapper<Product>()
                .in(Product::getCategoryId, allCategoryIds)
                .eq(Product::getStatus, 1) // 仅查询上架商品
                .orderByDesc(Product::getCreateTime));
    }

    @Override
    public Page<ProductListVO> getProductsByCouponScope(Long couponId, ProductQueryDTO queryDTO) {
        // 1. 创建 MyBatis-Plus 分页对象，声明范型为 ProductListVO
        Page<ProductListVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 直接由 Mapper 返回 VO 分页数据，效率更高
        return productMapper.selectProductVOByCouponScope(page, couponId,
                queryDTO.getSortField(),
                queryDTO.getSortOrder());
    }

    @Override
    public ProductDetailVO getProductDetail(Long id) {
        String cacheKey = "product:detail:" + id;

        // 1. 优先从 Redis 获取
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null) {
            String cachedStr = cachedObj.toString();
            // 【防缓存穿透】：如果是特殊标识 "null"，直接抛出异常（避免穿透到数据库）
            if ("null".equals(cachedStr)) {
                throw new BusinessException("该商品不存在或已下架");
            }
            // 命中缓存：直接反序列化返回
            return JSON.parseObject(cachedStr, ProductDetailVO.class);
        }

        // 2. Redis 无数据，查询数据库商品基础信息 (SPU)
        Product product = this.getById(id);
        if (product == null || product.getIsDeleted()) {
            // 【防缓存穿透】：数据库查不到商品，存入 "null" 标识，过期时间设置为短暂的 2 分钟
            redisTemplate.opsForValue().set(cacheKey, "null", 2, java.util.concurrent.TimeUnit.MINUTES);
            throw new BusinessException("该商品不存在或已下架");
        }
        
        Category category = categoryService.getById(product.getCategoryId());

        // 查询该商品下的所有 SKU
        List<Sku> skus = skuService.lambdaQuery()
                .eq(Sku::getProductId, id)
                .list();

        // 转换基础信息到 VO
        ProductDetailVO detailVO = new ProductDetailVO();
        BeanUtils.copyProperties(product, detailVO);

        if (category != null && StringUtils.hasText(category.getPath())) {
            // 将 "/1/5/22" 拆分为数组，过滤空字符串
            List<Long> pathIds = Arrays.stream(category.getPath().split("/"))
                    .filter(StringUtils::hasText)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            detailVO.setCategoryPath(pathIds);
        }

        // 处理 SKU 列表转换
        List<SkuVO> skuVOs = skus.stream().map(sku -> {
            SkuVO skuVO = new SkuVO();
            BeanUtils.copyProperties(sku, skuVO);
            return skuVO;
        }).collect(Collectors.toList());

        detailVO.setSkuList(skuVOs);

        // 3. 将组装好的结果序列化存入 Redis
        // 【防缓存雪崩】：基础时间 (1小时 = 60分钟) + 随机时间 (10~30分钟)
        long baseMinutes = 60;
        long randomMinutes = 10 + new java.util.Random().nextInt(21); // 生成 10 到 30 的随机数
        redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(detailVO), baseMinutes + randomMinutes, java.util.concurrent.TimeUnit.MINUTES);

        return detailVO;
    }

    @Override
    public ProductDetailManageVO getProductDetailManage(Long id) {
        // ⚠️ 核心注意：管理端的 cacheKey 必须加上 :manage 隔离，防止敏感数据被 C 端前端读取
        String cacheKey = "product:detail:manage:" + id;

        // 1. 优先从 Redis 获取
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null) {
            String cachedStr = cachedObj.toString();
            // 【防缓存穿透】：如果是特殊标识 "null"，直接抛出异常
            if ("null".equals(cachedStr)) {
                throw new BusinessException("该商品不存在或已下架");
            }
            // 命中缓存：反序列化为管理端专用的 VO
            return JSON.parseObject(cachedStr, ProductDetailManageVO.class);
        }

        // 2. Redis 无数据，查询数据库商品基础信息 (SPU)
        Product product = this.getById(id);
        if (product == null || product.getIsDeleted()) {
            // 【防缓存穿透】
            redisTemplate.opsForValue().set(cacheKey, "null", 2, java.util.concurrent.TimeUnit.MINUTES);
            throw new BusinessException("该商品不存在或已下架");
        }

        Category category = categoryService.getById(product.getCategoryId());

        // 查询该商品下的所有 SKU
        List<Sku> skus = skuService.lambdaQuery()
                .eq(Sku::getProductId, id)
                .list();

        // 转换基础信息到管理端 VO
        ProductDetailManageVO detailVO = new ProductDetailManageVO();
        BeanUtils.copyProperties(product, detailVO);
        // 如果你的 t_product 表里有成本价字段且 Product 实体类里有对应属性，copyProperties 会自动赋值
        // 如果属性名不一致，需要手动 set：detailVO.setCostPrice(product.getCostPrice());

        if (category != null && StringUtils.hasText(category.getPath())) {
            List<Long> pathIds = Arrays.stream(category.getPath().split("/"))
                    .filter(StringUtils::hasText)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            detailVO.setCategoryPath(pathIds);
        }

        // 处理 SKU 列表转换
        List<SkuVO> skuVOs = skus.stream().map(sku -> {
            SkuVO skuVO = new SkuVO();
            BeanUtils.copyProperties(sku, skuVO);
            return skuVO;
        }).collect(Collectors.toList());

        detailVO.setSkuList(skuVOs);

        // 3. 将组装好的结果序列化存入 Redis
        // 【防缓存雪崩】：基础时间 (1小时 = 60分钟) + 随机时间 (10~30分钟)
        long baseMinutes = 60;
        long randomMinutes = 10 + new java.util.Random().nextInt(21);
        redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(detailVO), baseMinutes + randomMinutes, java.util.concurrent.TimeUnit.MINUTES);

        return detailVO;
    }

    public IPage<ProductListVO> getProductPage(ProductCustomerQueryDTO queryDTO, Long userId) {
        // 1. 初始化分页对象 (MyBatis-Plus)
        Page<Product> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();

        // 2. 基础强制限定：必须是上架商品
        wrapper.eq(Product::getStatus, 1);

        // 3. 业务筛选：分类过滤
        wrapper.eq(queryDTO.getCategoryId() != null, Product::getCategoryId, queryDTO.getCategoryId());

        // 4. 搜索逻辑处理
        boolean isSearch = StringUtils.hasText(queryDTO.getKeyword());
        if (isSearch) {
            String k = queryDTO.getKeyword();
            wrapper.and(q -> q.like(Product::getName, k).or().like(Product::getDescription, k));
        }

        // 5. 动态排序逻辑（精准对接前端 ProductSortBar 组件）
        // 假设 queryDTO 中包含了 field (default/sales/time/price) 和 order (asc/desc)
        String field = queryDTO.getSortField();
        String order = queryDTO.getSortOrder();
        boolean isAsc = "asc".equalsIgnoreCase(order);

        if (StringUtils.hasText(field) && !"default".equals(field)) {
            switch (field) {
                case "sales":
                    // 对应数据库的销量字段
                    wrapper.orderBy(true, isAsc, Product::getSalesCount);
                    break;
                case "price":
                    // 对应数据库的价格字段
                    wrapper.orderBy(true, isAsc, Product::getPrice);
                    break;
                case "time":
                    // 对应数据库的创建时间字段
                    wrapper.orderBy(true, isAsc, Product::getCreateTime);
                    break;
                default:
                    // 防御性策略：兜底排序
                    wrapper.orderByDesc(Product::getCreateTime);
            }
        } else {
            // 6. 默认排序：用户选择“默认”或者未传入排序时，按创建时间倒序
            wrapper.orderByDesc(Product::getCreateTime);
        }

        // --- Kafka 行为埋点逻辑 ---
        sendTrafficLog(queryDTO, userId, isSearch);
        // ----------------------------

        // 7. 查询并转换结果集，填满 VO 缺失的字段
        IPage<Product> productPage = productMapper.selectPage(page, wrapper);

        return productPage.convert(item -> {
            ProductListVO vo = new ProductListVO();
            // 属性克隆（能对上的字段：id, name, categoryId, price, salesCount, createTime）
            BeanUtils.copyProperties(item, vo);

            // 【关键手工填补 1】：通过库存数字，计算出 hasStock 的布尔状态
            // 假设 Product 实体类中的库存字段为 getStock()
            vo.setHasStock(item.getStock() != null && item.getStock() > 0);

            return vo;
        });
    }

    private void sendTrafficLog(ProductCustomerQueryDTO queryDTO, Long userId, boolean isSearch) {
        try {

            // 1. 构造具体的业务数据
            TrafficLogMessageDTO payload = TrafficLogMessageDTO.builder()
                    .categoryId(queryDTO.getCategoryId())
                    .keyword(queryDTO.getKeyword())
                    .build();

            // 2. 统一封装进 GenericEventDTO 信封
            GenericEventDTO<TrafficLogMessageDTO> event = GenericEventDTO.<TrafficLogMessageDTO>builder()
                    .eventId(UUID.randomUUID().toString())
                    .type(isSearch ? EventType.SEARCH : EventType.LIST) // 明确类型
                    .userId(userId)
                    .timestamp(System.currentTimeMillis())
                    .platform("Web")
                    .data(payload) // 业务数据全部塞进 data
                    .build();

            String key = isSearch ? queryDTO.getKeyword() : "LIST_EVENT";
            kafkaTemplate.send("nxg_traffic_log", key, JSON.toJSONString(event));
        } catch (Exception e) {
            log.error("Failed to send traffic log", e);
        }
    }

    @Override
    public IPage<ProductManageVO> getManageProductPage(ProductManageQueryDTO queryDTO) {
        // 注意：这里的 Page 泛型也要改为 VO
        IPage<ProductManageVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 1. 如果是查询回收站
        if (Boolean.TRUE.equals(queryDTO.getQueryDeleted())) {
            return productMapper.selectDeletedPage(page, queryDTO);
        }

        // 2. 正常查询逻辑：直接调用自定义的 Mapper 方法返回 VO
        return productMapper.selectProductManagePage(page, queryDTO);
    }

    @Override
    public void updateShelfStatus(Long id, Boolean isOnShelf) {
        // 根据前端逻辑：1 为销售中，0 为已下架
        Integer statusCode = isOnShelf ? 1 : 0;

        boolean success = this.update(new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .set(Product::getStatus, statusCode));

        if (!success) {
            throw new BusinessException("操作失败，商品不存在或已被删除");
        }
    }

    @Override
    public void restoreProduct(Long id) {
        // 调用自定义 Mapper 方法还原
        int rows = baseMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException("还原失败，商品不存在或未在回收站中");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductInfo(Long productId, ProductUpdateDTO dto) {
        // ==========================================
        // 1. 核心防御：校验商品是否存在
        // ==========================================
        Product oldProduct = this.getById(productId);
        if (oldProduct == null) {
            throw new BusinessException("要修改的商品不存在或已被删除");
        }

        // ==========================================
        // 2. 更新商品主表信息 (SPU)
        // ==========================================
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setId(productId);

        // 如果前端传了 categoryPath 并且不为空，用数组的最后一位来纠正/兜底 categoryId
        if (dto.getCategoryPath() != null && !dto.getCategoryPath().isEmpty()) {
            Long lastCategoryId = dto.getCategoryPath().get(dto.getCategoryPath().size() - 1);
            product.setCategoryId(lastCategoryId);
        }

        // ⬇️【重构核心】：使用刚刚封装的独立安全工具类处理 SPU 图片
        product.setMainImage(ImageSecurityUtils.sanitizeAndExtractFileName(dto.getMainImage()));
        product.setSubImages(ImageSecurityUtils.processMultiplePathsSecurely(dto.getSubImages()));

        this.updateById(product);

        // ==========================================
        // 3. 处理 SKU 列表更新
        // ==========================================
        List<SkuDTO> dtoList = dto.getSkuList();
        if (dtoList == null || dtoList.isEmpty()) {
            throw new BusinessException("商品至少需要包含一个SKU");
        }

        // 获取前端提交上来的、需要保留的旧 SKU ID 集合
        List<Long> keepSkuIds = dtoList.stream()
                .map(SkuDTO::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        // 删除不在本次提交列表中的旧 SKU
        LambdaQueryWrapper<Sku> deleteWrapper = new LambdaQueryWrapper<Sku>()
                .eq(Sku::getProductId, productId);
        if (!keepSkuIds.isEmpty()) {
            deleteWrapper.notIn(Sku::getId, keepSkuIds);
        }
        skuService.remove(deleteWrapper);

        // 4. 计算当前已有 SKU 的自增序列基数，防止重复生成 sku_code
        int sequence = 1;
        if (!keepSkuIds.isEmpty()) {
            long existingCount = skuService.lambdaQuery().eq(Sku::getProductId, productId).count();
            sequence = (int) existingCount + 1;
        }

        // ==========================================
        // 5. 循环执行 SKU 的 新增 或 更新
        // ==========================================
        for (SkuDTO skuDto : dtoList) {
            Sku sku = new Sku();
            BeanUtils.copyProperties(skuDto, sku);
            sku.setProductId(productId);

            // ⬇️【重构核心】：对 SKU 主图也采用相同的独立安全工具类进行清洗
            sku.setMainImage(ImageSecurityUtils.sanitizeAndExtractFileName(skuDto.getMainImage()));

            if (sku.getId() != null) {
                // --- 更新逻辑 ---
                Sku oldSku = skuService.getById(sku.getId());
                if (oldSku != null && productId.equals(oldSku.getProductId())) {
                    skuService.updateById(sku);
                }
            } else {
                // --- 新增逻辑 ---
                String generatedCode = skuService.generateSkuCode(productId, sequence++);
                sku.setSkuCode(generatedCode);
                sku.setStatus(1); // 默认上架
                skuService.save(sku);
            }
        }

        // ==========================================
        // 6. 联动同步清理缓存
        // ==========================================
        String cUserCacheKey = "product:detail:" + productId;
        String bManageCacheKey = "product:detail:manage:" + productId;

        redisTemplate.delete(Arrays.asList(cUserCacheKey, bManageCacheKey));
    }
    /**
     * 提取相对路径
     * 输入: http://localhost:8080/api/v1/upload/2026/04/abc.jpg
     * 输出: /api/v1/upload/2026/04/abc.jpg
     */
    private String extractRelativePath(String fullUrl) {
        if (fullUrl == null || !fullUrl.startsWith("http")) {
            return fullUrl;
        }
        try {
            URI uri = new URI(fullUrl);
            return uri.getPath();
        } catch (Exception e) {
            // 如果解析失败，返回原值或处理异常
            return fullUrl;
        }
    }

    /**
     * 处理逗号分隔的多张图片
     */
    private String processMultiplePaths(String subImages) {
        if (subImages == null || subImages.isEmpty()) {
            return subImages;
        }
        return Arrays.stream(subImages.split(","))
                .map(this::extractRelativePath)
                .collect(Collectors.joining(","));
    }

    @Override
    public List<ProductListVO> getRelatedProducts(Long productId) {
        // 1. 优先尝试从 Redis 获取预计算的推荐（高性能）
        String key = "product:relation:" + productId;
        List<Object> relatedIdsObj = redisTemplate.opsForList().range(key, 0, -1);

        if (relatedIdsObj != null && !relatedIdsObj.isEmpty()) {
            List<Long> relatedIds = relatedIdsObj.stream()
                    .map(obj -> Long.parseLong(obj.toString())).collect(Collectors.toList());
            return this.listByIds(relatedIds).stream()
                    .map(p -> { ProductListVO vo = new ProductListVO(); BeanUtils.copyProperties(p, vo); return vo; })
                    .collect(Collectors.toList());
        }

        // 2. Redis 无数据时的 SQL 兜底策略
        return productMapper.selectRelatedByOrderHistory(productId,10);
    }

    @Override
    public List<ProductListVO> getHomeRecommendProducts(Long userId) {
        // 1. 使用 LinkedHashSet 保持推荐顺序，并自动过滤不同榜单之间重复的商品 ID
        java.util.Set<Long> productIdSet = new java.util.LinkedHashSet<>();

        // ================= 步骤 A：个性化分类推荐 =================
        if (userId != null) {
            String userClickKey = "user:click:category:" + userId;
            java.util.Map<Object, Object> clickMap = redisTemplate.opsForHash().entries(userClickKey);

            if (clickMap != null && !clickMap.isEmpty()) {
                Long maxCategoryId = null;
                int maxCount = -1;

                for (java.util.Map.Entry<Object, Object> entry : clickMap.entrySet()) {
                    try {
                        Long categoryId = Long.parseLong(entry.getKey().toString());
                        int count = Integer.parseInt(entry.getValue().toString());
                        if (count > maxCount) {
                            maxCount = count;
                            maxCategoryId = categoryId;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Parse click count error for user: {}", userId, e);
                    }
                }

                if (maxCategoryId != null) {
                    String categoryRankKey = "rank:category:" + maxCategoryId + ":sales";
                    java.util.Set<Object> topCategoryIds = redisTemplate.opsForZSet().reverseRange(categoryRankKey, 0, 9);
                    if (topCategoryIds != null) {
                        topCategoryIds.forEach(id -> productIdSet.add(Long.parseLong(id.toString())));
                    }
                }
            }
        }

        // ================= 步骤 B：全站热卖榜兜底补齐 =================
        if (productIdSet.size() < 10) {
            String globalRankKey = "rank:global:sales";
            java.util.Set<Object> topGlobalIds = redisTemplate.opsForZSet().reverseRange(globalRankKey, 0, 19); // 稍微多取几个防重
            if (topGlobalIds != null) {
                for (Object idObj : topGlobalIds) {
                    productIdSet.add(Long.parseLong(idObj.toString()));
                    if (productIdSet.size() >= 10) {
                        break;
                    }
                }
            }
        }

        // ================= 步骤 C：Redis 彻底皆空时的【终极数据库兜底】 =================
        if (productIdSet.isEmpty()) {
            log.warn("首页推荐 Redis 榜单为空，触发数据库兜底查询");
            List<Product> backupProducts = this.lambdaQuery()
                    // 💡 注意：如果你在 Product 实体类上加了 @TableLogic，
                    // MyBatis-Plus 会自动追加 is_deleted = 0，此处手动 .eq(Product::getIsDeleted, false) 可省略
                    .eq(Product::getStatus, 1) // 顺手加上防御：必须是上架商品
                    .orderByDesc(Product::getSalesCount)       // 既然是推荐，兜底也按销量排行更合理
                    .last("LIMIT 10")
                    .list();

            // ✨ 核心修复：直接在这里转换并返回，不再往下走空集合的 listByIds 流程
            return convertToVOList(backupProducts);
        }

        // ================= 步骤 D：批量数据装配（正常有缓存流程） =================
        java.util.List<Long> productIds = new java.util.ArrayList<>(productIdSet);
        java.util.List<Product> products = this.listByIds(productIds);
        if (products == null || products.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // 将查询结果转换为 Map，用于还原并保持原本由 LinkedHashSet 决定的推荐顺序
        java.util.Map<Long, Product> productMap = products.stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));

        // 按照原本的 productIds 顺序返回 VO，顺便过滤掉刚好在 DB 中被硬删除或下架的异常商品
        return productIds.stream()
                .map(productMap::get)
                .filter(java.util.Objects::nonNull)
                .filter(p -> Product.STATUS_ON.equals(p.getStatus())) // 只展示上架商品
                .map(this::mapToVO)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 提取出来的公共单体转换方法
     */
    private ProductListVO mapToVO(Product p) {
        ProductListVO vo = new ProductListVO();
        org.springframework.beans.BeanUtils.copyProperties(p, vo);
        vo.setHasStock(p.getStock() != null && p.getStock() > 0);
        return vo;
    }

    /**
     * 提取出来的公共列表转换方法
     */
    private java.util.List<ProductListVO> convertToVOList(java.util.List<Product> products) {
        if (products == null || products.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return products.stream()
                .map(this::mapToVO)
                .collect(java.util.stream.Collectors.toList());
    }
}

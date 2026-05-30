package com.nxg.mall.api.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.common.dto.CartUpdateDTO;
import com.nxg.mall.common.dto.GenericEventDTO;
import com.nxg.mall.common.entity.Sku;
import com.nxg.mall.api.service.ISkuService;
import com.nxg.mall.common.enums.EventType;
import com.nxg.mall.common.exception.BusinessException;
import com.nxg.mall.common.vo.CartVO;
import com.nxg.mall.common.entity.Cart;
import com.nxg.mall.common.entity.Product;
import com.nxg.mall.api.mapper.CartMapper;
import com.nxg.mall.api.service.ICartService;
import com.nxg.mall.api.service.IProductService;
import com.nxg.mall.common.constant.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {

    @Value("${file.domain}")
    private String domain;

    @Autowired
    private IProductService productService;

    @Autowired
    private ISkuService skuService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToCart(Long userId, Long skuId, Integer count) {
        // 1. 校验 SKU 是否有效
        Sku sku = skuService.getById(skuId);
        if (sku == null || sku.getStatus() != 1) {
            throw new RuntimeException("该规格商品已下架或不存在");
        }

        // 2. 校验库存是否足够 (这一步在“加入购物车”时是可选的，但在“下单”时是必须的)
        if (sku.getStock() < count) {
            throw new RuntimeException("库存不足，无法加入购物车");
        }

        // 3. 查询该用户购物车中是否已存在该 SKU (根据 userId 和 skuId 查询)
        Cart cart = this.getOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getSkuId, skuId));

        if (cart == null) {
            // 4. 如果没有，新增记录
            cart = new Cart();
            cart.setUserId(userId);
            cart.setSkuId(skuId);
            cart.setProductId(sku.getProductId()); // 关联 SPU ID，方便展示
            cart.setQuantity(count);
            this.save(cart);
        } else {
            // 5. 如果已有，累加数量并重新校验总数是否超过库存
            int newQuantity = cart.getQuantity() + count;
            if (sku.getStock() < newQuantity) {
                throw new RuntimeException("购物车商品总数已超过限购库存");
            }
            cart.setQuantity(newQuantity);
            this.updateById(cart);
        }

        // ======= 新增：异步发送加购埋点消息 =======
        sendAddCartLog(userId, sku, count);
    }

    /**
     * 发送加购日志
     */
    private void sendAddCartLog(Long userId, Sku sku, Integer count) {
        try {
            // 1. 构造业务数据（Payload）
            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("productId", sku.getProductId());
            data.put("skuId", sku.getId());
            data.put("quantity", count);
            data.put("price", sku.getPrice()); // 记录加购时的单价，用于分析用户消费档次

            // 2. 封装为 GenericEventDTO
            GenericEventDTO<Map<String, Object>> event = new GenericEventDTO<>();
            event.setType(EventType.ADD_CART); // 确保枚举中已定义 ADD_CART
            event.setData(data);
            event.setPlatform("Web");
            event.setTimestamp(System.currentTimeMillis());

            // 3. 发送到流量日志 Topic (nxg_traffic_log)
            // 理由：加购属于用户意向行为，属于流量/转化路径范畴
            kafkaTemplate.send("nxg_traffic_log", JSON.toJSONString(event));

            log.debug("已发送加购埋点，用户ID: {}, 商品ID: {}", userId, sku.getProductId());
        } catch (Exception e) {
            log.error("发送加购埋点失败", e);
        }
    }

    @Override
    public List<CartVO> listMyCart(Long userId) {
        // 1. 获取该用户所有购物车条目
        List<Cart> carts = this.list(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .orderByDesc(Cart::getCreateTime)); // 按加入时间倒序

        if (carts.isEmpty()) return Collections.emptyList();

        // 2. 批量获取 SKU 详情
        List<Long> skuIds = carts.stream().map(Cart::getSkuId).distinct().collect(Collectors.toList());
        Map<Long, Sku> skuMap = skuService.listByIds(skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));

        // 3. 批量获取商品(SPU)详情
        List<Long> pIds = skuMap.values().stream().map(Sku::getProductId).distinct().collect(Collectors.toList());
        Map<Long, Product> pMap = productService.listByIds(pIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // 4. 组装 VO
        return carts.stream().map(cart -> {
            CartVO vo = new CartVO();
            BeanUtils.copyProperties(cart, vo); // 拷贝 id, skuId, productId, quantity, isChecked

            Sku sku = skuMap.get(cart.getSkuId());
            if (sku != null) {
                vo.setPrice(sku.getPrice());
                vo.setSpecs(sku.getSpecs());
                vo.setStock(sku.getStock());
                // 计算小计：SKU 单价 * 数量
                vo.setSubTotal(sku.getPrice().multiply(new BigDecimal(cart.getQuantity())));

                // 查询关联的商品信息
                Product p = pMap.get(sku.getProductId());
                if (p != null) {
                    vo.setProductName(p.getName());
                    vo.setProductMainImage(p.getMainImage());

                    // 图片展示逻辑：优先用 SKU 图片，没有则用 SPU 主图
                    String displayImg = (sku.getMainImage() != null) ? sku.getMainImage() : p.getMainImage();
                    vo.setSkuImageUrl(sku.getMainImage() != null ? sku.getMainImage() : null);
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Integer getCartCountByUserId(Long userId) {
        if (userId == null) {
            return 0;
        }

        // this.count() 返回的是 long
        long count = this.count(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId));

        // 将 long 安全地转换为 Integer
        return Math.toIntExact(count);
        // 或者直接 (int) count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 涉及数据库变更，建议开启事务
    public void updateCartItem(Long userId, CartUpdateDTO updateDto) {
        // 1. 统一查询：根据 userId 和 skuId 定位唯一的购物车记录
        Cart cart = this.getOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getSkuId, updateDto.getSkuId()));

        // 2. 如果记录不存在，直接返回或抛出业务异常
        if (cart == null) {
            throw new BusinessException("购物车商品不存在");
        }

        // 3. 动态判断并设置属性
        boolean isChanged = false;

        // 更新数量逻辑
        if (updateDto.getCount() != null) {
            cart.setQuantity(updateDto.getCount());
            isChanged = true;
        }

        // 更新选中状态逻辑
        if (updateDto.getChecked() != null) {
            cart.setIsChecked(updateDto.getChecked());
            isChanged = true;
        }

        // 4. 只有当确实有字段发生变化时，才执行数据库写操作
        if (isChanged) {
            this.updateById(cart);
        }
    }

    @Override
    public void deleteItems(Long userId, List<Long> skuIds) {
        this.remove(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .in(Cart::getSkuId, skuIds));
    }

    @Override
    @Transactional // 建议开启事务，保证批量更新的原子性
    public void updateAllCheckedStatus(Long userId, Integer checked) {
        // 使用 LambdaUpdateWrapper 构造更新条件
        LambdaUpdateWrapper<Cart> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Cart::getUserId, userId)
                .set(Cart::getIsChecked, checked);
    }

    // ================== 基于 Redis 的购物车操作 ==================

    @Override
    public void addCartItem(Long userId, Long skuId, Integer quantity) {
        if (userId == null || skuId == null || quantity == null || quantity <= 0) {
            throw new BusinessException("参数错误：userId, skuId, quantity 不能为空且 quantity 需大于0");
        }
        String key = RedisConstants.CART_PREFIX + userId;
        // 使用 increment 方法：如果 field 不存在会自动创建，已存在则累加
        redisTemplate.opsForHash().increment(key, String.valueOf(skuId), quantity);
    }

    @Override
    public void updateCartItem(Long userId, Long skuId, Integer quantity) {
        if (userId == null || skuId == null || quantity == null || quantity < 0) {
            throw new BusinessException("参数错误：userId, skuId, quantity 不能为空且 quantity 不能小于0");
        }
        String key = RedisConstants.CART_PREFIX + userId;
        if (quantity == 0) {
            // 如果更新数量为 0，相当于从购物车移除该商品
            redisTemplate.opsForHash().delete(key, String.valueOf(skuId));
        } else {
            // 覆盖现有数量
            redisTemplate.opsForHash().put(key, String.valueOf(skuId), String.valueOf(quantity));
        }
    }

    @Override
    public Map<Long, Integer> getCartList(Long userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }
        String key = RedisConstants.CART_PREFIX + userId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Integer> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                Long skuId = Long.parseLong(entry.getKey().toString());
                Integer quantity = Integer.parseInt(entry.getValue().toString());
                result.put(skuId, quantity);
            } catch (NumberFormatException e) {
                log.warn("Parse cart item error for user: {}, key: {}, value: {}", userId, entry.getKey(), entry.getValue(), e);
            }
        }
        return result;
    }

    @Override
    public void removeCartItem(Long userId, Long skuId) {
        if (userId == null || skuId == null) {
            return;
        }
        String key = RedisConstants.CART_PREFIX + userId;
        redisTemplate.opsForHash().delete(key, String.valueOf(skuId));
    }
}

package com.nxg.mall.common.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nxg.mall.common.entity.Product;
import lombok.Data;

import java.util.List;

@Data
public class CategoryPageVO {
    // 路径导航：[{id:1, name:"服装"}, {id:5, name:"女装"}]
    private List<CategoryPathVO> breadcrumbs;

    // 分页商品数据 (MyBatis-Plus 的 IPage)
    private IPage<Product> productPage;
}

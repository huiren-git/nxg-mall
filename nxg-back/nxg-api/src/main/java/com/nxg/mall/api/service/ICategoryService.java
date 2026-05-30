package com.nxg.mall.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nxg.mall.common.dto.CategoryDTO;
import com.nxg.mall.common.dto.CategoryTransferDTO;
import com.nxg.mall.common.vo.CategoryVO;
import com.nxg.mall.common.entity.Category;
import com.nxg.mall.common.vo.CategoryPathVO;

import java.util.List;

public interface ICategoryService extends IService<Category> {
    /**
     * 返回商品种类对应的树结构 用来渲染banner中的多级商品列表
     * @return 商品种类对应的树结构
     */
    List<CategoryVO> getCategoryTree();

    /**
     * 根据分类 ID 获取完整的面包屑路径
     * @param id 当前分类 ID
     * @return 包含 ID 和名称的路径列表 (从父到子)
     */
    List<CategoryPathVO> getCategoryPath(Long id);

    /**
     * 新增商品分类
     */
    void saveCategory(CategoryDTO categoryDTO);

    /**
     * 修改分类排序值 (局部更新)
     * @param id        分类ID
     * @param sortOrder 新的排序值
     */
    void updateSort(Long id, Integer sortOrder);

    /**
     * 将源分类下的所有商品转移到目标分类
     * @param transferDTO 转移参数
     */
    void transferProducts(CategoryTransferDTO transferDTO);

    /**
     * 逻辑删除 某个商品类别
     * @param id 分类 ID
     */
    void deleteCategory(Long id);

    /**
     * 逻辑删除 批量删除商品类别
     * @param ids
     */
    void deleteCategories(List<Long> ids);

}

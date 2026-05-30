package com.nxg.mall.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nxg.mall.common.Result;
import com.nxg.mall.common.annotation.Log;
import com.nxg.mall.common.dto.CategoryDTO;
import com.nxg.mall.common.dto.CategoryTransferDTO;
import com.nxg.mall.common.enums.ActionType;
import com.nxg.mall.common.enums.TargetType;
import com.nxg.mall.common.vo.CategoryVO;
import com.nxg.mall.common.vo.CategoryPageVO;
import com.nxg.mall.common.entity.Product;
import com.nxg.mall.api.service.ICategoryService;
import com.nxg.mall.api.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@Validated
public class CategoryController {

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private IProductService productService;

    @GetMapping("/category/tree")
    public Result<List<CategoryVO>> getTree() {
        // 调用之前写好的递归组装逻辑
        List<CategoryVO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    /**
     * 分页获取某一商品类别的所有商品
     * @param id   商品类别ID
     * @param page
     * @param size
     * @return
     */
    @GetMapping("/category/{id}/products")
    public Result<CategoryPageVO> getCategoryProducts(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        // 参数校验
        if (id == null || id <= 0) {
            return Result.error("无效的分类ID");
        }

        CategoryPageVO data = productService.getCategoryPageData(id, page, size);

        return Result.success(data);
    }

    /**
     * 新增商品分类
     * POST
     */
    @Log(
            title = "增加商品分类",
            actionType = ActionType.ADD_CATEGORY, // 或者使用你定义的 RESET_PWD（如果是ActionType枚举的话）
            targetType = TargetType.CATEGORY,
            isSaveRequestData = true // 密码重置操作，建议关闭请求参数保存，防止敏感信息进入日志
    )
    @PostMapping("/sales/categories")
    public Result<Void> addCategory(@Validated @RequestBody CategoryDTO categoryDTO) {
        categoryService.saveCategory(categoryDTO);
        return Result.success();
    }

    /**
     * 修改排序值 (局部更新使用 PATCH)
     */
    @Log(title = "修改分类排序", actionType = ActionType.UPDATE_CATEGORY, targetType = TargetType.CATEGORY)
    @PatchMapping("/sales/categories/{id}/sort")
    public Result<Void> updateSort(@PathVariable Long id, @RequestParam Integer sortOrder) {
        categoryService.updateSort(id, sortOrder);
        return Result.success();
    }

    /**
     * 转移指定类别下的商品 到其他类别
     * @param transferDTO
     * @return
     */
    @Log(title = "转移分类商品", actionType = ActionType.UPDATE_CATEGORY, targetType = TargetType.CATEGORY)
    @PostMapping("/sales/categories/transfer-products")
    public Result<Void> transferProducts(@Validated @RequestBody CategoryTransferDTO transferDTO) {
        categoryService.transferProducts(transferDTO);
        return Result.success();
    }

    /**
     * 删除某商品分类
     * DELETE
     */
    @Log(title = "删除商品分类", actionType = ActionType.DELETE_CATEGORY, targetType = TargetType.CATEGORY)
    @DeleteMapping("/sales/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }

    /**
     * 批量删除商品分类
     * DELETE /sales/categories?ids=1,2,3
     */
    @Log(title = "批量删除商品分类", actionType = ActionType.DELETE_CATEGORY, targetType = TargetType.CATEGORY)
    @DeleteMapping("/sales/categories")
    public Result<Void> deleteCategories(@RequestParam List<Long> ids) {
        categoryService.deleteCategories(ids);
        return Result.success();
    }


}

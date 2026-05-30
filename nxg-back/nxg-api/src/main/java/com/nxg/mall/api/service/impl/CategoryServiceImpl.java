package com.nxg.mall.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.common.dto.CategoryDTO;
import com.nxg.mall.common.dto.CategoryTransferDTO;
import com.nxg.mall.common.entity.Product;
import com.nxg.mall.common.exception.BusinessException;
import com.nxg.mall.api.mapper.ProductMapper;
import com.nxg.mall.common.vo.CategoryVO;
import com.nxg.mall.common.vo.CategoryPathVO;
import com.nxg.mall.common.entity.Category;
import com.nxg.mall.api.mapper.CategoryMapper;
import com.nxg.mall.api.service.ICategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public List<CategoryVO> getCategoryTree() {
        List<Category> allCategories = this.list();
        Map<Long, String> idToNameMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        // 1. 转换为 VO 并处理路径
        List<CategoryVO> allVOs = allCategories.stream().map(category -> {
            CategoryVO vo = new CategoryVO();
            BeanUtils.copyProperties(category, vo);
            vo.setPath(convertPathToName(category.getPath(), idToNameMap));
            return vo;
        }).collect(Collectors.toList());

        // 2. 按 parentId 分组
        Map<Long, List<CategoryVO>> childrenMap = allVOs.stream()
                .collect(Collectors.groupingBy(CategoryVO::getParentId));

        // 3. 组装树（将子列表填充到对应的父节点中）
        allVOs.forEach(node -> {
            List<CategoryVO> children = childrenMap.get(node.getId());
            if (children != null) {
                // 排序并设置子节点
                children.sort(Comparator.comparingInt(CategoryVO::getSortOrder));
                node.setChildren(children);
            } else {
                // ✨ 显式设为 null，确保 JSON 不会出现 children: []
                node.setChildren(null);
            }
        });

        // 4. 返回顶级节点
        return allVOs.stream()
                .filter(v -> v.getParentId() == 0)
                .sorted(Comparator.comparingInt(CategoryVO::getSortOrder))
                .collect(Collectors.toList());
    }

    /**
     * 路径转换工具逻辑
     */
    private String convertPathToName(String idPath, Map<Long, String> idToNameMap) {
        if (idPath == null || idPath.isEmpty() || "/".equals(idPath)) {
            return "/";
        }

        // 1. 拆分路径（去除空元素）
        // 2. 根据 ID 查 Map 换成名称
        // 3. 重新拼接成 /A/B 格式
        String namePath = Arrays.stream(idPath.split("/"))
                .filter(idStr -> !idStr.isEmpty())
                .map(idStr -> {
                    Long id = Long.valueOf(idStr);
                    return idToNameMap.getOrDefault(id, idStr); // 找不到则保留 ID
                })
                .collect(Collectors.joining("/", "/", ""));

        return namePath;
    }

    @Override
    public List<CategoryPathVO> getCategoryPath(Long id) {
        // 1. 获取当前分类信息
        Category current = this.getById(id);
        if (current == null) return Collections.emptyList();

        // 2. 解析路径字段 (假设 path 为 "/1/5/12")
        // 去掉首尾斜杠并分割
        String pathStr = current.getPath();
        if (StringUtils.isBlank(pathStr)) return Collections.emptyList();

        List<Long> ids = Arrays.stream(pathStr.split("/"))
                .filter(StringUtils::isNotBlank)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        // 把当前节点的 ID 也加进去
        if (!ids.contains(id)) ids.add(id);

        // 3. 批量查询路径上所有分类的名字
        List<Category> categories = this.list(new LambdaQueryWrapper<Category>()
                .in(Category::getId, ids));

        // 4. 按照 ids 的顺序重排结果（保证面包屑顺序：父 -> 子）
        Map<Long, String> nameMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return ids.stream()
                .map(nodeId -> new CategoryPathVO(nodeId, nameMap.get(nodeId)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCategory(CategoryDTO categoryDTO) {
        // 1. 检查父节点是否存在 (如果是 0 则跳过检查)
        Category parent = null;
        if (categoryDTO.getParentId() != 0) {
            parent = this.getById(categoryDTO.getParentId());
            if (parent == null) {
                throw new BusinessException("操作失败：父级分类不存在");
            }
        }

        // 2. 检查同级目录下是否存在相同名称
        long count = this.count(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, categoryDTO.getParentId())
                .eq(Category::getName, categoryDTO.getName()));

        if (count > 0) {
            throw new BusinessException("在该层级下已存在名为 [" + categoryDTO.getName() + "] 的分类");
        }

        // 3. 转换为实体类并设置初始值
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        category.setPath(""); // 占位，防止数据库非空约束报错

        // 4. 初次保存获取自增 ID
        this.save(category);

        // 5. 构建并更新 Path
        String fullPath = (categoryDTO.getParentId() == 0)
                ? "/" + category.getId()
                : parent.getPath() + "/" + category.getId();

        this.update(new LambdaUpdateWrapper<Category>()
                .eq(Category::getId, category.getId())
                .set(Category::getPath, fullPath));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSort(Long id, Integer sortOrder) {

        // 1. 检查分类是否存在
        Category category = this.getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        // 2. 更新 sort_order 字段 (局部更新)
        LambdaUpdateWrapper<Category> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Category::getId, id)
                .set(Category::getSortOrder, sortOrder);

        boolean success = this.update(updateWrapper);
        if (!success) {
            throw new BusinessException("更新排序失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferProducts(CategoryTransferDTO transferDTO) {
        Long targetId = transferDTO.getTargetCategoryId();
        List<Long> sourceIds = transferDTO.getSourceCategoryIds();

        // 1. 校验目标分类是否存在且未被逻辑删除
        Category targetCategory = this.getById(targetId);
        if (targetCategory == null) {
            throw new BusinessException("目标分类不存在");
        }

        // 2. 校验源分类和目标分类是否相同（防止无效操作）
        if (sourceIds.contains(targetId)) {
            throw new BusinessException("目标分类不能在待转移的源分类列表中");
        }

        // 3. 执行商品所属分类的批量更新
        // 逻辑：UPDATE product SET category_id = targetId WHERE category_id IN (sourceIds)
        LambdaUpdateWrapper<Product> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Product::getCategoryId, sourceIds)
                .set(Product::getCategoryId, targetId);

        int affectedRows = productMapper.update(null, updateWrapper);

        log.info("商品转移完成，从分类 {} 转移到 {}，共计 {} 个商品", sourceIds, targetId, affectedRows);

        if (affectedRows == 0) {
            // 注意：这里可以根据业务逻辑决定是否抛出异常。
            // 如果源分类下本来就没有商品，affectedRows 为 0 是正常的。
            log.warn("源分类下没有发现可转移的商品");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        // 1. 检查是否存在未删除的子类
        long subCategoryCount = this.count(new LambdaQueryWrapper<Category>()
                .eq(Category::getParentId, id));
        if (subCategoryCount > 0) {
            throw new BusinessException("该分类下尚有子分类，请先处理子分类");
        }

        // 2. 检查该分类下是否有未删除的商品
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(Product::getCategoryId, id));
        if (productCount > 0) {
            throw new BusinessException("该分类下关联有商品，请先转移或删除商品");
        }

        // 3. 执行逻辑删除
        boolean result = this.removeById(id);

        if (!result) {
            throw new BusinessException("删除失败，该分类可能已被删除");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategories(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 1. 【核心修改】检查是否存在“未包含在本次删除名单中”的子类
        // 逻辑：查找 parentId 在 ids 中，但 id 不在 ids 中的数据
        long subCategoryCount = this.count(new LambdaQueryWrapper<Category>()
                .in(Category::getParentId, ids)
                .notIn(Category::getId, ids)); // 关键：排除掉本次也要删除的子类

        if (subCategoryCount > 0) {
            throw new BusinessException("选中的部分分类下仍有未勾选的子分类，请先处理子分类");
        }

        // 2. 检查这些分类下是否有未删除的商品
        // 注意：这里不需要排除，因为只要这批分类里的任何一个有关联商品，都不能删
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>()
                .in(Product::getCategoryId, ids));
        if (productCount > 0) {
            throw new BusinessException("选中的分类中，某些分类关联有商品，请先转移或删除商品");
        }

        // 3. 执行批量逻辑删除
        boolean result = this.removeByIds(ids);

        if (!result) {
            throw new BusinessException("批量删除失败，部分分类可能已被删除");
        }
    }
}

package com.nxg.mall.analytics.service;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.nxg.mall.analytics.mapper.CategoryMapper;
import com.nxg.mall.common.entity.Category;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CategorySyncService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 根据 ID 获取分类名称
     * 逻辑：优先从本地镜像表查询。
     * 建议：如果性能要求极高，可以在此处加上 @Cacheable(value = "category", key = "#id")
     */
    @DS("analytics") // 明确指定从统计库读取同步过来的镜像数据
    public String getNameById(Long id) {
        if (id == null) return "未知分类";

        // 从本地镜像表中查询
        Category entity = categoryMapper.selectById(id);

        if (entity != null) {
            return entity.getName();
        }

        log.warn("未能在本地镜像表中找到分类 ID: {}", id);
        return "未知分类";
    }

    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void sync() {
        log.info("开始跨库同步分类数据...");

        // 1. 此时会触发 @DS("nxg")，从业务库读取
        List<Category> categories = categoryMapper.selectFromApi();

        if (categories == null || categories.isEmpty()) {
            log.warn("业务库未查询到分类数据，取消同步");
            return;
        }

        // 2. 切换到统计库写入
        // 如果你的统计库 Mapper 继承了 BaseMapper，可以直接利用其功能
        // 这里演示手动循环写入，实际开发建议使用 saveOrUpdateBatch
        saveToLocal(categories);

        log.info("同步成功，共计 {} 条", categories.size());
    }

    @DS("analytics")
    public void saveToLocal(List<Category> list) {
        for (Category entity : list) {
            // 这里建议使用自定义 SQL: REPLACE INTO t_category (id, name) VALUES (#{id}, #{name})
            // 这样即使 ID 已存在也会更新 Name
            categoryMapper.insert(entity);
        }
    }
}

package com.nxg.mall.analytics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nxg.mall.analytics.dto.OperationLogQueryDTO;
import com.nxg.mall.analytics.entity.OperationLog;
import com.nxg.mall.analytics.mapper.OperationLogMapper;
import com.nxg.mall.analytics.service.IOperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements IOperationLogService {

    @Override
    public IPage<OperationLog> getLogPage(OperationLogQueryDTO queryDTO) {
        Page<OperationLog> page = new Page<>(queryDTO.getCurrent(), queryDTO.getPageSize());
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        // 1. 角色筛选
        if (StringUtils.hasText(queryDTO.getRole()) && !"ALL".equals(queryDTO.getRole())) {
            wrapper.eq(OperationLog::getOperatorRole, queryDTO.getRole());
        }

        // 2. 时间范围
        wrapper.between(queryDTO.getStartTime() != null,
                OperationLog::getCreateTime, queryDTO.getStartTime(), queryDTO.getEndTime());

        // 3. 关键词模糊查询 (账号或操作内容)
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(OperationLog::getOperatorAccount, queryDTO.getKeyword())
                    .or()
                    .like(OperationLog::getActionContent, queryDTO.getKeyword()));
        }

        // 4. 业务模块多选
        if (queryDTO.getActionTypes() != null && !queryDTO.getActionTypes().isEmpty()) {
            wrapper.in(OperationLog::getActionModule, queryDTO.getActionTypes());
        }

        // 5. 排序：按时间倒序
        wrapper.orderByDesc(OperationLog::getCreateTime);

        return this.page(page, wrapper);
    }
}

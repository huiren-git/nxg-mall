package com.nxg.mall.analytics.operator;

import com.nxg.mall.analytics.model.dto.CategoryStatistics;
import com.nxg.mall.analytics.model.pojo.PurchaseRecord;
import org.apache.flink.api.common.functions.AggregateFunction;

public class CategoryAggregator implements AggregateFunction<PurchaseRecord, CategoryStatistics, CategoryStatistics> {
    @Override
    public CategoryStatistics createAccumulator() {
        return new CategoryStatistics("", 0.0, 0, "");
    }

    @Override
    public CategoryStatistics add(PurchaseRecord record, CategoryStatistics acc) {
        acc.setCategoryName(record.getCategoryName());
        acc.setTotalAmount(acc.getTotalAmount() + (record.getPrice() * record.getQuantity()));
        acc.setTotalCount(acc.getTotalCount() + record.getQuantity());
        return acc;
    }

    @Override
    public CategoryStatistics getResult(CategoryStatistics acc) {
        return acc;
    }

    @Override
    public CategoryStatistics merge(CategoryStatistics a, CategoryStatistics b) {
        return new CategoryStatistics(
                a.getCategoryName(),
                a.getTotalAmount() + b.getTotalAmount(),
                a.getTotalCount() + b.getTotalCount(),
                a.getStatsTime()
        );
    }
}

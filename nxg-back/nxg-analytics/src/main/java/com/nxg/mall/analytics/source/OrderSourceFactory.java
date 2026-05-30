package com.nxg.mall.analytics.source;

import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.SqlTimeTypeInfo;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.connector.jdbc.JdbcInputFormat;

public class OrderSourceFactory {

    public static JdbcInputFormat createMySQLSource() {
        // 1. 定义 SQL
        String querySql = "SELECT c.name as category_name, oi.current_unit_price, oi.quantity, o.create_time " +
                "FROM t_order_item oi " +
                "JOIN t_order o ON oi.order_no = o.order_no " +
                "JOIN t_product p ON oi.product_id = p.id " +
                "JOIN t_category c ON p.category_id = c.id " +
                "WHERE o.status >= 20";

        // 2. 使用 JdbcInputFormat 构建
        return JdbcInputFormat.buildJdbcInputFormat()
                .setDrivername("com.mysql.cj.jdbc.Driver")
                .setDBUrl("jdbc:mysql://localhost:3306/nxg_mall")
                .setUsername("root")
                .setPassword("你的密码")
                .setQuery(querySql)
                // 3. 必须显式定义字段类型，顺序要和 SQL 里的 Select 顺序一致
                .setRowTypeInfo(new RowTypeInfo(
                        BasicTypeInfo.STRING_TYPE_INFO,    // category_name
                        BasicTypeInfo.DOUBLE_TYPE_INFO,    // current_unit_price
                        BasicTypeInfo.INT_TYPE_INFO,       // quantity
                        SqlTimeTypeInfo.TIMESTAMP          // create_time
                ))
                .finish();
    }
}
package com.nxg.mall.common.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 统一处理金额反序列化，解决精度丢失问题
 */
public class BigDecimalDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 获取当前输入的值
        String value = p.getValueAsString();

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        // 使用 String 构造函数创建 BigDecimal，这是最安全的方式
        // 随后强制设置 2 位小数精度
        return new BigDecimal(value.trim())
                .setScale(2, RoundingMode.HALF_UP);
    }
}

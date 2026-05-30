package com.nxg.mall.common.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.JsonSerializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册 Java 8 时间类型支持（关键！）
        mapper.registerModule(new JavaTimeModule());
        // 禁用将日期序列化为时间戳（例如 1698765432000），改为字符串格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 注册模块
        SimpleModule module = new SimpleModule();
        // 1. 反序列化：处理前端传来的数据
        module.addDeserializer(BigDecimal.class, new NumberDeserializers.BigDecimalDeserializer());

        // 2. 序列化（可选）：处理后端返回给前端的数据格式（统一返回 297.00 而不是 297）
        module.addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.setScale(2, RoundingMode.HALF_UP).toPlainString());
            }
        });

        mapper.registerModule(module);
        return mapper;
    }
}

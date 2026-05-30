package com.nxg.mall.analytics.config;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlinkConfig {

    @Bean
    public StreamExecutionEnvironment streamExecutionEnvironment() {
        // 创建 Flink 流处理执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 这里可以根据需要设置并行度等参数
        // env.setParallelism(1);

        return env;
    }
}
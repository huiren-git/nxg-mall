package com.nxg.mall.analytics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.nxg.mall")
@MapperScan(basePackages = {"com.nxg.mall.analytics.mapper", "com.nxg.mall.common.mapper"})
@EnableScheduling
public class NxgAnalyticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(NxgAnalyticsApplication.class, args);
    }
}

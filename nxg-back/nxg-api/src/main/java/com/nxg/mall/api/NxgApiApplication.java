package com.nxg.mall.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.nxg.mall")
@MapperScan(basePackages = {"com.nxg.mall.api.mapper", "com.nxg.mall.common.mapper"})
public class NxgApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NxgApiApplication.class, args);
    }

}

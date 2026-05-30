package com.nxg.mall.api.config;

import com.nxg.mall.common.interceptor.AntiCrawlerInterceptor;
import com.nxg.mall.common.interceptor.ProductRateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Autowired
    private AntiCrawlerInterceptor antiCrawlerInterceptor;

    @Autowired
    private ProductRateLimitInterceptor productRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 方案一：把拦截器路径中的 /api 去掉
        registry.addInterceptor(productRateLimitInterceptor)
                .addPathPatterns("/products/**"); // 这样才能匹配到上下文剥离后的真实路径

        // 同样的，防爬拦截器也要改
        registry.addInterceptor(antiCrawlerInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/login", "/auth/register");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 确保物理路径以斜杠结尾，这是最常见的失败原因
        String location = uploadPath;
        if (!location.endsWith("/") && !location.endsWith("\\")) {
            location += "/";
        }

        // 2. 对于 Windows 系统，更稳健的写法是 file:/// (三个斜杠)
        // 如果 location 已经是 D:/... 格式，拼接后变为 file:///D:/...
        String finalLocation = "file:///" + location;

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(finalLocation)
                .setCachePeriod(0); // 课设阶段建议禁用缓存，方便调试

        // 调试辅助：在控制台打印出最终映射的路径，对比一下
        System.out.println("static path: /api/uploads/**");
        System.out.println("static resource path: " + finalLocation);
    }
}
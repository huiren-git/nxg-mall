package com.nxg.mall.common.config;

import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class Ip2regionConfig {

    @Bean(name = "ipV4Searcher")
    public Searcher ipV4Searcher() throws IOException {
        return createSearcher("ip2region_v4.xdb");
    }

    @Bean(name = "ipV6Searcher")
    public Searcher ipV6Searcher() throws IOException {
        return createSearcher("ip2region_v6.xdb");
    }

    private Searcher createSearcher(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(fileName);
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] cBuff = FileCopyUtils.copyToByteArray(inputStream);
            return Searcher.newWithBuffer(cBuff);
        }
    }
}

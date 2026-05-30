package com.nxg.mall.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic trafficLog() {
        return TopicBuilder.name("nxg_traffic_log").partitions(3).build();
    }

    @Bean
    public NewTopic businessEvent() {
        return TopicBuilder.name("nxg_business_event").partitions(2).build();
    }

    @Bean
    public NewTopic systemAudit() {
        return TopicBuilder.name("nxg_system_audit").partitions(1).build();
    }
}

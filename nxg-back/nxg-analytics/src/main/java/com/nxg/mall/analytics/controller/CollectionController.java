package com.nxg.mall.analytics.controller;

import com.alibaba.fastjson2.JSON;
import com.nxg.mall.common.Result;
import com.nxg.mall.common.dto.GenericEventDTO;
import com.nxg.mall.analytics.dto.BrowseLogDTO;
import com.nxg.mall.common.enums.EventType;
import com.nxg.mall.common.utils.IpUtils;
import com.nxg.mall.common.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/collect")
public class CollectionController {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 接收前端上报的浏览行为数据
     */
    @PostMapping("/browse")
    public Result<Void> collectBrowseLog(@RequestBody GenericEventDTO<BrowseLogDTO> eventDTO,
                                         HttpServletRequest request) {
        // 1. 此时 eventDTO 已经自动接到了前端的 type, platform, timestamp 和内层的 data
        // 此时它的 eventId, userId, ip 都是 null

        if (eventDTO.getData() == null) {
            return Result.error("数据解析失败：核心载荷为空");
        }

        // 2. 后端开始“补全”专属元数据，把 null 填满
        // 🛡️ 补全用户 ID：如果未登录，外层写死为 0L（代表匿名用户），确保 Kafka 字段永远存在
        Long currentUserId = eventDTO.getUserId();
        eventDTO.setUserId(currentUserId == null ? 0L : currentUserId);

        // 🛡️ 补全 IP
        eventDTO.setIp(IpUtils.getIpAddr(request));

        // 🛡️ 补全 唯一事件 ID
        eventDTO.setEventId(UUID.randomUUID().toString());

        eventDTO.setType(EventType.BROWSE);

        // 🛡️ 容错：如果前端漏传了时间戳，后端补一下垫底
        if (eventDTO.getTimestamp() == null) {
            eventDTO.setTimestamp(System.currentTimeMillis());
        }

        // 3. 此时 eventDTO 已经是一个“完美、完整的信封”了，直接序列化发给 Kafka
        kafkaTemplate.send("nxg_traffic_log", JSON.toJSONString(eventDTO));

        return Result.success();
    }
}

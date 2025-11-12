package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.service.MqttService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MQTT 控制器
 * 提供 API 接口来查看和管理 MQTT 消息
 */
@Slf4j
@RestController
@RequestMapping("/mqtt")
public class MqttController {
    
    @Autowired
    private MqttService mqttService;
    
    /**
     * 获取所有接收到的消息
     */
    @GetMapping("/messages")
    public Result<List<MqttService.MqttMessageData>> getAllMessages() {
        try {
            List<MqttService.MqttMessageData> messages = mqttService.getReceivedMessages();
            return Result.success(messages);
        } catch (Exception e) {
            log.error("获取 MQTT 消息失败: {}", e.getMessage(), e);
            return Result.error("获取消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取最新的 N 条消息
     */
    @GetMapping("/messages/latest")
    public Result<List<MqttService.MqttMessageData>> getLatestMessages(
            @RequestParam(defaultValue = "10") int count) {
        try {
            List<MqttService.MqttMessageData> messages = mqttService.getLatestMessages(count);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("获取最新 MQTT 消息失败: {}", e.getMessage(), e);
            return Result.error("获取最新消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取连接状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("connected", mqttService.isConnected());
            status.put("messageCount", mqttService.getReceivedMessages().size());
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取 MQTT 状态失败: {}", e.getMessage(), e);
            return Result.error("获取状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 清空所有消息
     */
    @DeleteMapping("/messages")
    public Result<String> clearMessages() {
        try {
            mqttService.clearMessages();
            return Result.success("已清空所有消息");
        } catch (Exception e) {
            log.error("清空 MQTT 消息失败: {}", e.getMessage(), e);
            return Result.error("清空消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 发布消息到指定主题
     */
    @PostMapping("/publish")
    public Result<String> publishMessage(
            @RequestParam String topic,
            @RequestParam String message,
            @RequestParam(defaultValue = "1") int qos) {
        try {
            mqttService.publish(topic, message, qos);
            return Result.success("消息发布成功");
        } catch (Exception e) {
            log.error("发布 MQTT 消息失败: {}", e.getMessage(), e);
            return Result.error("发布消息失败: " + e.getMessage());
        }
    }
}


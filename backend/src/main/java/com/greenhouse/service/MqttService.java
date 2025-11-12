package com.greenhouse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenhouse.config.EmqxConfig;
import com.greenhouse.dto.SensorDataDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MQTT 服务类
 * 负责连接 EMQX，订阅 text1 主题并接收消息
 */
@Slf4j
@Service
public class MqttService implements MqttCallback {
    
    @Autowired
    private EmqxConfig emqxConfig;
    
    @Autowired
    private SensorDataService sensorDataService;
    
    private MqttClient mqttClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 存储接收到的消息
     * 使用线程安全的列表
     */
    private final List<MqttMessageData> receivedMessages = new CopyOnWriteArrayList<>();
    
    /**
     * 最大保存消息数量
     */
    private static final int MAX_MESSAGES = 1000;
    
    /**
     * 初始化 MQTT 客户端并连接
     */
    @PostConstruct
    public void init() {
        try {
            // 创建 MQTT 客户端
            mqttClient = new MqttClient(
                emqxConfig.getBrokerUrl(),
                emqxConfig.getClientId(),
                new MemoryPersistence()
            );
            
            // 设置回调
            mqttClient.setCallback(this);
            
            // 配置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(emqxConfig.getConnectionTimeout());
            options.setKeepAliveInterval(emqxConfig.getKeepAliveInterval());
            options.setAutomaticReconnect(emqxConfig.isAutomaticReconnect());
            
            // 如果配置了用户名和密码，则设置
            if (emqxConfig.getUsername() != null && !emqxConfig.getUsername().isEmpty()) {
                options.setUserName(emqxConfig.getUsername());
            }
            if (emqxConfig.getPassword() != null && !emqxConfig.getPassword().isEmpty()) {
                options.setPassword(emqxConfig.getPassword().toCharArray());
            }
            
            // 连接到 EMQX
            log.info("正在连接到 EMQX: {}", emqxConfig.getBrokerUrl());
            mqttClient.connect(options);
            log.info("成功连接到 EMQX");
            
            // 订阅主题
            String topic = emqxConfig.getTopic();
            int qos = emqxConfig.getQos();
            mqttClient.subscribe(topic, qos);
            log.info("成功订阅主题: {} (QoS: {})", topic, qos);
            
        } catch (MqttException e) {
            log.error("MQTT 连接失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 连接丢失时的回调
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT 连接丢失: {}", cause.getMessage());
        // 自动重连由 MqttConnectOptions 处理
    }
    
    /**
     * 收到消息时的回调
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            // 获取消息内容
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            
            // 创建消息数据对象
            MqttMessageData messageData = new MqttMessageData();
            messageData.setTopic(topic);
            messageData.setMessage(payload);
            messageData.setQos(message.getQos());
            messageData.setTimestamp(System.currentTimeMillis());
            
            // 保存消息
            synchronized (receivedMessages) {
                receivedMessages.add(messageData);
                // 如果消息数量超过限制，删除最旧的消息
                if (receivedMessages.size() > MAX_MESSAGES) {
                    receivedMessages.remove(0);
                }
            }
            
            log.info("收到 MQTT 消息 - 主题: {}, 内容: {}, QoS: {}", topic, payload, message.getQos());
            
            // 尝试解析为传感器数据并保存到数据库
            try {
                parseAndSaveSensorData(payload);
            } catch (Exception e) {
                log.warn("解析传感器数据失败，消息可能不是传感器数据格式: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("处理 MQTT 消息时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 解析 MQTT 消息为传感器数据并保存到数据库
     */
    private void parseAndSaveSensorData(String payload) throws Exception {
        // 尝试解析 JSON
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        
        // 创建传感器数据 DTO
        SensorDataDTO dto = new SensorDataDTO();
        
        // 解析记录时间（如果提供，否则使用当前时间）
        if (data.containsKey("recordTime") || data.containsKey("time")) {
            String timeStr = (String) data.getOrDefault("recordTime", data.get("time"));
            if (timeStr != null && !timeStr.isEmpty()) {
                try {
                    // 支持多种时间格式
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    dto.setRecordTime(LocalDateTime.parse(timeStr, formatter));
                } catch (Exception e) {
                    log.warn("解析时间失败，使用当前时间: {}", e.getMessage());
                    dto.setRecordTime(LocalDateTime.now());
                }
            } else {
                dto.setRecordTime(LocalDateTime.now());
            }
        } else {
            dto.setRecordTime(LocalDateTime.now());
        }
        
        // 解析温度
        if (data.containsKey("temperatureC") || data.containsKey("temperature")) {
            Object temp = data.getOrDefault("temperatureC", data.get("temperature"));
            if (temp != null) {
                dto.setTemperatureC(new BigDecimal(temp.toString()));
            }
        }
        
        // 解析土壤湿度
        if (data.containsKey("soilMoisturePct") || data.containsKey("soilMoisture") || data.containsKey("moisture")) {
            Object moisture = data.getOrDefault("soilMoisturePct", 
                data.getOrDefault("soilMoisture", data.get("moisture")));
            if (moisture != null) {
                dto.setSoilMoisturePct(new BigDecimal(moisture.toString()));
            }
        }
        
        // 解析光照强度
        if (data.containsKey("lightLux") || data.containsKey("light")) {
            Object light = data.getOrDefault("lightLux", data.get("light"));
            if (light != null) {
                dto.setLightLux(Integer.parseInt(light.toString()));
            }
        }
        
        // 解析是否下雨
        if (data.containsKey("isRaining") || data.containsKey("raining")) {
            Object raining = data.getOrDefault("isRaining", data.get("raining"));
            if (raining != null) {
                if (raining instanceof Boolean) {
                    dto.setIsRaining((Boolean) raining);
                } else {
                    dto.setIsRaining("true".equalsIgnoreCase(raining.toString()) || "1".equals(raining.toString()));
                }
            }
        }
        
        // 验证必要字段（至少需要一个传感器数据）
        if (dto.getTemperatureC() == null && dto.getSoilMoisturePct() == null && dto.getLightLux() == null) {
            log.debug("消息不包含传感器数据字段，跳过保存");
            return;
        }
        
        // 为缺失的字段设置默认值（数据库要求 NOT NULL）
        if (dto.getTemperatureC() == null) {
            dto.setTemperatureC(BigDecimal.ZERO);
        }
        if (dto.getSoilMoisturePct() == null) {
            dto.setSoilMoisturePct(BigDecimal.ZERO);
        }
        if (dto.getLightLux() == null) {
            dto.setLightLux(0);
        }
        if (dto.getIsRaining() == null) {
            dto.setIsRaining(false);
        }
        
        // 保存到数据库
        sensorDataService.create(dto);
        log.info("成功保存传感器数据到数据库: 温度={}, 湿度={}, 光照={}, 下雨={}", 
            dto.getTemperatureC(), dto.getSoilMoisturePct(), dto.getLightLux(), dto.getIsRaining());
    }
    
    /**
     * 消息发送完成时的回调
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.debug("消息发送完成");
    }
    
    /**
     * 获取所有接收到的消息
     */
    public List<MqttMessageData> getReceivedMessages() {
        return new ArrayList<>(receivedMessages);
    }
    
    /**
     * 获取最新的 N 条消息
     */
    public List<MqttMessageData> getLatestMessages(int count) {
        List<MqttMessageData> messages = new ArrayList<>(receivedMessages);
        int size = messages.size();
        if (size <= count) {
            return messages;
        }
        return messages.subList(size - count, size);
    }
    
    /**
     * 清空所有消息
     */
    public void clearMessages() {
        receivedMessages.clear();
        log.info("已清空所有 MQTT 消息");
    }
    
    /**
     * 发布消息到指定主题
     */
    public void publish(String topic, String message, int qos) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
                mqttMessage.setQos(qos);
                mqttClient.publish(topic, mqttMessage);
                log.info("发布消息到主题 {}: {}", topic, message);
            } else {
                log.warn("MQTT 客户端未连接，无法发布消息");
            }
        } catch (MqttException e) {
            log.error("发布消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }
    
    /**
     * 销毁时断开连接
     */
    @PreDestroy
    public void destroy() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                log.info("MQTT 客户端已断开连接");
            }
        } catch (MqttException e) {
            log.error("断开 MQTT 连接时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * MQTT 消息数据类
     */
    public static class MqttMessageData {
        private String topic;
        private String message;
        private int qos;
        private long timestamp;
        
        // Getters and Setters
        public String getTopic() {
            return topic;
        }
        
        public void setTopic(String topic) {
            this.topic = topic;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public int getQos() {
            return qos;
        }
        
        public void setQos(int qos) {
            this.qos = qos;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}


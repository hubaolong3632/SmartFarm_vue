package com.greenhouse.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * EMQX MQTT 配置类
 * 从 application.yml 读取 EMQX 相关配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "emqx")
public class EmqxConfig {
    
    /**
     * EMQX 服务器地址
     * 格式: tcp://host:port
     */
    private String brokerUrl = "tcp://localhost:1883";
    
    /**
     * MQTT 客户端ID
     */
    private String clientId = "greenhouse-client";
    
    /**
     * MQTT 用户名（可选）
     */
    private String username;
    
    /**
     * MQTT 密码（可选）
     */
    private String password;
    
    /**
     * 订阅的主题
     */
    private String topic = "text1";
    
    /**
     * 服务质量等级 (0, 1, 2)
     * 0: 最多一次
     * 1: 至少一次
     * 2: 仅一次
     */
    private int qos = 1;
    
    /**
     * 连接超时时间（秒）
     */
    private int connectionTimeout = 30;
    
    /**
     * 心跳间隔（秒）
     */
    private int keepAliveInterval = 60;
    
    /**
     * 是否自动重连
     */
    private boolean automaticReconnect = true;
}


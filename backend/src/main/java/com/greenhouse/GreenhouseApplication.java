package com.greenhouse;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
/// /
/**
 * 智能温室管理系统主启动类
 */
@SpringBootApplication
@MapperScan("com.greenhouse.mapper")
@EnableScheduling
public class GreenhouseApplication {
    public static void main(String[] args) {
        SpringApplication.run(GreenhouseApplication.class, args);
    }
}


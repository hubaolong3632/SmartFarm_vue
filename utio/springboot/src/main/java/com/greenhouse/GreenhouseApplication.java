package com.greenhouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 智能温室管理系统主启动类
 */
@SpringBootApplication
@EnableJpaAuditing
public class GreenhouseApplication {
    public static void main(String[] args) {
        SpringApplication.run(GreenhouseApplication.class, args);
    }
}


package com.greenhouse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 控制操作日志实体类
 * 对应表：control_logs
 */
@Entity
@Table(name = "control_logs", indexes = {
    @Index(name = "idx_control_type", columnList = "control_type"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@EntityListeners(AuditingEntityListener.class)
public class ControlLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 控制类型（light/cleaning等）
     */
    @Column(name = "control_type", nullable = false, length = 50)
    private String controlType;

    /**
     * 操作动作（on/off/start/stop等）
     */
    @Column(nullable = false, length = 50)
    private String action;

    /**
     * 操作状态（success/failed）
     */
    @Column(nullable = false, length = 20)
    private String status;

    /**
     * 操作消息
     */
    @Column(length = 200)
    private String message;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}


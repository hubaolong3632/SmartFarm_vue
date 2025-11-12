package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 控制操作日志实体类
 * 对应表：control_logs
 */
@Data
@TableName("control_logs")
public class ControlLog {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 控制类型（light/cleaning等）
     */
    private String controlType;

    /**
     * 操作动作（on/off/start/stop等）
     */
    private String action;

    /**
     * 操作状态（success/failed）
     */
    private String status;

    /**
     * 操作消息
     */
    private String message;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

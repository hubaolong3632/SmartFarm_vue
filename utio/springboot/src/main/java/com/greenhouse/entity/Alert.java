package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报警记录实体类
 * 对应表：alerts
 */
@Data
@TableName("alerts")
public class Alert {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 报警级别（info/warning/error）
     */
    private String level;

    /**
     * 报警消息
     */
    private String message;

    /**
     * 报警类型（temperature/soil_moisture/light/automation等）
     */
    private String alertType;

    /**
     * 关联数据（JSON格式）
     */
    private String relatedData;

    /**
     * 是否已读（0未读/1已读）
     */
    private Boolean isRead;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

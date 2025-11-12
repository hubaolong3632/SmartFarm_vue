package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 自动化设置实体类
 * 对应表：automation_settings
 */
@Data
@TableName("automation_settings")
public class AutomationSetting {
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 设置键名
     */
    private String settingKey;

    /**
     * 设置值（JSON格式）
     */
    private String settingValue;

    /**
     * 设置描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}

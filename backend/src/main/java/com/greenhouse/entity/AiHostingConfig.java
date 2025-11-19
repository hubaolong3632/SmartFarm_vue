package com.greenhouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * AI托管配置实体类
 */
@Data
@TableName("ai_hosting_config")
public class AiHostingConfig {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private Boolean enabled;
    
    private Boolean emailEnabled;
    
    private String emailAddress;
    
    private Integer checkIntervalMinutes;
    
    private Boolean waterControlEnabled;
    
    private Boolean lightControlEnabled;
    
    private Boolean recipeExecutionEnabled;
    
    private Date createdAt;
    
    private Date updatedAt;
}


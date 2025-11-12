package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.entity.AutomationSetting;
import com.greenhouse.mapper.AutomationSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动化设置控制器
 */
@RestController
@RequestMapping("/automation")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AutomationController {
    
    private final AutomationSettingMapper automationSettingMapper;
    
    /**
     * 获取所有自动化设置
     */
    @GetMapping
    public Result<Map<String, Object>> getAll() {
        List<AutomationSetting> settings = automationSettingMapper.selectList(null);
        Map<String, Object> result = new HashMap<>();
        settings.forEach(setting -> {
            try {
                // 尝试解析 JSON 值
                Object value = setting.getSettingValue();
                if ("true".equals(value) || "false".equals(value)) {
                    value = Boolean.parseBoolean(setting.getSettingValue());
                } else {
                    try {
                        value = Integer.parseInt(setting.getSettingValue());
                    } catch (NumberFormatException e) {
                        // 保持原值
                    }
                }
                result.put(setting.getSettingKey(), value);
            } catch (Exception e) {
                result.put(setting.getSettingKey(), setting.getSettingValue());
            }
        });
        return Result.success(result);
    }
    
    /**
     * 更新自动化设置
     */
    @PutMapping
    @Transactional
    public Result<Map<String, Object>> update(@RequestBody Map<String, Object> settings) {
        settings.forEach((key, value) -> {
            AutomationSetting setting = automationSettingMapper.findBySettingKey(key);
            if (setting == null) {
                setting = new AutomationSetting();
                setting.setSettingKey(key);
                setting.setSettingValue(String.valueOf(value));
                setting.setCreatedAt(LocalDateTime.now());
                setting.setUpdatedAt(LocalDateTime.now());
                automationSettingMapper.insert(setting);
            } else {
                setting.setSettingValue(String.valueOf(value));
                setting.setUpdatedAt(LocalDateTime.now());
                automationSettingMapper.updateById(setting);
            }
        });
        return getAll();
    }
    
    /**
     * 获取单个设置
     */
    @GetMapping("/setting")
    public Result<Object> getSetting(@RequestParam String key) {
        AutomationSetting setting = automationSettingMapper.findBySettingKey(key);
        if (setting == null) {
            return Result.error(404, "设置不存在");
        }
        Object value = setting.getSettingValue();
        if ("true".equals(value) || "false".equals(value)) {
            value = Boolean.parseBoolean(setting.getSettingValue());
        } else {
            try {
                value = Integer.parseInt(setting.getSettingValue());
            } catch (NumberFormatException e) {
                // 保持原值
            }
        }
        return Result.success(value);
    }
}

package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.entity.AutomationSetting;
import com.greenhouse.repository.AutomationSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动化设置控制器
 */
@RestController
@RequestMapping("/automation")
@RequiredArgsConstructor
public class AutomationController {
    
    private final AutomationSettingRepository automationSettingRepository;
    
    /**
     * 获取所有自动化设置
     */
    @GetMapping
    public Result<Map<String, Object>> getAll() {
        List<AutomationSetting> settings = automationSettingRepository.findAll();
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
            AutomationSetting setting = automationSettingRepository.findBySettingKey(key)
                    .orElse(new AutomationSetting());
            setting.setSettingKey(key);
            setting.setSettingValue(String.valueOf(value));
            automationSettingRepository.save(setting);
        });
        return getAll();
    }
    
    /**
     * 获取单个设置
     */
    @GetMapping("/{key}")
    public Result<Object> getSetting(@PathVariable String key) {
        AutomationSetting setting = automationSettingRepository.findBySettingKey(key)
                .orElse(null);
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


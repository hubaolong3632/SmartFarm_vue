package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.entity.AutomationSetting;
import com.greenhouse.mapper.AutomationSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
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
                Date now = new Date();
                setting.setCreatedAt(now);
                setting.setUpdatedAt(now);
                automationSettingMapper.insert(setting);
            } else {
                setting.setSettingValue(String.valueOf(value));
                setting.setUpdatedAt(new Date());
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





    /**
     * 获取设置的时间秒数
     */
    @GetMapping("/time")
    public Object time() {
        String key="imageUploadIntervalSeconds";
        AutomationSetting setting = automationSettingMapper.findBySettingKey(key);
        if (setting == null) {
            return "99999";
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
        return value;
    }
    
    /**
     * 获取图片上传间隔时间对应的秒数
     * @param hours 小时数（1-24），可选
     * @param seconds 秒数，可选
     * @return 对应的秒数
     */
    @GetMapping("/image-upload-interval-seconds")
    public Result<Long> getImageUploadIntervalSeconds(
            @RequestParam(required = false) Integer hours,
            @RequestParam(required = false) Long seconds) {
        // 如果提供了秒数，直接返回
        if (seconds != null && seconds > 0) {
            return Result.success(seconds);
        }
        // 如果提供了小时数，转换为秒数
        if (hours != null && hours >= 1 && hours <= 24) {
            long result = hours * 3600L;
            return Result.success(result);
        }
        // 参数无效
        return Result.error(400, "请提供有效的小时数（1-24）或秒数（>0）");
    }
}

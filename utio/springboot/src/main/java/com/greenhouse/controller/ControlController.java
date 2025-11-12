package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.entity.ControlLog;
import com.greenhouse.mapper.ControlLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制操作控制器
 */
@RestController
@RequestMapping("/control")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ControlController {
    
    private final ControlLogMapper controlLogMapper;
    
    /**
     * 清理搅拌熔炉
     */
    @PostMapping("/cleaning")
    @Transactional
    public Result<Map<String, Object>> triggerCleaning() {
        ControlLog log = new ControlLog();
        log.setControlType("cleaning");
        log.setAction("start");
        log.setStatus("success");
        log.setMessage("清理搅拌熔炉操作已启动");
        log.setCreatedAt(LocalDateTime.now());
        controlLogMapper.insert(log);
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "清理搅拌熔炉操作已启动");
        return Result.success(result);
    }
    
    /**
     * 打开/关闭植物补光灯
     * action: 1=打开, 0=关闭
     */
    @PostMapping("/light")
    @Transactional
    public Result<Map<String, Object>> toggleLight(@RequestParam Integer action) {
        if (action == null || (action != 1 && action != 0)) {
            return Result.error(400, "操作参数错误，应为 1(打开) 或 0(关闭)");
        }
        String actionStr = action == 1 ? "on" : "off";
        
        ControlLog log = new ControlLog();
        log.setControlType("light");
        log.setAction(actionStr);
        log.setStatus("success");
        log.setMessage("植物补光灯已" + (action == 1 ? "打开" : "关闭"));
        log.setCreatedAt(LocalDateTime.now());
        controlLogMapper.insert(log);
        
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("lightOn", action == 1);
        result.put("message", "植物补光灯已" + (action == 1 ? "打开" : "关闭"));
        return Result.success(result);
    }
    
    /**
     * 获取控制日志
     */
    @GetMapping("/logs")
    public Result<List<ControlLog>> getLogs() {
        return Result.success(controlLogMapper.selectList(null));
    }
}

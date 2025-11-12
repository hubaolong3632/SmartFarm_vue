package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.dto.SensorDataDTO;
import com.greenhouse.entity.SensorData;
import com.greenhouse.service.SensorDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 传感器数据控制器
 */
@RestController
@RequestMapping("/sensor-data")
@RequiredArgsConstructor
public class SensorDataController {
    
    private final SensorDataService sensorDataService;
    
    /**
     * 创建传感器数据
     */
    @PostMapping
    public Result<SensorData> create(@Valid @RequestBody SensorDataDTO dto) {
        SensorData sensorData = sensorDataService.create(dto);
        return Result.success(sensorData);
    }
    
    /**
     * 批量创建传感器数据
     */
    @PostMapping("/batch")
    public Result<List<SensorData>> batchCreate(@Valid @RequestBody List<SensorDataDTO> dtos) {
        List<SensorData> sensorDataList = sensorDataService.batchCreate(dtos);
        return Result.success(sensorDataList);
    }
    
    /**
     * 获取最新数据
     */
    @GetMapping("/latest")
    public Result<SensorData> getLatest() {
        SensorData sensorData = sensorDataService.getLatest();
        return Result.success(sensorData);
    }
    
    /**
     * 获取最近24小时数据
     */
    @GetMapping("/last-24-hours")
    public Result<List<SensorData>> getLast24Hours() {
        List<SensorData> sensorDataList = sensorDataService.getLast24Hours();
        return Result.success(sensorDataList);
    }
    
    /**
     * 获取指定时间范围的数据
     */
    @GetMapping("/range")
    public Result<List<SensorData>> getByTimeRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<SensorData> sensorDataList = sensorDataService.getByTimeRange(startTime, endTime);
        return Result.success(sensorDataList);
    }
}


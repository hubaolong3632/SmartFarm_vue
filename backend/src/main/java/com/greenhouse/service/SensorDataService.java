package com.greenhouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greenhouse.dto.SensorDataDTO;
import com.greenhouse.entity.SensorData;
import com.greenhouse.mapper.SensorDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 传感器数据服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorDataService {
    
    private final SensorDataMapper sensorDataMapper;
    
    /**
     * 创建传感器数据
     */
    @Transactional
    public SensorData create(SensorDataDTO dto) {
        // 如果没有提供记录时间，使用当前时间
        if (dto.getRecordTime() == null) {
            dto.setRecordTime(LocalDateTime.now());
        }
        
        SensorData sensorData = new SensorData();
        sensorData.setRecordTime(dto.getRecordTime());
        
        // 只设置非空字段
        if (dto.getTemperatureC() != null) {
            sensorData.setTemperatureC(dto.getTemperatureC());
        }
        if (dto.getSoilMoisturePct() != null) {
            sensorData.setSoilMoisturePct(dto.getSoilMoisturePct());
        }
        if (dto.getLightLux() != null) {
            sensorData.setLightLux(dto.getLightLux());
        }
        if (dto.getIsRaining() != null) {
            sensorData.setIsRaining(dto.getIsRaining());
        }
        
        sensorData.setCreatedAt(LocalDateTime.now());
        sensorData.setUpdatedAt(LocalDateTime.now());
        sensorDataMapper.insert(sensorData);
        return sensorData;
    }
    
    /**
     * 获取最新数据
     */
    public SensorData getLatest() {
        return sensorDataMapper.findLatest();
    }
    
    /**
     * 获取最近24小时数据
     */
    public List<SensorData> getLast24Hours() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(23);
        return sensorDataMapper.findLast24Hours(startTime, endTime);
    }
    
    /**
     * 获取指定时间范围的数据
     */
    public List<SensorData> getByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<SensorData> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(SensorData::getRecordTime, startTime, endTime)
               .orderByAsc(SensorData::getRecordTime);
        return sensorDataMapper.selectList(wrapper);
    }
    
    /**
     * 批量创建传感器数据
     */
    @Transactional
    public List<SensorData> batchCreate(List<SensorDataDTO> dtos) {
        List<SensorData> sensorDataList = dtos.stream().map(dto -> {
            SensorData sensorData = new SensorData();
            sensorData.setRecordTime(dto.getRecordTime());
            sensorData.setTemperatureC(dto.getTemperatureC());
            sensorData.setSoilMoisturePct(dto.getSoilMoisturePct());
            sensorData.setLightLux(dto.getLightLux());
            sensorData.setIsRaining(dto.getIsRaining());
            sensorData.setCreatedAt(LocalDateTime.now());
            sensorData.setUpdatedAt(LocalDateTime.now());
            return sensorData;
        }).collect(Collectors.toList());
        
        for (SensorData sensorData : sensorDataList) {
            sensorDataMapper.insert(sensorData);
        }
        return sensorDataList;
    }
}


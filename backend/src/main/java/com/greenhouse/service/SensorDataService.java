package com.greenhouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greenhouse.dto.SensorDataDTO;
import com.greenhouse.entity.SensorData;
import com.greenhouse.mapper.SensorDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
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
        // 记录时间统一使用后端当前系统时间，忽略前端传递的时间
        // 这样可以确保时间准确，不受设备时间影响
        Date now = new Date();
        dto.setRecordTime(now);
        
        SensorData sensorData = new SensorData();
        sensorData.setRecordTime(now);
        
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
        
        // 创建时间和更新时间使用当前时间
        sensorData.setCreatedAt(now);
        sensorData.setUpdatedAt(now);
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
        Date endTime = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(endTime);
        cal.add(Calendar.HOUR_OF_DAY, -23);
        Date startTime = cal.getTime();
        return sensorDataMapper.findLast24Hours(startTime, endTime);
    }
    
    /**
     * 获取最新的30条记录（用于对比）
     */
    public List<SensorData> getToday() {
        // 获取最新的30条记录，按时间倒序排列
        List<SensorData> records = sensorDataMapper.findLatestRecords(30);
        // 反转列表，使其按时间正序排列（从早到晚），便于折线图显示
        java.util.Collections.reverse(records);
        return records;
    }
    
    /**
     * 获取指定时间范围的数据
     */
    public List<SensorData> getByTimeRange(Date startTime, Date endTime) {
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
        Date now = new Date();
        List<SensorData> sensorDataList = dtos.stream().map(dto -> {
            SensorData sensorData = new SensorData();
            // 记录时间统一使用后端当前系统时间，忽略前端传递的时间
            sensorData.setRecordTime(now);
            sensorData.setTemperatureC(dto.getTemperatureC());
            sensorData.setSoilMoisturePct(dto.getSoilMoisturePct());
            sensorData.setLightLux(dto.getLightLux());
            sensorData.setIsRaining(dto.getIsRaining());
            sensorData.setCreatedAt(now);
            sensorData.setUpdatedAt(now);
            return sensorData;
        }).collect(Collectors.toList());
        
        for (SensorData sensorData : sensorDataList) {
            sensorDataMapper.insert(sensorData);
        }
        return sensorDataList;
    }
}


package com.greenhouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greenhouse.dto.SensorDataDTO;
import com.greenhouse.entity.SensorData;
import com.greenhouse.mapper.SensorDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        
        // 设置所有字段（包括默认值）
        sensorData.setTemperatureC(dto.getTemperatureC() != null ? dto.getTemperatureC() : BigDecimal.ZERO);
        sensorData.setHumidityPct(dto.getHumidityPct() != null ? dto.getHumidityPct() : BigDecimal.ZERO);
        sensorData.setSoilMoisturePct(dto.getSoilMoisturePct() != null ? dto.getSoilMoisturePct() : BigDecimal.ZERO);
        sensorData.setLightLux(dto.getLightLux() != null ? dto.getLightLux() : 0);
        sensorData.setIsRaining(dto.getIsRaining() != null ? dto.getIsRaining() : false);
        sensorData.setOxygenPct(dto.getOxygenPct() != null ? dto.getOxygenPct() : BigDecimal.ZERO);
        sensorData.setCo2Ppm(dto.getCo2Ppm() != null ? dto.getCo2Ppm() : 0);
        
        // 创建时间和更新时间使用当前时间
        sensorData.setCreatedAt(now);
        sensorData.setUpdatedAt(now);
        
        try {
            // 先尝试使用标准插入方法（包含所有字段）
            sensorDataMapper.insert(sensorData);
            log.debug("传感器数据已插入数据库 - ID: {}, recordTime: {}", sensorData.getId(), sensorData.getRecordTime());
        } catch (Exception e) {
            // 如果标准插入失败（可能是字段不存在），尝试使用基础字段插入
            if (e.getMessage() != null && (e.getMessage().contains("Unknown column") || 
                e.getMessage().contains("humidity_pct") || 
                e.getMessage().contains("oxygen_pct") || 
                e.getMessage().contains("co2_ppm"))) {
                log.warn("标准插入失败（可能缺少新字段），尝试使用基础字段插入: {}", e.getMessage());
                try {
                    sensorDataMapper.insertBasicFields(sensorData);
                    // 重新查询获取ID
                    SensorData latest = sensorDataMapper.findLatest();
                    if (latest != null) {
                        sensorData.setId(latest.getId());
                    }
                    log.warn("使用基础字段成功插入传感器数据（新字段未保存，请执行数据库迁移脚本添加字段） - recordTime: {}", sensorData.getRecordTime());
                } catch (Exception e2) {
                    log.error("基础字段插入也失败: {}", e2.getMessage(), e2);
                    throw new RuntimeException("插入传感器数据失败，请检查数据库表结构是否正确", e2);
                }
            } else {
                log.error("插入传感器数据失败: {}", e.getMessage(), e);
                throw e;
            }
        }
        
        return sensorData;
    }
    
    /**
     * 获取最新数据
     */
    public SensorData getLatest() {
        SensorData latest = sensorDataMapper.findLatest();
        if (latest != null) {
            // 为 NULL 值设置默认值
            if (latest.getHumidityPct() == null) {
                latest.setHumidityPct(BigDecimal.ZERO);
            }
            if (latest.getOxygenPct() == null) {
                latest.setOxygenPct(BigDecimal.ZERO);
            }
            if (latest.getCo2Ppm() == null) {
                latest.setCo2Ppm(0);
            }
        }
        return latest;
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
        List<SensorData> records = sensorDataMapper.findLast24Hours(startTime, endTime);
        
        // 为 NULL 值设置默认值
        for (SensorData record : records) {
            if (record.getHumidityPct() == null) {
                record.setHumidityPct(BigDecimal.ZERO);
            }
            if (record.getOxygenPct() == null) {
                record.setOxygenPct(BigDecimal.ZERO);
            }
            if (record.getCo2Ppm() == null) {
                record.setCo2Ppm(0);
            }
        }
        
        return records;
    }
    
    /**
     * 获取最新的30条记录（用于对比）
     */
    public List<SensorData> getToday() {
        // 获取最新的30条记录，按时间倒序排列
        List<SensorData> records = sensorDataMapper.findLatestRecords(30);
        log.debug("查询到最新30条记录，实际返回: {} 条", records.size());
        if (!records.isEmpty()) {
            log.debug("最新记录时间: {}, 最早记录时间: {}", 
                records.get(0).getRecordTime(), 
                records.get(records.size() - 1).getRecordTime());
        }
        
        // 为 NULL 值设置默认值，确保前端能正确显示
        for (SensorData record : records) {
            if (record.getHumidityPct() == null) {
                record.setHumidityPct(BigDecimal.ZERO);
            }
            if (record.getOxygenPct() == null) {
                record.setOxygenPct(BigDecimal.ZERO);
            }
            if (record.getCo2Ppm() == null) {
                record.setCo2Ppm(0);
            }
            // 确保其他字段也有默认值
            if (record.getTemperatureC() == null) {
                record.setTemperatureC(BigDecimal.ZERO);
            }
            if (record.getSoilMoisturePct() == null) {
                record.setSoilMoisturePct(BigDecimal.ZERO);
            }
            if (record.getLightLux() == null) {
                record.setLightLux(0);
            }
            if (record.getIsRaining() == null) {
                record.setIsRaining(false);
            }
        }
        
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
            sensorData.setTemperatureC(dto.getTemperatureC() != null ? dto.getTemperatureC() : BigDecimal.ZERO);
            sensorData.setHumidityPct(dto.getHumidityPct() != null ? dto.getHumidityPct() : BigDecimal.ZERO);
            sensorData.setSoilMoisturePct(dto.getSoilMoisturePct() != null ? dto.getSoilMoisturePct() : BigDecimal.ZERO);
            sensorData.setLightLux(dto.getLightLux() != null ? dto.getLightLux() : 0);
            sensorData.setIsRaining(dto.getIsRaining() != null ? dto.getIsRaining() : false);
            sensorData.setOxygenPct(dto.getOxygenPct() != null ? dto.getOxygenPct() : BigDecimal.ZERO);
            sensorData.setCo2Ppm(dto.getCo2Ppm() != null ? dto.getCo2Ppm() : 0);
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


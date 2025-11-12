package com.greenhouse.service;

import com.greenhouse.dto.SensorDataDTO;
import com.greenhouse.entity.SensorData;
import com.greenhouse.repository.SensorDataRepository;
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
    
    private final SensorDataRepository sensorDataRepository;
    
    /**
     * 创建传感器数据
     */
    @Transactional
    public SensorData create(SensorDataDTO dto) {
        SensorData sensorData = new SensorData();
        sensorData.setRecordTime(dto.getRecordTime());
        sensorData.setTemperatureC(dto.getTemperatureC());
        sensorData.setSoilMoisturePct(dto.getSoilMoisturePct());
        sensorData.setLightLux(dto.getLightLux());
        sensorData.setIsRaining(dto.getIsRaining());
        return sensorDataRepository.save(sensorData);
    }
    
    /**
     * 获取最新数据
     */
    public SensorData getLatest() {
        return sensorDataRepository.findFirstByOrderByRecordTimeDesc()
                .orElse(null);
    }
    
    /**
     * 获取最近24小时数据
     */
    public List<SensorData> getLast24Hours() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(23);
        return sensorDataRepository.findLast24Hours(startTime, endTime);
    }
    
    /**
     * 获取指定时间范围的数据
     */
    public List<SensorData> getByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return sensorDataRepository.findByRecordTimeBetweenOrderByRecordTimeAsc(startTime, endTime);
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
            return sensorData;
        }).collect(Collectors.toList());
        return sensorDataRepository.saveAll(sensorDataList);
    }
}


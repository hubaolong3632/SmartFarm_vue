package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.SensorData;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 传感器数据Mapper
 */
@Mapper
public interface SensorDataMapper extends BaseMapper<SensorData> {
    
    /**
     * 查询最近24小时的数据
     */
    @Select("SELECT * FROM sensor_data WHERE record_time >= #{startTime} AND record_time <= #{endTime} ORDER BY record_time ASC")
    List<SensorData> findLast24Hours(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
    
    /**
     * 获取最新的一条记录
     */
    @Select("SELECT * FROM sensor_data ORDER BY record_time DESC LIMIT 1")
    SensorData findLatest();
    
    /**
     * 获取最新的N条记录（按时间倒序）
     */
    @Select("SELECT * FROM sensor_data ORDER BY record_time DESC LIMIT #{limit}")
    List<SensorData> findLatestRecords(@Param("limit") int limit);
    
    /**
     * 查询今天最新的N条温度记录（按时间倒序）
     */
    @Select("SELECT * FROM sensor_data WHERE DATE(record_time) = CURDATE() ORDER BY record_time DESC LIMIT #{limit}")
    List<SensorData> findTodayLatestTemperatureRecords(@Param("limit") int limit);
    
    /**
     * 插入基础字段（兼容旧表结构，不包含新字段）
     * 用于降级处理：当新字段不存在时使用此方法
     */
    @Insert("INSERT INTO sensor_data (record_time, temperature_c, soil_moisture_pct, light_lux, is_raining, created_at, updated_at) " +
            "VALUES (#{recordTime}, #{temperatureC}, #{soilMoisturePct}, #{lightLux}, #{isRaining}, #{createdAt}, #{updatedAt})")
    int insertBasicFields(SensorData sensorData);
}


package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.SensorData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
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
    List<SensorData> findLast24Hours(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取最新的一条记录
     */
    @Select("SELECT * FROM sensor_data ORDER BY record_time DESC LIMIT 1")
    SensorData findLatest();
}


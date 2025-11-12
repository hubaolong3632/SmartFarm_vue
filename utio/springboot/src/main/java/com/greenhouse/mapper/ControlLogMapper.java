package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.ControlLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 控制操作日志Mapper
 */
@Mapper
public interface ControlLogMapper extends BaseMapper<ControlLog> {
    
    /**
     * 查询指定时间范围的控制日志
     */
    @Select("SELECT * FROM control_logs WHERE created_at >= #{startTime} AND created_at <= #{endTime} ORDER BY created_at DESC")
    List<ControlLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询指定类型的控制日志
     */
    @Select("SELECT * FROM control_logs WHERE control_type = #{controlType} ORDER BY created_at DESC")
    List<ControlLog> findByControlTypeOrderByCreatedAtDesc(@Param("controlType") String controlType);
}


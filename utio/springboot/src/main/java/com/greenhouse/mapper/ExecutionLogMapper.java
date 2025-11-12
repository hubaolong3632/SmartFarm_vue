package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.ExecutionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行日志Mapper
 */
@Mapper
public interface ExecutionLogMapper extends BaseMapper<ExecutionLog> {
    
    /**
     * 查询指定时间范围的执行日志
     */
    @Select("SELECT * FROM execution_logs WHERE executed_at >= #{startTime} AND executed_at <= #{endTime} ORDER BY executed_at DESC")
    List<ExecutionLog> findByExecutedAtBetweenOrderByExecutedAtDesc(
            @Param("startTime") LocalDateTime startTime, 
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询指定地块的执行日志
     */
    @Select("SELECT * FROM execution_logs WHERE plot_id = #{plotId} ORDER BY executed_at DESC")
    List<ExecutionLog> findByPlotIdOrderByExecutedAtDesc(@Param("plotId") Integer plotId);
    
    /**
     * 查询最近24小时的执行日志（按小时聚合）
     */
    @Select("SELECT DATE_FORMAT(executed_at, '%Y-%m-%d %H:00:00') as hour, " +
            "SUM(executions) as totalExecutions " +
            "FROM execution_logs " +
            "WHERE executed_at >= #{startTime} AND executed_at <= #{endTime} " +
            "GROUP BY hour " +
            "ORDER BY hour ASC")
    List<Object[]> findExecutionsByHour(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}


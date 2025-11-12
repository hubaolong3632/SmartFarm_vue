package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.ExecutionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

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
            @Param("startTime") Date startTime, 
            @Param("endTime") Date endTime);
    
    /**
     * 查询指定地块的执行日志
     */
    @Select("SELECT * FROM execution_logs WHERE plot_id = #{plotId} ORDER BY executed_at DESC")
    List<ExecutionLog> findByPlotIdOrderByExecutedAtDesc(@Param("plotId") Integer plotId);
    
    /**
     * 查询最近24小时的执行日志（按小时聚合）
     * 注意：GROUP BY 使用完整的表达式而不是别名，以兼容所有 MySQL 版本
     * 使用 Map 接收结果，因为 Object[] 无法被 MyBatis 正确映射
     */
    @Select("SELECT DATE_FORMAT(executed_at, '%Y-%m-%d %H:00:00') as hour, " +
            "COALESCE(SUM(executions), 0) as totalExecutions " +
            "FROM execution_logs " +
            "WHERE executed_at >= #{startTime} AND executed_at <= #{endTime} " +
            "GROUP BY DATE_FORMAT(executed_at, '%Y-%m-%d %H:00:00') " +
            "ORDER BY hour ASC")
    List<Map<String, Object>> findExecutionsByHour(
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime);
}


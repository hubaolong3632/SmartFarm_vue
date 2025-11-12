package com.greenhouse.repository;

import com.greenhouse.entity.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行日志Repository
 */
@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {
    
    /**
     * 查询指定时间范围的执行日志
     */
    List<ExecutionLog> findByExecutedAtBetweenOrderByExecutedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询指定地块的执行日志
     */
    List<ExecutionLog> findByPlotIdOrderByExecutedAtDesc(Integer plotId);
    
    /**
     * 查询最近24小时的执行日志（按小时聚合）
     */
    @Query("SELECT DATE_FORMAT(e.executedAt, '%Y-%m-%d %H:00:00') as hour, " +
           "SUM(e.executions) as totalExecutions " +
           "FROM ExecutionLog e " +
           "WHERE e.executedAt >= :startTime AND e.executedAt <= :endTime " +
           "GROUP BY hour " +
           "ORDER BY hour ASC")
    List<Object[]> findExecutionsByHour(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}


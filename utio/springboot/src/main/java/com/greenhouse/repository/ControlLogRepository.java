package com.greenhouse.repository;

import com.greenhouse.entity.ControlLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 控制操作日志Repository
 */
@Repository
public interface ControlLogRepository extends JpaRepository<ControlLog, Long> {
    
    /**
     * 查询指定时间范围的控制日志
     */
    List<ControlLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询指定类型的控制日志
     */
    List<ControlLog> findByControlTypeOrderByCreatedAtDesc(String controlType);
}


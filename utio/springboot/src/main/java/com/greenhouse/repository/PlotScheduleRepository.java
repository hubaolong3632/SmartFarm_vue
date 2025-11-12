package com.greenhouse.repository;

import com.greenhouse.entity.PlotSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 地块定时执行计划Repository
 */
@Repository
public interface PlotScheduleRepository extends JpaRepository<PlotSchedule, Long> {
    
    /**
     * 查询指定地块的所有定时计划
     */
    List<PlotSchedule> findByPlotIdOrderByScheduleTimeAsc(Integer plotId);
    
    /**
     * 查询启用的定时计划
     */
    List<PlotSchedule> findByIsEnabledTrueOrderByScheduleTimeAsc();
    
    /**
     * 查询指定地块的启用定时计划
     */
    List<PlotSchedule> findByPlotIdAndIsEnabledTrueOrderByScheduleTimeAsc(Integer plotId);
}


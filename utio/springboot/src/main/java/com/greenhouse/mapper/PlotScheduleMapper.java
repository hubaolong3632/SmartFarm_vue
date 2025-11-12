package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.PlotSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 地块定时执行计划Mapper
 */
@Mapper
public interface PlotScheduleMapper extends BaseMapper<PlotSchedule> {
    
    /**
     * 查询指定地块的所有定时计划
     */
    @Select("SELECT * FROM plot_schedules WHERE plot_id = #{plotId} ORDER BY schedule_time ASC")
    List<PlotSchedule> findByPlotIdOrderByScheduleTimeAsc(@Param("plotId") Integer plotId);
    
    /**
     * 查询启用的定时计划
     */
    @Select("SELECT * FROM plot_schedules WHERE is_enabled = 1 ORDER BY schedule_time ASC")
    List<PlotSchedule> findByIsEnabledTrueOrderByScheduleTimeAsc();
    
    /**
     * 查询指定地块的启用定时计划
     */
    @Select("SELECT * FROM plot_schedules WHERE plot_id = #{plotId} AND is_enabled = 1 ORDER BY schedule_time ASC")
    List<PlotSchedule> findByPlotIdAndIsEnabledTrueOrderByScheduleTimeAsc(@Param("plotId") Integer plotId);
}


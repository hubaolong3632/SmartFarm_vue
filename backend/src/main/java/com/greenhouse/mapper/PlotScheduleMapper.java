package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.PlotSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
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
    
    /**
     * 更新定时任务的扩展字段（如果字段存在）
     * 使用动态SQL，只更新非null的字段
     */
    @Update({
        "<script>",
        "UPDATE plot_schedules SET",
        "<if test='scheduleType != null'>schedule_type = #{scheduleType},</if>",
        "<if test='dayOfWeek != null'>day_of_week = #{dayOfWeek},</if>",
        "<if test='scheduleDatetime != null'>schedule_datetime = #{scheduleDatetime},</if>",
        "updated_at = NOW()",
        "WHERE id = #{id}",
        "</script>"
    })
    int updateScheduleFields(@Param("id") Long id, 
                            @Param("scheduleType") String scheduleType,
                            @Param("dayOfWeek") Integer dayOfWeek,
                            @Param("scheduleDatetime") Date scheduleDatetime);
}


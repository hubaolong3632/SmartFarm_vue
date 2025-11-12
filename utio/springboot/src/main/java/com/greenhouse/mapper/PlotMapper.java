package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.Plot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 地块Mapper
 */
@Mapper
public interface PlotMapper extends BaseMapper<Plot> {
    
    /**
     * 根据地块编号查询
     */
    @Select("SELECT * FROM plots WHERE plot_number = #{plotNumber}")
    Plot findByPlotNumber(Integer plotNumber);
}


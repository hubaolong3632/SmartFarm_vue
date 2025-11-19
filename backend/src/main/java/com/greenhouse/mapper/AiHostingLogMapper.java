package com.greenhouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greenhouse.entity.AiHostingLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI托管执行日志Mapper
 */
@Mapper
public interface AiHostingLogMapper extends BaseMapper<AiHostingLog> {
    
    @Select("SELECT * FROM ai_hosting_logs ORDER BY execution_time DESC LIMIT #{limit}")
    List<AiHostingLog> findRecentLogs(Integer limit);
}


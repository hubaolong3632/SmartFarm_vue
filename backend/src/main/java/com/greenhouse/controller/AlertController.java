package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.entity.Alert;
import com.greenhouse.mapper.AlertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报警记录控制器
 */
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AlertController {
    
    private final AlertMapper alertMapper;
    
    /**
     * 获取所有报警
     */
    @GetMapping
    public Result<List<Alert>> getAll() {
        return Result.success(alertMapper.selectList(null));
    }
    
    /**
     * 获取未读报警
     */
    @GetMapping("/unread")
    public Result<List<Alert>> getUnread() {
        return Result.success(alertMapper.findByIsReadFalseOrderByCreatedAtDesc());
    }
/// /
    /**
     * 获取指定级别的报警
     */
    @GetMapping("/level")
    public Result<List<Alert>> getByLevel(@RequestParam String level) {
        return Result.success(alertMapper.findByLevelOrderByCreatedAtDesc(level));
    }
    
    /**
     * 标记为已读
     */
    @PutMapping("/read")
    @Transactional
    public Result<Void> markAsRead(@RequestParam Long id) {
        alertMapper.markAsRead(id);
        return Result.success();
    }
    
    /**
     * 批量标记为已读
     */
    @PutMapping("/batch-read")
    @Transactional
    public Result<Void> markAsReadBatch(@RequestBody List<Long> ids) {
        ids.forEach(alertMapper::markAsRead);
        return Result.success();
    }
}

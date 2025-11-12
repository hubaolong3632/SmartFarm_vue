package com.greenhouse.controller;

import com.greenhouse.common.Result;
import com.greenhouse.entity.Alert;
import com.greenhouse.repository.AlertRepository;
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
public class AlertController {
    
    private final AlertRepository alertRepository;
    
    /**
     * 获取所有报警
     */
    @GetMapping
    public Result<List<Alert>> getAll() {
        return Result.success(alertRepository.findAll());
    }
    
    /**
     * 获取未读报警
     */
    @GetMapping("/unread")
    public Result<List<Alert>> getUnread() {
        return Result.success(alertRepository.findByIsReadFalseOrderByCreatedAtDesc());
    }
    
    /**
     * 获取指定级别的报警
     */
    @GetMapping("/level/{level}")
    public Result<List<Alert>> getByLevel(@PathVariable String level) {
        return Result.success(alertRepository.findByLevelOrderByCreatedAtDesc(level));
    }
    
    /**
     * 标记为已读
     */
    @PutMapping("/{id}/read")
    @Transactional
    public Result<Void> markAsRead(@PathVariable Long id) {
        alertRepository.markAsRead(id);
        return Result.success();
    }
    
    /**
     * 批量标记为已读
     */
    @PutMapping("/batch-read")
    @Transactional
    public Result<Void> markAsReadBatch(@RequestBody List<Long> ids) {
        alertRepository.markAsReadBatch(ids);
        return Result.success();
    }
}


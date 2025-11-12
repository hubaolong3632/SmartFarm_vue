package com.greenhouse.repository;

import com.greenhouse.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 报警记录Repository
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    /**
     * 查询未读报警
     */
    List<Alert> findByIsReadFalseOrderByCreatedAtDesc();
    
    /**
     * 查询指定级别的报警
     */
    List<Alert> findByLevelOrderByCreatedAtDesc(String level);
    
    /**
     * 标记为已读
     */
    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.id = :id")
    void markAsRead(@Param("id") Long id);
    
    /**
     * 批量标记为已读
     */
    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.id IN :ids")
    void markAsReadBatch(@Param("ids") List<Long> ids);
}


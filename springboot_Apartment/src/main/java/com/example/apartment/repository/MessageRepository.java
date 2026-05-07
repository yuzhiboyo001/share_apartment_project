package com.example.apartment.repository;

import com.example.apartment.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 获取用户消息列表（按时间倒序）
    Page<Message> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    // 获取未读消息
    List<Message> findByUserIdAndIsRead(Long userId, Integer isRead);

    // 统计未读消息数
    long countByUserIdAndIsRead(Long userId, Integer isRead);

    // 标记所有消息为已读
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = 1 WHERE m.userId = :userId AND m.isRead = 0")
    void markAllAsRead(@Param("userId") Long userId);

    // 根据业务ID查询消息
    List<Message> findByBizId(Long bizId);
}
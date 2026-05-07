package com.example.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "message_type", nullable = false)
    private Integer messageType; // 1-账单提醒，2-事务分配，3-状态变更

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(name = "biz_id")
    private Long bizId;

    @Column(name = "is_read", nullable = false)
    private Integer isRead = 0; // 0-未读，1-已读

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
}
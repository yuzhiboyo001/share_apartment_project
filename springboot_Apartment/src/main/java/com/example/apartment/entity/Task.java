package com.example.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apartment_id", nullable = false)
    private Long apartmentId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "task_type", nullable = false)
    private Integer taskType; // 1-值日，2-维修，3-采购，4-其他

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "assignee_id")
    private Long assigneeId;

    private LocalDateTime deadline;

    @Column(nullable = false)
    private Integer status = 0; // 0-待处理，1-进行中，2-已完成，3-已取消

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
}
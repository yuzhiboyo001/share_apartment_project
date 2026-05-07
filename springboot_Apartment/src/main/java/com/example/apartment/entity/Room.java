package com.example.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apartment_id", nullable = false)
    private Long apartmentId;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(precision = 5, scale = 2)
    private BigDecimal area = BigDecimal.ZERO;

    @Column(name = "current_user_id")
    private Long currentUserId;

    @Column(nullable = false)
    private Integer status = 0; // 0-空置，1-已入住

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
    // 非数据库字段，用于前端显示
    @Transient
    private String tenantName;

    @Transient
    private String statusText;
}
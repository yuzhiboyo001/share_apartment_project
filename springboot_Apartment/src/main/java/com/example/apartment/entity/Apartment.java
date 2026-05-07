package com.example.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "apartment")
public class Apartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(name = "total_rooms", nullable = false)
    private Integer totalRooms = 0;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "invite_code", nullable = false, unique = true, length = 10)
    private String inviteCode;

    @Column()
    private String description;

    @Column(nullable = false)
    private Integer status = 1; // 0-已解散，1-正常

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
}
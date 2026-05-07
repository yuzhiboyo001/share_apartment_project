package com.example.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 11)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column()
    private String avatar;

    @Column(nullable = false)
    private Integer role = 0; // 0-租客，1-出租房东

    @Column(name = "apartment_id")
    private Long apartmentId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "check_in_date")
    private LocalDate checkInDate;

    @Column(nullable = false)
    private Integer status = 1; // 0-禁用，1-正常

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
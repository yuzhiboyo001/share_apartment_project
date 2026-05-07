package com.example.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bill")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apartment_id", nullable = false)
    private Long apartmentId;

    @Column(name = "bill_name", nullable = false, length = 100)
    private String billName;

    @Column(name = "bill_type", nullable = false)
    private Integer billType; // 1-水电，2-燃气，3-网费，4-房租，5-其他

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "bill_period", nullable = false, length = 20)
    private String billPeriod;

    @Column()
    private String attachment;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(nullable = false)
    private Integer status = 0; // 0-待缴费，1-已结清

    @Column()
    private String remark;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();

}
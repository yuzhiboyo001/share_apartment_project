package com.example.apartment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer status = 0; // 0-未支付，1-已支付

    @Column(name = "pay_time")
    private LocalDateTime payTime;

    @Column(name = "pay_method")
    private Integer payMethod; // 1-微信，2-支付宝，3-现金

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime = LocalDateTime.now();
}
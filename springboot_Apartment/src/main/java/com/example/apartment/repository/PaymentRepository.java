package com.example.apartment.repository;

import com.example.apartment.entity.Payment;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBillId(Long billId);

    List<Payment> findByUserId(Long userId);

    @Query("SELECT p FROM Payment p WHERE p.billId = ?1 AND p.userId = ?2")
    Payment findByBillIdAndUserId(Long billId, Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Payment p WHERE p.billId = ?1")
    void deleteByBillId(Long billId);

    @Query("SELECT p FROM Payment p WHERE p.userId = ?1 AND p.status = 0")
    List<Payment> findUnpaidByUserId(Long userId);
    // 根据用户ID查询所有缴费记录

    // 根据用户ID和状态查询
    List<Payment> findByUserIdAndStatus(Long userId, Integer status);


}
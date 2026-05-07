package com.example.apartment.repository;

import com.example.apartment.entity.Bill;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    // 根据公寓ID查询账单
    List<Bill> findByApartmentId(Long apartmentId);

    // 根据公寓ID查询账单（分页）
    Page<Bill> findByApartmentId(Long apartmentId, Pageable pageable);

    Page<Bill> findByApartmentIdAndStatus(Long apartmentId, Integer status, Pageable pageable);
    // 根据账单周期查询

    // 根据公寓ID和状态查询
    List<Bill> findByApartmentIdAndStatus(Long apartmentId, Integer status);

    // 根据账单周期查询
    List<Bill> findByBillPeriod(String billPeriod);

    // 根据关键字搜索（账单名称或周期）
    @Query("SELECT b FROM Bill b WHERE b.apartmentId = :apartmentId AND " +
            "(:keyword IS NULL OR :keyword = '' OR b.billName LIKE %:keyword% OR b.billPeriod LIKE %:keyword%)")
    Page<Bill> searchByKeyword(@Param("apartmentId") Long apartmentId,
                               @Param("keyword") String keyword,
                               Pageable pageable);
    @Query("SELECT b.billPeriod, COUNT(b), SUM(b.totalAmount) FROM Bill b " +
            "WHERE b.apartmentId = ?1 GROUP BY b.billPeriod ORDER BY b.billPeriod DESC")
    List<Object[]> getMonthlyBillStats(Long apartmentId);

    @Query("SELECT b.billType, COUNT(b), SUM(b.totalAmount) FROM Bill b " +
            "WHERE b.apartmentId = ?1 GROUP BY b.billType")
    List<Object[]> getBillTypeStats(Long apartmentId);

    @Query("SELECT b FROM Bill b WHERE b.apartmentId = ?1 AND b.billPeriod = ?2")
    List<Bill> findByApartmentIdAndPeriod(Long apartmentId, String billPeriod);


}
package com.example.apartment.repository;

import com.example.apartment.entity.Apartment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    // 根据房东ID分页查询公寓（可选，但前端可能需要所有公寓？）
    Page<Apartment> findByAdminId(Long adminId, Pageable pageable);

    // 模糊搜索公寓名称或地址，并分页
    @Query("SELECT a FROM Apartment a WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR a.name LIKE %:keyword% OR a.address LIKE %:keyword%)")
    Page<Apartment> search(@Param("keyword") String keyword, Pageable pageable);
    // 统计符合条件的总数
    @Query("SELECT COUNT(a) FROM Apartment a WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR a.name LIKE %:keyword% OR a.address LIKE %:keyword%)")
    long countSearch(@Param("keyword") String keyword);

    boolean existsByInviteCode(String inviteCode);

    // 统计符合条件的总数（可选，Page对象已包含）
    long countByNameContainingOrAddressContaining(String keyword, String keyword2);
    // 根据邀请码查询公寓（唯一）
    Apartment findByInviteCode(String inviteCode);


}
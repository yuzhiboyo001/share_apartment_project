package com.example.apartment.repository;

import com.example.apartment.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 根据手机号查询用户
    Optional<User> findByPhone(String phone);

    // 根据手机号和状态查询用户
    Optional<User> findByPhoneAndStatus(String phone, Integer status);

    // 根据公寓ID和角色查询用户
    List<User> findByApartmentIdAndRole(Long apartmentId, Integer role);

    // 分页查询公寓内的租客
    Page<User> findByApartmentIdAndRole(Long apartmentId, Integer role, Pageable pageable);

    // 查询未分配房间的租客
    @Query("SELECT u FROM User u WHERE u.apartmentId = :apartmentId AND u.role = 0 AND u.roomId IS NULL")
    List<User> findTenantsWithoutRoom(@Param("apartmentId") Long apartmentId);

    // 根据用户名模糊查询
    List<User> findByUsernameContaining(String username);

}
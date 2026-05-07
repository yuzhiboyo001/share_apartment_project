package com.example.apartment.service;

import com.example.apartment.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    /**
     * 根据手机号查询用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 验证用户登录
     */
    Optional<User> validateLogin(String phone, String password);
    User register(User user);
    void updateUser(User user);
    /**
     * 根据公寓ID查询所有租客
     */
    List<User> findTenantsByApartmentId(Long apartmentId);  // 去掉 @Query 注解
}
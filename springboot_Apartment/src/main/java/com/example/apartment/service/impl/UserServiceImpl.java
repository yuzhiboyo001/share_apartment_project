package com.example.apartment.service.impl;

import com.example.apartment.entity.User;
import com.example.apartment.repository.UserRepository;
import com.example.apartment.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhoneAndStatus(phone, 1);
    }
    @Override
    public User register(User user) {
        // 检查手机号是否已存在
        if (userRepository.findByPhone(user.getPhone()).isPresent()) {
            throw new RuntimeException("手机号已注册");
        }
        return userRepository.save(user);
    }

    @Override
    public void updateUser(User user) {
        userRepository.save(user);
    }
    @Override
    public Optional<User> validateLogin(String phone, String password) {
        Optional<User> userOpt = userRepository.findByPhoneAndStatus(phone, 1);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    // ========== 添加这个缺失的方法 ==========
    @Override
    public List<User> findTenantsByApartmentId(Long apartmentId) {
        return userRepository.findByApartmentIdAndRole(apartmentId, 0);
    }
}
package com.example.apartment.dto;

import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {
    private String phone;
    private String password;
}
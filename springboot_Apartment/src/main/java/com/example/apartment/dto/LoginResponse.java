package com.example.apartment.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponse {
    private Integer code;
    private Data data;

    @lombok.Data
    public static class Data {
        private List<Menu> menuList;
        private String token;
        private String message;
        private String name;
        private int role;
    }

    @lombok.Data
    public static class Menu {
        private String path;
        private String name;
        private String label;
        private String icon;
        private String url;
        private List<Menu> children;
    }

    public static LoginResponse success(List<Menu> menuList, String token, String name, int role) {
        LoginResponse response = new LoginResponse();
        response.setCode(200);
        Data data = new Data();
        data.setName(name);
        data.setRole(role);
        data.setMenuList(menuList);
        data.setToken(token);
        data.setMessage("登录成功");

        response.setData(data);
        return response;
    }
}
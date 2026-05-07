package com.example.apartment.config;

import com.example.apartment.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;  // 改为 jakarta
import jakarta.servlet.http.HttpServletResponse; // 改为 jakarta

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("========== JwtInterceptor 开始执行 ==========");
        System.out.println("请求URI: " + request.getRequestURI());

        // 放行登录接口和根据邀请码查询公寓的接口
        if (request.getRequestURI().contains("/api/permission/") ||
                request.getRequestURI().contains("/api/apartment/getByInviteCode")) {  // 添加这行
            System.out.println("公开接口，放行");
            return true;
        }

        // 从header中获取token
        String authHeader = request.getHeader("Authorization");
        System.out.println("Authorization header: " + authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                Claims claims = jwtUtil.parseToken(token);

                Long userId = claims.get("userId", Long.class);
                Integer role = claims.get("role", Integer.class);
                String phone = claims.getSubject();

                System.out.println("解析结果 - userId: " + userId + ", role: " + role + ", phone: " + phone);

                // 将用户信息存入request
                request.setAttribute("userId", userId);
                request.setAttribute("userRole", role);
                request.setAttribute("userPhone", phone);

                System.out.println("属性已设置到request中");
                System.out.println("========== JwtInterceptor 执行结束 ==========");
                return true;
            } else {
                System.out.println("token验证失败");
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"token无效\"}");
                return false;
            }
        } else {
            System.out.println("没有Authorization header");
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
            return false;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 可以留空
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 可以留空
    }
}
package com.example.apartment.controller;

import com.example.apartment.dto.LoginRequest;
import com.example.apartment.dto.LoginResponse;
import com.example.apartment.entity.User;
import com.example.apartment.service.UserService;
import com.example.apartment.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/permission")
@CrossOrigin(origins = "*")  // 允许跨域
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/getMenu")
    public Map<String, Object> login(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = new HashMap<>();

        try {
            String phone = loginRequest.getPhone();
            String password = loginRequest.getPassword();

            // 参数校验
            if (phone == null || phone.trim().isEmpty()) {
                response.put("code", 400);
                response.put("message", "手机号不能为空");
                return response;
            }

            if (password == null || password.trim().isEmpty()) {
                response.put("code", 400);
                response.put("message", "密码不能为空");
                return response;
            }

            // 检查手机号是否存在
            Optional<User> userOpt = userService.findByPhone(phone);
            if (userOpt.isEmpty()) {
                response.put("code", 400);
                response.put("message", "该用户不存在，请先注册");
                return response;
            }

            // 验证密码
            User user = userOpt.get();
            // 检查用户状态
            if (user.getStatus() != 1) {
                response.put("code", 400);
                response.put("message", "用户已被禁用，请联系管理员");
                return response;
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
                response.put("code", 400);
                response.put("message", "密码错误，请重新输入");
                return response;
            }

            // 生成JWT token
            String token = jwtUtil.generateToken(phone, user.getId(), user.getRole());

            // 根据角色返回不同的菜单
            List<LoginResponse.Menu> menuList;
            if (user.getRole() == 1) {
                menuList = getAdminMenu();
            } else {
                menuList = getTenantMenu();
            }

            // 构建成功响应
            Map<String, Object> data = new HashMap<>();
            data.put("menuList", menuList);
            data.put("token", token);
            data.put("name", user.getUsername());
            data.put("role", user.getRole());
            data.put("apartmentId", user.getApartmentId());
            data.put("createTime", user.getCreateTime());
            data.put("userId", user.getId());
            data.put("message", "登录成功");

            response.put("code", 200);
            response.put("data", data);

        } catch (Exception e) {
            log.error("登录失败", e);
            response.put("code", 500);
            response.put("message", "登录失败：" + e.getMessage());
        }

        return response;
    }


    /**
     * 用户注册（接收角色参数）
     */
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> registerData) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 从Map中获取参数
            String phone = (String) registerData.get("phone");
            String password = (String) registerData.get("password");
            Integer role = (Integer) registerData.get("role");  // 获取角色参数

            // 参数校验
            if (phone == null || phone.isEmpty()) {
                response.put("code", 400);
                response.put("message", "手机号不能为空");
                return response;
            }

            if (password == null || password.length() < 6) {
                response.put("code", 400);
                response.put("message", "密码长度不能少于6位");
                return response;
            }

            // 角色参数校验
            if (role == null) {
                // 如果前端没传role，默认设为0（租客）
                role = 0;
            }

            // 检查手机号是否已注册
            Optional<User> existingUser = userService.findByPhone(phone);
            if (existingUser.isPresent()) {
                response.put("code", 400);
                response.put("message", "该手机号已注册");
                return response;
            }

            // 创建新用户
            User newUser = new User();
            newUser.setPhone(phone);
            newUser.setUsername("用户" + phone.substring(phone.length() - 4)); // 默认用户名：用户+手机后4位
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setRole(role); // 使用前端传递的角色
            newUser.setStatus(1);
            newUser.setCreateTime(LocalDateTime.now());

            // 保存用户
            User savedUser = userService.register(newUser);

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("id", savedUser.getId());
            data.put("phone", savedUser.getPhone());
            data.put("username", savedUser.getUsername());
            data.put("role", savedUser.getRole());

            response.put("code", 200);
            response.put("message", "注册成功");
            response.put("data", data);

        } catch (ClassCastException e) {
            log.error("参数类型错误", e);
            response.put("code", 400);
            response.put("message", "参数格式错误");
        } catch (Exception e) {
            log.error("注册失败", e);
            response.put("code", 500);
            response.put("message", "注册失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 重置密码
     */
    @PostMapping("/resetPassword")
    public Map<String, Object> resetPassword(@RequestBody Map<String, String> resetData) {
        Map<String, Object> response = new HashMap<>();

        try {
            String phone = resetData.get("phone");
            String newPassword = resetData.get("newPassword");

            // 参数校验
            if (phone == null || phone.isEmpty()) {
                response.put("code", 400);
                response.put("message", "手机号不能为空");
                return response;
            }

            if (newPassword == null || newPassword.length() < 6) {
                response.put("code", 400);
                response.put("message", "密码长度不能少于6位");
                return response;
            }

            // 查找用户
            Optional<User> userOpt = userService.findByPhone(phone);
            if (!userOpt.isPresent()) {
                response.put("code", 400);
                response.put("message", "该手机号未注册");
                return response;
            }

            // 更新密码
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userService.updateUser(user);

            response.put("code", 200);
            response.put("message", "密码重置成功");

        } catch (Exception e) {
            log.error("重置密码失败", e);
            response.put("code", 500);
            response.put("message", "重置失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 获取公寓的租客列表（用于房间分配）
     */
    @GetMapping("/tenantList")
    public Map<String, Object> getTenantList(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long apartmentId) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证token
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            // 获取该公寓的所有租客 (role = 0)
            List<User> tenants = userService.findTenantsByApartmentId(apartmentId);

            List<Map<String, Object>> tenantList = tenants.stream().map(tenant -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", tenant.getId());
                map.put("username", tenant.getUsername());
                map.put("phone", tenant.getPhone());
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("list", tenantList);
            data.put("count", tenantList.size());

            response.put("code", 200);
            response.put("data", data);
        } catch (Exception e) {
            log.error("获取租客列表失败", e);
            response.put("code", 500);
            response.put("message", "获取失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 管理员菜单
     */
    private List<LoginResponse.Menu> getAdminMenu() {
        List<LoginResponse.Menu> menuList = new ArrayList<>();

        // 首页
        LoginResponse.Menu home = new LoginResponse.Menu();
        home.setPath("/home");
        home.setName("home");
        home.setLabel("首页");
        home.setIcon("house");
        home.setUrl("Home");
        menuList.add(home);

        // 公寓管理
        LoginResponse.Menu apartment = new LoginResponse.Menu();
        apartment.setPath("/apartment");
        apartment.setName("apartment");
        apartment.setLabel("公寓管理");
        apartment.setIcon("video-play");
        apartment.setUrl("Apartment");
        menuList.add(apartment);

        // 账单统计
        LoginResponse.Menu bill = new LoginResponse.Menu();
        bill.setPath("/landlordBill");
        bill.setName("landlordBill");
        bill.setLabel("账单统计");
        bill.setIcon("user");
        bill.setUrl("LandlordBill");
        menuList.add(bill);

        // 其他（包含子菜单）
        LoginResponse.Menu other = new LoginResponse.Menu();
        other.setPath("other");
        other.setLabel("其他");
        other.setIcon("location");

        List<LoginResponse.Menu> children = new ArrayList<>();

        LoginResponse.Menu page1 = new LoginResponse.Menu();
        page1.setPath("/message");
        page1.setName("message");
        page1.setLabel("发布消息");
        page1.setIcon("setting");
        page1.setUrl("Message");
        children.add(page1);

        other.setChildren(children);
        menuList.add(other);

        return menuList;
    }

    /**
     * 租客菜单
     */
    private List<LoginResponse.Menu> getTenantMenu() {
        List<LoginResponse.Menu> menuList = new ArrayList<>();

        // 首页
        LoginResponse.Menu home = new LoginResponse.Menu();
        home.setPath("/home");
        home.setName("home");
        home.setLabel("首页");
        home.setIcon("house");
        home.setUrl("Home");
        menuList.add(home);

        // 账单管理
        LoginResponse.Menu bill = new LoginResponse.Menu();
        bill.setPath("/tenantBill");
        bill.setName("tenantBill");
        bill.setLabel("账单管理");
        bill.setIcon("money");
        bill.setUrl("TenantBill");
        menuList.add(bill);

        // 公寓事务
        LoginResponse.Menu task = new LoginResponse.Menu();
        task.setPath("/task");
        task.setName("task");
        task.setLabel("公寓事务");
        task.setIcon("user");
        task.setUrl("Task");
        menuList.add(task);


        return menuList;
    }

    /**
     * 从token中提取用户ID
     */
    private Long getUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return null;
        }
        Claims claims = jwtUtil.parseToken(token);
        return claims.get("userId", Long.class);
    }
}
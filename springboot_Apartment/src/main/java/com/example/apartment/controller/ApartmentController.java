package com.example.apartment.controller;

import com.example.apartment.entity.Apartment;
import com.example.apartment.entity.Room;
import com.example.apartment.entity.User;
import com.example.apartment.repository.ApartmentRepository;
import com.example.apartment.repository.RoomRepository;
import com.example.apartment.repository.UserRepository;
import com.example.apartment.service.ApartmentService;
import com.example.apartment.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/apartment")
@CrossOrigin(origins = "*")
public class ApartmentController {

    private static final Logger log = LoggerFactory.getLogger(ApartmentController.class);

    @Autowired
    private ApartmentService apartmentService;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取公寓列表（支持分页和关键字搜索）
     */
    @GetMapping("/list")
    public Map<String, Object> getApartmentList(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        Map<String, Object> response = new HashMap<>();

        try {
            // ========== 最关键的三行日志 ==========
            System.out.println("★★★★★ 控制器接收原始page参数: " + page);
            System.out.println("★★★★★ 控制器接收原始limit参数: " + limit);
            System.out.println("★★★★★ 控制器接收原始keyword参数: " + keyword);

            Long adminId = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                adminId = getUserIdFromToken(authHeader);
                System.out.println("★★★★★ 解析出的adminId: " + adminId);
            }

            Map<String, Object> data = apartmentService.getApartmentList(keyword, page, limit, adminId);

            System.out.println("★★★★★ Service返回的currentPage: " + data.get("currentPage"));

            response.put("code", 200);
            response.put("data", data);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("code", 500);
            response.put("message", "获取失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 新增公寓
     */
    @PostMapping("/add")
    public Map<String, Object> addApartment(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Apartment apartment) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long adminId = getUserIdFromToken(authHeader);
            if (adminId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            Apartment saved = apartmentService.addApartment(apartment, adminId);

            response.put("code", 200);
            response.put("data", saved);
            response.put("message", "新增成功");
        } catch (Exception e) {
            log.error("新增公寓失败", e);
            response.put("code", 500);
            response.put("message", "新增失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 编辑公寓
     */
    @PutMapping("/edit")
    public Map<String, Object> editApartment(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Apartment apartment) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long adminId = getUserIdFromToken(authHeader);
            if (adminId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            // 可以检查该公寓是否属于当前房东，防止越权
            // 这里简单实现，直接调用service
            Apartment updated = apartmentService.editApartment(apartment);

            response.put("code", 200);
            response.put("data", updated);
            response.put("message", "编辑成功");
        } catch (Exception e) {
            log.error("编辑公寓失败", e);
            response.put("code", 500);
            response.put("message", "编辑失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 删除公寓
     */
    @DeleteMapping("/delete")
    public Map<String, Object> deleteApartment(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long id) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long adminId = getUserIdFromToken(authHeader);
            if (adminId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            apartmentService.deleteApartment(id);

            response.put("code", 200);
            response.put("message", "删除成功");
        } catch (Exception e) {
            log.error("删除公寓失败", e);
            response.put("code", 500);
            response.put("message", "删除失败：" + e.getMessage());
        }
        return response;
    }
    /**
     * 根据邀请码获取公寓信息（用于租客加入）
     */
    @GetMapping("/getByInviteCode")
    public Map<String, Object> getApartmentByInviteCode(@RequestParam String inviteCode) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 根据邀请码查询公寓
            Apartment apartment = apartmentRepository.findByInviteCode(inviteCode);
            if (apartment == null) {
                response.put("code", 404);
                response.put("message", "邀请码无效");
                return response;
            }

            // 查询该公寓的所有房间
            List<Room> rooms = roomRepository.findByApartmentId(apartment.getId());

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("apartment", apartment);
            data.put("rooms", rooms);

            response.put("code", 200);
            response.put("data", data);
        } catch (Exception e) {
            log.error("查询公寓失败", e);
            response.put("code", 500);
            response.put("message", "查询失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 租客加入公寓
     */
    @PostMapping("/join")
    public Map<String, Object> joinApartment(@RequestBody Map<String, Object> joinData,
                                             @RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 从token获取用户ID
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            String inviteCode = (String) joinData.get("inviteCode");
            Integer roomId = (Integer) joinData.get("roomId");

            // 参数校验
            if (inviteCode == null || inviteCode.isEmpty()) {
                response.put("code", 400);
                response.put("message", "邀请码不能为空");
                return response;
            }

            if (roomId == null) {
                response.put("code", 400);
                response.put("message", "请选择房间");
                return response;
            }

            // 1. 根据邀请码查询公寓
            Apartment apartment = apartmentRepository.findByInviteCode(inviteCode);
            if (apartment == null) {
                response.put("code", 404);
                response.put("message", "邀请码无效");
                return response;
            }

            Long apartmentId = apartment.getId(); // 获取公寓ID

            // 2. 查询房间是否存在且空置
            Room room = roomRepository.findById(roomId.longValue()).orElse(null);
            if (room == null) {
                response.put("code", 404);
                response.put("message", "房间不存在");
                return response;
            }

            if (room.getStatus() == 1) {
                response.put("code", 400);
                response.put("message", "该房间已被占用");
                return response;
            }

            if (!room.getApartmentId().equals(apartmentId)) {
                response.put("code", 400);
                response.put("message", "房间不属于该公寓");
                return response;
            }

            // 3. 更新用户信息
            User user = userRepository.findById(userId).orElseThrow(() ->
                    new RuntimeException("用户不存在"));

            // 如果用户之前有房间，需要先释放（可选）
            if (user.getRoomId() != null) {
                Room oldRoom = roomRepository.findById(user.getRoomId()).orElse(null);
                if (oldRoom != null) {
                    oldRoom.setCurrentUserId(null);
                    oldRoom.setStatus(0);
                    roomRepository.save(oldRoom);
                }
            }

            user.setApartmentId(apartmentId);
            user.setRoomId(roomId.longValue());
            userRepository.save(user);

            // 4. 更新房间状态
            room.setCurrentUserId(userId);
            room.setStatus(1);
            roomRepository.save(room);

            // 5. 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("userId", user.getId());
            data.put("apartmentId", apartmentId);
            data.put("apartmentName", apartment.getName());
            data.put("roomId", room.getId());
            data.put("roomNumber", room.getRoomNumber());

            response.put("code", 200);
            response.put("message", "加入成功");
            response.put("data", data);

        } catch (Exception e) {
            log.error("加入公寓失败", e);
            response.put("code", 500);
            response.put("message", "加入失败：" + e.getMessage());
        }

        return response;
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
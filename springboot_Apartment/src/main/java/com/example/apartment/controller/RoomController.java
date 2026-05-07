package com.example.apartment.controller;

import com.example.apartment.entity.Room;
import com.example.apartment.service.RoomService;
import com.example.apartment.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/room")
@CrossOrigin(origins = "*")
public class RoomController {

    private static final Logger log = LoggerFactory.getLogger(RoomController.class);

    @Autowired
    private RoomService roomService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取房间列表
     */
    @GetMapping("/list")
    public Map<String, Object> getRoomList(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long apartmentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            Map<String, Object> data = roomService.getRoomList(apartmentId, page, limit);

            response.put("code", 200);
            response.put("data", data);
        } catch (Exception e) {
            log.error("获取房间列表失败", e);
            response.put("code", 500);
            response.put("message", "获取失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 更新房间
     */
    @PutMapping("/update")
    public Map<String, Object> updateRoom(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Room room) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            Room updated = roomService.updateRoom(room);

            response.put("code", 200);
            response.put("data", updated);
            response.put("message", "更新成功");
        } catch (Exception e) {
            log.error("更新房间失败", e);
            response.put("code", 500);
            response.put("message", "更新失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 新增房间
     */
    @PostMapping("/add")
    public Map<String, Object> addRoom(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Room room) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证权限
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            // 校验参数
            if (room.getRoomNumber() == null || room.getRoomNumber().isEmpty()) {
                response.put("code", 400);
                response.put("message", "房间名称不能为空");
                return response;
            }

            if (room.getArea() == null || room.getArea().doubleValue() <= 0) {
                response.put("code", 400);
                response.put("message", "面积必须大于0");
                return response;
            }

            // 调用service新增房间
            Room savedRoom = roomService.addRoom(room);

            response.put("code", 200);
            response.put("data", savedRoom);
            response.put("message", "新增成功");
        } catch (Exception e) {
            log.error("新增房间失败", e);
            response.put("code", 500);
            response.put("message", "新增失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 删除房间
     */
    @DeleteMapping("/delete")
    public Map<String, Object> deleteRoom(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long id) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            roomService.deleteRoom(id);

            response.put("code", 200);
            response.put("message", "删除成功");
        } catch (Exception e) {
            log.error("删除房间失败", e);
            response.put("code", 500);
            response.put("message", "删除失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 分配租客
     */
    @PostMapping("/assign")
    public Map<String, Object> assignTenant(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long roomId,
            @RequestParam(required = false) Long userId) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long currentUserId = getUserIdFromToken(authHeader);
            if (currentUserId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            Room updated = roomService.assignTenant(roomId, userId);

            response.put("code", 200);
            response.put("data", updated);
            response.put("message", userId != null ? "分配成功" : "已清空租客");
        } catch (Exception e) {
            log.error("分配租客失败", e);
            response.put("code", 500);
            response.put("message", "分配失败：" + e.getMessage());
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
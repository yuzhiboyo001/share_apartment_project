package com.example.apartment.controller;

import com.example.apartment.entity.Message;
import com.example.apartment.entity.User;
import com.example.apartment.repository.MessageRepository;
import com.example.apartment.repository.UserRepository;
import com.example.apartment.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/message")
@CrossOrigin(origins = "*")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 房东发布事务（同时发送消息给租客和房东自己）
     */
    @PostMapping("/publishTask")
    @Transactional
    public Map<String, Object> publishTask(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> taskData) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证房东权限
            Claims claims = jwtUtil.parseToken(authHeader.substring(7));
            Integer userRole = claims.get("role", Integer.class);
            if (userRole != 1) {
                response.put("code", 403);
                response.put("message", "只有房东可以发布事务");
                return response;
            }

            Long landlordId = claims.get("userId", Long.class);
            User landlord = userRepository.findById(landlordId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            Long apartmentId = landlord.getApartmentId();
            if (apartmentId == null) {
                response.put("code", 400);
                response.put("message", "您还没有公寓");
                return response;
            }

            // 获取请求参数
            String title = (String) taskData.get("title");
            String description = (String) taskData.get("description");
            Integer taskType = (Integer) taskData.get("taskType");
            Long assigneeId = null;
            if (taskData.get("assigneeId") != null && !taskData.get("assigneeId").toString().isEmpty()) {
                assigneeId = Long.parseLong(taskData.get("assigneeId").toString());
            }
            String deadline = (String) taskData.get("deadline");

            // 2. 获取公寓所有租客
            List<User> tenants = userRepository.findByApartmentIdAndRole(apartmentId, 0);

            // 3. 为每个租客创建消息
            List<Message> messages = new ArrayList<>();

            // 租客消息
            for (User tenant : tenants) {
                Message msg = new Message();
                msg.setUserId(tenant.getId());
                msg.setMessageType(2); // 事务分配
                msg.setTitle("新事务：" + title);

                String content;
                if (assigneeId != null && assigneeId.equals(tenant.getId())) {
                    content = "您被指定为责任人：" + description;
                } else {
                    content = "公寓有新事务：" + description;
                }
                msg.setContent(content);
                msg.setBizId(null);
                msg.setIsRead(0);
                msg.setCreateTime(LocalDateTime.now());
                messages.add(msg);
            }

            // 4. 为房东自己也创建一条消息（记录发布）
            Message landlordMsg = new Message();
            landlordMsg.setUserId(landlordId);
            landlordMsg.setMessageType(2);
            landlordMsg.setTitle("您发布了一个新事务：" + title);
            landlordMsg.setContent("您发布的事务：" + description);
            landlordMsg.setBizId(null);
            landlordMsg.setIsRead(0);
            landlordMsg.setCreateTime(LocalDateTime.now());
            messages.add(landlordMsg);

            // 批量保存消息
            messageRepository.saveAll(messages);

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("messageCount", messages.size());

            response.put("code", 200);
            response.put("message", "事务发布成功，已通知 " + tenants.size() + " 位租客");
            response.put("data", data);

        } catch (Exception e) {
            log.error("发布事务失败", e);
            response.put("code", 500);
            response.put("message", "发布失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 获取我的消息列表（分页）
     */
    @GetMapping("/list")
    public Map<String, Object> getMyMessages(
            @RequestHeader("Authorization") String authHeader,
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

            Pageable pageable = PageRequest.of(page - 1, limit);
            Page<Message> pageResult = messageRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);

            List<Map<String, Object>> messageList = pageResult.getContent().stream().map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", msg.getId());
                map.put("title", msg.getTitle());
                map.put("content", msg.getContent());
                map.put("messageType", msg.getMessageType());
                map.put("isRead", msg.getIsRead());
                map.put("createTime", msg.getCreateTime());
                map.put("bizId", msg.getBizId());
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("list", messageList);
            data.put("total", pageResult.getTotalElements());
            data.put("totalPages", pageResult.getTotalPages());
            data.put("currentPage", pageResult.getNumber() + 1);

            response.put("code", 200);
            response.put("data", data);

        } catch (Exception e) {
            log.error("获取消息列表失败", e);
            response.put("code", 500);
            response.put("message", "获取失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread/count")
    public Map<String, Object> getUnreadCount(
            @RequestHeader("Authorization") String authHeader) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            long count = messageRepository.countByUserIdAndIsRead(userId, 0);

            response.put("code", 200);
            response.put("data", count);

        } catch (Exception e) {
            log.error("获取未读消息数失败", e);
            response.put("code", 500);
            response.put("message", "获取失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/read/{id}")
    public Map<String, Object> markAsRead(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            Message message = messageRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("消息不存在"));

            // 验证权限
            if (!message.getUserId().equals(userId)) {
                response.put("code", 403);
                response.put("message", "无权操作此消息");
                return response;
            }

            message.setIsRead(1);
            messageRepository.save(message);

            response.put("code", 200);
            response.put("message", "已标记为已读");

        } catch (Exception e) {
            log.error("标记已读失败", e);
            response.put("code", 500);
            response.put("message", "操作失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 一键已读
     */
    @PutMapping("/read/all")
    public Map<String, Object> markAllAsRead(
            @RequestHeader("Authorization") String authHeader) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            messageRepository.markAllAsRead(userId);

            response.put("code", 200);
            response.put("message", "全部已读");

        } catch (Exception e) {
            log.error("一键已读失败", e);
            response.put("code", 500);
            response.put("message", "操作失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            Message message = messageRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("消息不存在"));

            if (!message.getUserId().equals(userId)) {
                response.put("code", 403);
                response.put("message", "无权操作此消息");
                return response;
            }

            messageRepository.delete(message);

            response.put("code", 200);
            response.put("message", "删除成功");

        } catch (Exception e) {
            log.error("删除消息失败", e);
            response.put("code", 500);
            response.put("message", "删除失败：" + e.getMessage());
        }

        return response;
    }

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
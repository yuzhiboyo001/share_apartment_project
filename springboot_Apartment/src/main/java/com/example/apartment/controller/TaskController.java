package com.example.apartment.controller;

import com.example.apartment.entity.Task;
import com.example.apartment.entity.User;
import com.example.apartment.entity.Message;
import com.example.apartment.repository.TaskRepository;
import com.example.apartment.repository.UserRepository;
import com.example.apartment.repository.MessageRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/task")
@CrossOrigin(origins = "*")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建事务（房东或租客均可）
     */
    @PostMapping("/create")
    @Transactional
    public Map<String, Object> createTask(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> taskData) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 获取当前用户
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            Long apartmentId = user.getApartmentId();
            if (apartmentId == null) {
                response.put("code", 400);
                response.put("message", "您还没有加入公寓");
                return response;
            }

            // 获取请求参数
            String title = (String) taskData.get("title");
            String description = (String) taskData.get("description");
            Integer taskType = (Integer) taskData.get("taskType");
            Long assigneeId;
            if (taskData.get("assigneeId") != null && !taskData.get("assigneeId").toString().isEmpty()) {
                assigneeId = Long.parseLong(taskData.get("assigneeId").toString());
            } else {
                assigneeId = null;
            }
            String deadlineStr = (String) taskData.get("deadline");
            LocalDateTime deadline = deadlineStr != null ? LocalDateTime.parse(deadlineStr) : null;

            // 创建事务
            Task task = new Task();
            task.setApartmentId(apartmentId);
            task.setTitle(title);
            task.setDescription(description);
            task.setTaskType(taskType);
            task.setCreatorId(userId);
            task.setAssigneeId(assigneeId);
            task.setDeadline(deadline);
            task.setStatus(0);
            task.setCreateTime(LocalDateTime.now());

            Task savedTask = taskRepository.save(task);

            // 获取公寓所有租客（包括房东自己？这里只给租客发消息，房东自己也能看到事务列表）
            List<User> tenants = userRepository.findByApartmentIdAndRole(apartmentId, 0);

            // 为每个租客创建消息
            List<Message> messages = tenants.stream().map(tenant -> {
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
                msg.setBizId(savedTask.getId());
                msg.setIsRead(0);
                msg.setCreateTime(LocalDateTime.now());
                return msg;
            }).collect(Collectors.toList());

            messageRepository.saveAll(messages);

            response.put("code", 200);
            response.put("message", "事务创建成功");
            response.put("data", savedTask);

        } catch (Exception e) {
            log.error("创建事务失败", e);
            response.put("code", 500);
            response.put("message", "创建失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 获取当前公寓的事务列表（分页）
     */
    @GetMapping("/list")
    public Map<String, Object> getTaskList(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int limit) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            Long apartmentId = user.getApartmentId();
            if (apartmentId == null) {
                response.put("code", 200);
                response.put("data", buildEmptyPage());
                return response;
            }

            Pageable pageable = PageRequest.of(page - 1, limit);
            Page<Task> pageResult;

            if (keyword != null && !keyword.isEmpty()) {
                pageResult = taskRepository.searchByKeyword(apartmentId, keyword, pageable);
            } else {
                pageResult = taskRepository.findByApartmentId(apartmentId, pageable);
            }

            // 获取创建者和责任人的用户名
            List<Long> userIds = pageResult.getContent().stream()
                    .flatMap(task -> java.util.stream.Stream.of(task.getCreatorId(), task.getAssigneeId()))
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());

            Map<Long, String> userNames;
            if (!userIds.isEmpty()) {
                List<User> users = userRepository.findAllById(userIds);
                userNames = users.stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername));
            } else {
                userNames = new HashMap<>();
            }

            List<Map<String, Object>> taskList = pageResult.getContent().stream().map(task -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", task.getId());
                map.put("title", task.getTitle());
                map.put("description", task.getDescription());
                map.put("taskType", task.getTaskType());
                map.put("creatorId", task.getCreatorId());
                map.put("creatorName", userNames.getOrDefault(task.getCreatorId(), "未知"));
                map.put("assigneeId", task.getAssigneeId());
                map.put("assigneeName", task.getAssigneeId() != null ?
                        userNames.getOrDefault(task.getAssigneeId(), "未知") : null);
                map.put("deadline", task.getDeadline());
                map.put("status", task.getStatus());
                map.put("createTime", task.getCreateTime());
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("list", taskList);
            data.put("total", pageResult.getTotalElements());
            data.put("totalPages", pageResult.getTotalPages());
            data.put("currentPage", pageResult.getNumber() + 1);

            response.put("code", 200);
            response.put("data", data);

        } catch (Exception e) {
            log.error("获取事务列表失败", e);
            response.put("code", 500);
            response.put("message", "获取失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 获取事务详情
     */
    @GetMapping("/detail/{id}")
    public Map<String, Object> getTaskDetail(
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

            Task task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("事务不存在"));

            // 权限验证：只有同公寓的用户可查看
            User user = userRepository.findById(userId).orElseThrow();
            if (!task.getApartmentId().equals(user.getApartmentId())) {
                response.put("code", 403);
                response.put("message", "无权查看此事务");
                return response;
            }

            // 获取创建者和责任人姓名
            User creator = userRepository.findById(task.getCreatorId()).orElse(null);
            User assignee = task.getAssigneeId() != null ?
                    userRepository.findById(task.getAssigneeId()).orElse(null) : null;

            Map<String, Object> data = new HashMap<>();
            data.put("id", task.getId());
            data.put("title", task.getTitle());
            data.put("description", task.getDescription());
            data.put("taskType", task.getTaskType());
            data.put("creatorName", creator != null ? creator.getUsername() : "未知");
            data.put("assigneeName", assignee != null ? assignee.getUsername() : null);
            data.put("deadline", task.getDeadline());
            data.put("status", task.getStatus());
            data.put("createTime", task.getCreateTime());

            response.put("code", 200);
            response.put("data", data);

        } catch (Exception e) {
            log.error("获取事务详情失败", e);
            response.put("code", 500);
            response.put("message", "获取失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 更新事务状态
     */
    @PutMapping("/updateStatus")
    @Transactional
    public Map<String, Object> updateTaskStatus(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> statusData) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            Long taskId = Long.parseLong(statusData.get("taskId").toString());
            Integer status = (Integer) statusData.get("status");

            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("事务不存在"));

            // 权限：只有责任人、创建者或房东可以更新状态
            User user = userRepository.findById(userId).orElseThrow();
            boolean isLandlord = user.getRole() == 1;
            boolean isCreator = task.getCreatorId().equals(userId);
            boolean isAssignee = task.getAssigneeId() != null && task.getAssigneeId().equals(userId);

            if (!isLandlord && !isCreator && !isAssignee) {
                response.put("code", 403);
                response.put("message", "无权操作此事务");
                return response;
            }

            task.setStatus(status);
            taskRepository.save(task);

            response.put("code", 200);
            response.put("message", "状态更新成功");

        } catch (Exception e) {
            log.error("更新状态失败", e);
            response.put("code", 500);
            response.put("message", "更新失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 删除事务（仅房东或创建者）
     */
    @DeleteMapping("/delete/{id}")
    @Transactional
    public Map<String, Object> deleteTask(
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

            Task task = taskRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("事务不存在"));

            User user = userRepository.findById(userId).orElseThrow();
            boolean isLandlord = user.getRole() == 1;
            boolean isCreator = task.getCreatorId().equals(userId);

            if (!isLandlord && !isCreator) {
                response.put("code", 403);
                response.put("message", "无权删除此事务");
                return response;
            }

            taskRepository.delete(task);

            response.put("code", 200);
            response.put("message", "删除成功");

        } catch (Exception e) {
            log.error("删除事务失败", e);
            response.put("code", 500);
            response.put("message", "删除失败：" + e.getMessage());
        }
        return response;
    }

    private Map<String, Object> buildEmptyPage() {
        Map<String, Object> data = new HashMap<>();
        data.put("list", new java.util.ArrayList<>());
        data.put("total", 0);
        data.put("totalPages", 0);
        data.put("currentPage", 1);
        return data;
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
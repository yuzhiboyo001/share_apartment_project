package com.example.apartment.service;

import com.example.apartment.entity.Room;
import com.example.apartment.entity.User;
import com.example.apartment.repository.RoomRepository;
import com.example.apartment.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取公寓的房间列表
     */
    public Map<String, Object> getRoomList(Long apartmentId, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<Room> pageResult = roomRepository.findByApartmentId(apartmentId, pageable);

        // 获取所有租客信息用于填充 tenantName
        List<Long> userIds = pageResult.getContent().stream()
                .map(Room::getCurrentUserId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        Map<Long, String> userMap;
        if (!userIds.isEmpty()) {
            List<User> users = userRepository.findAllById(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername));
        } else {
            userMap = new HashMap<>();
        }

        // 转换结果
        List<Map<String, Object>> roomList = pageResult.getContent().stream().map(room -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", room.getId());
            map.put("apartmentId", room.getApartmentId());
            map.put("roomNumber", room.getRoomNumber());
            map.put("area", room.getArea());
            map.put("currentUserId", room.getCurrentUserId());
            map.put("status", room.getStatus());
            map.put("statusText", room.getStatus() == 1 ? "已入住" : "空置");

            // 关键：通过 currentUserId 获取租客姓名
            String tenantName = "未分配";
            if (room.getCurrentUserId() != null && userMap.containsKey(room.getCurrentUserId())) {
                tenantName = userMap.get(room.getCurrentUserId());
            }
            map.put("tenantName", tenantName);

            map.put("createTime", room.getCreateTime());
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", roomList);
        result.put("count", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("currentPage", pageResult.getNumber() + 1);
        return result;
    }
    /**
     * 获取公寓的所有房间（不分页）
     */
    public List<Room> getAllRooms(Long apartmentId) {
        return roomRepository.findByApartmentId(apartmentId);
    }

    /**
     * 更新房间信息
     */
    @Transactional
    public Room updateRoom(Room room) {
        Room existing = roomRepository.findById(room.getId())
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        existing.setRoomNumber(room.getRoomNumber());
        existing.setArea(room.getArea());
        existing.setCurrentUserId(room.getCurrentUserId());

        // 如果有租客，状态设为已入住，否则空置
        existing.setStatus(room.getCurrentUserId() != null ? 1 : 0);

        return roomRepository.save(existing);
    }

    /**
     * 新增房间
     */
    @Transactional
    public Room addRoom(Room room) {
        // 设置创建时间
        room.setCreateTime(LocalDateTime.now());

        // 根据是否有租客设置状态
        room.setStatus(room.getCurrentUserId() != null ? 1 : 0);

        // 检查同一公寓下房间名是否重复
        List<Room> existingRooms = roomRepository.findByApartmentId(room.getApartmentId());
        boolean roomNumberExists = existingRooms.stream()
                .anyMatch(r -> r.getRoomNumber().equals(room.getRoomNumber()));

        if (roomNumberExists) {
            throw new RuntimeException("该公寓已存在相同名称的房间");
        }

        return roomRepository.save(room);
    }
    /**
     * 删除房间
     */
    @Transactional
    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new RuntimeException("房间不存在");
        }
        roomRepository.deleteById(id);
    }

    /**
     * 分配租客
     */
    @Transactional
    public Room assignTenant(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("房间不存在"));

        // 如果该租客已经分配了其他房间，需要先解除
        if (userId != null) {
            Room existingRoom = roomRepository.findById(userId)
                    .orElse(null);
            if (existingRoom != null && !existingRoom.getId().equals(roomId)) {
                existingRoom.setCurrentUserId(null);
                existingRoom.setStatus(0);
                roomRepository.save(existingRoom);
            }
        }

        room.setCurrentUserId(userId);
        room.setStatus(userId != null ? 1 : 0);
        return roomRepository.save(room);
    }
}
package com.example.apartment.service;

import com.example.apartment.entity.Apartment;
import com.example.apartment.entity.Room;
import com.example.apartment.repository.ApartmentRepository;
import com.example.apartment.repository.RoomRepository;
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
import java.util.UUID;

@Service
public class ApartmentService {

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private RoomRepository roomRepository;

    /**
     * 分页查询公寓列表（支持关键字搜索）
     */
    public Map<String, Object> getApartmentList(String keyword, int page, int limit, Long adminId) {
        System.out.println("★★★★★ Service接收page: " + page + ", limit: " + limit);

        Pageable pageable = PageRequest.of(page - 1, limit);
        System.out.println("★★★★★ 创建的Pageable页码: " + pageable.getPageNumber() + " (应该是" + (page-1) + ")");

        Page<Apartment> pageResult;

        if (keyword != null && !keyword.isEmpty()) {
            pageResult = apartmentRepository.search(keyword, pageable);
        } else if (adminId != null) {
            pageResult = apartmentRepository.findByAdminId(adminId, pageable);
        } else {
            pageResult = apartmentRepository.findAll(pageable);
        }

        System.out.println("★★★★★ 查询结果 - 总记录数: " + pageResult.getTotalElements());
        System.out.println("★★★★★ 查询结果 - 总页数: " + pageResult.getTotalPages());
        System.out.println("★★★★★ 查询结果 - 当前页(0起始): " + pageResult.getNumber());
        System.out.println("★★★★★ 查询结果 - 当前页数据条数: " + pageResult.getNumberOfElements());

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getContent());
        result.put("count", pageResult.getTotalElements());
        result.put("totalPages", pageResult.getTotalPages());
        result.put("currentPage", pageResult.getNumber() + 1);
        return result;
    }

    /**
     * 新增公寓
     */
    @Transactional
    public Apartment addApartment(Apartment apartment, Long adminId) {
        apartment.setAdminId(adminId);
        apartment.setCreateTime(LocalDateTime.now());
        if (apartment.getInviteCode() == null || apartment.getInviteCode().isEmpty()) {
            apartment.setInviteCode(generateUniqueInviteCode());
        }
        return apartmentRepository.save(apartment);
    }

    /**
     * 编辑公寓
     */
    @Transactional
    public Apartment editApartment(Apartment apartment) {
        Apartment existing = apartmentRepository.findById(apartment.getId())
                .orElseThrow(() -> new RuntimeException("公寓不存在"));
        existing.setName(apartment.getName());
        existing.setAddress(apartment.getAddress());
        existing.setTotalRooms(apartment.getTotalRooms());
        existing.setDescription(apartment.getDescription());
        existing.setStatus(apartment.getStatus());
        return apartmentRepository.save(existing);
    }

    /**
     * 删除公寓
     */
    @Transactional
    public void deleteApartment(Long id) {
        // 1. 检查公寓是否存在
        if (!apartmentRepository.existsById(id)) {
            throw new RuntimeException("公寓不存在");
        }

        // 2. 检查该公寓下是否有房间
        List<Room> rooms = roomRepository.findByApartmentId(id);
        if (rooms != null && !rooms.isEmpty()) {
            throw new RuntimeException("请先删除该公寓的房间");
        }

        // 3. 没有房间，直接删除公寓
        apartmentRepository.deleteById(id);
    }

    /**
     * 生成唯一的邀请码
     */
    private String generateUniqueInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (apartmentRepository.existsByInviteCode(code));
        return code;
    }
}
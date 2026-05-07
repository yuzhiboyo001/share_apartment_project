package com.example.apartment.controller;

import com.example.apartment.entity.Bill;
import com.example.apartment.entity.Payment;
import com.example.apartment.entity.User;
import com.example.apartment.entity.Room;
import com.example.apartment.repository.BillRepository;
import com.example.apartment.repository.PaymentRepository;
import com.example.apartment.repository.UserRepository;
import com.example.apartment.repository.RoomRepository;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bill/landlord")
@CrossOrigin(origins = "*")
public class LandlordBillController {

    private static final Logger log = LoggerFactory.getLogger(LandlordBillController.class);

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取房东账单列表（支持分页）
     */
    @GetMapping("/list")
    public Map<String, Object> getBillList(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证房东权限
            Claims claims = jwtUtil.parseToken(authHeader.substring(7));
            Integer userRole = claims.get("role", Integer.class);
            if (userRole != 1) {
                response.put("code", 403);
                response.put("message", "无权限访问");
                return response;
            }

            Long userId = claims.get("userId", Long.class);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            Long apartmentId = user.getApartmentId();
            if (apartmentId == null) {
                response.put("code", 200);
                response.put("data", buildEmptyPage());
                return response;
            }

            // 分页查询
            Pageable pageable = PageRequest.of(page - 1, limit);
            Page<Bill> pageResult;

            if (keyword != null && !keyword.isEmpty()) {
                pageResult = billRepository.searchByKeyword(apartmentId, keyword, pageable);
            } else {
                pageResult = billRepository.findByApartmentId(apartmentId, pageable);
            }

            List<Map<String, Object>> billList = pageResult.getContent().stream().map(bill -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", bill.getId());
                map.put("name", bill.getBillName());
                map.put("billType", bill.getBillType());
                map.put("totalAmount", bill.getTotalAmount());
                map.put("billPeriod", bill.getBillPeriod());
                map.put("status", bill.getStatus());
                // 状态文字
                String statusText = bill.getStatus() == 1 ? "已结清" : "待缴费";
                map.put("statusText", statusText);
                // 真实备注
                map.put("remark", bill.getRemark());
                map.put("createTime", bill.getCreateTime());
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("list", billList);
            data.put("total", pageResult.getTotalElements());
            data.put("totalPages", pageResult.getTotalPages());
            data.put("currentPage", pageResult.getNumber() + 1);

            response.put("code", 200);
            response.put("data", data);

        } catch (Exception e) {
            log.error("获取账单列表失败", e);
            response.put("code", 500);
            response.put("message", "获取失败：" + e.getMessage());
        }
        return response;
    }

    /**
     * 创建账单
     */
    @PostMapping("/add")
    @Transactional
    public Map<String, Object> addBill(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Bill bill) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证房东权限
            Claims claims = jwtUtil.parseToken(authHeader.substring(7));
            Integer userRole = claims.get("role", Integer.class);
            if (userRole != 1) {
                response.put("code", 403);
                response.put("message", "只有房东可以创建账单");
                return response;
            }

            Long userId = claims.get("userId", Long.class);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 设置创建信息
            bill.setApartmentId(user.getApartmentId());
            bill.setCreatorId(userId);
            bill.setCreateTime(LocalDateTime.now());

            // 保存账单
            Bill savedBill = billRepository.save(bill);
            log.info("账单创建成功: {}", savedBill.getId());

            // 生成缴费记录
            generatePayments(savedBill);

            response.put("code", 200);
            response.put("message", "账单创建成功");
            response.put("data", savedBill);

        } catch (Exception e) {
            log.error("创建账单失败", e);
            response.put("code", 500);
            response.put("message", "创建失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 编辑账单
     */
    @PutMapping("/edit")
    @Transactional
    public Map<String, Object> editBill(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Bill bill) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证房东权限
            Claims claims = jwtUtil.parseToken(authHeader.substring(7));
            Integer userRole = claims.get("role", Integer.class);
            if (userRole != 1) {
                response.put("code", 403);
                response.put("message", "只有房东可以编辑账单");
                return response;
            }

            // 检查账单是否存在
            Bill existingBill = billRepository.findById(bill.getId())
                    .orElseThrow(() -> new RuntimeException("账单不存在"));

            // 更新账单信息
            existingBill.setBillName(bill.getBillName());
            existingBill.setBillType(bill.getBillType());
            existingBill.setTotalAmount(bill.getTotalAmount());
            existingBill.setBillPeriod(bill.getBillPeriod());
            existingBill.setRemark(bill.getRemark());
            existingBill.setStatus(bill.getStatus());

            Bill updatedBill = billRepository.save(existingBill);

            // 删除旧的缴费记录，重新生成
            paymentRepository.deleteByBillId(bill.getId());
            generatePayments(updatedBill);

            response.put("code", 200);
            response.put("message", "账单更新成功");
            response.put("data", updatedBill);

        } catch (Exception e) {
            log.error("更新账单失败", e);
            response.put("code", 500);
            response.put("message", "更新失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 删除账单
     */
    @DeleteMapping("/delete")
    @Transactional
    public Map<String, Object> deleteBill(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long id) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证房东权限
            Claims claims = jwtUtil.parseToken(authHeader.substring(7));
            Integer userRole = claims.get("role", Integer.class);
            if (userRole != 1) {
                response.put("code", 403);
                response.put("message", "只有房东可以删除账单");
                return response;
            }

            // 检查账单是否存在
            if (!billRepository.existsById(id)) {
                response.put("code", 404);
                response.put("message", "账单不存在");
                return response;
            }

            // 先删除关联的缴费记录
            paymentRepository.deleteByBillId(id);

            // 删除账单
            billRepository.deleteById(id);

            response.put("code", 200);
            response.put("message", "删除成功");

        } catch (Exception e) {
            log.error("删除账单失败", e);
            response.put("code", 500);
            response.put("message", "删除失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 获取账单详情
     */
    @GetMapping("/detail/{billId}")
    public Map<String, Object> getBillDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long billId) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            Bill bill = billRepository.findById(billId)
                    .orElseThrow(() -> new RuntimeException("账单不存在"));

            List<Payment> payments = paymentRepository.findByBillId(billId);

            List<Map<String, Object>> paymentDetails = payments.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("amount", p.getAmount());
                map.put("status", p.getStatus());
                map.put("payTime", p.getPayTime());
                map.put("payMethod", p.getPayMethod());

                User user = userRepository.findById(p.getUserId()).orElse(null);
                if (user != null) {
                    map.put("tenantName", user.getUsername());
                }
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("id", bill.getId());
            data.put("name", bill.getBillName());
            data.put("billType", bill.getBillType());
            data.put("totalAmount", bill.getTotalAmount());
            data.put("billPeriod", bill.getBillPeriod());
            data.put("status", bill.getStatus());
            data.put("statusText", bill.getStatus() == 1 ? "已结清" : "待缴费");
            data.put("remark", bill.getRemark());
            data.put("createTime", bill.getCreateTime());
            data.put("paymentDetails", paymentDetails);

            response.put("code", 200);
            response.put("data", data);

        } catch (Exception e) {
            log.error("查询账单详情失败", e);
            response.put("code", 500);
            response.put("message", "查询失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 获取账单的缴费明细
     */
    @GetMapping("/payments")
    public Map<String, Object> getBillPayments(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Long billId) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            List<Payment> payments = paymentRepository.findByBillId(billId);

            List<Map<String, Object>> paymentList = payments.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("amount", p.getAmount());
                map.put("status", p.getStatus());
                map.put("payTime", p.getPayTime());
                map.put("payMethod", p.getPayMethod());

                User user = userRepository.findById(p.getUserId()).orElse(null);
                if (user != null) {
                    map.put("tenantName", user.getUsername());
                }
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("list", paymentList);
            data.put("total", paymentList.size());

            response.put("code", 200);
            response.put("data", data);

        } catch (Exception e) {
            log.error("获取缴费明细失败", e);
            response.put("code", 500);
            response.put("message", "获取失败：" + e.getMessage());
        }

        return response;
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> buildEmptyPage() {
        Map<String, Object> data = new HashMap<>();
        data.put("list", new ArrayList<>());
        data.put("total", 0);
        data.put("totalPages", 0);
        data.put("currentPage", 1);
        return data;
    }

    private void generatePayments(Bill bill) {
        List<User> tenants = userRepository.findByApartmentIdAndRole(
                bill.getApartmentId(), 0);

        if (tenants.isEmpty()) {
            log.warn("该公寓没有租客，无需生成缴费记录");
            return;
        }

        boolean isRentBill = isRentBill(bill.getBillType());

        List<Payment> payments;
        if (isRentBill) {
            payments = calculateByArea(bill, tenants);
        } else {
            payments = calculateByHead(bill, tenants);
        }

        paymentRepository.saveAll(payments);
        log.info("已生成 {} 条缴费记录", payments.size());
    }

    private List<Payment> calculateByHead(Bill bill, List<User> tenants) {
        int count = tenants.size();
        BigDecimal perPerson = bill.getTotalAmount()
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        List<Payment> payments = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < tenants.size(); i++) {
            User tenant = tenants.get(i);
            BigDecimal amount;

            if (i == tenants.size() - 1) {
                amount = bill.getTotalAmount().subtract(total);
            } else {
                amount = perPerson;
                total = total.add(amount);
            }

            Payment payment = new Payment();
            payment.setBillId(bill.getId());
            payment.setUserId(tenant.getId());
            payment.setAmount(amount);
            payment.setStatus(0);
            payment.setCreateTime(LocalDateTime.now());
            payments.add(payment);
        }
        return payments;
    }

    private List<Payment> calculateByArea(Bill bill, List<User> tenants) {
        List<Room> rooms = roomRepository.findByApartmentId(bill.getApartmentId());
        BigDecimal totalArea = rooms.stream()
                .map(Room::getArea)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payment> payments = new ArrayList<>();
        for (User tenant : tenants) {
            Room room = rooms.stream()
                    .filter(r -> r.getCurrentUserId() != null &&
                            r.getCurrentUserId().equals(tenant.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("租客 " + tenant.getUsername() + " 没有分配房间"));

            BigDecimal amount = bill.getTotalAmount()
                    .multiply(room.getArea())
                    .divide(totalArea, 2, RoundingMode.HALF_UP);

            Payment payment = new Payment();
            payment.setBillId(bill.getId());
            payment.setUserId(tenant.getId());
            payment.setAmount(amount);
            payment.setStatus(0);
            payment.setCreateTime(LocalDateTime.now());
            payments.add(payment);
        }
        return payments;
    }

    private boolean isRentBill(Integer billType) {
        return billType == 4 || billType == 5;
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
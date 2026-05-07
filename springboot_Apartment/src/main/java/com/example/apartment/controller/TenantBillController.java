package com.example.apartment.controller;

import com.example.apartment.entity.Bill;
import com.example.apartment.entity.Payment;
import com.example.apartment.entity.User;
import com.example.apartment.repository.BillRepository;
import com.example.apartment.repository.PaymentRepository;
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
@RequestMapping("/api/bill/tenant")
@CrossOrigin(origins = "*")
public class TenantBillController {

    private static final Logger log = LoggerFactory.getLogger(TenantBillController.class);

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PaymentRepository paymentRepository;


    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取租客个人账单列表
     */
    @GetMapping("/list")
    public Map<String, Object> getMyBills(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String keyword,
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

            // 获取该租客的所有缴费记录
            List<Payment> allPayments = paymentRepository.findByUserId(userId);

            // 过滤和分页
            List<Map<String, Object>> billList = new ArrayList<>();
            for (Payment payment : allPayments) {
                Bill bill = billRepository.findById(payment.getBillId()).orElse(null);
                if (bill == null) continue;

                // 关键字过滤
                if (keyword != null && !keyword.isEmpty()) {
                    if (!bill.getBillName().contains(keyword) &&
                            !bill.getBillPeriod().contains(keyword)) {
                        continue;
                    }
                }

                Map<String, Object> map = new HashMap<>();
                map.put("id", bill.getId());
                map.put("name", bill.getBillName());
                map.put("billType", bill.getBillType());
                map.put("totalAmount", payment.getAmount()); // 个人应缴金额
                map.put("billPeriod", bill.getBillPeriod());
                map.put("status", payment.getStatus());
                String statusText = payment.getStatus() == 1 ? "已结清" : "待缴费";
                map.put("statusText", statusText);
                map.put("payTime", payment.getPayTime());
                map.put("payMethod", payment.getPayMethod());
                map.put("createTime", bill.getCreateTime());
                billList.add(map);
            }

            // 分页
            int total = billList.size();
            int start = (page - 1) * limit;
            int end = Math.min(start + limit, total);
            List<Map<String, Object>> pageList = start < total ? billList.subList(start, end) : new ArrayList<>();

            Map<String, Object> data = new HashMap<>();
            data.put("list", pageList);
            data.put("total", total);
            data.put("totalPages", (int) Math.ceil((double) total / limit));
            data.put("currentPage", page);

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
     * 租客支付账单
     */
    @PostMapping("/pay")
    @Transactional
    public Map<String, Object> payBill(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payData) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                response.put("code", 401);
                response.put("message", "未授权");
                return response;
            }

            Long billId = Long.parseLong(payData.get("billId").toString());
            Integer payMethod = (Integer) payData.get("payMethod");

            Payment payment = paymentRepository.findByBillIdAndUserId(billId, userId);
            if (payment == null) {
                response.put("code", 404);
                response.put("message", "缴费记录不存在");
                return response;
            }

            if (payment.getStatus() == 1) {
                response.put("code", 400);
                response.put("message", "账单已支付");
                return response;
            }

            // 更新支付状态
            payment.setStatus(1);
            payment.setPayTime(LocalDateTime.now());
            payment.setPayMethod(payMethod != null ? payMethod : 1);
            payment.setTransactionId("PAY" + System.currentTimeMillis());

            paymentRepository.save(payment);

            // 检查该账单是否所有租客都已支付
            List<Payment> billPayments = paymentRepository.findByBillId(billId);
            boolean allPaid = billPayments.stream().allMatch(p -> p.getStatus() == 1);

            if (allPaid) {
                Bill bill = billRepository.findById(billId).orElse(null);
                if (bill != null) {
                    bill.setStatus(1);
                    billRepository.save(bill);
                }
            }

            response.put("code", 200);
            response.put("message", "支付成功");

        } catch (Exception e) {
            log.error("支付失败", e);
            response.put("code", 500);
            response.put("message", "支付失败：" + e.getMessage());
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
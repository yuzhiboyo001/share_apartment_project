package com.example.apartment.controller;

import com.example.apartment.entity.Bill;
import com.example.apartment.entity.User;
import com.example.apartment.entity.Payment;
import com.example.apartment.repository.BillRepository;
import com.example.apartment.repository.UserRepository;
import com.example.apartment.repository.PaymentRepository;
import com.example.apartment.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/home")
@CrossOrigin(origins = "*")
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/getTableData")
    public Map<String, Object> getTableData(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 从header中提取token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.put("code", 401);
                response.put("message", "未登录或token无效");
                return response;
            }

            String token = authHeader.substring(7);

            // 验证token
            if (!jwtUtil.validateToken(token)) {
                response.put("code", 401);
                response.put("message", "token无效");
                return response;
            }

            // 解析token
            Claims claims = jwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            Integer userRole = claims.get("role", Integer.class);

            log.info("从token解析 - userId: {}, userRole: {}", userId, userRole);

            // 查询用户完整信息
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            Long apartmentId = user.getApartmentId();
            log.info("用户apartmentId: {}", apartmentId);

            if (apartmentId == null) {
                // 返回空数据
                Map<String, Object> dataWrapper = new HashMap<>();
                dataWrapper.put("tableData", new ArrayList<>());
                response.put("code", 200);
                response.put("data", dataWrapper);
                return response;
            }

            // 根据角色返回不同的数据
            Map<String, Object> dataWrapper;
            if (userRole == 1) {
                // 房东：从bill表获取所有账单
                dataWrapper = getLandlordTableData(apartmentId);
            } else {
                // 租客：从payment表获取个人账单
                dataWrapper = getTenantTableData(userId);
            }

            response.put("code", 200);
            response.put("data", dataWrapper);

        } catch (Exception e) {
            log.error("查询首页表格数据失败", e);
            response.put("code", 500);
            response.put("message", "查询失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 房东视角：从bill表获取所有账单
     */
    private Map<String, Object> getLandlordTableData(Long apartmentId) {
        List<Bill> bills = billRepository.findByApartmentId(apartmentId);

        List<Map<String, Object>> tableData = new ArrayList<>();

        for (Bill bill : bills) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", bill.getBillName());
            row.put("totalAmount", bill.getTotalAmount());
            row.put("billPeriod", bill.getBillPeriod());
            row.put("createTime", bill.getCreateTime());
            // 根据status生成状态文字
            String statusText = bill.getStatus() == 1 ? "已结清" : "待缴费";
            row.put("remark", statusText);
            row.put("remark1", bill.getRemark());
            tableData.add(row);
        }

        Map<String, Object> dataWrapper = new HashMap<>();
        dataWrapper.put("tableData", tableData);

        return dataWrapper;
    }

    /**
     * 租客视角：从payment表获取个人账单
     */
    private Map<String, Object> getTenantTableData(Long userId) {
        // 获取该租客的所有缴费记录
        List<Payment> payments = paymentRepository.findByUserId(userId);

        List<Map<String, Object>> tableData = new ArrayList<>();

        for (Payment payment : payments) {
            // 查询对应的账单信息
            Bill bill = billRepository.findById(payment.getBillId()).orElse(null);
            if (bill == null) continue;

            Map<String, Object> row = new HashMap<>();
            row.put("name", bill.getBillName());
            row.put("totalAmount", payment.getAmount());  // 注意这里用payment的金额，不是bill的
            row.put("billPeriod", bill.getBillPeriod());

            // 支付状态
            String statusText = payment.getStatus() == 1 ? "已结清" : "待缴费";
            row.put("remark", statusText);

            // 可选：如果已支付，显示支付时间
            if (payment.getPayTime() != null) {
                row.put("payTime", payment.getPayTime());
            }

            tableData.add(row);
        }

        Map<String, Object> dataWrapper = new HashMap<>();
        dataWrapper.put("tableData", tableData);

        return dataWrapper;
    }
}
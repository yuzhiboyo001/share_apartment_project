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
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chart")
@CrossOrigin(origins = "*")
public class ChartController {

    private static final Logger log = LoggerFactory.getLogger(ChartController.class);

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取图表数据（根据角色返回不同数据）
     */
    @GetMapping("/getChartData")
    public Map<String, Object> getChartData(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 解析token获取用户信息
            String token = authHeader.substring(7);
            Claims claims = jwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            Integer userRole = claims.get("role", Integer.class);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            Map<String, Object> data = new HashMap<>();

            if (userRole == 1) {
                // 房东视角：公寓整体数据
                data = getLandlordChartData(user.getApartmentId());
            } else {
                // 租客视角：个人数据
                data = getTenantChartData(userId, user.getApartmentId());
            }

            response.put("code", 200);
            response.put("data", data);

        } catch (Exception e) {
            log.error("获取图表数据失败", e);
            response.put("code", 500);
            response.put("message", "获取失败：" + e.getMessage());
        }

        return response;
    }

    /**
     * 房东视角的图表数据
     */
    private Map<String, Object> getLandlordChartData(Long apartmentId) {
        Map<String, Object> data = new HashMap<>();

        // 1. 月度账单趋势图（类似orderData）
        data.put("monthlyBillData", getMonthlyBillTrend(apartmentId));

        // 2. 费用类型占比图（类似videoData）
        data.put("billTypeDistribution", getBillTypeDistribution(apartmentId));

        // 3. 租客缴费情况（类似userData）
        data.put("tenantPaymentStatus", getTenantPaymentStatus(apartmentId));

        // 4. 公寓收支概览
        data.put("apartmentOverview", getApartmentOverview(apartmentId));

        return data;
    }

    /**
     * 租客视角的图表数据
     */
    private Map<String, Object> getTenantChartData(Long userId, Long apartmentId) {
        Map<String, Object> data = new HashMap<>();

        // 1. 个人月度支出趋势
        data.put("personalMonthlyExpense", getPersonalMonthlyExpense(userId));

        // 2. 个人费用类型占比
        data.put("personalBillDistribution", getPersonalBillDistribution(userId));

        // 3. 个人缴费历史
        data.put("personalPaymentHistory", getPersonalPaymentHistory(userId));

        // 4. 公寓人均对比
        data.put("compareWithAverage", getCompareWithAverage(userId, apartmentId));

        return data;
    }

    /**
     * 1. 月度账单趋势（房东）
     * 返回格式类似 orderData
     */
    private Map<String, Object> getMonthlyBillTrend(Long apartmentId) {
        // 获取最近6个月的账单
        List<Bill> bills = billRepository.findByApartmentId(apartmentId);

        // 按月份分组
        Map<String, List<Bill>> billsByMonth = bills.stream()
                .collect(Collectors.groupingBy(Bill::getBillPeriod));

        // 准备日期标签（最近6个月）
        List<String> months = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            months.add(month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }

        // 准备各类费用的月度数据
        List<Map<String, Object>> monthlyData = new ArrayList<>();

        for (String month : months) {
            Map<String, Object> monthData = new HashMap<>();
            List<Bill> monthBills = billsByMonth.getOrDefault(month, new ArrayList<>());

            // 按费用类型汇总
            BigDecimal waterElectric = BigDecimal.ZERO;  // 水电
            BigDecimal gas = BigDecimal.ZERO;            // 燃气
            BigDecimal rent = BigDecimal.ZERO;           // 房租
            BigDecimal other = BigDecimal.ZERO;          // 其他

            for (Bill bill : monthBills) {
                switch (bill.getBillType()) {
                    case 1: waterElectric = waterElectric.add(bill.getTotalAmount()); break;
                    case 2: gas = gas.add(bill.getTotalAmount()); break;
                    case 4: rent = rent.add(bill.getTotalAmount()); break;
                    default: other = other.add(bill.getTotalAmount()); break;
                }
            }

            monthData.put("水电", waterElectric);
            monthData.put("燃气", gas);
            monthData.put("房租", rent);
            monthData.put("其他", other);

            monthlyData.add(monthData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", months);
        result.put("data", monthlyData);

        return result;
    }

    /**
     * 2. 费用类型占比（房东）
     * 返回格式类似 videoData
     */
    private List<Map<String, Object>> getBillTypeDistribution(Long apartmentId) {
        List<Bill> bills = billRepository.findByApartmentId(apartmentId);

        // 按费用类型汇总金额
        Map<Integer, BigDecimal> typeSum = new HashMap<>();
        for (Bill bill : bills) {
            typeSum.merge(bill.getBillType(), bill.getTotalAmount(), BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();

        // 水电费
        if (typeSum.containsKey(1)) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "水电费");
            item.put("value", typeSum.get(1));
            result.add(item);
        }

        // 燃气费
        if (typeSum.containsKey(2)) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "燃气费");
            item.put("value", typeSum.get(2));
            result.add(item);
        }

        // 网费
        if (typeSum.containsKey(3)) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "网费");
            item.put("value", typeSum.get(3));
            result.add(item);
        }

        // 房租
        if (typeSum.containsKey(4)) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "房租");
            item.put("value", typeSum.get(4));
            result.add(item);
        }

        // 物业费
        if (typeSum.containsKey(5)) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "物业费");
            item.put("value", typeSum.get(5));
            result.add(item);
        }

        // 其他
        if (typeSum.containsKey(6)) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", "其他");
            item.put("value", typeSum.get(6));
            result.add(item);
        }

        return result;
    }

    /**
     * 3. 租客缴费情况（房东）
     * 返回格式类似 userData
     */
    private List<Map<String, Object>> getTenantPaymentStatus(Long apartmentId) {
        List<User> tenants = userRepository.findByApartmentIdAndRole(apartmentId, 0);
        List<Bill> bills = billRepository.findByApartmentId(apartmentId);

        List<Map<String, Object>> result = new ArrayList<>();

        for (User tenant : tenants) {
            Map<String, Object> tenantData = new HashMap<>();
            tenantData.put("name", tenant.getUsername());

            // 统计该租客的缴费情况
            List<Payment> payments = paymentRepository.findByUserId(tenant.getId());

            int totalBills = 0;
            int paidBills = 0;
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal paidAmount = BigDecimal.ZERO;

            for (Payment payment : payments) {
                totalBills++;
                totalAmount = totalAmount.add(payment.getAmount());

                if (payment.getStatus() == 1) {
                    paidBills++;
                    paidAmount = paidAmount.add(payment.getAmount());
                }
            }

            tenantData.put("totalBills", totalBills);
            tenantData.put("paidBills", paidBills);
            tenantData.put("unpaidBills", totalBills - paidBills);
            tenantData.put("totalAmount", totalAmount);
            tenantData.put("paidAmount", paidAmount);
            tenantData.put("unpaidAmount", totalAmount.subtract(paidAmount));
            tenantData.put("completionRate", totalBills > 0 ?
                    (paidBills * 100 / totalBills) : 0);

            result.add(tenantData);
        }

        return result;
    }

    /**
     * 4. 公寓收支概览（房东）
     */
    private Map<String, Object> getApartmentOverview(Long apartmentId) {
        List<Bill> bills = billRepository.findByApartmentId(apartmentId);
        List<Payment> allPayments = new ArrayList<>();

        for (Bill bill : bills) {
            allPayments.addAll(paymentRepository.findByBillId(bill.getId()));
        }

        BigDecimal totalBills = bills.stream()
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = allPayments.stream()
                .filter(p -> p.getStatus() == 1)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUnpaid = allPayments.stream()
                .filter(p -> p.getStatus() == 0)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalBills", totalBills);
        overview.put("totalPaid", totalPaid);
        overview.put("totalUnpaid", totalUnpaid);
        overview.put("billCount", bills.size());
        overview.put("paymentCount", allPayments.size());
        overview.put("collectionRate", totalBills.compareTo(BigDecimal.ZERO) > 0 ?
                totalPaid.multiply(new BigDecimal("100")).divide(totalBills, 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);

        return overview;
    }

    /**
     * 租客：个人月度支出趋势
     */
    private List<Map<String, Object>> getPersonalMonthlyExpense(Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);

        // 按月份分组
        Map<String, List<Payment>> byMonth = new HashMap<>();
        for (Payment payment : payments) {
            Bill bill = billRepository.findById(payment.getBillId()).orElse(null);
            if (bill != null) {
                byMonth.computeIfAbsent(bill.getBillPeriod(), k -> new ArrayList<>())
                        .add(payment);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Payment>> entry : byMonth.entrySet()) {
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", entry.getKey());

            BigDecimal total = entry.getValue().stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal paid = entry.getValue().stream()
                    .filter(p -> p.getStatus() == 1)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthData.put("total", total);
            monthData.put("paid", paid);
            monthData.put("unpaid", total.subtract(paid));

            result.add(monthData);
        }

        // 按月份排序
        result.sort((a, b) -> b.get("month").toString().compareTo(a.get("month").toString()));

        return result;
    }

    /**
     * 租客：个人费用类型占比
     */
    private List<Map<String, Object>> getPersonalBillDistribution(Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);

        Map<String, BigDecimal> typeSum = new HashMap<>();

        for (Payment payment : payments) {
            Bill bill = billRepository.findById(payment.getBillId()).orElse(null);
            if (bill != null) {
                String typeName = getBillTypeName(bill.getBillType());
                typeSum.merge(typeName, payment.getAmount(), BigDecimal::add);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : typeSum.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", entry.getKey());
            item.put("value", entry.getValue());
            result.add(item);
        }

        return result;
    }

    /**
     * 租客：个人缴费历史
     */
    private List<Map<String, Object>> getPersonalPaymentHistory(Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Payment payment : payments) {
            Bill bill = billRepository.findById(payment.getBillId()).orElse(null);
            if (bill != null) {
                Map<String, Object> history = new HashMap<>();
                history.put("billName", bill.getBillName());
                history.put("billPeriod", bill.getBillPeriod());
                history.put("amount", payment.getAmount());
                history.put("status", payment.getStatus() == 1 ? "已支付" : "待支付");
                history.put("payTime", payment.getPayTime());
                history.put("payMethod", getPayMethodName(payment.getPayMethod()));
                result.add(history);
            }
        }

        return result;
    }

    /**
     * 租客：与公寓平均值对比
     */
    private Map<String, Object> getCompareWithAverage(Long userId, Long apartmentId) {
        List<User> tenants = userRepository.findByApartmentIdAndRole(apartmentId, 0);
        List<Payment> allPayments = new ArrayList<>();

        for (User tenant : tenants) {
            allPayments.addAll(paymentRepository.findByUserId(tenant.getId()));
        }

        // 计算公寓人均支出
        BigDecimal totalAmount = allPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgAmount = tenants.isEmpty() ? BigDecimal.ZERO :
                totalAmount.divide(BigDecimal.valueOf(tenants.size()), 2, RoundingMode.HALF_UP);

        // 计算个人支出
        BigDecimal personalAmount = paymentRepository.findByUserId(userId).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> compare = new HashMap<>();
        compare.put("personalAmount", personalAmount);
        compare.put("averageAmount", avgAmount);
        compare.put("difference", personalAmount.subtract(avgAmount));
        compare.put("isAboveAverage", personalAmount.compareTo(avgAmount) > 0);

        return compare;
    }

    /**
     * 辅助方法：获取费用类型名称
     */
    private String getBillTypeName(Integer type) {
        switch (type) {
            case 1: return "水电费";
            case 2: return "燃气费";
            case 3: return "网费";
            case 4: return "房租";
            case 5: return "物业费";
            default: return "其他";
        }
    }

    /**
     * 辅助方法：获取支付方式名称
     */
    private String getPayMethodName(Integer method) {
        if (method == null) return "-";
        switch (method) {
            case 1: return "微信";
            case 2: return "支付宝";
            case 3: return "现金";
            default: return "其他";
        }
    }
}
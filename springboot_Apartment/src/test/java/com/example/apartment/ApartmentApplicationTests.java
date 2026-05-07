package com.example.apartment;

import com.example.apartment.controller.ChartController;
import com.example.apartment.dto.LoginRequest;
import com.example.apartment.dto.LoginResponse;
import com.example.apartment.entity.Bill;
import com.example.apartment.entity.Payment;
import com.example.apartment.entity.Room;
import com.example.apartment.entity.User;
import com.example.apartment.repository.*;
import com.example.apartment.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApartmentApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApartmentRepository apartmentRepository;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    private static final Random random = new Random();


    @Autowired
    private ChartController chartController;

    private static final Logger log = LoggerFactory.getLogger(ApartmentApplicationTests.class);

    /**
     * 测试数据库连接
     */
    @Test
    void testConnection() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn).isNotNull();
            System.out.println("✅ 数据库连接成功！");
            System.out.println("数据库: " + conn.getMetaData().getURL());
        }
    }

    /**
     * 使用User实体类查询所有用户
     */
    @Test
    void testFindAllUsers() {
        String sql = "SELECT id, username, phone, email, role, status FROM user";

        List<User> userList = jdbcTemplate.query(sql,
                new BeanPropertyRowMapper<>(User.class));

        System.out.println("✅ 共查询到 " + userList.size() + " 个用户");
        userList.forEach(user ->
                System.out.println("   " + user.getUsername() + " - " +
                        (user.getRole() == 1 ? "管理员" : "租客"))
        );

        assertThat(userList).isNotEmpty();
    }

    /**
     * 简单计数查询
     */
    @Test
    void testCountUsers() {
        String sql = "SELECT COUNT(*) FROM user";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);

        System.out.println("✅ user表总记录数: " + count + " 条");
        assertThat(count).isGreaterThan(0);
    }

    /**
     * 生成BCrypt加密密码（用于初始化用户数据）
     */
    @Test
    void generateBcryptPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 生成密码 123456 的加密结果
        String encodedPassword = encoder.encode("123456");
        System.out.println("========================================");
        System.out.println("原始密码: 123456");
        System.out.println("加密后的密码: " + encodedPassword);

        // 验证
        boolean matches = encoder.matches("123456", encodedPassword);
        System.out.println("密码验证: " + (matches ? "✅ 成功" : "❌ 失败"));
        System.out.println("========================================");

        // 生成多条不同密码的加密结果
        String[] testPasswords = {"admin123", "user123", "12345678"};
        for (String pwd : testPasswords) {
            String encoded = encoder.encode(pwd);
            System.out.println("原始密码: " + pwd + " → 加密后: " + encoded);
        }
    }

    /**
     * 测试管理员登录（手机号: 13800138002, 密码: 123456）
     */
    @Test
    void testAdminLogin() {
        // 构建登录请求
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone("13800138002");
        loginRequest.setPassword("123456");

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 创建HTTP实体
        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        // 发送POST请求
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/user/login",
                request,
                LoginResponse.class
        );

        // 打印请求信息
        System.out.println("\n=========================================");
        System.out.println("🔐 登录测试 - 管理员");
        System.out.println("请求URL: POST /api/user/login");
        System.out.println("请求参数: {phone: \"13800138001\", password: \"123456\"}");
        System.out.println("-----------------------------------------");

        // 验证响应状态码
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("响应状态码: " + response.getStatusCode());

        // 获取响应体
        LoginResponse loginResponse = response.getBody();
        assertThat(loginResponse).isNotNull();

        // 打印响应结果
        System.out.println("响应code: " + loginResponse.getCode());
        System.out.println("响应message: " + loginResponse.getData().getMessage());
        System.out.println("Token: " + loginResponse.getData().getToken());

        // 验证登录成功
        assertThat(loginResponse.getCode()).isEqualTo(200);

        // 打印菜单列表
        System.out.println("\n📋 菜单列表:");
        List<LoginResponse.Menu> menuList = loginResponse.getData().getMenuList();
        for (LoginResponse.Menu menu : menuList) {
            printMenu(menu, 0);
        }
        System.out.println("=========================================\n");
    }

    /**
     * 测试租客登录（手机号: 13800138002, 密码: 123456）
     */
    @Test
    void testTenantLogin() {
        // 构建登录请求
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone("13800138001");
        loginRequest.setPassword("123456");

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 创建HTTP实体
        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        // 发送POST请求
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/user/login",
                request,
                LoginResponse.class
        );

        // 打印请求信息
        System.out.println("\n=========================================");
        System.out.println("🔐 登录测试 - 租客");
        System.out.println("请求URL: POST /api/user/login");
        System.out.println("请求参数: {phone: \"13800138002\", password: \"123456\"}");
        System.out.println("-----------------------------------------");

        // 验证响应状态码
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        System.out.println("响应状态码: " + response.getStatusCode());

        // 获取响应体
        LoginResponse loginResponse = response.getBody();
        assertThat(loginResponse).isNotNull();

        // 打印响应结果
        System.out.println("响应code: " + loginResponse.getCode());
        System.out.println("响应message: " + loginResponse.getData().getMessage());
        System.out.println("Token: " + loginResponse.getData().getToken());

        // 验证登录成功
        assertThat(loginResponse.getCode()).isEqualTo(200);

        // 打印菜单列表
        System.out.println("\n📋 菜单列表:");
        List<LoginResponse.Menu> menuList = loginResponse.getData().getMenuList();
        for (LoginResponse.Menu menu : menuList) {
            printMenu(menu, 0);
        }
        System.out.println("=========================================\n");
    }

    /**
     * 测试登录失败（错误密码）
     */
    @Test
    void testLoginFailed() {
        // 构建登录请求（错误密码）
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone("13800138001");
        loginRequest.setPassword("wrongpassword");

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 创建HTTP实体
        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        // 发送POST请求
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/user/login",
                request,
                LoginResponse.class
        );

        // 打印请求信息
        System.out.println("\n=========================================");
        System.out.println("🔐 登录测试 - 失败场景（错误密码）");
        System.out.println("请求URL: POST /api/user/login");
        System.out.println("请求参数: {phone: \"13800138001\", password: \"wrongpassword\"}");
        System.out.println("-----------------------------------------");

        // 获取响应体
        LoginResponse loginResponse = response.getBody();
        assertThat(loginResponse).isNotNull();

        // 打印响应结果
        System.out.println("响应code: " + loginResponse.getCode());
        System.out.println("响应message: " + loginResponse.getData().getMessage());

        // 验证登录失败
        assertThat(loginResponse.getCode()).isEqualTo(-999);
        System.out.println("=========================================\n");
    }

    /**
     * 测试用户不存在
     */
    @Test
    void testUserNotFound() {
        // 构建登录请求（不存在的手机号）
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setPhone("13900000000");
        loginRequest.setPassword("123456");

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 创建HTTP实体
        HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);

        // 发送POST请求
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/user/login",
                request,
                LoginResponse.class
        );

        // 打印请求信息
        System.out.println("\n=========================================");
        System.out.println("🔐 登录测试 - 失败场景（用户不存在）");
        System.out.println("请求URL: POST /api/user/login");
        System.out.println("请求参数: {phone: \"13900000000\", password: \"123456\"}");
        System.out.println("-----------------------------------------");

        // 获取响应体
        LoginResponse loginResponse = response.getBody();
        assertThat(loginResponse).isNotNull();

        // 打印响应结果
        System.out.println("响应code: " + loginResponse.getCode());
        System.out.println("响应message: " + loginResponse.getData().getMessage());

        // 验证登录失败
        assertThat(loginResponse.getCode()).isEqualTo(-999);
        System.out.println("=========================================\n");
    }

    /**
     * 递归打印菜单（带缩进）
     */
    private void printMenu(LoginResponse.Menu menu, int level) {
        // 缩进
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indent.append("  ");
        }

        // 打印当前菜单
        System.out.println(indent + "• " + menu.getLabel() +
                " (path: " + menu.getPath() +
                ", icon: " + menu.getIcon() +
                ", url: " + menu.getUrl() + ")");

        // 如果有子菜单，递归打印
        if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
            for (LoginResponse.Menu child : menu.getChildren()) {
                printMenu(child, level + 1);
            }
        }
    }
    /**
     * 使用指定token测试首页表格数据接口
     */
    @Test
    void testHomeGetTableDataWithToken() {
        // 你提供的token
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMzgwMDEzODAwMSIsInVzZXJJZCI6MSwicm9sZSI6MSwianRpIjoiZGMwNmVmOGUtNzExZi00NzYzLWE0MzAtZTAzNjZkM2IzZTk0IiwiaWF0IjoxNzcxOTQzMTU1LCJleHAiOjE3NzIwMjk1NTV9.94f-yyePtsn3_Eb4x47s7x3Egd1mKOoMzUEFOlBpuu0";

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);

        // 创建HTTP实体（GET请求不需要body）
        HttpEntity<String> request = new HttpEntity<>(headers);

        // 发送GET请求
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/home/getTableData",
                HttpMethod.GET,
                request,
                Map.class
        );

        // 打印请求信息
        System.out.println("\n=========================================");
        System.out.println("🏠 测试首页表格数据接口");
        System.out.println("请求URL: GET /api/home/getTableData");
        System.out.println("请求Header: Authorization: Bearer " + token.substring(0, 20) + "...");
        System.out.println("-----------------------------------------");

        // 打印响应状态
        System.out.println("响应状态码: " + response.getStatusCode());

        // 获取响应体
        Map<String, Object> responseBody = response.getBody();
        System.out.println("响应body: " + responseBody);

        // 验证响应
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        if (responseBody != null) {
            System.out.println("响应code: " + responseBody.get("code"));

            if (responseBody.get("code").equals(200)) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                System.out.println("数据条数: " + (data != null ? data.size() : 0));

                if (data != null && !data.isEmpty()) {
                    System.out.println("\n📋 账单列表:");
                    for (Map<String, Object> item : data) {
                        System.out.println("  - " + item.get("name") +
                                " | 金额: " + item.get("totalAmount") +
                                " | 周期: " + item.get("billPeriod") +
                                " | 状态: " + item.get("remark"));
                    }
                }
            } else {
                System.out.println("错误信息: " + responseBody.get("message"));
            }
        }

        System.out.println("=========================================\n");
    }
    /**
     * 测试解析Token
     */
    @Test
    void testParseToken() {
        // 你的token
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMzgwMDEzODAwMSIsInVzZXJJZCI6MSwicm9sZSI6MSwianRpIjoiZGMwNmVmOGUtNzExZi00NzYzLWE0MzAtZTAzNjZkM2IzZTk0IiwiaWF0IjoxNzcxOTQzMTU1LCJleHAiOjE3NzIwMjk1NTV9.94f-yyePtsn3_Eb4x47s7x3Egd1mKOoMzUEFOlBpuu0";

        System.out.println("\n========== 开始解析Token ==========");

        // 1. 先验证token是否有效
        boolean isValid = jwtUtil.validateToken(token);
        System.out.println("Token是否有效: " + isValid);

        if (isValid) {
            // 2. 解析token内容
            Claims claims = jwtUtil.parseToken(token);

            System.out.println("\n----- Token内容 -----");
            System.out.println("subject (手机号): " + claims.getSubject());
            System.out.println("userId: " + claims.get("userId"));
            System.out.println("role: " + claims.get("role"));
            System.out.println("jti: " + claims.getId());
            System.out.println("签发时间: " + claims.getIssuedAt());
            System.out.println("过期时间: " + claims.getExpiration());

            // 3. 获取具体值
            Long userId = claims.get("userId", Long.class);
            Integer role = claims.get("role", Integer.class);
            String phone = claims.getSubject();

            System.out.println("\n----- 转换后的值 -----");
            System.out.println("手机号: " + phone);
            System.out.println("用户ID: " + userId + " (类型: " + userId.getClass().getSimpleName() + ")");
            System.out.println("角色: " + role + " (类型: " + role.getClass().getSimpleName() + ")");
        }

        System.out.println("========== 解析完成 ==========\n");
    }

    /**
     * 测试生成新token（用于对比）
     */
    @Test
    void testGenerateToken() {
        String phone = "13800138001";
        Long userId = 1L;
        Integer role = 1;

        String newToken = jwtUtil.generateToken(phone, userId, role);
        System.out.println("\n========== 生成新Token ==========");
        System.out.println("新Token: " + newToken);

        // 解析新token
        Claims claims = jwtUtil.parseToken(newToken);
        System.out.println("新Token内容:");
        System.out.println("  userId: " + claims.get("userId") + " (类型: " + claims.get("userId").getClass().getSimpleName() + ")");
        System.out.println("  role: " + claims.get("role"));
        System.out.println("  subject: " + claims.getSubject());
        System.out.println("================================\n");
    }

    /**
     * 生成大量测试数据（不回滚，真正写入数据库）
     */
    @Test
    @Transactional
    @Rollback(value = false)
    public void generateMassTestData() {
        log.info("========== 开始生成大量测试数据 ==========");

        // 1. 清空现有数据
        paymentRepository.deleteAll();
        billRepository.deleteAll();
        log.info("✅ 已清空payment表和bill表");

        // 2. 生成24个月（2年）的账单数据
        generateBillsForLastTwoYears();
        // 3. 生成所有支付记录
        generateAllPayments();
        // 4. 验证数据
        verifyData();

        log.info("========== 测试数据生成完成 ==========");
    }

    /**
     * 生成过去24个月的账单数据
     */
    private void generateBillsForLastTwoYears() {
        Long apartmentId = 1L;
        LocalDate now = LocalDate.now();

        for (int i = 23; i >= 0; i--) {
            LocalDate billDate = now.minusMonths(i);
            String yearMonth = billDate.getYear() + "-" + String.format("%02d", billDate.getMonthValue());

            log.info("生成 {} 月份账单", yearMonth);

            // 每个月生成4-6个账单
            int billCount = 4 + random.nextInt(3);

            for (int j = 0; j < billCount; j++) {
                // 随机选择账单类型
                int billType = random.nextInt(6) + 1; // 1-6
                String billName = getBillTypeName(billType);

                // 随机金额（100-2000元）
                BigDecimal amount = BigDecimal.valueOf(100 + random.nextInt(1900));

                // 随机状态（70%已结清，30%待缴费）
                int status = random.nextInt(10) < 7 ? 1 : 0;

                // 创建账单
                Bill bill = new Bill();
                bill.setApartmentId(apartmentId);
                bill.setBillName(billName + " - " + yearMonth);
                bill.setBillType(billType);
                bill.setTotalAmount(amount);
                bill.setBillPeriod(yearMonth);
                bill.setCreatorId(1L); // 房东ID
                bill.setStatus(status);
                bill.setRemark(status == 1 ? "已结清" : "待缴费");
                bill.setCreateTime(LocalDateTime.of(
                        billDate.getYear(),
                        billDate.getMonthValue(),
                        15 + random.nextInt(10), // 15-25日
                        10 + random.nextInt(8),  // 10-18点
                        0
                ));

                billRepository.save(bill);
            }
        }

        log.info("✅ 已生成过去24个月的账单数据");
    }

    /**
     * 为所有账单生成payment记录
     */
    private void generateAllPayments() {
        List<Bill> bills = billRepository.findAll();
        log.info("开始为{}个账单生成缴费记录", bills.size());

        for (Bill bill : bills) {
            generatePaymentsForBill(bill);
        }
    }

    /**
     * 为单个账单生成payment记录
     */
    private void generatePaymentsForBill(Bill bill) {
        Long apartmentId = bill.getApartmentId();

        // 获取公寓所有租客（role=0）
        List<User> tenants = userRepository.findByApartmentIdAndRole(apartmentId, 0);

        if (tenants.isEmpty()) {
            log.warn("公寓{}没有租客，跳过账单{}", apartmentId, bill.getId());
            return;
        }

        // 判断是否为租金类账单
        boolean isRentBill = isRentBill(bill.getBillType());

        if (isRentBill) {
            // 租金：按面积分摊
            generateRentPayments(bill, tenants);
        } else {
            // 其他：按人头均摊
            generateOtherPayments(bill, tenants);
        }
    }

    /**
     * 生成租金类账单的缴费记录（按面积）
     */
    private void generateRentPayments(Bill bill, List<User> tenants) {
        Long apartmentId = bill.getApartmentId();

        // 获取所有房间
        List<Room> rooms = roomRepository.findByApartmentId(apartmentId);

        // 计算总面积
        BigDecimal totalArea = rooms.stream()
                .map(Room::getArea)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (User tenant : tenants) {
            // 找到租客的房间
            Room room = rooms.stream()
                    .filter(r -> r.getCurrentUserId() != null &&
                            r.getCurrentUserId().equals(tenant.getId()))
                    .findFirst()
                    .orElse(null);

            if (room == null) {
                continue;
            }

            // 个人金额 = 总金额 * (房间面积 / 总面积)
            BigDecimal amount = bill.getTotalAmount()
                    .multiply(room.getArea())
                    .divide(totalArea, 2, RoundingMode.HALF_UP);

            createPayment(bill, tenant.getId(), amount);
        }
    }

    /**
     * 生成其他账单的缴费记录（按人头）
     */
    private void generateOtherPayments(Bill bill, List<User> tenants) {
        int tenantCount = tenants.size();
        BigDecimal perPerson = bill.getTotalAmount()
                .divide(BigDecimal.valueOf(tenantCount), 2, RoundingMode.HALF_UP);

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < tenants.size(); i++) {
            User tenant = tenants.get(i);

            BigDecimal amount;
            if (i == tenants.size() - 1) {
                // 最后一个人，用总金额减去前面所有人的和
                amount = bill.getTotalAmount().subtract(total);
            } else {
                amount = perPerson;
                total = total.add(amount);
            }

            createPayment(bill, tenant.getId(), amount);
        }
    }

    /**
     * 创建支付记录
     */
    private void createPayment(Bill bill, Long userId, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setBillId(bill.getId());
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setStatus(bill.getStatus());

        // 如果账单已结清，设置支付时间和支付方式
        if (bill.getStatus() == 1) {
            // 支付时间在账单创建后的1-5天内
            LocalDateTime payTime = bill.getCreateTime()
                    .plusDays(1 + random.nextInt(4))
                    .plusHours(random.nextInt(8));

            payment.setPayTime(payTime);
            payment.setPayMethod(random.nextInt(3) + 1); // 1-3
            payment.setTransactionId("TRX" + System.currentTimeMillis() + random.nextInt(1000));
        }

        payment.setCreateTime(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    /**
     * 验证生成的数据
     */
    private void verifyData() {
        long billCount = billRepository.count();
        long paymentCount = paymentRepository.count();

        log.info("========== 数据统计 ==========");
        log.info("总账单数: {} 条", billCount);
        log.info("总缴费记录: {} 条", paymentCount);

        // 按月份统计
        List<Object[]> monthlyStats = billRepository.getMonthlyBillStats(1L);
        log.info("\n月度账单统计:");
        for (Object[] stat : monthlyStats) {
            log.info("  {}: {}个账单, 总额: {}",
                    stat[0], stat[1], stat[2]);
        }

        // 按类型统计
        List<Object[]> typeStats = billRepository.getBillTypeStats(1L);
        log.info("\n费用类型统计:");
        for (Object[] stat : typeStats) {
            log.info("  {}: {}个账单, 总额: {}",
                    getBillTypeName(((Number) stat[0]).intValue()),
                    stat[1], stat[2]);
        }
    }

    /**
     * 测试ChartController接口
     */
    @Test
    @Transactional
    @Rollback(value = false)
    public void testChartController() {
        log.info("========== 测试ChartController接口 ==========");

        try {
            // 模拟一个token（实际测试时需要有效token）
            String token = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMzgwMDEzODAwMiIsInVzZXJJZCI6Miwicm9sZSI6MCwianRpIjoiNDg0MmFhNDQtYjczMS00NWEyLWE4MDItMDIyOTNmYmYzZWE2IiwiaWF0IjoxNzcyMDkyMDM2LCJleHAiOjE3NzIxNzg0MzZ9.zXxZsKGE1cn3tLP8lcxq62HD5vzOjUrLE3UV3CqmhwE"; // 替换为实际的token

            Map<String, Object> response = chartController.getChartData(token);

            log.info("响应code: {}", response.get("code"));

            if (response.get("code").equals(200)) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");

                log.info("\n=== 房东视角图表数据 ===");

                // 月度账单趋势
                Map<String, Object> monthlyData = (Map<String, Object>) data.get("monthlyBillData");
                if (monthlyData != null) {
                    List<String> dates = (List<String>) monthlyData.get("date");
                    List<Map<String, Object>> values = (List<Map<String, Object>>) monthlyData.get("data");
                    log.info("月度趋势 - 月份: {}", dates);
                    log.info("月度趋势 - 数据条数: {}", values.size());
                }

                // 费用类型占比
                List<Map<String, Object>> typeDist = (List<Map<String, Object>>) data.get("billTypeDistribution");
                if (typeDist != null) {
                    log.info("费用类型占比:");
                    for (Map<String, Object> item : typeDist) {
                        log.info("  {}: {}", item.get("name"), item.get("value"));
                    }
                }

                // 租客缴费情况
                List<Map<String, Object>> tenantStatus = (List<Map<String, Object>>) data.get("tenantPaymentStatus");
                if (tenantStatus != null) {
                    log.info("租客缴费情况:");
                    for (Map<String, Object> tenant : tenantStatus) {
                        log.info("  {} - 总应缴: {}, 已缴: {}, 完成率: {}%",
                                tenant.get("name"),
                                tenant.get("totalAmount"),
                                tenant.get("paidAmount"),
                                tenant.get("completionRate"));
                    }
                }

                // 公寓概览
                Map<String, Object> overview = (Map<String, Object>) data.get("apartmentOverview");
                if (overview != null) {
                    log.info("\n公寓概览:");
                    log.info("  总账单金额: {}", overview.get("totalBills"));
                    log.info("  已收金额: {}", overview.get("totalPaid"));
                    log.info("  待收金额: {}", overview.get("totalUnpaid"));
                    log.info("  收缴率: {}%", overview.get("collectionRate"));
                }
            }

        } catch (Exception e) {
            log.error("测试ChartController失败", e);
        }

        log.info("========== ChartController测试完成 ==========");
    }

    /**
     * 判断是否为租金类账单
     */
    private boolean isRentBill(Integer billType) {
        return billType == 4 || billType == 5;
    }

    /**
     * 获取费用类型名称
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
     * 测试分页接口：验证第1页和第2页返回的数据不同
     */
    @Test
    public void testApartmentPagination() {
        // 先确保数据库中有足够的数据（至少20条）
        long count = apartmentRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(20)
                .withFailMessage("数据库公寓数据不足20条，请先插入测试数据");

        // 你的token
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMzgwMDEzODAwMSIsInVzZXJJZCI6MSwicm9sZSI6MSwianRpIjoiYThmYjE3OWYtOWM5YS00ZDlmLTg2ZjgtZTcyY2I5M2VkMTMzIiwiaWF0IjoxNzcyMTM5ODM4LCJleHAiOjE3NzIyMjYyMzh9.yBTuXWv1UiEEACV_gAmdA-X4bBBzxiAIV6LuT9q5ITU";

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // 定义请求URL
        String url = "/api/apartment/list?page={page}&limit=10";

        // 请求第1页
        ResponseEntity<Map<String, Object>> responsePage1 = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,  // 使用带header的entity
                new ParameterizedTypeReference<Map<String, Object>>() {},
                1
        );

        // 请求第2页
        ResponseEntity<Map<String, Object>> responsePage2 = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,  // 使用带header的entity
                new ParameterizedTypeReference<Map<String, Object>>() {},
                2
        );

        // 检查响应状态
        assertThat(responsePage1.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(responsePage2.getStatusCode().is2xxSuccessful()).isTrue();

        // 打印响应体查看实际返回内容
        System.out.println("第1页响应: " + responsePage1.getBody());
        System.out.println("第2页响应: " + responsePage2.getBody());

        // 解析数据
        Map<String, Object> body1 = responsePage1.getBody();
        Map<String, Object> body2 = responsePage2.getBody();

        // 提取 data 部分（根据你的返回结构）
        Map<String, Object> data1 = (Map<String, Object>) body1.get("data");
        Map<String, Object> data2 = (Map<String, Object>) body2.get("data");

        List<Map<String, Object>> list1 = (List<Map<String, Object>>) data1.get("list");
        List<Map<String, Object>> list2 = (List<Map<String, Object>>) data2.get("list");

        // 打印信息
        System.out.println("第1页数据条数：" + list1.size());
        System.out.println("第2页数据条数：" + list2.size());

        if (list1.size() > 0 && list2.size() > 0) {
            // 获取第一页第一条的id
            Long firstIdPage1 = ((Number) list1.get(0).get("id")).longValue();
            Long firstIdPage2 = ((Number) list2.get(0).get("id")).longValue();

            System.out.println("第1页第一条id：" + firstIdPage1);
            System.out.println("第2页第一条id：" + firstIdPage2);

            // 断言：第一页和第二页的第一条id应该不同
            assertThat(firstIdPage1).isNotEqualTo(firstIdPage2);
        } else {
            System.out.println("警告：某一页没有数据");
        }
    }

    /**
     * 测试分页接口：验证当前页码返回正确
     */
    @Test
    public void testCurrentPageField() {
        // 你的token
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMzgwMDEzODAwMSIsInVzZXJJZCI6MSwicm9sZSI6MSwianRpIjoiYThmYjE3OWYtOWM5YS00ZDlmLTg2ZjgtZTcyY2I5M2VkMTMzIiwiaWF0IjoxNzcyMTM5ODM4LCJleHAiOjE3NzIyMjYyMzh9.yBTuXWv1UiEEACV_gAmdA-X4bBBzxiAIV6LuT9q5ITU";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        String url = "/api/apartment/list?page=2&limit=10";

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> body = response.getBody();
        System.out.println("响应body: " + body);

        Map<String, Object> data = (Map<String, Object>) body.get("data");
        Integer currentPage = (Integer) data.get("currentPage");

        System.out.println("返回的currentPage：" + currentPage);

        // 期望currentPage为2
        assertThat(currentPage).isEqualTo(2);
    }

}
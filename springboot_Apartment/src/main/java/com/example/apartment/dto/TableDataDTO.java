package com.example.apartment.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 首页表格数据DTO
 * 对应前端tableData的字段
 */
@Data
public class TableDataDTO {
    private String name;        // 账单名称 (bill_name)
    private BigDecimal totalAmount;  // 总金额 (total_amount)
    private String billPeriod;  // 账单日期 (bill_period)
    private String remark;      // 账单状态/备注 (remark)

    // 或者如果remark不是状态，可以用status转换
    private String status;      // 账单状态（如果需要）
}
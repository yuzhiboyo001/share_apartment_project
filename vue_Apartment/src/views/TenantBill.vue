<template>
  <div class="bill-container">
    <!-- 头部：搜索框（租客只有搜索功能） -->
    <div class="bill-header">
      <div class="header-left">
        <span class="page-title">我的账单</span>
      </div>
      <el-form :inline="true" :model="searchForm">
        <el-form-item>
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入账单名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格区域 -->
    <div class="bill-table">
      <el-table :data="tableData" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="name" label="账单名称" min-width="150" />
        
        <el-table-column label="应缴金额" width="150">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: bold"
              >￥{{ formatAmount(row.totalAmount) }}</span
            >
          </template>
        </el-table-column>
        
        <el-table-column prop="billPeriod" label="账单周期" width="120" />
        
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.statusText === '已结清' ? 'success' : 'danger'">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="payTime" label="支付时间" width="180" align="center">
          <template #default="{ row }">
            {{ row.payTime ? formatDateTime(row.payTime) : '未支付' }}
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column fixed="right" label="操作" width="150" align="center">
          <template #default="scope">
            <div class="action-buttons">
              <!-- 待缴费状态显示缴费按钮 -->
              <el-button
                v-if="scope.row.statusText === '待缴费'"
                type="danger"
                size="small"
                @click="handlePayment(scope.row)"
              >
                去缴费
              </el-button>
              <!-- 已结清状态显示查看按钮 -->
              <el-button
                v-else
                type="success"
                size="small"
                @click="handleViewDetail(scope.row)"
              >
                查看
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        class="pager"
        background
        layout="total, prev, pager, next, jumper"
        :total="config.total"
        :page-size="config.limit"
        v-model:current-page="config.page"
        @current-change="handleChange"
      />
    </div>

    <!-- 缴费对话框 -->
    <el-dialog
      v-model="paymentDialogVisible"
      title="缴费确认"
      width="380px"
      class="payment-dialog"
      :close-on-click-modal="false"
      :show-close="true"
    >
      <div class="payment-dialog-content" v-if="currentBill">
        <div class="payment-icon">
          <el-icon :size="40" color="#f56c6c"><Money /></el-icon>
        </div>

        <div class="bill-detail-card">
          <div class="bill-detail-item">
            <span class="detail-label">账单名称：</span>
            <span class="detail-value">{{ currentBill.name }}</span>
          </div>
          <div class="bill-detail-item">
            <span class="detail-label">账单周期：</span>
            <span class="detail-value">{{ currentBill.billPeriod }}</span>
          </div>
          <div class="bill-detail-item highlight">
            <span class="detail-label">应缴金额：</span>
            <span class="detail-value amount"
              >￥{{ formatAmount(currentBill.totalAmount) }}</span
            >
          </div>
        </div>

        <div class="payment-tip">
          <el-icon :size="16" color="#909399"><Warning /></el-icon>
          <span>请确认账单信息无误后点击确认支付</span>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="paymentDialogVisible = false">取消</el-button>
          <el-button
            type="danger"
            @click="confirmPayment"
            :loading="paymentLoading"
          >
            确认支付
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 账单详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="账单详情"
      width="40%"
      :before-close="() => (detailDialogVisible = false)"
    >
      <div v-if="currentBill" class="bill-detail">
        <div class="detail-header">
          <h3>{{ currentBill.name }}</h3>
          <el-tag
            :type="currentBill.statusText === '已结清' ? 'success' : 'info'"
            size="large"
          >
            {{ currentBill.statusText }}
          </el-tag>
        </div>

        <el-descriptions :column="1" border class="detail-content">
          <el-descriptions-item label="账单周期">
            {{ currentBill.billPeriod }}
          </el-descriptions-item>
          <el-descriptions-item label="应缴金额">
            <span style="color: #f56c6c; font-weight: bold"
              >￥{{ formatAmount(currentBill.totalAmount) }}</span
            >
          </el-descriptions-item>
          <el-descriptions-item label="支付状态">
            {{ currentBill.statusText }}
          </el-descriptions-item>
          <el-descriptions-item label="支付时间" v-if="currentBill.payTime">
            {{ formatDateTime(currentBill.payTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="支付方式" v-if="currentBill.payMethod">
            {{ getPayMethodText(currentBill.payMethod) }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(currentBill.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关 闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Money, Warning } from "@element-plus/icons-vue";

const { proxy } = getCurrentInstance();

const loading = ref(false);
const paymentDialogVisible = ref(false);
const detailDialogVisible = ref(false);
const paymentLoading = ref(false);
const currentBill = ref(null);

// 搜索表单
const searchForm = reactive({
  keyword: "",
});

// 分页配置
const config = reactive({
  keyword: "",
  total: 0,
  page: 1,
  limit: 10,
});

// 表格数据
const tableData = ref([]);

// 格式化金额
const formatAmount = (amount) => {
  if (!amount) return "0.00";
  return Number(amount).toFixed(2);
};

// 格式化日期时间
const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return "";
  const date = new Date(dateTimeStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
};

// 获取支付方式文本
const getPayMethodText = (method) => {
  const map = {
    1: "微信支付",
    2: "支付宝支付",
    3: "现金支付",
  };
  return map[method] || "未知";
};

// 获取账单列表
const getBillData = async () => {
  loading.value = true;
  try {
    const params = {
      keyword: config.keyword,
      page: config.page,
      limit: config.limit,
    };
    
    const result = await proxy.$api.getTenantBillList(params);
    
    if (result && result.list) {
      tableData.value = result.list;
      config.total = result.total || 0;
    }
  } catch (error) {
    console.error("获取账单列表失败:", error);
    ElMessage.error("获取数据失败");
  } finally {
    loading.value = false;
  }
};

// 处理搜索
const handleSearch = () => {
  config.keyword = searchForm.keyword;
  config.page = 1;
  getBillData();
};

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = "";
  config.keyword = "";
  config.page = 1;
  getBillData();
};

// 分页变化
const handleChange = (page) => {
  config.page = page;
  getBillData();
};

// 处理缴费
const handlePayment = (row) => {
  currentBill.value = row;
  paymentDialogVisible.value = true;
};

// 确认支付
const confirmPayment = async () => {
  paymentLoading.value = true;
  try {
    // 调用支付接口
    await proxy.$api.payTenantBill({
      billId: currentBill.value.id,
      payMethod: 1, // 默认微信支付
    });
    
    ElMessage.success("支付成功");
    paymentDialogVisible.value = false;
    
    // 刷新列表
    getBillData();
  } catch (error) {
    console.error("支付失败:", error);
    ElMessage.error("支付失败");
  } finally {
    paymentLoading.value = false;
  }
};

// 查看详情
const handleViewDetail = (row) => {
  currentBill.value = row;
  detailDialogVisible.value = true;
};

onMounted(() => {
  getBillData();
});
</script>

<style scoped lang="less">
.bill-container {
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
}

.bill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  .header-left {
    .page-title {
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }
  }
}

.bill-table {
  position: relative;
  min-height: 500px;

  .el-table {
    width: 100%;
    min-height: 450px;

    :deep(.el-table__cell) {
      padding: 8px 0;
    }

    .action-buttons {
      display: flex;
      justify-content: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    :deep(.el-button) {
      padding: 5px 10px;
      margin: 0;
    }
  }

  .pager {
    margin-top: 20px;
    text-align: right;
  }
}

.bill-detail {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 10px;
    border-bottom: 1px solid #f0f0f0;

    h3 {
      margin: 0;
      font-size: 18px;
      color: #333;
    }
  }

  .detail-content {
    margin-bottom: 20px;
  }
}

.payment-dialog {
  .payment-dialog-content {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .payment-icon {
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, #fff5f5 0%, #ffeaea 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 20px;
    box-shadow: 0 4px 12px rgba(245, 108, 108, 0.15);
  }

  .bill-detail-card {
    width: 100%;
    background: #f9f9f9;
    border-radius: 12px;
    padding: 18px 20px;
    margin-bottom: 20px;
    border: 1px solid #f0f0f0;

    .bill-detail-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px dashed #e0e0e0;

      &.highlight {
        padding: 16px 0 8px 0;
        border-bottom: none;
        border-top: 2px solid #ffd7d7;
      }

      .detail-label {
        color: #666;
        font-size: 14px;
        min-width: 80px;
      }

      .detail-value {
        color: #333;
        font-weight: 500;
        font-size: 15px;
        text-align: right;

        &.amount {
          color: #f56c6c;
          font-size: 26px;
          font-weight: bold;
        }
      }
    }
  }

  .payment-tip {
    display: flex;
    align-items: center;
    gap: 8px;
    background: #f5f7fa;
    padding: 12px 16px;
    border-radius: 8px;
    width: 100%;
    color: #909399;
    font-size: 13px;
  }

  .dialog-footer {
    display: flex;
    gap: 12px;
    padding: 0 10px 20px;

    .el-button {
      flex: 1;
      border-radius: 30px;
      height: 42px;
    }
  }
}

// 响应式调整
@media (max-width: 1366px) {
  .bill-table {
    :deep(.el-table__cell) {
      padding: 6px 0;
    }

    .action-buttons {
      gap: 4px;
    }

    :deep(.el-button) {
      padding: 4px 8px;
      font-size: 12px;
    }
  }
}
</style>
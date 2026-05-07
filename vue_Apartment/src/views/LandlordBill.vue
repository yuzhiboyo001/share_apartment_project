<template>
  <div class="bill-container">
    <!-- 头部：统一为 user-header 样式（与公寓管理一致） -->
    <div class="user-header">
      <el-button type="primary" @click="handleAdd">新增账单</el-button>
      <el-form :inline="true" :model="searchForm">
        <el-form-item>
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入账单名称或周期"
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

    <!-- 表格区域：统一为 table 样式（与公寓管理一致） -->
    <div class="table">
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="账单名称" min-width="180" />
        
        <el-table-column label="总金额" width="120">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: bold"
              >￥{{ formatAmount(row.totalAmount) }}</span
            >
          </template>
        </el-table-column>
        
        <el-table-column prop="billPeriod" label="账单周期" width="100" align="center" />
        
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.statusText === '已结清' ? 'success' : 'danger'" size="default">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="160" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="remark" label="备注" min-width="200" :show-overflow-tooltip="true" align="center">
          <template #default="{ row }">
            {{ row.remark || '无' }}
          </template>
        </el-table-column>
        
        <el-table-column fixed="right" label="操作" width="250" align="center">
          <template #default="scope">
            <div class="action-buttons">
              <el-button type="primary" size="default" @click="handleEdit(scope.row)">
                编辑
              </el-button>
              <el-button type="success" size="default" @click="handleViewDetails(scope.row)">
                详情
              </el-button>
              <el-button type="danger" size="default" @click="handleDelete(scope.row)">
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页：统一为 pager 样式（与公寓管理一致） -->
      <el-pagination
        class="pager"
        background
        layout="prev, pager, next, total"
        :total="config.total"
        :page-size="config.limit"
        v-model:current-page="config.page"
        @current-change="handleChange"
      />
    </div>

    <!-- 账单表单对话框（新增/编辑）- 保持原有样式 -->
    <el-dialog
      v-model="dialogVisible"
      :title="action === 'add' ? '新增账单' : '编辑账单'"
      width="40%"
      :before-close="handleClose"
      destroy-on-close
    >
      <el-form
        ref="billFormRef"
        :model="billForm"
        :rules="billRules"
        label-width="100px"
      >
        <el-form-item label="账单名称" prop="billName">
          <el-input v-model="billForm.billName" placeholder="请输入账单名称" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="账单类型" prop="billType">
              <el-select
                v-model="billForm.billType"
                placeholder="请选择类型"
                style="width: 100%"
              >
                <el-option :value="1" label="水电费" />
                <el-option :value="2" label="燃气费" />
                <el-option :value="3" label="网费" />
                <el-option :value="4" label="房租" />
                <el-option :value="5" label="物业费" />
                <el-option :value="6" label="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="总金额" prop="totalAmount">
              <el-input-number
                v-model="billForm.totalAmount"
                :precision="2"
                :min="0.01"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="账单周期" prop="billPeriod">
              <el-input
                v-model="billForm.billPeriod"
                placeholder="例如：2026-03"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select
                v-model="billForm.status"
                placeholder="请选择状态"
                style="width: 100%"
              >
                <el-option :value="0" label="待缴费" />
                <el-option :value="1" label="已结清" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="billForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="onSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 账单详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="账单详情"
      width="45%"
      :before-close="() => (detailDialogVisible = false)"
    >
      <div v-if="currentBill" class="bill-detail">
        <div class="detail-header">
          <h3>{{ currentBill.name }}</h3>
          <el-tag
            :type="currentBill.statusText === '已结清' ? 'success' : 'danger'"
            size="large"
          >
            {{ currentBill.statusText }}
          </el-tag>
        </div>

        <el-descriptions :column="2" border class="detail-content">
          <el-descriptions-item label="账单周期">{{
            currentBill.billPeriod
          }}</el-descriptions-item>
          <el-descriptions-item label="总金额">
            <span style="color: #f56c6c; font-weight: bold"
              >￥{{ formatAmount(currentBill.totalAmount) }}</span
            >
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{
            formatDateTime(currentBill.createTime)
          }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{
            currentBill.remark || "无"
          }}</el-descriptions-item>
        </el-descriptions>

        <!-- 租客缴费明细 -->
        <div v-if="paymentDetails.length > 0" class="payment-details">
          <h4>缴费明细</h4>
          <el-table :data="paymentDetails" size="small" border>
            <el-table-column prop="tenantName" label="租客" width="120" />
            <el-table-column prop="amount" label="金额" width="120">
              <template #default="{ row }"
                >￥{{ formatAmount(row.amount) }}</template
              >
            </el-table-column>
            <el-table-column prop="payTime" label="缴费时间" width="180">
              <template #default="{ row }">{{
                formatDateTime(row.payTime)
              }}</template>
            </el-table-column>
            <el-table-column prop="payMethod" label="支付方式">
              <template #default="{ row }">
                {{
                  row.payMethod === 1
                    ? "微信"
                    : row.payMethod === 2
                      ? "支付宝"
                      : "现金"
                }}
              </template>
            </el-table-column>
          </el-table>
        </div>
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
import { ref, reactive, onMounted, getCurrentInstance, nextTick } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";

const { proxy } = getCurrentInstance();

const loading = ref(false);
const action = ref("add");
const dialogVisible = ref(false);
const detailDialogVisible = ref(false);
const currentBill = ref(null);
const paymentDetails = ref([]);

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

// 账单表单
const billForm = reactive({
  id: null,
  billName: "",
  billType: 1,
  totalAmount: 0,
  billPeriod: "",
  status: 0,
  remark: "",
});

// 表单校验规则
const billRules = {
  billName: [{ required: true, message: "请输入账单名称", trigger: "blur" }],
  billType: [{ required: true, message: "请选择账单类型", trigger: "change" }],
  totalAmount: [{ required: true, message: "请输入总金额", trigger: "blur" }],
  billPeriod: [
    { required: true, message: "请输入账单周期", trigger: "blur" },
    { pattern: /^\d{4}-\d{2}$/, message: "格式必须为YYYY-MM", trigger: "blur" },
  ],
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
};

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

// 获取账单列表
const getBillData = async () => {
  loading.value = true;
  try {
    const params = {
      keyword: config.keyword,
      page: config.page,
      limit: config.limit,
    };
    
    const result = await proxy.$api.getLandlordBillList(params);
    
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

// 新增账单
const handleAdd = () => {
  action.value = "add";
  dialogVisible.value = true;
  Object.assign(billForm, {
    id: null,
    billName: "",
    billType: 1,
    totalAmount: 0,
    billPeriod: "",
    status: 0,
    remark: "",
  });
};

// 编辑账单
const handleEdit = (row) => {
  action.value = "edit";
  dialogVisible.value = true;
  nextTick(() => {
    Object.assign(billForm, {
      id: row.id,
      billName: row.name,
      billType: row.billType,
      totalAmount: row.totalAmount,
      billPeriod: row.billPeriod,
      status: row.status,
      remark: row.remark,
    });
  });
};

// 查看详情
const handleViewDetails = async (row) => {
  currentBill.value = row;

  try {
    const result = await proxy.$api.getBillPayments({ billId: row.id });
    paymentDetails.value = result?.list || [];
  } catch (error) {
    console.error("获取缴费明细失败:", error);
    paymentDetails.value = [];
  }

  detailDialogVisible.value = true;
};

// 删除账单
const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除账单 "${row.name}" 吗？`)
    .then(async () => {
      try {
        await proxy.$api.deleteLandlordBill({ id: row.id });
        ElMessage.success("删除成功");
        getBillData();
      } catch (error) {
        ElMessage.error("删除失败");
      }
    })
    .catch(() => {});
};

// 提交表单
const onSubmit = () => {
  proxy.$refs.billFormRef.validate(async (valid) => {
    if (valid) {
      try {
        let res;
        if (action.value === "add") {
          res = await proxy.$api.addLandlordBill({
            billName: billForm.billName,
            billType: billForm.billType,
            totalAmount: billForm.totalAmount,
            billPeriod: billForm.billPeriod,
            status: billForm.status,
            remark: billForm.remark,
          });
          ElMessage.success("新增账单成功");
        } else {
          res = await proxy.$api.editLandlordBill({
            id: billForm.id,
            billName: billForm.billName,
            billType: billForm.billType,
            totalAmount: billForm.totalAmount,
            billPeriod: billForm.billPeriod,
            status: billForm.status,
            remark: billForm.remark,
          });
          ElMessage.success("编辑账单成功");
        }

        if (res) {
          dialogVisible.value = false;
          getBillData();
        }
      } catch (error) {
        ElMessage.error("操作失败");
      }
    }
  });
};

// 处理关闭
const handleClose = () => {
  dialogVisible.value = false;
  proxy.$refs.billFormRef?.resetFields();
};

onMounted(() => {
  getBillData();
});
</script>

<style scoped lang="less">
// 统一使用公寓管理的样式
.user-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.table {
  position: relative;
  min-height: 520px;

  .el-table {
    width: 100%;
    min-height: 500px;

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

      & + .el-button {
        margin-left: 0;
      }
    }
  }

  .pager {
    margin-top: 20px;
    text-align: right;
  }
}

// 响应式调整
@media (max-width: 1366px) {
  .table {
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

// 以下是账单组件特有的样式（保持不变）
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

  .payment-details {
    h4 {
      margin: 20px 0 15px;
      font-size: 16px;
      color: #333;
    }
  }
}
</style>
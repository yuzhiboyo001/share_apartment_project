<script setup>
import {
  ref,
  reactive,
  getCurrentInstance,
  onMounted,
  onUnmounted,
  watch,
  computed,
} from "vue";
import * as echarts from "echarts";
import { ElMessage } from "element-plus";

const { proxy } = getCurrentInstance();

// ==================== 工具函数 ====================
const formatDate = (dateStr) => {
  if (!dateStr) return "未知";
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
};

const formatAmount = (amount) => {
  if (!amount) return "0.00";
  return Number(amount).toFixed(2);
};

const getImageUrl = (user) => {
  return new URL(`../assets/images/${user}.webp`, import.meta.url).href;
};

const getCarouselImageUrl = (picture) => {
  return new URL(`../assets/images/${picture}.png`, import.meta.url).href;
};

// ==================== 轮播图逻辑 ====================
const carouselImages = ref([
  {
    id: 1,
    name: "picture1",
    link: "https://www.bilibili.com/video/BV1qyHnzbEYY/",
  },
  {
    id: 2,
    name: "picture2",
    link: "https://www.bilibili.com/video/BV174eFzeE5H/",
  },
  {
    id: 3,
    name: "picture3",
    link: "https://www.bilibili.com/video/BV1go5Dz7EKQ/",
  },
  {
    id: 4,
    name: "picture4",
    link: "https://www.bilibili.com/video/BV1C9xvzWEcA/",
  },
]);

const handleCarouselClick = (item) => {
  if (item.link) window.open(item.link, "_blank");
};

// ==================== 用户状态 ====================
const userRole = ref(localStorage.getItem("role") || "0");
const username = ref(localStorage.getItem("name") || "用户");
const hasApartment = ref(localStorage.getItem("hasApartment") !== "null");

// 判断是否首次登录（创建时间与当前时间相差小于24小时）
const isFirstLogin = computed(() => {
  const createTime = localStorage.getItem("createTime");
  if (!createTime) return false;
  return Date.now() - new Date(createTime) < 24 * 60 * 60 * 1000;
});

// ==================== 对话框控制 ====================
const showFirstLoginDialog = ref(false);
const showJoinDialog = ref(false);
const paymentDialogVisible = ref(false);

const closeFirstLoginDialog = () => {
  showFirstLoginDialog.value = false;
  localStorage.setItem("isFirstLogin", "false");
};

// ==================== 加入公寓逻辑 ====================
const step = ref(1);
const joining = ref(false);
const apartmentInfo = ref(null);
const apartmentRooms = ref([]);

const joinForm = reactive({
  inviteCode: "",
  roomId: "",
});

const joinRules = {
  inviteCode: [
    { required: true, message: "请输入邀请码", trigger: "blur" },
    { min: 6, max: 6, message: "邀请码必须为6位", trigger: "blur" },
  ],
  roomId: [{ required: true, message: "请选择房间", trigger: "change" }],
};

// 计算可用房间
const availableRooms = computed(() => {
  return apartmentRooms.value.filter((room) => room.status === 0);
});

// 根据邀请码查询公寓房间
const fetchApartmentRooms = async (inviteCode) => {
  try {
    const res = await proxy.$api.getApartmentByInviteCode({ inviteCode });
    if (res?.rooms) {
      apartmentRooms.value = res.rooms.map((room) => ({
        ...room,
        displayLabel: room.status === 1 ? `${room.roomNumber} (已入住)` : room.roomNumber,
        disabled: room.status === 1,
      }));
      if (res.apartment) ElMessage.success(`找到公寓：${res.apartment.name}`);
    } else {
      apartmentRooms.value = [];
      ElMessage.warning("该公寓暂无房间");
    }
  } catch (error) {
    console.error("获取房间失败:", error);
    apartmentRooms.value = [];
  }
};

// 监听邀请码输入
watch(
  () => joinForm.inviteCode,
  (newVal) => {
    if (newVal?.length === 6) {
      fetchApartmentRooms(newVal);
    } else if (newVal?.length < 6) {
      apartmentRooms.value = [];
    }
  }
);

// 处理邀请码确认
const handleInviteCodeConfirm = async () => {
  if (joinForm.inviteCode.length !== 6) {
    ElMessage.warning("请输入6位邀请码");
    return;
  }

  try {
    const res = await proxy.$api.getApartmentByInviteCode({
      inviteCode: joinForm.inviteCode,
    });

    if (res) {
      apartmentInfo.value = res.apartment;
      apartmentRooms.value = (res.rooms || []).map((room) => ({
        ...room,
        tenantName: room.currentUserId ? "已入住" : null,
      }));
      step.value = 2;
    }
  } catch (error) {
    console.error("获取公寓信息失败:", error);
  }
};

// 加入公寓
const joinApartment = async () => {
  if (!joinForm.roomId) {
    ElMessage.warning("请选择房间");
    return;
  }

  joining.value = true;
  try {
    const res = await proxy.$api.joinApartment({
      inviteCode: joinForm.inviteCode,
      roomId: joinForm.roomId,
    });

    ElMessage.success("成功加入公寓");

    // 更新状态
    hasApartment.value = true;
    localStorage.setItem("hasApartment", "true");
    if (res?.apartmentId) localStorage.setItem("apartmentId", res.apartmentId);
    if (res?.roomId) localStorage.setItem("roomId", res.roomId);

    showJoinDialog.value = false;

    // 刷新数据
    getTableData();
    getChartData();
  } catch (error) {
    console.error("加入公寓失败:", error);
  } finally {
    joining.value = false;
  }
};

// 重置表单
const resetJoinForm = () => {
  step.value = 1;
  joinForm.inviteCode = "";
  joinForm.roomId = "";
  apartmentInfo.value = null;
  apartmentRooms.value = [];
};

// ==================== 表格数据 ====================
const tableData = ref([]);

const tableLabel = {
  name: "业务账单",
  totalAmount: "总金额",
  billPeriod: "账单日期",
  remark: "账单状态",
};

// 获取表格数据
const getTableData = async () => {
  try {
    const data = await proxy.$api.getTableData();

    if (userRole.value === "1") {
      tableData.value = data.tableData || [];
    } else {
      tableData.value = data.tableData || data.personalBills || [];
      
      if (data.personalPaymentHistory?.length > 0) {
        tableData.value = data.personalPaymentHistory.map((item) => ({
          name: item.billName,
          totalAmount: item.amount,
          billPeriod: item.billPeriod,
          remark: item.status,
        }));
      }
    }
  } catch (error) {
    console.error("获取表格数据失败:", error);
  }
};

// ==================== 支付逻辑 ====================
const currentBill = ref({});



const confirmPayment = () => {
  ElMessage.success("支付成功");
  paymentDialogVisible.value = false;
};

// ==================== 图表数据 ====================
const apartmentOverview = ref({
  totalBills: 0,
  totalPaid: 0,
  totalUnpaid: 0,
  billCount: 0,
  collectionRate: 0,
});

const tenantPaymentStatus = ref([]);
const billTypeDistribution = ref([]);

// ==================== ECharts 逻辑 ====================
// 存储图表实例以便清理
const charts = ref([]);
// 使用被动事件监听器解决警告
const observerOptions = { passive: true };

const observeResize = (chart, element) => {
  if (!element) return;
  
  // 使用被动事件监听器
  const resizeObserver = new ResizeObserver(() => {
    chart.resize();
  }, observerOptions);
  
  resizeObserver.observe(element);
  charts.value.push({ chart, observer: resizeObserver, element });
};

// 渲染月度账单趋势图
const renderMonthlyChart = (monthlyData) => {
  if (!monthlyData?.date || !monthlyData?.data) return;
  if (!proxy.$refs["monthlyEchart"]) return;

  const lineOptions = {
    textStyle: { color: "#333" },
    legend: {},
    grid: { left: "15%", right: "5%" },
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: monthlyData.date,
      axisLine: { lineStyle: { color: "#17b3a3" } },
      axisLabel: { color: "#333" },
    },
    yAxis: {
      type: "value",
      axisLine: { lineStyle: { color: "#17b3a3" } },
    },
    color: ["#2ec7c9", "#b6a2de", "#5ab1ef", "#ffb980", "#d87a80", "#8d98b3"],
    series: []
  };

  const categories = ["水电", "燃气", "房租", "其他"];
  const categoryNames = ["水电费", "燃气费", "房租", "其他费用"];

  categories.forEach((category, index) => {
    const data = monthlyData.data.map((item) => item[category] || 0);
    if (data.some((val) => val > 0)) {
      lineOptions.series.push({
        name: categoryNames[index],
        data: data,
        type: "line",
        smooth: true,
      });
    }
  });

  const chart = echarts.init(proxy.$refs["monthlyEchart"]);
  chart.setOption(lineOptions);
  observeResize(chart, proxy.$refs["monthlyEchart"]);
};

// 渲染费用类型分布饼图
const renderPieChart = (distributionData) => {
  if (!distributionData?.length) return;
  if (!proxy.$refs["typeEchart"]) return;

  const pieOptions = {
    tooltip: { trigger: "item" },
    legend: { orient: "vertical", left: "left" },
    color: [
      "#0f78f4",
      "#dd536b",
      "#9462e5",
      "#a6a6a6",
      "#e1bb22",
      "#39c362",
      "#3ed1cf",
    ],
    series: [
      {
        type: "pie",
        radius: ["40%", "70%"],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: { scale: true },
        data: distributionData.map((item) => ({
          name: item.name,
          value: item.value,
        })),
      },
    ],
  };

  const chart = echarts.init(proxy.$refs["typeEchart"]);
  chart.setOption(pieOptions);
  observeResize(chart, proxy.$refs["typeEchart"]);
};

// 渲染租客缴费柱状图
const renderTenantBarChart = (tenantData) => {
  if (!tenantData?.length) return;
  if (!proxy.$refs["tenantEchart"]) return;

  const barOptions = {
    textStyle: { color: "#333" },
    legend: {},
    grid: { left: "15%", right: "5%" },
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: tenantData.map((item) => item.name),
      axisLine: { lineStyle: { color: "#17b3a3" } },
      axisLabel: { color: "#333" },
    },
    yAxis: {
      type: "value",
      axisLine: { lineStyle: { color: "#17b3a3" } },
    },
    color: ["#2ec7c9", "#b6a2de", "#5ab1ef", "#ffb980", "#d87a80"],
    series: [
      {
        name: "已缴金额",
        data: tenantData.map((item) => item.paidAmount),
        type: "bar",
        itemStyle: { color: "#2ec7c9" },
      },
      {
        name: "待缴金额",
        data: tenantData.map((item) => item.unpaidAmount),
        type: "bar",
        itemStyle: { color: "#d87a80" },
      },
    ],
  };

  const chart = echarts.init(proxy.$refs["tenantEchart"]);
  chart.setOption(barOptions);
  observeResize(chart, proxy.$refs["tenantEchart"]);
};

// 渲染个人支出趋势图
const renderPersonalExpenseChart = (expenseData) => {
  if (!expenseData?.length) return;
  if (!proxy.$refs["personalEchart"]) return;

  const barOptions = {
    textStyle: { color: "#333" },
    legend: {},
    grid: { left: "15%", right: "5%" },
    tooltip: { trigger: "axis" },
    xAxis: {
      type: "category",
      data: expenseData.map((item) => item.month || item.date),
      axisLine: { lineStyle: { color: "#17b3a3" } },
      axisLabel: { color: "#333" },
    },
    yAxis: {
      type: "value",
      axisLine: { lineStyle: { color: "#17b3a3" } },
    },
    color: ["#2ec7c9"],
    series: [
      {
        name: "我的支出",
        data: expenseData.map((item) => item.total || item.amount),
        type: "bar",
      },
    ],
  };

  const chart = echarts.init(proxy.$refs["personalEchart"]);
  chart.setOption(barOptions);
  observeResize(chart, proxy.$refs["personalEchart"]);
};

// 渲染个人费用构成饼图
const renderPersonalPieChart = (distributionData) => {
  if (!distributionData?.length) return;
  if (!proxy.$refs["personalPieEchart"]) return;

  const personalPieOptions = {
    tooltip: { trigger: "item" },
    legend: {
      orient: "horizontal",
      bottom: 10,
      left: "center",
      itemWidth: 12,
      itemHeight: 8,
      textStyle: { fontSize: 11 },
    },
    color: [
      "#0f78f4",
      "#dd536b",
      "#9462e5",
      "#a6a6a6",
      "#e1bb22",
      "#39c362",
      "#3ed1cf",
      "#ff9800",
    ],
    series: [
      {
        type: "pie",
        radius: ["45%", "70%"],
        center: ["50%", "45%"],
        avoidLabelOverlap: false,
        label: { show: false },
        emphasis: { scale: true },
        data: distributionData.map((item) => ({
          name: item.name,
          value: item.value,
        })),
      },
    ],
  };

  const chart = echarts.init(proxy.$refs["personalPieEchart"]);
  chart.setOption(personalPieOptions);
  observeResize(chart, proxy.$refs["personalPieEchart"]);
};

// 获取图表数据
const getChartData = async () => {
  try {
    const response = await proxy.$api.getChartData();
    const chartData = response?.data || response;
    
    if (!chartData) {
      console.warn("图表数据为空");
      return;
    }

    apartmentOverview.value = chartData.apartmentOverview || {};
    billTypeDistribution.value = chartData.billTypeDistribution || [];

    if (chartData.monthlyBillData) {
      renderMonthlyChart(chartData.monthlyBillData);
    }

    if (chartData.billTypeDistribution) {
      renderPieChart(chartData.billTypeDistribution);
    }

    if (userRole.value === "1") {
      tenantPaymentStatus.value = chartData.tenantPaymentStatus || [];
      renderTenantBarChart(chartData.tenantPaymentStatus);
    } else {
      if (chartData.personalMonthlyExpense) {
        renderPersonalExpenseChart(chartData.personalMonthlyExpense);
      }
      if (chartData.personalBillDistribution) {
        renderPersonalPieChart(chartData.personalBillDistribution);
      }
    }
  } catch (error) {
    console.error("获取图表数据失败:", error);
  }
};

// ==================== 生命周期钩子 ====================
onMounted(() => {
  // 首次登录弹窗逻辑
  if (
    userRole.value === "0" &&
    !hasApartment.value &&
    isFirstLogin.value &&
    !localStorage.getItem("isFirstLogin")
  ) {
    showFirstLoginDialog.value = true;
  }
  
  getTableData();
  getChartData();
});

// 组件卸载时清理图表实例和观察者
onUnmounted(() => {
  charts.value.forEach(({ chart, observer }) => {
    if (observer) observer.disconnect();
    if (chart) chart.dispose();
  });
  charts.value = [];
});
</script>
<template>
  <el-row class="home" :gutter="16">
    <!-- 左侧区域 - 宽度调整为 7 -->
    <el-col :span="7" style="margin-top: 16px">
      <!-- 用户信息卡片 -->
      <el-card shadow="hover" :body-style="{ padding: '15px' }">
        <div class="user">
          <img :src="getImageUrl('user')" class="user-avatar" />
          <div class="user-info">
            <p class="user-info-admin">{{ username }}</p>
            <p class="user-info-p">
              {{ userRole === "1" ? "出租管理员" : "租客" }}
            </p>
          </div>
        </div>
        <div class="login-info">
          <p>
            <span class="label">登录时间：</span
            ><span class="value">2026-05-01 10:00</span>
          </p>
          <p>
            <span class="label">登录地址：</span
            ><span class="value">192.168.1.1</span>
          </p>
        </div>
      </el-card>
      <!-- 新租客加入公寓引导卡片 -->
      <el-card
        v-if="userRole === '0' && !hasApartment"
        class="join-apartment-card"
        shadow="hover"
        :body-style="{ padding: '15px' }"
      >
        <div class="join-apartment">
          <el-icon class="join-icon"><HomeFilled /></el-icon>
          <h3>加入公寓</h3>
          <p class="join-desc">
            您还没有加入任何公寓，请填写房东提供的邀请码加入
          </p>
          <el-button type="primary" @click="showJoinDialog = true" size="large">
            立即加入
          </el-button>
        </div>
      </el-card>

      <!-- 首次登录弹窗 -->
      <el-dialog
        v-model="showFirstLoginDialog"
        title="欢迎新租客"
        width="400px"
        :close-on-click-modal="false"
        :show-close="true"
        class="first-login-dialog"
      >
        <div class="first-login-content">
          <el-icon class="welcome-icon"><GoldMedal /></el-icon>
          <h2>欢迎加入合租公寓</h2>
          <p>看起来您还没有加入任何公寓，现在需要加入公寓才能开始使用</p>
          <div class="action-buttons">
            <el-button
              type="primary"
              @click="
                showJoinDialog = true;
                showFirstLoginDialog = false;
              "
              size="large"
            >
              立即加入公寓
            </el-button>
            <el-button @click="closeFirstLoginDialog" size="large">
              稍后再说
            </el-button>
          </div>
          <p class="tip">提示：您可以在首页随时加入公寓</p>
        </div>
      </el-dialog>

      <!-- 加入公寓对话框 -->
      <el-dialog
        v-model="showJoinDialog"
        title="加入公寓"
        width="450px"
        class="join-dialog"
        @closed="resetJoinForm"
        :close-on-click-modal="false"
      >
        <!-- 步骤指示器 -->
        <div class="steps-container">
          <div class="step-item" :class="{ active: step === 1 }">
            <div class="step-number" :class="{ completed: step > 1 }">1</div>
            <div class="step-label">验证邀请码</div>
          </div>
          <div class="step-line" :class="{ active: step > 1 }"></div>
          <div class="step-item" :class="{ active: step === 2 }">
            <div class="step-number" :class="{ completed: step > 2 }">2</div>
            <div class="step-label">选择房间</div>
          </div>
        </div>

        <!-- 步骤1：输入邀请码 -->
        <div v-if="step === 1" class="step-content">
          <div class="invite-tip">
            <el-icon class="tip-icon"><InfoFilled /></el-icon>
            <span>请输入房东提供的6位邀请码，系统将自动查询公寓信息</span>
          </div>

          <el-form
            :model="joinForm"
            :rules="joinRules"
            ref="joinFormRef"
            label-width="0"
          >
            <el-form-item prop="inviteCode">
              <el-input
                v-model="joinForm.inviteCode"
                placeholder="请输入6位邀请码"
                maxlength="6"
                show-word-limit
                size="large"
                @keyup.enter="handleInviteCodeConfirm"
              >
                <template #prepend>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-form>

          <div class="step-actions">
            <el-button @click="showJoinDialog = false">取消</el-button>
            <el-button
              type="primary"
              @click="handleInviteCodeConfirm"
              :disabled="joinForm.inviteCode.length !== 6"
            >
              下一步
            </el-button>
          </div>
        </div>

        <!-- 步骤2：公寓信息和房间选择 -->
        <div v-if="step === 2" class="step-content">
          <!-- 公寓信息卡片 -->
          <div v-if="apartmentInfo" class="apartment-card">
            <div class="apartment-header">
              <el-icon class="apartment-icon"><HomeFilled /></el-icon>
              <div class="apartment-title">
                <h3>{{ apartmentInfo.name }}</h3>
                <span
                  class="apartment-status"
                  :class="{ active: apartmentInfo.status === 1 }"
                >
                  {{ apartmentInfo.status === 1 ? "正常" : "已解散" }}
                </span>
              </div>
            </div>

            <div class="apartment-details">
              <div class="detail-item">
                <el-icon><Location /></el-icon>
                <span>{{ apartmentInfo.address }}</span>
              </div>
              <div class="detail-item">
                <el-icon><House /></el-icon>
                <span>共 {{ apartmentInfo.totalRooms }} 个房间</span>
              </div>
              <div class="detail-item">
                <el-icon><Calendar /></el-icon>
                <span>创建于 {{ formatDate(apartmentInfo.createTime) }}</span>
              </div>
              <div class="detail-item" v-if="apartmentInfo.description">
                <el-icon><InfoFilled /></el-icon>
                <span>{{ apartmentInfo.description }}</span>
              </div>
            </div>
          </div>

          <!-- 房间选择 -->
          <div class="room-selector">
            <div class="room-selector-header">
              <span class="room-selector-title">选择房间</span>
              <span class="room-stats">
                可用: {{ availableRooms.length }} / 总数:
                {{ apartmentRooms.length }}
              </span>
            </div>

            <el-radio-group v-model="joinForm.roomId" class="room-list">
              <el-radio
                v-for="room in apartmentRooms"
                :key="room.id"
                :label="room.id"
                :disabled="room.status === 1"
                class="room-item"
              >
                <div class="room-info">
                  <div class="room-name">{{ room.roomNumber }}</div>
                  <div class="room-area">{{ room.area }}㎡</div>
                  <div
                    class="room-status"
                    :class="{ occupied: room.status === 1 }"
                  >
                    {{ room.status === 1 ? "已入住" : "可入住" }}
                  </div>
                  <div v-if="room.currentUserId" class="room-tenant">
                    租客: {{ room.tenantName || "未知" }}
                  </div>
                </div>
              </el-radio>
            </el-radio-group>

            <div v-if="availableRooms.length === 0" class="no-rooms">
              <el-empty description="暂无可用房间" :image-size="80">
                <p class="no-rooms-tip">该公寓所有房间均已入住，请联系房东</p>
              </el-empty>
            </div>
          </div>

          <div class="step-actions">
            <el-button @click="step = 1">上一步</el-button>
            <el-button
              type="primary"
              @click="joinApartment"
              :loading="joining"
              :disabled="!joinForm.roomId"
            >
              确认加入
            </el-button>
          </div>
        </div>
      </el-dialog>
      <!-- 业务账单卡片 - 固定高度并使用滚动 -->
      <el-card
        shadow="hover"
        class="user-table"
        :body-style="{ padding: '10px' }"
      >
        <template #header>
          <span class="card-title">近期账单</span>
        </template>
        <el-table
          :data="tableData"
          height="44vh"
          style="width: 100%"
          size="small"
        >
          <el-table-column
            v-for="(val, key) in tableLabel"
            :key="key"
            :prop="key"
            :label="val"
            :min-width="key === 'name' ? 120 : 80"
          >
            <template #default="{ row }" v-if="key === 'totalAmount'">
              ￥{{ formatAmount(row[key]) }}
            </template>
          <template #default="{ row }" v-else-if="key === 'remark'">
              <!-- 已结清状态显示为标签 -->
              <el-tag
                v-if="row[key] === '已结清'"
                type="success"
                size="small"
                effect="light"
              >
                {{ row[key] }}
              </el-tag>
              <!-- 待缴费状态显示为按钮 -->
              <el-tag
                v-else
                type="danger"
                size="small"
              >
                {{ row[key] }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
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
          <!-- 账单图标和标题 -->
          <div
            class="payment-icon"
            style="
              width: 80px;
              height: 80px;
              background: linear-gradient(135deg, #fff5f5 0%, #ffeaea 100%);
              border-radius: 50%;
              display: flex;
              align-items: center;
              justify-content: center;
              margin: 0 auto 20px auto;
              box-shadow: 0 4px 12px rgba(245, 108, 108, 0.15);
            "
          >
            <el-icon :size="40" color="#f56c6c">
              <component :is="'Money'" />
            </el-icon>
          </div>

          <!-- 账单详情卡片 -->
          <div
            class="bill-detail-card"
            style="
              width: 100%;
              background: #f9f9f9;
              border-radius: 12px;
              padding: 18px 20px;
              margin-bottom: 20px;
              border: 1px solid #f0f0f0;
              box-sizing: border-box;
            "
          >
            <div
              class="bill-detail-item"
              style="
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 12px 0;
                border-bottom: 1px dashed #e0e0e0;
                line-height: 1.5;
              "
            >
              <span
                class="detail-label"
                style="color: #666; font-size: 14px; min-width: 80px"
                >账单名称：</span
              >
              <span
                class="detail-value"
                style="
                  color: #333;
                  font-weight: 500;
                  font-size: 15px;
                  text-align: right;
                "
                >{{ currentBill.name }}</span
              >
            </div>
            <div
              class="bill-detail-item"
              style="
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 12px 0;
                border-bottom: 1px dashed #e0e0e0;
                line-height: 1.5;
              "
            >
              <span
                class="detail-label"
                style="color: #666; font-size: 14px; min-width: 80px"
                >账单周期：</span
              >
              <span
                class="detail-value"
                style="
                  color: #333;
                  font-weight: 500;
                  font-size: 15px;
                  text-align: right;
                "
                >{{ currentBill.billPeriod }}</span
              >
            </div>
            <div
              class="bill-detail-item highlight"
              style="
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 16px 0 8px 0;
                margin-top: 4px;
                line-height: 1.5;
              "
            >
              <span
                class="detail-label"
                style="
                  color: #666;
                  font-size: 15px;
                  min-width: 80px;
                  font-weight: 600;
                "
                >应缴金额：</span
              >
              <span
                class="detail-value amount"
                style="
                  color: #f56c6c;
                  font-size: 26px;
                  font-weight: bold;
                  text-align: right;
                "
                >￥{{ formatAmount(currentBill.totalAmount) }}</span
              >
            </div>
          </div>

          <!-- 温馨提示 -->
          <div
            class="payment-tip"
            style="
              display: flex;
              align-items: center;
              gap: 8px;
              background: #f5f7fa;
              padding: 12px 16px;
              border-radius: 8px;
              width: 100%;
              color: #909399;
              font-size: 13px;
              box-sizing: border-box;
              line-height: 1.5;
              margin-top: 5px;
            "
          >
            <el-icon :size="16" color="#909399"><Warning /></el-icon>
            <span style="flex: 1">请确认账单信息无误后点击确认支付</span>
          </div>
        </div>

        <template #footer>
          <div
            class="dialog-footer"
            style="
              display: flex;
              justify-content: space-between;
              gap: 12px;
              padding: 0 10px 20px;
            "
          >
            <el-button
              @click="paymentDialogVisible = false"
              size="large"
              style="flex: 1; border-radius: 30px; height: 42px"
              >取 消</el-button
            >
            <el-button
              type="danger"
              @click="confirmPayment"
              size="large"
              style="
                flex: 1;
                border-radius: 30px;
                height: 42px;
                background: linear-gradient(135deg, #f56c6c, #f78989);
                border: none;
                box-shadow: 0 4px 12px rgba(245, 108, 108, 0.3);
              "
            >
              确认支付
            </el-button>
          </div>
        </template>
      </el-dialog>
    </el-col>

    <!-- 右侧区域 - 宽度调整为 17 -->
    <el-col :span="17" style="margin-top: 16px">
      <!-- 合租小技巧轮播图 (租客可见) - 放在最上方 -->
      <div
        v-if="userRole === '0'"
        class="block text-center"
        style="
          margin-bottom: 16px;
          border: 1px solid #e0e0e0;
          border-radius: 4px;
        "
      >
        <el-carousel :interval="4000" type="card" height="240px" arrow="always">
          <el-carousel-item v-for="item in carouselImages" :key="item.id">
            <div
              class="carousel-container clickable"
              @click="handleCarouselClick(item)"
            >
              <img
                :src="getCarouselImageUrl(item.name)"
                class="carousel-image"
              />
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <!-- 月度账单趋势图 -->
      <el-card
        v-if="userRole === '1'"
        class="top-echart"
        style="margin-top: 0"
        :body-style="{ padding: '10px' }"
      >
        <template #header>
          <span class="card-title">月度账单趋势</span>
        </template>
        <div ref="monthlyEchart" style="height: 260px; width: 100%"></div>
      </el-card>
      <!-- 图表区域（房东可见）- 放在下方 -->
      <div v-if="userRole === '1'" class="graph" style="margin-top: 16px">
        <el-card :body-style="{ padding: '10px' }" style="width: 49%">
          <template #header>
            <span class="card-title">费用类型分布</span>
          </template>
          <div ref="typeEchart" style="height: 300px"></div>
        </el-card>
        <el-card :body-style="{ padding: '10px' }" style="width: 49%">
          <template #header>
            <span class="card-title">租客缴费情况</span>
          </template>
          <div ref="tenantEchart" style="height: 300px"></div>
        </el-card>
      </div>

      <!-- 租客个人图表区域 -->
      <div v-if="userRole === '0'" class="graph" style="margin-top: 16px">
        <el-card :body-style="{ padding: '10px' }" style="width: 49%">
          <template #header>
            <span class="card-title">我的支出趋势</span>
          </template>
          <div ref="personalEchart" style="height: 350px"></div>
        </el-card>
        <el-card :body-style="{ padding: '10px' }" style="width: 49%">
          <template #header>
            <span class="card-title">我的费用构成</span>
          </template>
          <div ref="personalPieEchart" style="height: 350px"></div>
        </el-card>
      </div>
    </el-col>
  </el-row>
</template>

<style scoped src="./../components/Home/Home.css"></style>
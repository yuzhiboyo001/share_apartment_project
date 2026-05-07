<template>
  <div class="task-container fancy-container">
    <!-- 头部：新增事务按钮和搜索框 -->
    <div class="header-glass">
      <div class="title-wrapper">
        <span class="title-icon">📋</span>
        <h2 class="page-title">公寓事务</h2>
        <span class="title-badge">{{ userRole === 1 ? "房东" : "租客" }}</span>
      </div>
      <div class="header-right">
        <el-button class="fancy-btn primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          新增事务
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-glass">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item>
          <div class="search-input-wrapper">
            <el-icon class="search-icon"><Search /></el-icon>
            <el-input
              v-model="searchForm.keyword"
              placeholder="搜索事务标题或通知内容..."
              clearable
              @keyup.enter="handleSearch"
              class="fancy-input"
            />
          </div>
        </el-form-item>
         <el-form-item>
          <el-radio-group v-model="activeTab" size="small" @change="handleTabChange">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="task">公寓事务</el-radio-button>
            <el-radio-button value="message">房东通知</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button class="fancy-btn search" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button class="fancy-btn reset" @click="resetSearch">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 混合卡片列表（事务卡片 + 消息卡片） -->
    <div class="cards-container" v-loading="loading">
      <div v-if="mixedList.length === 0" class="empty-state">
        <el-empty description="暂无内容">
          <p class="empty-tip">{{ activeTab === 'message' ? '暂无房东通知' : '点击"新增事务"创建公寓事务' }}</p>
        </el-empty>
      </div>

      <el-row :gutter="20">
        <el-col
          v-for="item in mixedList"
          :key="item.uniqueKey"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <!-- 事务卡片 -->
          <div v-if="item.type === 'task'" class="task-card" :class="getTaskCardClass(item)">
            <div class="card-header">
              <div class="header-left">
                <span class="task-type">{{ getTaskTypeText(item.taskType) }}</span>
                <span class="badge badge-task">公寓事务</span>
              </div>
              <span class="task-status" :class="getStatusClass(item.status)">
                {{ getStatusText(item.status) }}
              </span>
            </div>
            <div class="card-body">
              <h3 class="task-title">{{ item.title }}</h3>
              <p class="task-desc">{{ item.description }}</p>
              <div class="task-meta">
                <div class="meta-item">
                  <el-icon><User /></el-icon>
                  <span>发起人：{{ item.creatorName }}</span>
                </div>
                <div class="meta-item" v-if="item.assigneeName">
                  <el-icon><UserFilled /></el-icon>
                  <span>责任人：{{ item.assigneeName }}</span>
                </div>
                <div class="meta-item" v-if="item.deadline">
                  <el-icon><Timer /></el-icon>
                  <span>截止：{{ formatDate(item.deadline) }}</span>
                </div>
              </div>
            </div>
            <div class="card-footer">
              <span class="create-time">
                <el-icon><Calendar /></el-icon>
                {{ formatDate(item.createTime) }}
              </span>
              <div class="card-actions">
                <!-- 事务操作下拉菜单 -->
                <el-dropdown
                  v-if="canComplete(item)"
                  trigger="click"
                  @command="(command) => handleTaskAction(command, item)"
                >
                  <el-button class="action-btn more" size="small">
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        command="complete"
                        :disabled="item.status === 2"
                      >
                        <el-icon><Check /></el-icon> 标记完成
                      </el-dropdown-item>
                      <el-dropdown-item
                        command="progress"
                        :disabled="item.status === 1"
                      >
                        <el-icon><VideoPlay /></el-icon> 开始处理
                      </el-dropdown-item>
                      <el-dropdown-item
                        command="cancel"
                        :disabled="item.status === 3"
                      >
                        <el-icon><Close /></el-icon> 取消事务
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>

                <el-tooltip content="查看详情" placement="top">
                  <el-button
                    class="action-btn view"
                    size="small"
                    @click="viewTaskDetail(item)"
                  >
                    <el-icon><View /></el-icon>
                  </el-button>
                </el-tooltip>

                <el-tooltip
                  v-if="canDelete(item)"
                  content="删除"
                  placement="top"
                >
                  <el-button
                    class="action-btn delete"
                    size="small"
                    @click="deleteTask(item)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>
          </div>

          <!-- 消息卡片（房东通知） -->
          <div v-else class="message-card" :class="{ 'unread': item.isRead === 0 }">
            <div class="card-header">
              <div class="header-left">
                <span class="message-type">{{ getMessageTypeText(item.messageType) }}</span>
                <span class="badge badge-message">房东通知</span>
              </div>
              <span v-if="item.isRead === 0" class="unread-tag">未读</span>
            </div>
            <div class="card-body">
              <h3 class="message-title">{{ item.title }}</h3>
              <p class="message-content">{{ item.content }}</p>
            </div>
            <div class="card-footer">
              <span class="create-time">
                <el-icon><Timer /></el-icon>
                {{ formatDateTime(item.createTime) }}
              </span>
              <div class="card-actions">
                <el-tooltip content="标记为已读" placement="top" v-if="item.isRead === 0">
                  <el-button
                    class="action-btn read"
                    size="small"
                    @click="markMessageAsRead(item.id)"
                  >
                    <el-icon><Check /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="mixedList.length > 0">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="config.total"
          :page-size="config.limit"
          v-model:current-page="config.page"
          @current-change="handleChange"
          class="fancy-pagination"
        />
      </div>
    </div>

    <!-- 创建事务对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="📝 新增事务"
      width="45%"
      class="fancy-dialog"
      :before-close="() => (showCreateDialog = false)"
    >
      <el-form
        ref="taskFormRef"
        :model="taskForm"
        :rules="taskRules"
        label-width="100px"
        class="fancy-form"
      >
        <div class="form-section">
          <div class="section-title">
            <span class="section-icon">📋</span>
            <span>事务信息</span>
          </div>

          <el-form-item label="事务标题" prop="title">
            <el-input
              v-model="taskForm.title"
              placeholder="例如：本周值日安排"
              class="fancy-form-input"
            />
          </el-form-item>

          <el-form-item label="事务类型" prop="taskType">
            <el-select
              v-model="taskForm.taskType"
              placeholder="请选择类型"
              class="fancy-select"
            >
              <el-option :value="1" label="🧹 值日" />
              <el-option :value="2" label="🔧 维修" />
              <el-option :value="3" label="🛒 采购" />
              <el-option :value="4" label="📌 其他" />
            </el-select>
          </el-form-item>

          <el-form-item label="详细描述" prop="description">
            <el-input
              v-model="taskForm.description"
              type="textarea"
              :rows="4"
              placeholder="请详细描述事务内容..."
              class="fancy-textarea"
            />
          </el-form-item>
        </div>

        <div class="form-section">
          <div class="section-title">
            <span class="section-icon">👤</span>
            <span>责任人设置</span>
          </div>

          <el-form-item label="指定责任人" prop="assigneeId">
            <el-select
              v-model="taskForm.assigneeId"
              placeholder="可选，不指定则为公共事务"
              clearable
              class="fancy-select"
            >
              <el-option
                v-for="tenant in tenantList"
                :key="tenant.id"
                :label="tenant.username"
                :value="tenant.id"
              />
            </el-select>
            <div class="form-tip">指定责任人后，该租客会收到特别提醒</div>
          </el-form-item>

          <el-form-item label="截止日期" prop="deadline">
            <el-date-picker
              v-model="taskForm.deadline"
              type="datetime"
              placeholder="选择截止时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              class="fancy-date-picker"
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button class="fancy-btn cancel" @click="showCreateDialog = false">
            取消
          </el-button>
          <el-button
            class="fancy-btn confirm"
            @click="createTask"
            :loading="creating"
          >
            <el-icon><Check /></el-icon>
            确认创建
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 事务详情对话框（可在此更新状态） -->
    <el-dialog
      v-model="showDetailDialog"
      title="📋 事务详情"
      width="40%"
      class="fancy-dialog"
    >
      <div v-if="currentTask" class="task-detail">
        <div class="detail-header">
          <span class="detail-type">{{
            getTaskTypeText(currentTask.taskType)
          }}</span>
          <span
            class="detail-status"
            :class="getStatusClass(currentTask.status)"
          >
            {{ getStatusText(currentTask.status) }}
          </span>
        </div>
        <h3 class="detail-title">{{ currentTask.title }}</h3>
        <p class="detail-description">{{ currentTask.description }}</p>

        <div class="detail-meta-grid">
          <div class="meta-item">
            <span class="meta-label">发起人：</span>
            <span class="meta-value">{{ currentTask.creatorName }}</span>
          </div>
          <div class="meta-item" v-if="currentTask.assigneeName">
            <span class="meta-label">责任人：</span>
            <span class="meta-value">{{ currentTask.assigneeName }}</span>
          </div>
          <div class="meta-item" v-if="currentTask.deadline">
            <span class="meta-label">截止时间：</span>
            <span class="meta-value">{{
              formatDateTime(currentTask.deadline)
            }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">创建时间：</span>
            <span class="meta-value">{{
              formatDateTime(currentTask.createTime)
            }}</span>
          </div>
        </div>

        <!-- 状态更新按钮（权限控制） -->
        <div v-if="canUpdateStatus(currentTask)" class="status-update">
          <el-divider />
          <div class="update-buttons">
            <span class="update-label">更新状态：</span>
            <el-radio-group
              v-model="updateStatus"
              size="small"
              @change="handleStatusChange"
            >
              <el-radio-button :label="0">待处理</el-radio-button>
              <el-radio-button :label="1">进行中</el-radio-button>
              <el-radio-button :label="2">已完成</el-radio-button>
              <el-radio-button :label="3">已取消</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button class="fancy-btn close" @click="showDetailDialog = false">
            关 闭
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance, computed, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Plus,
  Search,
  Refresh,
  User,
  UserFilled,
  Timer,
  Calendar,
  View,
  Delete,
  Check,
  MoreFilled,
  VideoPlay,
  Close,
} from "@element-plus/icons-vue";

const { proxy } = getCurrentInstance();

// 状态
const loading = ref(false);
const creating = ref(false);
const showCreateDialog = ref(false);
const showDetailDialog = ref(false);
const taskList = ref([]);
const messageList = ref([]);
const tenantList = ref([]);
const currentTask = ref(null);
const updateStatus = ref(0);
const activeTab = ref("all");

// 用户角色
const userRole = Number(localStorage.getItem("role") || 0);
const userId = Number(localStorage.getItem("userId") || 0);

// 搜索表单
const searchForm = reactive({ keyword: "" });

// 分页配置
const config = reactive({
  keyword: "",
  total: 0,
  page: 1,
  limit: 12,
  taskTotal: 0,
  messageTotal: 0,
});

// 混合列表（合并事务和消息）
const mixedList = computed(() => {
  if (activeTab.value === "task") {
    return taskList.value.map(item => ({ 
      ...item, 
      type: "task",
      uniqueKey: `task_${item.id}`  // 添加唯一标识符
    }));
  } else if (activeTab.value === "message") {
    return messageList.value.map(item => ({ 
      ...item, 
      type: "message",
      uniqueKey: `message_${item.id}`  // 添加唯一标识符
    }));
  } else {
    // 合并并按时间倒序，确保key唯一
    const tasks = taskList.value.map(item => ({ 
      ...item, 
      type: "task",
      uniqueKey: `task_${item.id}`
    }));
    const messages = messageList.value.map(item => ({ 
      ...item, 
      type: "message",
      uniqueKey: `message_${item.id}`
    }));
    return [...tasks, ...messages].sort((a, b) => 
      new Date(b.createTime) - new Date(a.createTime)
    ).slice(0, config.limit);
  }
});

// 创建事务表单
const taskForm = reactive({
  title: "",
  taskType: 1,
  description: "",
  assigneeId: "",
  deadline: "",
});

// 表单校验规则
const taskRules = {
  title: [
    { required: true, message: "请输入事务标题", trigger: "blur" },
    { min: 2, max: 50, message: "长度在 2 到 50 个字符", trigger: "blur" },
  ],
  taskType: [{ required: true, message: "请选择事务类型", trigger: "change" }],
  description: [
    { required: true, message: "请输入事务描述", trigger: "blur" },
    { min: 5, max: 200, message: "长度在 5 到 200 个字符", trigger: "blur" },
  ],
};

// 获取事务类型文本
const getTaskTypeText = (type) => {
  const map = { 1: "🧹 值日", 2: "🔧 维修", 3: "🛒 采购", 4: "📌 其他" };
  return map[type] || "📌 其他";
};

// 获取状态文本
const getStatusText = (status) => {
  const map = { 0: "待处理", 1: "进行中", 2: "已完成", 3: "已取消" };
  return map[status] || "待处理";
};

// 获取状态样式类
const getStatusClass = (status) => {
  const map = {
    0: "status-pending",
    1: "status-progress",
    2: "status-completed",
    3: "status-cancelled",
  };
  return map[status] || "status-pending";
};

// 获取消息类型文本
const getMessageTypeText = (type) => {
  const map = {
    1: "账单提醒",
    2: "事务分配",
    3: "状态变更"
  };
  return map[type] || "房东通知";
};

// 卡片样式类
const getTaskCardClass = (item) => {
  const classes = [];
  if (item.status === 0) classes.push("card-pending");
  if (item.assigneeId) classes.push("has-assignee");
  return classes.join(" ");
};

// 判断是否有权操作事务
const canComplete = (item) => {
  if (!item) return false;
  if (item.status === 2 || item.status === 3) return false;
  return (
    userRole === 1 || item.creatorId === userId || item.assigneeId === userId
  );
};

// 判断是否有权删除
const canDelete = (task) => {
  return userRole === 1 || task.creatorId === userId;
};
const canUpdateStatus = (task) => {
  if (!task) return false;
  // 只有待处理或进行中的事务可以更新状态
  if (task.status !== 0 && task.status !== 1) return false;
  // 房东、发起人、责任人都可以更新状态
  return userRole === 1 || task.creatorId === userId || task.assigneeId === userId;
};
// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
};

const formatDateTime = (dateStr) => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
};

// 获取租客列表
const getTenantList = async () => {
  try {
    const apartmentId = localStorage.getItem("apartmentId");
    if (!apartmentId) return;
    const result = await proxy.$api.getTenantList({ apartmentId });
    tenantList.value = result?.list || [];
  } catch (error) {
    console.error("获取租客列表失败:", error);
  }
};

// 处理事务操作（带确认弹窗）
const handleTaskAction = async (command, item) => {
  let newStatus;
  let message;
  let confirmText;
  
  switch(command) {
    case 'complete':
      newStatus = 2;
      message = '事务已完成';
      confirmText = '确认要将此事务标记为已完成吗？';
      break;
    case 'progress':
      newStatus = 1;
      message = '事务已开始处理';
      confirmText = '确认开始处理此事务吗？';
      break;
    case 'cancel':
      newStatus = 3;
      message = '事务已取消';
      confirmText = '确认要取消此事务吗？';
      break;
    default:
      return;
  }
  
  try {
    await ElMessageBox.confirm(confirmText, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    
    await proxy.$api.updateTaskStatus({
      taskId: item.id,
      status: newStatus,
    });
    ElMessage.success(message);
    getTaskList();
  } catch (error) {
    if (error !== 'cancel') {
      console.error("操作失败:", error);
      ElMessage.error("操作失败");
    }
  }
};

const getTaskList = async () => {
  try {
    loading.value = true;
    const result = await proxy.$api.getTaskList({
      page: config.page,
      limit: config.limit,
      keyword: config.keyword,
    });
    
    // 确保每个事务都有唯一ID，如果没有则生成
    taskList.value = (result?.list || []).map((item, index) => ({
      ...item,
      id: item.id || `temp_task_${Date.now()}_${index}` // 临时ID生成
    }));
    
    config.taskTotal = result?.total || 0;
    config.total = config.taskTotal;
  } catch (error) {
    console.error("获取事务列表失败:", error);
    ElMessage.error("获取事务列表失败");
  } finally {
    loading.value = false;
  }
};

// 获取消息列表
const getMessageList = async () => {
  try {
    const result = await proxy.$api.getMessageList({
      page: config.page,
      limit: config.limit,
      keyword: config.keyword,
    });
    
    // 确保每个消息都有唯一ID，如果没有则生成
    messageList.value = (result?.list || []).map((item, index) => ({
      ...item,
      id: item.id || `temp_message_${Date.now()}_${index}` // 临时ID生成
    }));
    
    config.messageTotal = result?.total || 0;
  } catch (error) {
    console.error("获取消息列表失败:", error);
  }
};

// 标记消息为已读
const markMessageAsRead = async (id) => {
  try {
    await proxy.$api.markMessageRead(id);
    ElMessage.success("已标记为已读");
    getMessageList();
  } catch (error) {
    ElMessage.error("操作失败");
  }
};

// 创建事务
const createTask = async () => {
  proxy.$refs.taskFormRef.validate(async (valid) => {
    if (!valid) return;
    creating.value = true;
    try {
      const submitData = {
        ...taskForm,
        assigneeId: taskForm.assigneeId || null,
      };
      await proxy.$api.createTask(submitData);
      ElMessage.success("创建成功");
      showCreateDialog.value = false;
      getTaskList();
      // 重置表单
      Object.assign(taskForm, {
        title: "",
        taskType: 1,
        description: "",
        assigneeId: "",
        deadline: "",
      });
    } catch (error) {
      console.error("创建失败:", error);
      ElMessage.error("创建失败");
    } finally {
      creating.value = false;
    }
  });
};

// 查看详情
const viewTaskDetail = async (task) => {
  try {
    const result = await proxy.$api.getTaskDetail(task.id);
    currentTask.value = result;
    updateStatus.value = result.status;
    showDetailDialog.value = true;
  } catch (error) {
    console.error("获取详情失败:", error);
    ElMessage.error("获取详情失败");
  }
};

// 更新状态
const handleStatusChange = async (val) => {
  try {
    await proxy.$api.updateTaskStatus({
      taskId: currentTask.value.id,
      status: val,
    });
    ElMessage.success("状态已更新");
    getTaskList();
    currentTask.value.status = val;
  } catch (error) {
    console.error("更新状态失败:", error);
    ElMessage.error("更新失败");
  }
};

// 删除事务
const deleteTask = (task) => {
  ElMessageBox.confirm(`确认删除事务 "${task.title}" 吗？`, "删除确认", {
    confirmButtonText: "确定删除",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      try {
        await proxy.$api.deleteTask(task.id);
        ElMessage.success("删除成功");
        getTaskList();
      } catch (error) {
        ElMessage.error("删除失败");
      }
    })
    .catch(() => {});
};

// 搜索
const handleSearch = () => {
  config.page = 1;
  if (activeTab.value === 'task') {
    getTaskList();
  } else if (activeTab.value === 'message') {
    getMessageList();
  } else {
    Promise.all([getTaskList(), getMessageList()]);
  }
};

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = "";
  config.keyword = "";
  config.page = 1;
  handleSearch();
};

// 切换标签
const handleTabChange = () => {
  config.page = 1;
  if (activeTab.value === 'task') {
    config.total = config.taskTotal;
    getTaskList();
  } else if (activeTab.value === 'message') {
    config.total = config.messageTotal;
    getMessageList();
  } else {
    config.total = config.taskTotal + config.messageTotal;
    Promise.all([getTaskList(), getMessageList()]);
  }
};

// 分页变化
const handleChange = (page) => {
  config.page = page;
  handleSearch();
};

onMounted(() => {
  Promise.all([getTaskList(), getMessageList(), getTenantList()]);
});
</script>

<style scoped lang="less">
.fancy-container {
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9ecf5 100%);
  min-height: 100vh;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header-glass {
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 20px 28px;
  margin-bottom: 20px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.5);
  display: flex;
  justify-content: space-between;
  align-items: center;

  .title-wrapper {
    display: flex;
    align-items: center;
    gap: 12px;

    .title-icon {
      font-size: 28px;
    }

    .page-title {
      font-size: 24px;
      font-weight: 700;
      background: linear-gradient(135deg, #1e293b, #3b82f6);
      background-clip: text;
      -webkit-text-fill-color: transparent;
      margin: 0;
    }

    .title-badge {
      background: linear-gradient(135deg, #3b82f6, #8b5cf6);
      color: white;
      padding: 3px 10px;
      border-radius: 30px;
      font-size: 11px;
      font-weight: 600;
    }
  }

  .header-right {
    display: flex;
    gap: 12px;
  }
}

.search-glass {
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  padding: 16px 24px;
  margin-bottom: 20px;
  border: 1px solid rgba(255, 255, 255, 0.5);

  .search-form {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
  }

  .search-input-wrapper {
    position: relative;
    width: 300px;

    .search-icon {
      position: absolute;
      left: 12px;
      top: 50%;
      transform: translateY(-50%);
      color: #94a3b8;
      z-index: 1;
    }

    .fancy-input :deep(.el-input__wrapper) {
      padding-left: 35px;
      border-radius: 30px;
    }
  }
}

.cards-container {
  flex: 1;
  overflow-y: auto;
  padding: 4px 4px 20px 4px;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 3px;

    &:hover {
      background: #94a3b8;
    }
  }
}

// 统一卡片基础样式
.task-card, .message-card {
  background: white;
  border-radius: 16px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
  overflow: hidden;
  height: 320px;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  }

  .card-header {
    padding: 14px 16px;
    background: linear-gradient(135deg, #f8faff, #f0f4ff);
    border-bottom: 1px solid #e2e8f0;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;

    .header-left {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    .badge {
      font-size: 10px;
      padding: 2px 8px;
      border-radius: 20px;
      font-weight: 500;
      
      &.badge-task {
        background: #3b82f6;
        color: white;
      }
      
      &.badge-message {
        background: #e6a23c;
        color: white;
      }
    }
  }

  .card-body {
    padding: 16px;
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .card-footer {
    padding: 12px 16px;
    background: #f8fafc;
    border-top: 1px solid #f0f0f0;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;

    .create-time {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 11px;
      color: #94a3b8;
    }

    .card-actions {
      display: flex;
      gap: 6px;

      .action-btn {
        width: 28px;
        height: 28px;
        padding: 0;
        border-radius: 8px;
        transition: all 0.2s;

        &.more {
          background: #e2e3e5;
          color: #41464b;
          &:hover {
            background: #6c757d;
            color: white;
          }
        }

        &.view {
          background: #e0f2fe;
          color: #0369a1;
          &:hover {
            background: #0284c7;
            color: white;
          }
        }

        &.delete {
          background: #fee2e2;
          color: #991b1b;
          &:hover {
            background: #dc2626;
            color: white;
          }
        }

        &.read {
          background: #d1e7dd;
          color: #0f5132;
          &:hover {
            background: #198754;
            color: white;
          }
        }
      }
    }
  }
}

// 事务卡片特有样式
.task-card {
  .card-header {
    .task-type {
      font-size: 13px;
      font-weight: 600;
      color: #1e293b;
    }

    .task-status {
      padding: 2px 10px;
      border-radius: 30px;
      font-size: 11px;
      font-weight: 500;

      &.status-pending {
        background: #fee2e2;
        color: #991b1b;
      }
      &.status-progress {
        background: #fff3cd;
        color: #856404;
      }
      &.status-completed {
        background: #d1e7dd;
        color: #0f5132;
      }
      &.status-cancelled {
        background: #e2e3e5;
        color: #41464b;
      }
    }
  }

  .task-title {
    margin: 0 0 8px 0;
    font-size: 16px;
    font-weight: 600;
    color: #1e293b;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .task-desc {
    margin: 0 0 12px 0;
    font-size: 13px;
    color: #64748b;
    line-height: 1.5;
    display: -webkit-box;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    flex: 1;
  }

  .task-meta {
    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      color: #94a3b8;
      margin-bottom: 4px;

      .el-icon {
        font-size: 14px;
      }
    }
  }
}

// 消息卡片特有样式
.message-card {
  &.unread {
    background: linear-gradient(135deg, #fff7e6, #fff1d6);
    border-left: 4px solid #e6a23c;
  }

  .card-header {
    .message-type {
      font-size: 13px;
      font-weight: 600;
      color: #1e293b;
    }

    .unread-tag {
      background: #e6a23c;
      color: white;
      padding: 2px 8px;
      border-radius: 20px;
      font-size: 10px;
      font-weight: 500;
    }
  }

  .message-title {
    margin: 0 0 8px 0;
    font-size: 16px;
    font-weight: 600;
    color: #1e293b;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .message-content {
    margin: 0;
    font-size: 13px;
    color: #64748b;
    line-height: 1.5;
    display: -webkit-box;
    line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    flex: 1;
  }
}

.empty-state {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;

  .empty-tip {
    color: #94a3b8;
    font-size: 14px;
    margin-top: 8px;
  }
}

.pagination-wrapper {
  flex-shrink: 0;
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

// 表单样式
.fancy-form {
  .form-section {
    background: #f8fafc;
    border-radius: 16px;
    padding: 20px;
    margin-bottom: 20px;

    .section-title {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 20px;
      font-size: 16px;
      font-weight: 600;
      color: #1e293b;
    }
  }

  .form-tip {
    font-size: 12px;
    color: #94a3b8;
    margin-top: 4px;
  }
}

// 详情样式
.task-detail {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .detail-title {
    font-size: 20px;
    font-weight: 600;
    color: #1e293b;
    margin: 0 0 12px 0;
  }

  .detail-description {
    font-size: 14px;
    color: #64748b;
    line-height: 1.6;
    margin-bottom: 24px;
    padding: 16px;
    background: #f8fafc;
    border-radius: 12px;
  }

  .detail-meta-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    margin-bottom: 24px;

    .meta-item {
      .meta-label {
        display: block;
        font-size: 12px;
        color: #94a3b8;
        margin-bottom: 4px;
      }

      .meta-value {
        font-size: 14px;
        font-weight: 500;
        color: #1e293b;
      }
    }
  }

  .status-update {
    .update-buttons {
      display: flex;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
      
      .update-label {
        font-size: 14px;
        color: #1e293b;
      }
    }
  }
}

.fancy-btn {
  border-radius: 30px;
  padding: 10px 20px;
  font-weight: 500;
  transition: all 0.3s;
  border: none;
  display: inline-flex;
  align-items: center;
  gap: 8px;

  &.primary {
    background: linear-gradient(135deg, #3b82f6, #8b5cf6);
    color: white;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 10px 20px -5px rgba(59, 130, 246, 0.4);
    }
  }

  &.search {
    background: white;
    color: #3b82f6;
    border: 1px solid #e2e8f0;

    &:hover {
      background: #3b82f6;
      color: white;
    }
  }

  &.reset {
    background: white;
    color: #64748b;
    border: 1px solid #e2e8f0;

    &:hover {
      background: #64748b;
      color: white;
      border-color: #64748b;
    }
  }

  &.cancel {
    background: white;
    color: #64748b;
    border: 1px solid #e2e8f0;
  }

  &.confirm {
    background: linear-gradient(135deg, #3b82f6, #8b5cf6);
    color: white;
  }

  &.close {
    background: white;
    color: #64748b;
    border: 1px solid #e2e8f0;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

@media (max-width: 1366px) {
  .fancy-container {
    padding: 16px;
  }

  .header-glass {
    padding: 16px 20px;
  }

  .search-glass {
    padding: 12px 20px;
  }

  .task-card, .message-card {
    height: 300px;
  }
}
</style>
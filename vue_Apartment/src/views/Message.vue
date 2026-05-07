<template>
  <div class="message-container fancy-container">
    <!-- 头部：发布事务按钮和未读消息数 -->
    <div class="header-glass">
      <div class="title-wrapper">
        <span class="title-icon">📬</span>
        <h2 class="page-title">消息中心</h2>
        <span class="title-badge">房东</span>
      </div>
      <div class="header-right">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="unread-badge">
          <el-button class="fancy-btn primary" @click="showPublishDialog = true">
            <el-icon><Plus /></el-icon>
            发布事务
          </el-button>
        </el-badge>
        <el-button class="fancy-btn secondary" @click="markAllAsRead" :disabled="unreadCount === 0">
          <el-icon><Check /></el-icon>
          全部已读
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
              placeholder="搜索消息内容..."
              clearable
              @keyup.enter="handleSearch"
              class="fancy-input"
            />
          </div>
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

    <!-- 消息列表（时间轴形式） -->
    <div class="messages-container" v-loading="loading">
      <div v-if="messageList.length === 0" class="empty-state">
        <el-empty description="暂无消息">
          <p class="empty-tip">点击"发布事务"按钮向租客发送通知</p>
        </el-empty>
      </div>

      <el-timeline>
        <el-timeline-item
          v-for="item in messageList"
          :key="item.id"
          :type="getMessageType(item.messageType)"
          :color="item.isRead ? '#909399' : '#3b82f6'"
          :timestamp="formatDateTime(item.createTime)"
          placement="top"
        >
          <div class="message-card" :class="{ 'unread': item.isRead === 0 }">
            <div class="message-header">
              <div class="message-type">
                <el-tag :type="getMessageTagType(item.messageType)" size="small" effect="light">
                  {{ getMessageTypeText(item.messageType) }}
                </el-tag>
                <span v-if="item.isRead === 0" class="unread-dot"></span>
              </div>
              <div class="message-actions">
                <el-tooltip content="标记为已读" placement="top" v-if="item.isRead === 0">
                  <el-button class="action-icon" size="small" @click="markAsRead(item.id)">
                    <el-icon><Check /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button class="action-icon" size="small" @click="deleteMessage(item.id)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>
            <h4 class="message-title">{{ item.title }}</h4>
            <p class="message-content">{{ item.content }}</p>
            <div class="message-footer">
              <span class="message-time">
                <el-icon><Timer /></el-icon>
                {{ formatDateTime(item.createTime) }}
              </span>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="messageList.length > 0">
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

    <!-- 发布事务对话框 -->
    <el-dialog
      v-model="showPublishDialog"
      title="📝 发布新事务"
      width="45%"
      class="fancy-dialog"
      :before-close="() => showPublishDialog = false"
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
            <span>通知设置</span>
          </div>

          <el-form-item label="指定责任人" prop="assigneeId">
            <el-select 
              v-model="taskForm.assigneeId" 
              placeholder="可选，不指定则通知所有租客"
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
          <el-button class="fancy-btn cancel" @click="showPublishDialog = false">
            取消
          </el-button>
          <el-button class="fancy-btn confirm" @click="publishTask" :loading="publishing">
            <el-icon><Check /></el-icon>
            确认发布
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Search, Refresh, Check, Delete, Timer, ArrowRight } from "@element-plus/icons-vue";

const { proxy } = getCurrentInstance();

// 状态
const loading = ref(false);
const publishing = ref(false);
const showPublishDialog = ref(false);
const messageList = ref([]);
const unreadCount = ref(0);
const tenantList = ref([]);

// 搜索表单
const searchForm = reactive({ keyword: "" });

// 分页配置
const config = reactive({
  keyword: "",
  total: 0,
  page: 1,
  limit: 10,
});

// 发布事务表单
const taskForm = reactive({
  title:"",
  taskType: 1,
  description:"",
  assigneeId:"",
  deadline:"",
});

// 表单校验规则
const taskRules = {
  title: [
    { required: true, message: "请输入事务标题", trigger: "blur" },
    { min: 2, max: 50, message: "长度在 2 到 50 个字符", trigger: "blur" }
  ],
  taskType: [
    { required: true, message: "请选择事务类型", trigger: "change" }
  ],
  description: [
    { required: true, message: "请输入事务描述", trigger: "blur" },
    { min: 5, max: 200, message: "长度在 5 到 200 个字符", trigger: "blur" }
  ],
};

// 获取消息类型文本
const getMessageTypeText = (type) => {
  const map = {
    1: "账单提醒",
    2: "事务分配",
    3: "状态变更"
  };
  return map[type] || "系统通知";
};

// 获取消息标签类型
const getMessageTagType = (type) => {
  const map = {
    1: "danger",
    2: "warning",
    3: "info"
  };
  return map[type] || "info";
};

// 获取时间轴类型
const getMessageType = (type) => {
  const map = {
    1: "danger",
    2: "warning",
    3: "info"
  };
  return map[type] || "info";
};

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return "";
  const date = new Date(dateStr);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
};

// 获取租客列表
const getTenantList = async () => {
  try {
    const apartmentId = localStorage.getItem("hasApartment");
    if (!apartmentId) {
      console.warn("没有公寓ID");
      return;
    }
    const result = await proxy.$api.getTenantList({ apartmentId });
    tenantList.value = result?.list || [];
  } catch (error) {
    console.error("获取租客列表失败:", error);
  }
};

// 获取消息列表
const getMessageList = async () => {
  loading.value = true;
  try {
    const params = {
      keyword: config.keyword,
      page: config.page,
      limit: config.limit,
    };
    const result = await proxy.$api.getMessageList(params);
    if (result && result.list) {
      messageList.value = result.list;
      config.total = result.total || 0;
    }
  } catch (error) {
    console.error("获取消息列表失败:", error);
    ElMessage.error("获取数据失败");
  } finally {
    loading.value = false;
  }
};

// 获取未读消息数
const getUnreadCount = async () => {
  try {
    const count = await proxy.$api.getUnreadCount();
    unreadCount.value = count || 0;
  } catch (error) {
    console.error("获取未读消息数失败:", error);
  }
};

// 发布事务
// 发布事务
const publishTask = async () => {
  proxy.$refs.taskFormRef.validate(async (valid) => {
    if (!valid) return;

    publishing.value = true;
    try {
      // 处理数据：将空字符串的 assigneeId 转换为 null
      const submitData = {
        ...taskForm,
        assigneeId: taskForm.assigneeId || null  // 关键修复
      };
      
      const result = await proxy.$api.publishTask(submitData);
      ElMessage.success(result.message || "发布成功");
      showPublishDialog.value = false;
      
      // 刷新消息列表和未读数
      getMessageList();
      getUnreadCount();
      
      // 重置表单
      Object.assign(taskForm, {
        title:"",
        taskType: 1,
        description:"",
        assigneeId:"",
        deadline:"",
      });
    } catch (error) {
      console.error("发布失败:", error);
      ElMessage.error("发布失败");
    } finally {
      publishing.value = false;
    }
  });
};

// 标记单个消息为已读
const markAsRead = async (id) => {
  try {
    await proxy.$api.markMessageRead(id);
    ElMessage.success("已标记为已读");
    getMessageList();
    getUnreadCount();
  } catch (error) {
    ElMessage.error("操作失败");
  }
};

// 全部标记为已读
const markAllAsRead = async () => {
  try {
    await proxy.$api.markAllRead();
    ElMessage.success("全部已读");
    getMessageList();
    getUnreadCount();
  } catch (error) {
    ElMessage.error("操作失败");
  }
};

// 删除消息
const deleteMessage = (id) => {
  ElMessageBox.confirm("确认删除这条消息吗？", "删除确认", {
    confirmButtonText: "确定删除",
    cancelButtonText: "取消",
    type: "warning",
  }).then(async () => {
    try {
      await proxy.$api.deleteMessage(id);
      ElMessage.success("删除成功");
      getMessageList();
      getUnreadCount();
    } catch (error) {
      ElMessage.error("删除失败");
    }
  }).catch(() => {});
};

// 搜索
const handleSearch = () => {
  config.keyword = searchForm.keyword;
  config.page = 1;
  getMessageList();
};

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = "";
  config.keyword = "";
  config.page = 1;
  getMessageList();
};

// 分页变化
const handleChange = (page) => {
  config.page = page;
  getMessageList();
};

onMounted(() => {
  getMessageList();
  getUnreadCount();
  getTenantList();
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

    .unread-badge {
      :deep(.el-badge__content) {
        background: #f56c6c;
      }
    }
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
  }

  .search-input-wrapper {
    position: relative;
    width: 320px;

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

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 4px 20px 20px 20px;
  
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

.message-card {
  background: white;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  border: 1px solid #f0f0f0;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.05);
    border-color: #3b82f6;
  }

  &.unread {
    background: linear-gradient(135deg, #f0f9ff, #e6f0ff);
    border-left: 4px solid #3b82f6;
  }

  .message-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .message-type {
      display: flex;
      align-items: center;
      gap: 8px;

      .unread-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #3b82f6;
        box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2);
      }
    }

    .message-actions {
      display: flex;
      gap: 8px;

      .action-icon {
        width: 28px;
        height: 28px;
        padding: 0;
        border-radius: 8px;
        background: white;
        border: 1px solid #e2e8f0;
        color: #64748b;

        &:hover {
          background: #3b82f6;
          color: white;
          border-color: #3b82f6;
        }
      }
    }
  }

  .message-title {
    font-size: 16px;
    font-weight: 600;
    color: #1e293b;
    margin: 0 0 8px 0;
  }

  .message-content {
    font-size: 14px;
    color: #64748b;
    line-height: 1.6;
    margin: 0 0 12px 0;
  }

  .message-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    color: #94a3b8;

    .message-time {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .biz-link {
      display: flex;
      align-items: center;
      gap: 4px;
      color: #3b82f6;
      cursor: pointer;
      
      &:hover {
        text-decoration: underline;
      }
    }
  }
}

.empty-state {
  height: 400px;
  display: flex;
  flex-direction: column;
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

  &.secondary {
    background: white;
    color: #64748b;
    border: 1px solid #e2e8f0;

    &:hover:not(:disabled) {
      background: #3b82f6;
      color: white;
      border-color: #3b82f6;
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
}
</style>
<template>
  <div class="user-header">
    <el-button type="primary" @click="handleAdd">新增公寓</el-button>
    <el-form :inline="true" :model="formInline">
      <el-form-item>
        <el-input
          placeholder="请输入公寓名称或公寓地址"
          v-model="formInline.keyword"
        ></el-input>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </el-form-item>
    </el-form>
  </div>
  <div class="table">
    <el-table :data="tableData" style="width: 100%">
      <el-table-column
        v-for="item in tableLabel"
        :key="item.prop"
        :width="item.width"
        :prop="item.prop"
        :label="item.label"
        :show-overflow-tooltip="item.prop === 'description'"
        :align="item.prop === 'totalRooms' ? 'center' : 'left'"
      />
      <el-table-column fixed="right" label="操作" width="300" align="center">
        <template #default="scope">
          <div class="action-buttons">
            <el-button
              type="primary"
              size="default"
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              type="success"
              size="default"
              @click="handleRooms(scope.row)"
            >
              房间
            </el-button>
            <el-button
              type="danger"
              size="default"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
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

  <!-- 公寓表单对话框 -->
  <el-dialog
    v-model="dialogVisible"
    :title="action == 'add' ? '新增公寓' : '编辑公寓'"
    width="35%"
    :before-close="handleClose"
  >
    <el-form
      :inline="true"
      :model="formApartment"
      :rules="rules"
      ref="apartmentForm"
    >
      <el-row>
        <el-col :span="24">
          <el-form-item label="公寓名称" prop="name" style="width: 100%">
            <el-input
              v-model="formApartment.name"
              placeholder="请输入公寓名称"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="公寓地址" prop="address" style="width: 100%">
            <el-input
              v-model="formApartment.address"
              placeholder="请输入公寓地址"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item label="总房间数" prop="totalRooms">
            <el-input-number
              v-model="formApartment.totalRooms"
              :min="1"
              :max="10"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="邀请码" prop="inviteCode">
            <el-input
              v-model="formApartment.inviteCode"
              placeholder="自动生成"
              :disabled="true"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item label="公寓描述" prop="description" style="width: 100%">
            <el-input
              v-model="formApartment.description"
              type="textarea"
              :rows="3"
              placeholder="请输入公寓描述"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row style="justify-content: flex-end">
        <el-form-item>
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="primary" @click="onSubmit">确定</el-button>
        </el-form-item>
      </el-row>
    </el-form>
  </el-dialog>
  <!-- 房间管理对话框 -->
  <el-dialog
    v-model="roomDialogVisible"
    :title="`房间管理 - ${currentApartment.name}`"
    width="60%"
    :before-close="() => (roomDialogVisible = false)"
  >
    <div class="room-dialog-header">
      <el-button type="primary" size="small" @click="handleAddRoom"
        >新增房间</el-button
      >
      <span class="room-info">总房间数: {{ currentApartment.totalRooms }}</span>
    </div>

    <el-table :data="roomList" style="width: 100%" v-loading="roomLoading">
      <el-table-column prop="roomNumber" label="房间名称" width="120">
        <template #default="{ row }">
          <el-input
            v-model="row.roomNumber"
            size="small"
            @change="() => handleUpdateRoom(row)"
          />
        </template>
      </el-table-column>

      <el-table-column prop="tenantName" label="租客姓名" width="120">
        <template #default="{ row }">
          <el-select
            v-model="row.currentUserId"
            placeholder="请选择租客"
            size="small"
            @change="() => handleUpdateRoom(row)"
          >
            <el-option
              v-for="tenant in tenantList"
              :key="tenant.id"
              :label="tenant.username"
              :value="tenant.id"
            />
          </el-select>
        </template>
      </el-table-column>

      <el-table-column prop="area" label="面积(㎡)" width="100">
        <template #default="{ row }">
          <el-input-number
            v-model="row.area"
            :precision="2"
            :step="0.5"
            size="small"
            @change="() => handleUpdateRoom(row)"
          />
        </template>
      </el-table-column>

      <el-table-column prop="statusText" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.statusText }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button
            type="primary"
            size="small"
            @click="handleAssignTenant(row)"
            >保存</el-button
          >
          <el-button type="danger" size="small" @click="handleDeleteRoom(row)"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="roomDialogVisible = false">关 闭</el-button>
      </span>
    </template>
  </el-dialog>
  <!-- 新增房间对话框 -->
  <el-dialog
    v-model="addRoomDialogVisible"
    title="新增房间"
    width="30%"
    :before-close="() => (addRoomDialogVisible = false)"
  >
    <el-form
      :model="newRoomForm"
      :rules="addRoomRules"
      ref="addRoomFormRef"
      label-width="100px"
    >
      <el-form-item label="房间名称" prop="roomNumber">
        <el-input
          v-model="newRoomForm.roomNumber"
          placeholder="请输入房间名称"
        />
      </el-form-item>

      <el-form-item label="面积(㎡)" prop="area">
        <el-input-number
          v-model="newRoomForm.area"
          :precision="2"
          :step="0.5"
          :min="5"
          :max="50"
        />
      </el-form-item>

      <el-form-item label="分配租客">
        <el-select
          v-model="newRoomForm.currentUserId"
          placeholder="请选择租客（可选）"
          clearable
        >
          <el-option
            v-for="tenant in tenantList"
            :key="tenant.id"
            :label="tenant.username"
            :value="tenant.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="addRoomDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddRoom">确 定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ElMessage, ElMessageBox } from "element-plus";
import { ref, getCurrentInstance, onMounted, reactive, nextTick,markRaw} from "vue";
import { Delete } from '@element-plus/icons-vue'
const { proxy } = getCurrentInstance();
const action = ref("add");
const dialogVisible = ref(false);
const loading = ref(false);
// 房间管理相关
const roomDialogVisible = ref(false);
const currentApartment = ref({});
const roomList = ref([]);
const roomLoading = ref(false);
// 新增房间对话框
const addRoomDialogVisible = ref(false);
const newRoomForm = reactive({
  apartmentId: 0,
  roomNumber: "",
  area: 10.0,
  currentUserId: null,
});
const addRoomFormRef = ref(null);
// 新增房间表单校验规则
const addRoomRules = reactive({
  roomNumber: [
    { required: true, message: "房间名称是必填项", trigger: "blur" },
  ],
  area: [{ required: true, message: "面积是必填项", trigger: "blur" }],
});

// 打开新增房间对话框
const handleAddRoom = () => {
  newRoomForm.apartmentId = currentApartment.value.id;
  newRoomForm.roomNumber = "";
  newRoomForm.area = 10.0;
  newRoomForm.currentUserId = null;
  addRoomDialogVisible.value = true;
};

// 提交新增房间
const submitAddRoom = async () => {
  try {
    const result = await proxy.$api.addRoom(newRoomForm);
    if (result) {
      ElMessage.success("新增房间成功");
      addRoomDialogVisible.value = false;
      // 刷新房间列表
      await getRoomList(currentApartment.value.id);
    }
  } catch (error) {
    console.error("新增房间失败:", error);
    ElMessage.error("新增房间失败");
  }
};
// 处理房间管理
const handleRooms = async (row) => {
  currentApartment.value = row;
  roomDialogVisible.value = true;
  
  // 同时获取房间列表和租客列表
  await Promise.all([
    getRoomList(row.id),
    getTenantList(row.id)  // 获取租客列表用于分配
  ]);
};
// 房间表格列配置
const roomColumns = ref([
  { prop: "roomNumber", label: "房间名称", width: 120 },
  { prop: "tenantName", label: "租客姓名", width: 120 },
  { prop: "area", label: "面积(㎡)", width: 100 },
  { prop: "statusText", label: "状态", width: 80 },
  { prop: "action", label: "操作", width: 180 },
]);
// 获取房间列表
const getRoomList = async (apartmentId) => {
  roomLoading.value = true;
  try {
    const result = await proxy.$api.getRoomList({ apartmentId });
    if (result && result.list) {
      roomList.value = result.list.map((item) => ({
        ...item,
        statusText: item.status === 1 ? "已入住" : "空置"
      }));
    }
  } catch (error) {
    console.error("获取房间列表失败:", error);
  } finally {
    roomLoading.value = false;
  }
};
// 公寓表单数据
const formApartment = reactive({
  name: "",
  address: "",
  totalRooms: 3,
  inviteCode: "",
  description: "",
  status: 1,
});

// 表单校验规则
const rules = reactive({
  name: [{ required: true, message: "公寓名称是必填项", trigger: "blur" }],
  address: [{ required: true, message: "公寓地址是必填项", trigger: "blur" }],
  totalRooms: [{ required: true, message: "房间数是必填项", trigger: "blur" }],
});

// 搜索表单
const formInline = reactive({
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

const tableLabel = reactive([
  {
    prop: "name",
    label: "公寓名称",
    width: 200,
  },
  {
    prop: "address",
    label: "公寓地址",
    width: 280,
  },
  {
    prop: "totalRooms",
    label: "房间总数",
    width: 100,
  },
  {
    prop: "inviteCode",
    label: "邀请码",
    width: 100,
  },
  {
    prop: "description",
    label: "公寓描述",
    width: 400,
  },
  {
    prop: "statusText",
    label: "状态",
    width: 80,
  },
]);

// 生成随机邀请码
const generateInviteCode = () => {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  let code = "";
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return code;
};

// 获取公寓数据
const getApartmentData = async () => {
  loading.value = true;
  try {
    const params = {
      keyword: config.keyword,
      page: config.page,
      limit: config.limit,
    };

    const result = await proxy.$api.getApartmentList(params);

    if (result && result.list) {
      tableData.value = result.list.map((item) => ({
        ...item,
        statusText: item.status === 1 ? "正常" : "已解散",
      }));
      config.total = result.count || 0;
    }
  } catch (error) {
    console.error("获取公寓列表失败:", error);
    ElMessage.error("获取数据失败");
  } finally {
    loading.value = false;
  }
};

// 分页变化
const handleChange = (page) => {
  config.page = page;
  getApartmentData();
};

// 处理搜索
const handleSearch = () => {
  config.keyword = formInline.keyword;
  config.page = 1;
  getApartmentData();
};

// 重置搜索
const resetSearch = () => {
  formInline.keyword = "";
  config.keyword = "";
  config.page = 1;
  getApartmentData();
};

// 处理编辑
const handleEdit = (row) => {
  action.value = "edit";
  dialogVisible.value = true;
  nextTick(() => {
    Object.assign(formApartment, {
      id: row.id,
      name: row.name,
      address: row.address,
      totalRooms: row.totalRooms,
      inviteCode: row.inviteCode,
      description: row.description,
      status: row.status,
    });
  });
};

// 编辑房间
const handleEditRoom = (row) => {
  // 打开编辑房间的对话框
  console.log("编辑房间:", row);
  // TODO: 实现房间编辑功能
};


const handleAssignTenant = (row) => {
   ElMessage({
    message: '保存成功',
    type: 'success',
  })
};

// 删除房间
const handleDeleteRoom = async (row) => {
  ElMessageBox({
    title: '编辑房间',
    message: `确认删除房间 "${row.roomNumber}" 吗？`,
    type: 'warning',
    icon: markRaw(Delete),
    showCancelButton: true,
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  })
  
    .then(async () => {
      
      try {
        await proxy.$api.deleteRoom({ id: row.id });
        ElMessage.success("删除成功");
        await getRoomList(currentApartment.value.id);
      } catch (error) {
        ElMessage.error("删除失败");
      }
    })
    .catch(() => {});
};
const tenantList = ref([]);

// 获取租客列表
const getTenantList = async (apartmentId) => {
  try {
    const result = await proxy.$api.getTenantList({ apartmentId });
    if (result && result.list) {
      tenantList.value = result.list;
    }
  } catch (error) {
    console.error("获取租客列表失败:", error);
  }
};

// 更新房间
const handleUpdateRoom = async (row) => {
  try {
    await proxy.$api.updateRoom(row);
    ElMessage.success("更新成功");
  } catch (error) {
    ElMessage.error("更新失败");
  }
};

// 处理关闭
const handleClose = () => {
  dialogVisible.value = false;
  proxy.$refs["apartmentForm"]?.resetFields();
};

// 处理新增
const handleAdd = () => {
  dialogVisible.value = true;
  action.value = "add";
  Object.assign(formApartment, {
    name: "",
    address: "",
    totalRooms: 3,
    inviteCode: generateInviteCode(),
    description: "",
    status: 1,
  });
};

// 处理取消
const handleCancel = () => {
  dialogVisible.value = false;
  proxy.$refs["apartmentForm"]?.resetFields();
};

// 处理删除
const handleDelete = async (row) => {
  ElMessageBox.confirm(`确认删除公寓 "${row.name}" 吗？`)
    .then(async () => {
      try {
        await proxy.$api.deleteApartment({
          id: row.id,
        });
        ElMessage.success("删除成功");
        getApartmentData();
      } catch (error) {
        ElMessage.error("删除失败");
      }
    })
    .catch(() => {});
};

// 提交表单
const onSubmit = () => {
  proxy.$refs["apartmentForm"].validate(async (valid) => {
    if (valid) {
      try {
        let res = null;
        if (action.value === "add") {
          res = await proxy.$api.addApartment(formApartment);
          ElMessage.success("新增公寓成功");
        } else {
          res = await proxy.$api.editApartment(formApartment);
          ElMessage.success("编辑公寓成功");
        }
        if (res) {
          dialogVisible.value = false;
          proxy.$refs["apartmentForm"].resetFields();
          getApartmentData();
        }
      } catch (error) {
        ElMessage.error("操作失败");
      }
    } else {
      ElMessage.error("请输入正确信息");
    }
  });
};

onMounted(() => {
  if (!formApartment.inviteCode) {
    formApartment.inviteCode = generateInviteCode();
  }
  getApartmentData();
});
</script>

<style scoped lang="less">
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
.room-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  .room-info {
    font-size: 14px;
    color: #666;
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
</style>

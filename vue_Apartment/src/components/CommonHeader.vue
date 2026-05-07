<template>
  <div class="header">
    <div class="l-content">
      <el-button size="small" @click="handleCollapse">
        <component class="icons" is="menu"></component>
      </el-button>
      <el-breadcrumb separator="/" class="bread">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
         <el-breadcrumb-item v-if="current" :to="{ path: current.path }">{{ current.label }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="r-content">
      <el-dropdown>
        <span class="el-dropdown-link">
          <img :src="getImageUrl('user')" class="user" />
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item>个人中心</el-dropdown-item>
            <el-dropdown-item @click="handleLoginout">退出</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { useALLDataStore } from "@/stores";
import { useRoute } from "vue-router";
import router from "../router";
const stores = useALLDataStore();
const getImageUrl = (user) => {
  return new URL(`../assets/images/${user}.webp`, import.meta.url).href;
};
const handleLoginout = () => {
  stores.clean();
  router.push("/login");
};
const handleCollapse = () => {
  stores.state.isCollapse = !stores.state.isCollapse;
};
const current = computed(() => stores.state.currentMenu
 
);
</script>

<style lang="less" scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  height: 100%;
  background-color: #333;
}
.icons {
  width: 20px;
  height: 20px;
}

.l-content {
  display: flex;
  align-items: center;
  .el-button {
    margin-right: 20px;
  }
}
.r-content {
  .user {
    width: 40px;
    height: 40px;
    border-radius: 50%;
  }
}
:deep(.bread span) {
  color: #fff !important;
  cursor: pointer !important;
}
</style>

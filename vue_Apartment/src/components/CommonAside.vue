<template>
  <el-aside :width="width" style="height: 100vh">
    <el-menu
      :background-color="'#545c64'"
      :text-color="'#fff'"
      :active-text-color="'#ffd04b'"
      :collapse="isCollapse"
      :collapse-transition="false"
    >
      <h3 v-show="!isCollapse">合租公寓后台管理</h3>
      <h3 v-show="isCollapse">后台</h3>
      <el-menu-item
        v-for="item in noChildren"
        :index="item.path"
        :key="item.path"
        @click="clickMenu(item)"
      >
        <component class="icons" :is="item.icon"></component>
        <span>{{ item.label }}</span>
      </el-menu-item>
      <el-sub-menu
        v-for="item in hasChildren"
        :index="item.path"
        :key="item.path"
      >
        <template #title>
          <component class="icons" :is="item.icon"></component>
          <span>{{ item.label }}</span>
        </template>
        <el-menu-item-group>
          <el-menu-item
            v-for="subItem in item.children"
            :index="subItem.path"
            :key="subItem.path"
            @click="clickMenu(subItem)"
          >
            <component class="icons" :is="subItem.icon"></component>
            <span>{{ subItem.label }}</span>
          </el-menu-item>
        </el-menu-item-group>
      </el-sub-menu>
    </el-menu>
  </el-aside>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useALLDataStore } from "@/stores";
const router = useRouter();


const list = computed(() => stores.state.menuList);
const noChildren = computed(() => list.value.filter((item) => !item.children));
const hasChildren = computed(() => list.value.filter((item) => item.children));
const stores = useALLDataStore();
const isCollapse = computed(() => stores.state.isCollapse);
const width = computed(() => (stores.state.isCollapse ? "64px" : "180px"));

const clickMenu = (item) => {
  router.push(item.path);
  stores.selectMenu(item);
};
</script>

<style lang="less" scoped>
.icons {
  width: 18px;
  height: 18px;
  margin-right: 5px;
}
.el-menu {
  border-right: none;
  h3 {
    line-height: 48px;
    color: #fff;
    text-align: center;
  }
}
.el-aside {
  height: 100%;
  background-color: #545c64;
}
</style>

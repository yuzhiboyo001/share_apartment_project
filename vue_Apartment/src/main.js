import { createApp } from "vue";

import App from "./App.vue";
import "@/assets/less/index.less";
import router from "./router";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";
import { createPinia } from "pinia";
import api from "@/api/api.js";
import config from "@/config";
console.log('当前环境:', config.env);
console.log('Mock开关:', config.mock);
console.log('Base API:', config.baseApi);
console.log('Mock API:', config.mockApi);
console.log('当前环境:', import.meta.env.VITE_APP_ENV);
console.log('所有环境变量:', import.meta.env);
// Mock.js开关
if (config.mock) {
    import("@/api/mock.js").then(() => {
        console.log("Mock拦截器已启用");
    });
}
import { useALLDataStore } from "@/stores";

function isRoute(to) {
  let res = router.getRoutes();
  let resFil = res.filter((item) => item.path === to.path);
  return resFil.length > 0;
}

// 修复路由守卫逻辑
router.beforeEach((to, from, next) => {
  const store = useALLDataStore();

  // 检查是否是登录页面，如果是则直接放行
  if (to.path === "/login") {
    next();
    return;
  }

  // 检查路由是否存在
  if (!isRoute(to)) {
    next("/404"); // 重定向到首页而不是404，避免循环
    return;
  }

  // 检查token是否存在
  if (!store.state.token) {
    next("/login"); // 重定向到登录页
    return;
  }

  // 所有检查通过，放行
  next();
});
// 开发环境下，忽略 ECharts 事件监听优化
if (import.meta.env.DEV) {
  const originalAddEventListener = EventTarget.prototype.addEventListener;
  EventTarget.prototype.addEventListener = function(type, listener, options) {
    let modifiedOptions = options;
    if (type === 'wheel' || type === 'mousewheel' || type === 'touchstart' || type === 'touchmove') {
      // 如果 options 是布尔值，转换为对象
      if (typeof modifiedOptions === 'boolean') {
        modifiedOptions = { capture: modifiedOptions, passive: true };
      } else if (modifiedOptions === null || modifiedOptions === undefined) {
        modifiedOptions = { passive: true };
      } else if (typeof modifiedOptions === 'object') {
        // 确保 passive 为 true
        modifiedOptions = { ...modifiedOptions, passive: true };
      }
    }
    return originalAddEventListener.call(this, type, listener, modifiedOptions);
  };
}
const pinia = createPinia();
const app = createApp(App);
app.config.globalProperties.$api = api;
app.use(ElementPlus);
app.use(pinia);
const store = useALLDataStore();
store.addMenu(router, "refresh");
app.use(router).mount("#app");
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

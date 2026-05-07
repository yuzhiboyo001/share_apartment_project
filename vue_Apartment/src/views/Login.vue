<template>
  <div class="body-login">
    <!-- 登录表单 -->
    <el-form
      v-if="!isRegister && !isForgot"
      :model="loginForm"
      :rules="loginRules"
      ref="loginFormRef"
      class="login-container"
    >
      <h1>欢迎登录</h1>
      <el-form-item prop="phone">
        <el-input
          v-model="loginForm.phone"
          placeholder="请输入手机号"
          type="input"
        />
      </el-form-item>
      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          placeholder="请输入密码"
          type="password"
          show-password
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleLogin" style="width: 100%"
          >登录</el-button
        >
      </el-form-item>
      <div class="form-footer">
        <el-link type="primary" @click="isRegister = true">注册账号</el-link>
        <el-link type="primary" @click="isForgot = true">忘记密码</el-link>
      </div>
    </el-form>

    <!-- 注册表单 -->
    <el-form
      v-else-if="isRegister"
      :model="registerForm"
      :rules="registerRules"
      ref="registerFormRef"
      class="login-container"
    >
      <h1>注册账号</h1>
      
      <!-- 身份选择 -->
      <el-form-item prop="role">
        <el-radio-group v-model="registerForm.role" class="role-select">
          <el-radio :label="0" size="large">
            <el-icon><User /></el-icon>
            <span>我是租客</span>
          </el-radio>
          <el-radio :label="1" size="large">
            <el-icon><HomeFilled /></el-icon>
            <span>我是房东</span>
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item prop="phone">
        <el-input
          v-model="registerForm.phone"
          placeholder="请输入手机号"
          type="input"
        />
      </el-form-item>
      
      <el-form-item prop="password">
        <el-input
          v-model="registerForm.password"
          placeholder="请输入密码"
          type="password"
          show-password
        />
      </el-form-item>
      
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          placeholder="请确认密码"
          type="password"
          show-password
        />
      </el-form-item>

      <!-- 图形验证码 -->
      <el-form-item prop="captcha">
        <div class="captcha-container">
          <el-input
            v-model="registerForm.captcha"
            placeholder="请输入验证码"
            style="flex: 1; margin-right: 10px"
          />
          <div class="captcha-box" @click="refreshCaptcha">
            <canvas ref="captchaCanvas" width="100" height="40"></canvas>
          </div>
          <el-button type="primary" link @click="refreshCaptcha"
            >刷新</el-button
          >
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleRegister" style="width: 100%"
          >注册</el-button
        >
      </el-form-item>
      <div class="form-footer">
        <el-link
          type="primary"
          @click="
            isRegister = false;
            resetForm();
          "
          >返回登录</el-link
        >
      </div>
    </el-form>

    <!-- 忘记密码表单 -->
    <el-form
      v-else-if="isForgot"
      :model="forgotForm"
      :rules="forgotRules"
      ref="forgotFormRef"
      class="login-container"
    >
      <h1>重置密码</h1>
      <el-form-item prop="phone">
        <el-input
          v-model="forgotForm.phone"
          placeholder="请输入手机号"
          type="input"
        />
      </el-form-item>
      <el-form-item prop="newPassword">
        <el-input
          v-model="forgotForm.newPassword"
          placeholder="请输入新密码"
          type="password"
          show-password
        />
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="forgotForm.confirmPassword"
          placeholder="请确认新密码"
          type="password"
          show-password
        />
      </el-form-item>

      <!-- 图形验证码 -->
      <el-form-item prop="captcha">
        <div class="captcha-container">
          <el-input
            v-model="forgotForm.captcha"
            placeholder="请输入验证码"
            style="flex: 1; margin-right: 10px"
          />
          <div class="captcha-box" @click="refreshCaptcha">
            <canvas ref="captchaCanvas" width="100" height="40"></canvas>
          </div>
          <el-button type="primary" link @click="refreshCaptcha"
            >刷新</el-button
          >
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleForgot" style="width: 100%"
          >重置密码</el-button
        >
      </el-form-item>
      <div class="form-footer">
        <el-link
          type="primary"
          @click="
            isForgot = false;
            resetForm();
          "
          >返回登录</el-link
        >
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { reactive, ref, getCurrentInstance, onMounted, watch } from "vue";
import { useALLDataStore } from "@/stores";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { lo } from "element-plus/es/locales.mjs";

const { proxy } = getCurrentInstance();
const store = useALLDataStore();
const router = useRouter();

// 表单引用
const loginFormRef = ref(null);
const registerFormRef = ref(null);
const forgotFormRef = ref(null);

// 状态控制
const isRegister = ref(false);
const isForgot = ref(false);

// 记录表单是否已经初始化过验证码
const formInitialized = reactive({
  register: false,
  forgot: false,
});

// 登录表单
const loginForm = reactive({
  phone: "13800138001",
  password: "123456",
});

// 注册表单
const registerForm = reactive({
  phone: "13332869535",
  password: "xy13549163252",
  confirmPassword: "xy13549163252",
  captcha: "",
  role: 0, 
});

// 忘记密码表单
const forgotForm = reactive({
  phone: "13332869535",
  newPassword: "xy13549163252",
  confirmPassword: "xy13549163252",
  captcha: "",
});

// 验证码
const captchaCanvas = ref(null);
const captchaCode = ref("");

// 生成随机验证码
const generateCaptcha = () => {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
  let code = "";
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  captchaCode.value = code;
  drawCaptcha(code);
};

// 绘制验证码
const drawCaptcha = (code) => {
  const canvas = captchaCanvas.value;
  if (!canvas) return;
  const ctx = canvas.getContext("2d");

  // 清空画布
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // 背景
  ctx.fillStyle = "#f0f0f0";
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  // 绘制干扰线
  ctx.strokeStyle = "#ccc";
  for (let i = 0; i < 5; i++) {
    ctx.beginPath();
    ctx.moveTo(Math.random() * canvas.width, Math.random() * canvas.height);
    ctx.lineTo(Math.random() * canvas.width, Math.random() * canvas.height);
    ctx.stroke();
  }

  // 绘制验证码
  ctx.font = "bold 28px Arial";
  ctx.fillStyle = "#333";
  for (let i = 0; i < code.length; i++) {
    const x = 10 + i * 20 + Math.random() * 5;
    const y = 30 + Math.random() * 5;
    ctx.fillText(code[i], x, y);
  }

  // 绘制干扰点
  for (let i = 0; i < 50; i++) {
    ctx.fillStyle = "#999";
    ctx.fillRect(
      Math.random() * canvas.width,
      Math.random() * canvas.height,
      1,
      1,
    );
  }
};

// 刷新验证码
const refreshCaptcha = () => {
  generateCaptcha();
};

// 监听表单切换，在第一次打开时初始化验证码
watch([isRegister, isForgot], ([newIsRegister, newIsForgot]) => {
  // 使用nextTick确保DOM已经更新，canvas元素已经渲染
  proxy.$nextTick(() => {
    if (newIsRegister && !formInitialized.register) {
      refreshCaptcha();
      formInitialized.register = true;
    }

    if (newIsForgot && !formInitialized.forgot) {
      refreshCaptcha();
      formInitialized.forgot = true;
    }
  });
});

// 登录表单校验规则
const loginRules = {
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 20, message: "密码长度在6-20位之间", trigger: "blur" },
  ],
};

// 注册表单校验规则
// 注册表单校验规则（添加 role 的校验）
const registerRules = {
  role: [
    { required: true, message: "请选择身份", trigger: "change" },
  ],
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 20, message: "密码长度在6-20位之间", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认密码", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error("两次输入的密码不一致"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
  captcha: [
    { required: true, message: "请输入验证码", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value.toUpperCase() !== captchaCode.value.toUpperCase()) {
          callback(new Error("验证码错误"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
};

// 忘记密码表单校验规则
const forgotRules = {
  phone: [
    { required: true, message: "请输入手机号", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" },
  ],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, max: 20, message: "密码长度在6-20位之间", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认密码", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value !== forgotForm.newPassword) {
          callback(new Error("两次输入的密码不一致"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
  captcha: [
    { required: true, message: "请输入验证码", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value.toUpperCase() !== captchaCode.value.toUpperCase()) {
          callback(new Error("验证码错误"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
};

// 登录
const handleLogin = async () => {
  const res = await proxy.$api.getMenu(loginForm);
  localStorage.setItem("token", res.token);
  localStorage.setItem("name", res.name);
  localStorage.setItem("role", res.role);
  localStorage.setItem("hasApartment", res.apartmentId);
  localStorage.setItem("createTime", res.createTime);
  console.log("用户的公寓ID为：" + res.apartmentId);
  console.log("用户的创建时间为：" + res.createTime);
  store.updateMenuList(res.menuList);
  store.state.token = res.token;
  store.addMenu(router);
  router.push("/home");
};

// 注册
const handleRegister = async () => {
  const res = await proxy.$api.register(registerForm);
  console.log(res);
  ElMessage.success("注册成功，请登录");
  isRegister.value = false;
  resetForm();
};

// 重置密码
const handleForgot = async () => {
  const res = await proxy.$api.resetPassword(forgotForm);
  ElMessage.success("密码重置成功，请登录");
  isForgot.value = false;
  resetForm();
};

// 重置表单
const resetForm = () => {
  registerForm.phone = "";
  registerForm.password = "";
  registerForm.confirmPassword = "";
  registerForm.captcha = "";
  registerForm.role = 0;  // 重置身份选择
  forgotForm.phone = "";
  forgotForm.newPassword = "";
  forgotForm.confirmPassword = "";
  forgotForm.captcha = "";

  // 重置表单初始化状态
  formInitialized.register = false;
  formInitialized.forgot = false;
};

onMounted(() => {
  // 只在组件挂载时生成一次验证码（用于登录表单）
  generateCaptcha();
});
</script>

<style scoped lang="less">
.body-login {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-image: url("../assets/images/background.png");
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  display: flex;
  justify-content: center;
  align-items: center;
}
.login-container {
  width: 420px;
  background-color: #fff;
  border: 1px solid #eaeaea;
  border-radius: 15px;
  padding: 35px 35px 25px 35px;
  box-shadow: 0 0 25px rgba(0, 0, 0, 0.1);

  h1 {
    text-align: center;
    margin-bottom: 30px;
    color: #333;
    font-size: 28px;
  }

  .form-footer {
    display: flex;
    justify-content: space-between;
    margin-top: 15px;
  }

  .captcha-container {
    display: flex;
    align-items: center;
    width: 100%;
  }

  .captcha-box {
    width: 100px;
    height: 40px;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
    overflow: hidden;

    canvas {
      display: block;
      width: 100%;
      height: 100%;
    }
  }

  :deep(.el-form-item) {
    margin-bottom: 22px;
  }

  :deep(.el-input__wrapper) {
    width: 100%;
  }
}
</style>

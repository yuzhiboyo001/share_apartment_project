import axios from "axios";
import { ElMessage } from "element-plus";
import config from "../config";

// 创建axios实例，但不立即设置baseURL
const service = axios.create();

const NETWORK_ERROR = "网络错误...";

// 添加请求拦截器
service.interceptors.request.use(
  function (config) {
    // 在发送请求之前做些什么
    console.log("🌐 API请求URL:", config.baseURL + config.url);
    console.log("🌐 API请求参数:", config.params);
    const token = localStorage.getItem("token");
    if (token) {
      config.headers["Authorization"] = "Bearer " + token;
    }
    return config;
  },
  function (error) {
    // 对请求错误做些什么
    return Promise.reject(error);
  },
);

// 添加响应拦截器
service.interceptors.response.use((res) => {
  const { code, data, message } = res.data;
  if (code === 200) {
    return data;
  } else {
    ElMessage.error(message || NETWORK_ERROR);
    return Promise.reject(message || NETWORK_ERROR);
  }
});

function request(options) {
  options.method = options.method || "get";

  // 关于get请求参数的调整
  if (options.method.toLowerCase() === "get") {
    // 同时支持 options.params 和 options.data
    const requestParams = options.params || options.data;
    options.params = requestParams;
    delete options.data;
  }

  // 对mock的开关做一个处理
  let isMock = config.mock;
  if (typeof options.mock !== "undefined") {
    isMock = options.mock;
  }

  // 针对环境做一个处理
  if (config.env === "prod") {
    service.defaults.baseURL = config.baseApi;
  } else {
    service.defaults.baseURL = isMock ? config.mockApi : config.baseApi;
  }

  return service(options);
}

export default request;

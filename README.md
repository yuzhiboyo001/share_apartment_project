公寓管理系统+小糖人塔菲AI公寓助手

**预览图**：
<img width="2549" height="1403" alt="image" src="https://github.com/user-attachments/assets/20cc003a-40fa-4af3-9b3c-2dc73a820513" />


本项目是基于集成了 **RAG（重排序+多路检索）模块的LLM模型** AI公寓助手的智能公寓管理系统，
集成了 **Vue 3** 前端、**Spring Boot** 后端和 **FastAPI** AI 服务，支持文档智能问答、公寓管理、账单统计等功能。

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、Vite、Element Plus、Axios |
| 后端 | Spring Boot 3.0、MySQL、JPA、JWT |
| AI 服务 | FastAPI、LangChain、Chroma、Ollama |
| 部署 | Docker、Docker Compose |
| RAG 优化 | 多路检索、BM25、重排序（BGE-reranker） |

## 项目结构
share_apartment_project/
├── vue_Apartment/ # Vue 3 前端
├── springboot_Apartment/ # Spring Boot 后端
├── fastapi_AI/ # FastAPI AI 服务（RAG）
├── docker-compose.yml # Docker 编排文件
├── shared_apartment.sql # 数据库初始化脚本
└── .env.example # 环境变量模板


## 快速搭建

### 前置条件

- Docker & Docker Compose
- 内存：8GB+（推荐 16GB）
- 显卡：本地运行 AI 模型需要 6G以上 显存运行 deepseek-7b-chat 模型（或改用云端 API）

### 一键启动

```bash
# 克隆项目
git clone https://github.com/yuzhiboyo001/share_apartment_project.git
cd share_apartment_project

# 配置环境变量
⚠️找到fastapi_AI目录下的.env.example改为.env
# 填入你的 DeepSeek API Key（不配置的话，则需要额外下载 deepseek-7b-chat 模型）

# 启动所有服务
docker-compose up -d
首次启动会自动下载 AI 相关模型（约 2GB），请耐心等待

LLM 本地模型（deepseek-7b-chat）需要单独下载，或直接使用云端 API

全部服务启动后需等待约 1 分钟，用于创建数据库和插入相关数据

如果 Docker 拉取镜像较慢，可在 Docker Desktop → Settings → Docker Engine 中添加：

```json
{
  "registry-mirrors": [
    "https://docker.xuanyuan.me",
    "https://docker.1ms.run",
    "https://docker.m.daocloud.io"
  ]
}

全部服务启动后需等待1分钟左右创建数据库和插入相关数据
另外最好实时查看fastapi日志：
docker-compose logs -f backend-fastapi

Loading weights: 100%|██████████| 71/71 [00:00<00:00, 3155.92it/s]
Loading weights: 100%|██████████| 201/201 [00:00<00:00, 1911.35it/s]
apartment_fastapi  | INFO:     Started server process [1]
apartment_fastapi  | INFO:     Waiting for application startup.
apartment_fastapi  | INFO:     Application startup complete.
apartment_fastapi  | INFO:     Uvicorn running on http://0.0.0.0:8000 (Press CTRL+C to quit)
apartment_fastapi  | 全部配置已完成

当日志显示以上形式时，服务将正常运行。
（该部分可能会因为网络原因下载失败，可以去掉镜像源，找个梯子自行设置SOCKS5 代理端口，下载过程中会更稳定）

访问地址
主要操作界面	http://localhost	

默认账号
角色	手机号	密码
管理员	13800138000	123456
租客	13800138001	123456

 AI 助手功能
小唐人塔菲智能问答：基于公寓管理制度文档回答问题

双模式：支持本地 Ollama 和云端 DeepSeek API

切换 LLM 模式
在 fastapi_AI/ai_api.py 中修改 model_creation() 函数的 exchange 参数：

python
# 本地 Ollama 模式
rag_chain = create_rag_chain(rag_retriever, exchange=True)

# 云端 API 模式
rag_chain = create_rag_chain(rag_retriever, exchange=False)

RAG链路模块：
多路检索	向量检索 + BM25 关键词检索融合
重排序	BGE-reranker 模型精排
查询改写，口语化转正式表达
文档分块，按语义切分，保留上下文
知识库文档路径：fastapi/test_docs（因为里面也塞了与公司相关的测试文档进去，LLM模型输出可能不完全只包含公寓内容）

yuzhiboyo001

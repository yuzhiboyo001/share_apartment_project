import os
os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"
os.environ["NO_PROXY"] = "localhost,127.0.0.1"
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from ai_api import model_creation
from fastapi.middleware.cors import CORSMiddleware
app = FastAPI()
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 允许所有来源（开发环境）
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
rag_chain=model_creation()
@app.get("/")
def hello_world():
    return {"message": "Hello World"}

@app.get("/user/{user_id}")
def get_user(user_id:int):
    return {"user_id":user_id}

@app.get("/items/")
def get_items(skip:int=0,limit:int=10):
    return {"skip":skip,"limit":limit}

# 1. 定义请求体应该长什么样
class ChatRequest(BaseModel):
    question: str   # 必须有一个 question 字段，类型是字符串

# 2. 定义响应体应该长什么样
class ChatResponse(BaseModel):
    answer: str           # 回答内容
    question_length: int  # 问题长度

# 3. 创建 POST 接口
@app.post("/chat/")
def chat(request: ChatRequest) -> ChatResponse:
    if len(request.question)!=0:
        response = rag_chain.invoke({"input": request.question})
        return ChatResponse(
            answer=response["answer"],
            question_length=len(request.question)
        )
    else:
        raise HTTPException(status_code=400, detail="问题不能为空")

print("全部配置已完成")
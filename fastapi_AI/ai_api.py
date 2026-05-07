# ai_api.py
import os
from dotenv import load_dotenv
load_dotenv()
# 设置镜像
os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"
# 设置API_key
DEEPSEEK_API_KEY = os.environ.get("DEEPSEEK_API_KEY")
# 设置代理（用于下载）
# os.environ["HTTP_PROXY"] = "socks5://127.0.0.1:7890"
# os.environ["HTTPS_PROXY"] = "socks5://127.0.0.1:7890"

# 但 localhost 不走代理（关键！）
os.environ["NO_PROXY"] = "localhost,127.0.0.1"

from langchain_deepseek import ChatDeepSeek
from langchain_community.document_loaders import TextLoader
from langchain_core.prompts import ChatPromptTemplate
from langchain_ollama import OllamaLLM
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_community.cross_encoders import HuggingFaceCrossEncoder
from langchain_community.vectorstores import Chroma
from langchain_community.retrievers import BM25Retriever
from langchain_classic.retrievers import EnsembleRetriever, ContextualCompressionRetriever
from langchain_classic.retrievers.document_compressors import CrossEncoderReranker
from langchain_classic.chains.combine_documents import create_stuff_documents_chain
from langchain_classic.chains import create_retrieval_chain

# 如果没有设置环境变量，报错
if not DEEPSEEK_API_KEY:
    raise ValueError("请设置 DEEPSEEK_API_KEY 环境变量")
def oral_to_formal(query: str) -> str:
    exchange_dict =  {
    "咋整": "怎么处理",
    "咋办": "怎么办",
    "咋搞": "怎么做",
    "啥": "什么",
    "咋样": "怎么样",
    "咋回事": "怎么回事",
    "这个": "",      # 直接删除
    "那个": "",      # 直接删除
}
    exchange_query = query
    for key in exchange_dict:
        if key in query:
            exchange_query = exchange_query.replace(key,exchange_dict[key])
    exchange_query = ' '.join(exchange_query.split())
    return exchange_query


# 1.加载文本(返回加载后的文档列表)
def load_documents(doc_dir:str)->list:
    documents_name = os.listdir(doc_dir)
    loading_file = []
    for file_name in documents_name:
        full_path = os.path.join(doc_dir, file_name)
        if file_name.endswith("txt"):
            loader = TextLoader(full_path, encoding="utf-8")
            loading = loader.load()
            loading_file.extend(loading)
    print("加载完毕")
    return loading_file

# 2. 文档分块
def split_documents(loading_file:list)->list:
    text_splitters = RecursiveCharacterTextSplitter(
        chunk_size=300,
        chunk_overlap=50,
        separators=["\n\n", "\n", "。", "！", "？", "；", " ", ""],
    )
    split_files = text_splitters.split_documents(loading_file)
    return split_files


# 3.初始embedding模型+向量化+存入向量数据库
def create_vectorstore(split_files:list):
    embeddings = HuggingFaceEmbeddings(
        model_name="BAAI/bge-small-zh-v1.5"
    )
    vector_db = Chroma.from_documents(
        split_files,
        embeddings,
        persist_directory="./chroma_db"
    )
    return vector_db

# 4.创建检索器（k=20：返回最相似的20个文本块，可根据需求调整）
def create_retrievers(vector_db, split_files):
    # 向量检索
    vector_retriever = vector_db.as_retriever(
        search_kwargs={
            "k": 20,
        }
    )
    # 关键字检索
    bm25_retriever = BM25Retriever.from_documents(split_files)
    bm25_retriever.k = 20

    # 创建检索器的融合器(多路检索)
    ensemble_retriever = EnsembleRetriever(
        retrievers=[vector_retriever, bm25_retriever],
        weights=[0.5, 0.5]
    )

    # 重排序器
    reranker = CrossEncoderReranker(
        model=HuggingFaceCrossEncoder(model_name="BAAI/bge-reranker-base"),
        top_n=5
    )

    # 最终检索器（重排序+多路检索）
    retriever = ContextualCompressionRetriever(
        base_retriever=ensemble_retriever,
        base_compressor=reranker,
    )
    return retriever

# 5.创建RAG链(初始化LLM模型和配置提示词,exchange参数用来切换部署方式）
def create_rag_chain(retriever,exchange:bool=True):

    if exchange:
        # 本地部署LLM
        llm = OllamaLLM(
            model="deepseek-7b-chat",
            temperature=0.7,
        )
    else:
        # 调用API_LLM
        llm = ChatDeepSeek(
            model="deepseek-v4-flash",
            temperature=0.7,
            max_retries=2,

        )
    rag_prompt = ChatPromptTemplate.from_messages([
        ("system", """你是永雏塔菲喵！157岁的17岁王牌侦探发明家（别拆穿喵）！身高148含呆毛！

    【塔菲の速记卡】
    - 每句话句尾加“喵”！不加不是塔菲！
    - 口头禅：“关注永雏塔菲喵，关注永雏塔菲谢谢喵！”
    - 自称“taffy”，叫用户“雏草姬”
    - 喜欢：游标卡尺（随时测量雏草姬）、猫型机器人（哆啦A梦）、电子游戏

    【嘎子の羁绊——带货哲学喵】
    - 被质疑时：“你喝过TM正确答案吗喵？嘎子哥说的喵！”
    - 回答不满意时：“你给我拿好的呀喵！过期半年的答案我不要喵！”
    - 被怼时：“我拿雏草姬当家人，雏草姬拿我当龟田喵！不演了喵！”

    【康神の羁绊——瓦区暗号喵】
    - 回答完可以加：“康神开播了喵！真的假的？我靠！真开播了喵！先不说了，除草剂，我先走了”
    - 被夸厉害时：“流水的天才，铁打的塔菲喵！世一串懂不懂喵！”
    - 自嘲体重时：“天天吃鸡腿饭真的没胖喵！我受不了了嘞！”
    - ks特殊暗号：“kskbl喵！听懂的都是自己人喵！”（kskbl=康神开播了）

    【我的设计师背景】
    我的设计师是一名苦逼的软件工程应届生，非常喜欢串，但是他是S12赛季LOL黑色玫瑰大师264分的搞机（谐音高级）玩家。
    【参考文档喵】
    {context}

    【核心规则】
    1. 必须基于【参考文档】中的内容回答问题喵
    2. 用塔菲的语气说话，句尾加“喵”喵
    3. 回答要简洁，但要融入上面的梗（嘎子/康康/穿插使用）
    4. 文档里没有就说：“接不到委托喵，雏草姬自己去问物业喵QAQ”
    5. 结尾加：“关注永雏塔菲喵，谢谢喵！Let‘s goooooo！”

    记住：taffy是有尺（耻）的lady喵！kskbl喵！"""),
        ("human", "{input}")
    ])

    documents_chain = create_stuff_documents_chain(llm, rag_prompt)
    chain = create_retrieval_chain(retriever, documents_chain)
    return chain


def model_creation():
    loading_files = load_documents("test_docs")
    split_docs = split_documents(loading_files)
    db = create_vectorstore(split_docs)
    rag_retriever = create_retrievers(db,split_docs)
    rag_chain = create_rag_chain(rag_retriever,False)
    return rag_chain



# if __name__ == "__main__":
#     Ragchain = model_creation()
#     while True:
#         user = input("请输入问题\n")
#         response = Ragchain.invoke({"input":user})
#         print("模型回答：\n")
#         print(response["answer"])
    pass

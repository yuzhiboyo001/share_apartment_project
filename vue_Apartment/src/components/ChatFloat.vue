<template>
  <div class="chat-float-container">
    <!-- 浮动按钮 -->
    <div class="chat-toggle-btn" @click="toggleChat" :class="{ active: isOpen }">
      <svg v-if="!isOpen" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        <path d="M8 10h.01"/>
        <path d="M12 10h.01"/>
        <path d="M16 10h.01"/>
      </svg>
      <svg v-else xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <line x1="18" y1="6" x2="6" y2="18"/>
        <line x1="6" y1="6" x2="18" y2="18"/>
      </svg>
    </div>

    <!-- 对话框 -->
    <div class="chat-dialog" v-show="isOpen">
      <div class="chat-header">
        <div class="header-title">
          <div class="ai-avatar-small">
            <img src="/src/assets/images/AI.jpg" alt="AI" @error="handleAvatarError">
          </div>
          <div>
            <span class="title">我是塔菲给我好评喵</span>
            <span class="status">在线</span>
          </div>
        </div>
        <button class="close-btn" @click="toggleChat">×</button>
      </div>
      
      <div class="chat-messages" ref="messagesContainer">
        <div v-for="(msg, index) in messages" :key="index" :class="['message', msg.role]">
          <div class="message-avatar">
            <img 
              v-if="msg.role === 'assistant'" 
              src="/src/assets/images/AI.jpg" 
              alt="AI"
              @error="handleAvatarError"
            >
            <div v-else class="user-avatar-placeholder">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </div>
          </div>
          <div class="message-content-wrapper">
            <div class="message-content">{{ msg.content }}</div>
            <div class="message-time">{{ msg.time || formatTime(new Date()) }}</div>
          </div>
        </div>
        <div v-if="isLoading" class="message assistant">
          <div class="message-avatar">
            <img src="/src/assets/images/AI.jpg" alt="AI" @error="handleAvatarError">
          </div>
          <div class="message-content-wrapper">
            <div class="message-content typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="chat-input-area">
        <div class="input-wrapper">
          <input 
            type="text" 
            v-model="inputText" 
            @keyup.enter="sendMessage"
            :disabled="isLoading"
            placeholder="输入你的问题..."
          />
          <button @click="sendMessage" :disabled="isLoading || !inputText.trim()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"/>
              <polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
          </button>
        </div>
        <div class="disclaimer">AI 生成内容仅供参考</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'

const isOpen = ref(false)
const inputText = ref('')
const isLoading = ref(false)
const messages = ref([
  { 
    role: 'assistant', 
    content: '你好喵！我是塔菲喵，有什么可以糖到你的喵？',
    time: formatTime(new Date())
  }
])

const messagesContainer = ref(null)

// 格式化时间
function formatTime(date) {
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 切换对话框
const toggleChat = () => {
  isOpen.value = !isOpen.value
  if (isOpen.value) {
    scrollToBottom()
  }
}

// 头像加载失败时的处理
const handleAvatarError = (e) => {
  e.target.style.display = 'none'
}

// 发送消息
const sendMessage = async () => {
  if (!inputText.value.trim() || isLoading.value) return
  
  const userMessage = inputText.value.trim()
  messages.value.push({ 
    role: 'user', 
    content: userMessage,
    time: formatTime(new Date())
  })
  inputText.value = ''
  scrollToBottom()
  
  isLoading.value = true
  
  try {
    const response = await fetch('http://127.0.0.1:8000/chat/', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ question: userMessage })
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const data = await response.json()
    messages.value.push({ 
      role: 'assistant', 
      content: data.answer,
      time: formatTime(new Date())
    })
  } catch (error) {
    console.error('Error:', error)
    messages.value.push({ 
      role: 'assistant', 
      content: '抱歉，服务暂时不可用，请稍后再试。',
      time: formatTime(new Date())
    })
  } finally {
    isLoading.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat-float-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
}

/* 浮动按钮样式 */
.chat-toggle-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #1a1a2e;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  transition: all 0.25s ease;
  color: white;
  border: none;
}

.chat-toggle-btn:hover {
  transform: scale(1.05);
  background: #16213e;
  box-shadow: 0 6px 16px rgba(0,0,0,0.2);
}

.chat-toggle-btn svg {
  transition: transform 0.25s ease;
}

/* 对话框样式 */
.chat-dialog {
  position: absolute;
  bottom: 70px;
  right: 0;
  width: 380px;
  height: 560px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 20px 35px -10px rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  backdrop-filter: blur(0px);
  border: 1px solid rgba(0,0,0,0.05);
}

/* 头部样式 */
.chat-header {
  padding: 16px 20px;
  background: #ffffff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.ai-avatar-small img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.title {
  font-weight: 600;
  font-size: 16px;
  color: #1a1a2e;
  display: block;
  line-height: 1.2;
}

.status {
  font-size: 12px;
  color: #10b981;
  display: block;
  font-weight: 400;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #999;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f5f5f5;
  color: #333;
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background: #fafafa;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: flex-start;
}

.message-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-avatar-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.message-content-wrapper {
  flex: 1;
  max-width: calc(100% - 44px);
}

.message-content {
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.5;
  color: #333;
  background: #ffffff;
  border-radius: 16px;
  border-top-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.message.user .message-content {
  background: #1a1a2e;
  color: white;
  border-radius: 16px;
  border-top-right-radius: 4px;
}

.message-time {
  font-size: 10px;
  color: #aaa;
  margin-top: 6px;
  text-align: left;
  padding-left: 8px;
}

.message.user .message-time {
  text-align: right;
}

/* 打字指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
  align-items: center;
  background: transparent;
  box-shadow: none;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #999;
  animation: bounce 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-8px);
  }
}

/* 输入区域 */
.chat-input-area {
  padding: 16px 20px;
  background: #ffffff;
  border-top: 1px solid #f0f0f0;
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
  background: #f5f7fa;
  border-radius: 40px;
  padding: 4px 6px 4px 18px;
  transition: all 0.2s;
}

.input-wrapper:focus-within {
  background: #ffffff;
  box-shadow: 0 0 0 2px #e0e7ff;
}

.input-wrapper input {
  flex: 1;
  padding: 10px 0;
  background: transparent;
  border: none;
  outline: none;
  font-size: 14px;
  color: #333;
}

.input-wrapper input::placeholder {
  color: #aaa;
}

.input-wrapper button {
  background: #1a1a2e;
  border: none;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s;
  color: white;
}

.input-wrapper button:hover:not(:disabled) {
  background: #16213e;
}

.input-wrapper button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.disclaimer {
  text-align: center;
  font-size: 10px;
  color: #aaa;
  margin-top: 10px;
}
</style>
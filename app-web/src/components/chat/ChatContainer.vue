<template>
  <div class="chat-module">
    <SessionSidebar
      :sessions="sessions"
      :current-session-id="currentSessionId"
      :is-open="sessionSidebarOpen"
      :sessions-loading="sessionsLoading"
      :selected-model="selectedModel"
      @close="sessionSidebarOpen = false"
      @load-session="loadSession"
      @create-session="createNewSession"
      @select-model="selectModel"
    />
    <div class="session-overlay" v-if="sessionSidebarOpen" @click="sessionSidebarOpen = false"></div>

    <section class="chat-panel">
      <div class="chat-header">
        <div class="header-left">
          <button class="show-sessions-btn" @click="sessionSidebarOpen = true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
            历史记录
          </button>
          <div>
            <h2>{{ currentSessionTitle }}</h2>
            <div class="current-model-chip">当前模型：{{ currentModelLabel }}</div>
          </div>
        </div>
        <div class="model-selector-container">
          <select v-model="selectedModel" class="model-selector">
            <option v-for="option in MODEL_OPTIONS" :key="option.key" :value="option.key">
              {{ option.name }}
            </option>
          </select>
          <svg class="selector-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </div>
      </div>
      
      <MessageList
        :messages="messages"
        :loading="loading"
        ref="messageListRef"
        @regenerate="handleRegenerate"
        @edit="handleEdit"
      />

      <InputArea
        v-model="chatInput"
        :loading="loading"
        :is-recording="isRecording"
        :temp-attachment="tempAttachment"
        @send="sendChat"
        @toggle-recording="toggleVoiceRecording"
        @attachment-change="handleAttachmentChange"
        @remove-attachment="tempAttachment = null"
      />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import SessionSidebar from './SessionSidebar.vue'
import MessageList from './MessageList.vue'
import InputArea from './InputArea.vue'

// 使用 chatApi 替代硬编码
import * as chatApi from '../../services/chatApi'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:10088/api'
const DEVICE_INFO = typeof navigator !== 'undefined'
  ? `${navigator.userAgent} | ${window.screen.width}x${window.screen.height}`
  : 'unknown-device'

const MODEL_OPTIONS = [
  { key: 'doubao', name: '豆包 Doubao', desc: '火山引擎 · 豆包模型' },
  { key: 'deepseek', name: 'DeepSeek', desc: 'DeepSeek 官方模型' }
]

const selectedModel = ref(MODEL_OPTIONS[0].key)
const currentSessionId = ref(null)
const sessions = ref([])
const sessionsLoading = ref(false)
const messages = ref([])
const chatInput = ref('')
const loading = ref(false)
const sessionSidebarOpen = ref(false)
const messageListRef = ref(null)

const isRecording = ref(false)
const tempAttachment = ref(null)

const currentSessionTitle = computed(() => {
  const session = sessions.value.find((s) => s.id === currentSessionId.value)
  if (session) {
    return session.title || '未命名会话'
  }
  return '新的对话'
})

const currentModelLabel = computed(() => {
  const match = MODEL_OPTIONS.find((option) => option.key === selectedModel.value)
  return match ? match.name : ''
})

const selectModel = (model) => {
  selectedModel.value = model
}

const fetchSessions = async () => {
  sessionsLoading.value = true
  try {
    const res = await fetch(`${API_BASE}/chat/sessions`)
    if (!res.ok) throw new Error('无法获取会话列表')
    sessions.value = await res.json()
  } catch (err) {
    console.error(err)
  } finally {
    sessionsLoading.value = false
  }
}

const loadSession = async (sessionId) => {
  try {
    const res = await fetch(`${API_BASE}/chat/sessions/${sessionId}/messages`)
    if (!res.ok) throw new Error('无法获取会话消息')
    const payload = await res.json()
    messages.value = payload.map((msg) => ({
      sender: msg.role === 'USER' ? 'user' : 'ai',
      text: msg.content
    }))
    currentSessionId.value = sessionId
    const session = sessions.value.find((s) => s.id === sessionId)
    if (session?.model) {
      selectedModel.value = session.model
    }
    sessionSidebarOpen.value = false
    messageListRef.value?.scrollToBottom()
  } catch (err) {
    console.error(err)
  }
}

const createNewSession = async () => {
  try {
    const res = await fetch(`${API_BASE}/chat/sessions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: 'New Chat',
        model: selectedModel.value,
        deviceInfo: DEVICE_INFO
      })
    })
    if (!res.ok) throw new Error('无法创建会话')
    const session = await res.json()
    sessions.value.unshift(session)
    currentSessionId.value = session.id
    messages.value = []
  } catch (err) {
    console.error(err)
  }
}

// 流式响应控制器
let streamController = null

const sendChat = async () => {
  if (!chatInput.value.trim() && !tempAttachment.value) return
  
  const text = chatInput.value
  const attachment = tempAttachment.value
  
  const userMsg = { 
    sender: 'user', 
    text 
  }
  
  if (attachment) {
    userMsg.attachment = {
      name: attachment.name,
      type: attachment.type
    }
  }

  messages.value.push(userMsg)
  
  chatInput.value = ''
  tempAttachment.value = null
  loading.value = true
  messageListRef.value?.scrollToBottom()

  // 创建 AI 消息占位符（流式响应时不显示 loading indicator）
  const aiMessageIndex = messages.value.length
  messages.value.push({ 
    sender: 'ai', 
    text: '',
    isStreaming: true 
  })
  
  // 立即隐藏 loading，因为我们已经有了流式消息占位符
  loading.value = false

  try {
    // 尝试使用流式响应
    const { createStreamingChat, isStreamingSupported } = await import('../../services/streamService.js')
    
    if (isStreamingSupported()) {
      // 使用流式响应
      streamController = createStreamingChat(
        {
          text,
          model: selectedModel.value,
          sessionId: currentSessionId.value,
          deviceInfo: DEVICE_INFO
        },
        // onToken: 接收到新 token
        (token, data) => {
          if (data.sessionId && !currentSessionId.value) {
            currentSessionId.value = data.sessionId
          }
          
          if (token) {
            // 直接修改文本内容，Vue 3 的响应式系统会自动追踪
            messages.value[aiMessageIndex].text += token
            
            // 使用 nextTick 确保 DOM 更新后滚动
            nextTick(() => {
              messageListRef.value?.scrollToBottom()
            })
          }
        },
        // onComplete: 流式响应完成
        () => {
          messages.value[aiMessageIndex].isStreaming = false
          loading.value = false
          streamController = null
          fetchSessions()
          messageListRef.value?.scrollToBottom()
        },
        // onError: 发生错误
        (error) => {
          console.error('Streaming error:', error)
          messages.value[aiMessageIndex].isStreaming = false
          
          if (!messages.value[aiMessageIndex].text) {
            messages.value[aiMessageIndex].text = '流式响应失败，正在尝试普通请求...'
          }
          
          // 回退到普通请求
          fallbackToNormalChat(text, attachment, aiMessageIndex)
        }
      )
    } else {
      // 浏览器不支持流式响应，使用普通请求
      await fallbackToNormalChat(text, attachment, aiMessageIndex)
    }
  } catch (err) {
    console.error('Chat error:', err)
    messages.value[aiMessageIndex].isStreaming = false
    messages.value[aiMessageIndex].text = '调用后端失败，请稍后重试。'
    loading.value = false
  }
}

// 回退到普通聊天请求
const fallbackToNormalChat = async (text, attachment, messageIndex) => {
  try {
    const res = await fetch(`${API_BASE}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        text,
        model: selectedModel.value,
        sessionId: currentSessionId.value,
        deviceInfo: DEVICE_INFO,
        hasAttachment: !!attachment
      })
    })
    
    if (!res.ok) throw new Error('聊天接口返回错误')
    
    const data = await res.json()
    if (!currentSessionId.value && data.sessionId) {
      currentSessionId.value = data.sessionId
    }
    
    messages.value[messageIndex].text = data.text
    messages.value[messageIndex].isStreaming = false
    fetchSessions()
  } catch (err) {
    console.error('Fallback chat error:', err)
    messages.value[messageIndex].text = '调用后端失败，请稍后重试。'
    messages.value[messageIndex].isStreaming = false
  } finally {
    loading.value = false
    messageListRef.value?.scrollToBottom()
  }
}

// 停止流式响应
const stopStreaming = () => {
  if (streamController) {
    streamController.abort()
    streamController = null
    loading.value = false
  }
}

const handleAttachmentChange = (file) => {
  tempAttachment.value = file
}

const toggleVoiceRecording = () => {
  isRecording.value = !isRecording.value
  if (isRecording.value) {
    chatInput.value = '正在听...'
  } else {
    chatInput.value = '语音输入转文字测试内容'
  }
}

// 处理重新生成
const handleRegenerate = async (message) => {
  console.log('Regenerating message:', message)
  
  // 找到这条消息的索引
  const messageIndex = messages.value.findIndex(m => m === message)
  if (messageIndex === -1) return
  
  // 找到对应的用户消息（前一条）
  const userMessageIndex = messageIndex - 1
  if (userMessageIndex < 0 || messages.value[userMessageIndex].sender !== 'user') {
    console.error('Cannot find corresponding user message')
    return
  }
  
  const userMessage = messages.value[userMessageIndex]
  
  // 删除当前AI回复
  messages.value.splice(messageIndex, 1)
  
  // 重新发送
  loading.value = true
  
  // 创建新的AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({ 
    sender: 'ai', 
    text: '',
    isStreaming: true 
  })
  
  loading.value = false
  
  try {
    const { createStreamingChat, isStreamingSupported } = await import('../../services/streamService.js')
    
    if (isStreamingSupported()) {
      streamController = createStreamingChat(
        {
          text: userMessage.text,
          model: selectedModel.value,
          sessionId: currentSessionId.value,
          deviceInfo: DEVICE_INFO
        },
        (token) => {
          if (token) {
            messages.value[aiMessageIndex].text += token
            nextTick(() => {
              messageListRef.value?.scrollToBottom()
            })
          }
        },
        () => {
          messages.value[aiMessageIndex].isStreaming = false
          loading.value = false
          streamController = null
          messageListRef.value?.scrollToBottom()
        },
        (error) => {
          console.error('Regenerate streaming error:', error)
          messages.value[aiMessageIndex].isStreaming = false
          messages.value[aiMessageIndex].text = '重新生成失败，请稍后重试。'
          loading.value = false
        }
      )
    } else {
      await fallbackToNormalChat(userMessage.text, null, aiMessageIndex)
    }
  } catch (err) {
    console.error('Regenerate error:', err)
    messages.value[aiMessageIndex].isStreaming = false
    messages.value[aiMessageIndex].text = '重新生成失败，请稍后重试。'
    loading.value = false
  }
}

// 处理编辑 - 将消息内容填充到输入框
const handleEdit = (message) => {
  console.log('Editing message:', message)
  chatInput.value = message.text
  // 滚动到输入框
  nextTick(() => {
    const inputArea = document.querySelector('.auto-resize-textarea')
    if (inputArea) {
      inputArea.focus()
    }
  })
}

onMounted(() => {
  fetchSessions()
})
</script>

<style scoped>
.chat-module {
  flex: 1;
  display: flex;
  position: relative;
  min-width: 0; /* 防止 flex 溢出 */
  overflow: hidden;
}

.session-overlay {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 99;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.session-overlay.visible {
  opacity: 1;
  pointer-events: auto;
}

@media (max-width: 767px) {
  .session-overlay {
    display: block;
  }
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-color, #fff);
  min-width: 0; /* 防止 flex 溢出 */
  transition: all 0.3s ease;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-md, 1rem) var(--spacing-lg, 1.5rem);
  border-bottom: 1px solid var(--border-color, #e5e7eb);
  flex-shrink: 0;
  min-height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md, 1rem);
  flex: 1;
  min-width: 0; /* 允许文本截断 */
}

.header-left h2 {
  margin: 0;
  font-size: max(16px, 1.125rem); /* 确保最小字体大小 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.show-sessions-btn {
  display: none;
  background: transparent;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: var(--radius-md, 8px);
  padding: var(--spacing-sm, 0.5rem);
  cursor: pointer;
  color: var(--text-primary, #111827);
  transition: all 0.2s ease;
  min-height: 44px; /* 触摸目标大小 */
  font-size: max(14px, 0.875rem);
}

.show-sessions-btn:hover {
  background: var(--hover-bg, rgba(0, 0, 0, 0.05));
}

.show-sessions-btn:active {
  transform: scale(0.95);
}

@media (max-width: 767px) {
  .show-sessions-btn {
    display: flex;
    align-items: center;
    gap: var(--spacing-xs, 0.25rem);
  }

  .chat-header {
    padding: var(--spacing-sm, 0.5rem) var(--spacing-md, 1rem);
    min-height: 56px;
  }

  .header-left {
    gap: var(--spacing-sm, 0.5rem);
  }

  .header-left h2 {
    font-size: max(14px, 1rem);
  }
}

@media (min-width: 768px) and (max-width: 1023px) {
  .chat-header {
    padding: var(--spacing-md, 1rem);
  }
}

.current-model-chip {
  font-size: max(12px, 0.75rem); /* 确保最小字体大小 */
  color: var(--text-secondary, #6b7280);
  margin-top: 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 767px) {
  .current-model-chip {
    display: none; /* 移动端隐藏以节省空间 */
  }
}

.model-selector-container {
  position: relative;
  flex-shrink: 0;
}

.model-selector {
  padding: var(--spacing-sm, 0.5rem) 2rem var(--spacing-sm, 0.5rem) var(--spacing-md, 1rem);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: var(--radius-md, 8px);
  background: var(--bg-color, #fff);
  cursor: pointer;
  appearance: none;
  font-size: max(14px, 0.875rem);
  color: var(--text-primary, #111827);
  transition: all 0.2s ease;
  min-height: 44px; /* 触摸目标大小 */
}

.model-selector:hover {
  border-color: var(--accent-color, #10a37f);
}

.model-selector:focus {
  outline: 2px solid var(--accent-color, #10a37f);
  outline-offset: 2px;
}

@media (max-width: 767px) {
  .model-selector {
    padding: var(--spacing-xs, 0.25rem) 1.5rem var(--spacing-xs, 0.25rem) var(--spacing-sm, 0.5rem);
    font-size: max(14px, 0.8rem);
    min-height: 40px;
  }
}

.selector-icon {
  position: absolute;
  right: var(--spacing-sm, 0.5rem);
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
  color: var(--text-secondary, #6b7280);
}

/* 平板设备优化 */
@media (min-width: 768px) and (max-width: 1023px) {
  .chat-header {
    padding: var(--spacing-md, 1rem);
  }

  .header-left h2 {
    font-size: max(15px, 1.0625rem);
  }
}

/* 大屏幕优化 */
@media (min-width: 1440px) {
  .chat-header {
    padding: var(--spacing-lg, 1.5rem) var(--spacing-xl, 2rem);
  }
}

/* 确保内容不会被视口变化影响 */
@media (orientation: landscape) and (max-height: 500px) {
  .chat-header {
    min-height: 48px;
    padding: var(--spacing-xs, 0.25rem) var(--spacing-md, 1rem);
  }

  .show-sessions-btn {
    min-height: 36px;
    padding: var(--spacing-xs, 0.25rem);
  }
}
</style>

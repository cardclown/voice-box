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
import { ref, computed, onMounted } from 'vue'
import SessionSidebar from './SessionSidebar.vue'
import MessageList from './MessageList.vue'
import InputArea from './InputArea.vue'

const API_BASE = 'http://localhost:10088/api'
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
    messages.value.push({ sender: 'ai', text: data.text })
    fetchSessions()
  } catch (err) {
    console.error(err)
    messages.value.push({ sender: 'ai', text: '调用后端失败，请稍后重试。' })
  } finally {
    loading.value = false
    messageListRef.value?.scrollToBottom()
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

onMounted(() => {
  fetchSessions()
})
</script>

<style scoped>
.chat-module {
  flex: 1;
  display: flex;
  position: relative;
}

.session-overlay {
  display: none;
}

@media (max-width: 768px) {
  .session-overlay {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 99;
  }
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-color, #fff);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.show-sessions-btn {
  display: none;
  background: transparent;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 8px;
  padding: 0.5rem;
  cursor: pointer;
  color: var(--text-primary, #111827);
}

@media (max-width: 768px) {
  .show-sessions-btn {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
}

.current-model-chip {
  font-size: 0.75rem;
  color: var(--text-secondary, #6b7280);
  margin-top: 0.25rem;
}

.model-selector-container {
  position: relative;
}

.model-selector {
  padding: 0.5rem 2rem 0.5rem 1rem;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 8px;
  background: var(--bg-color, #fff);
  cursor: pointer;
  appearance: none;
}

.selector-icon {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
}
</style>

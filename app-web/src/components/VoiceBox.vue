<template>
  <div class="app-shell">
    <aside class="module-nav">
      <div class="nav-logo">VB</div>
      <button
        class="module-btn"
        :class="{ active: currentModule === 'chat' }"
        @click="switchModule('chat')"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
        <span>Chat</span>
      </button>
      <button
        class="module-btn"
        :class="{ active: currentModule === 'video' }"
        @click="switchModule('video')"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="23 7 16 12 23 17 23 7" />
          <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
        </svg>
        <span>Video</span>
      </button>
    </aside>

    <div v-if="currentModule === 'chat'" class="chat-module">
      <aside :class="['session-sidebar', { open: sessionSidebarOpen }]">
        <div class="sidebar-header">
          <div>
            <h3>Chat History</h3>
            <p class="sidebar-subtitle">存储在数据库的历史对话</p>
          </div>
          <button class="new-chat-btn" @click="createNewSession" :disabled="sessionsLoading">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            新建会话
          </button>
      </div>
      
        <div class="model-panel">
          <div class="panel-title">模型选择</div>
        <button 
            v-for="option in MODEL_OPTIONS"
            :key="option.key"
            :class="['model-option', { active: selectedModel === option.key }]"
            @click="selectModel(option.key)"
        >
            <div class="model-option-main">
              <span class="model-name">{{ option.name }}</span>
              <span class="model-desc">{{ option.desc }}</span>
            </div>
            <span class="model-pill-indicator">{{ selectedModel === option.key ? '已选' : '切换' }}</span>
        </button>
      </div>

        <div class="session-list" v-if="sessions.length">
          <div
            v-for="session in sessions"
            :key="session.id"
            :class="['session-item', { active: session.id === currentSessionId }]"
            @click="loadSession(session.id)"
          >
            <div class="session-title">{{ session.title || '未命名会话' }}</div>
            <div class="session-meta">
              <span>{{ session.model || '未指定模型' }}</span>
              <span>{{ formatDate(session.updatedAt) }}</span>
            </div>
          </div>
        </div>
        <div class="session-empty" v-else>
          <p>{{ sessionsLoading ? '加载中...' : '暂无会话，点击上方按钮新建' }}</p>
        </div>
      </aside>
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
        
        <div class="messages-container" ref="chatWindow">
          <div v-if="messages.length === 0" class="welcome-screen">
            <div class="welcome-icon">✨</div>
            <h2>开始新的对话</h2>
            <p>选择模型，输入内容，左侧会自动保存所有历史。</p>
          </div>

          <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
            <div :class="['message-row', msg.sender]">
              <div v-if="msg.sender === 'ai'" class="avatar ai-avatar">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 14a4 4 0 1 1 4-4 4 4 0 0 1-4 4z" />
                </svg>
              </div>
              <div class="message-content">
                <div class="sender-name">{{ msg.sender === 'user' ? '我' : 'VoiceBox' }}</div>
                <div class="bubble">{{ msg.text }}</div>
              </div>
              <div v-if="msg.sender === 'user'" class="avatar user-avatar">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
              </div>
            </div>
          </div>

          <div v-if="loading" class="message-wrapper">
            <div class="message-row ai">
              <div class="avatar ai-avatar">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 14a4 4 0 1 1 4-4 4 4 0 0 1-4 4z" />
                </svg>
              </div>
              <div class="message-content">
                <div class="sender-name">VoiceBox</div>
                <div class="bubble typing-indicator">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="input-wrapper">
          <!-- Attachment Preview -->
          <div v-if="tempAttachment" class="attachment-preview">
            <div class="file-icon">📎</div>
            <span class="file-name">{{ tempAttachment.name }}</span>
            <button class="remove-file" @click="tempAttachment = null">×</button>
          </div>

          <div class="input-box">
            <!-- Hidden File Input -->
            <input 
              type="file" 
              ref="attachmentInput" 
              class="hidden-input" 
              @change="handleAttachmentChange" 
            />
            
            <!-- Attachment Button -->
            <button class="action-btn" @click="handleAttachmentClick" title="添加附件">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
              </svg>
            </button>

            <input 
              v-model="chatInput" 
              @keyup.enter="sendChat" 
              :placeholder="isRecording ? '正在聆听...' : '输入消息...'"
              :disabled="loading || isRecording"
            />
            
            <!-- Voice Input Button -->
            <button 
              class="action-btn" 
              :class="{ recording: isRecording }" 
              @click="toggleVoiceRecording"
              title="语音输入"
            >
              <svg v-if="!isRecording" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" />
                <path d="M19 10v2a7 7 0 0 1-14 0v-2" />
                <line x1="12" y1="19" x2="12" y2="23" />
                <line x1="8" y1="23" x2="16" y2="23" />
              </svg>
              <div v-else class="recording-indicator">
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </div>
            </button>

            <button class="send-btn" @click="sendChat" :disabled="loading || (!chatInput.trim() && !tempAttachment)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="22" y1="2" x2="11" y2="13" />
                <polygon points="22 2 15 22 11 13 2 9 22 2" />
              </svg>
            </button>
          </div>
          <p class="footer-text">VoiceBox 可能出现错误，请自行核实重要内容。</p>
        </div>
      </section>
      </div>

    <div v-else class="video-module">
      <div class="video-card">
        <h2>视频转换接口</h2>
        <p class="subtitle">上传视频文件，后端会执行示例转换流程并返回结果。</p>
          
        <div class="upload-area">
          <input ref="videoInput" type="file" accept="video/*" class="hidden-input" @change="handleVideoSelect" />
          <button class="upload-btn" @click="videoInput?.click()">选择视频文件</button>
          <p v-if="selectedVideo" class="file-name">已选择：{{ selectedVideo.name }}</p>
          </div>

        <button class="convert-btn" @click="convertVideo" :disabled="!selectedVideo || conversionLoading">
          {{ conversionLoading ? '转换中...' : '开始转换' }}
        </button>

        <div class="result-box" v-if="conversionMessage">
          <p>{{ conversionMessage }}</p>
          <p v-if="conversionOutput">
            输出路径：
            <code>{{ conversionOutput }}</code>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useVoiceInput } from '@/composables/useVoiceInput'

const API_BASE = 'http://localhost:10088/api'
const DEVICE_INFO = typeof navigator !== 'undefined'
  ? `${navigator.userAgent} | ${window.screen.width}x${window.screen.height}`
  : 'unknown-device'

const MODEL_OPTIONS = [
  { key: 'doubao', name: '豆包 Doubao', desc: '火山引擎 · 豆包模型' },
  { key: 'deepseek', name: 'DeepSeek', desc: 'DeepSeek 官方模型' }
]

const currentModule = ref('chat')
const selectedModel = ref(MODEL_OPTIONS[0].key)
const currentSessionId = ref(null)
const sessions = ref([])
const sessionsLoading = ref(false)
const messages = ref([])
const chatInput = ref('')
const loading = ref(false)
const chatWindow = ref(null)
const sessionSidebarOpen = ref(false)
const videoInput = ref(null)

// New refs for attachments and voice
const attachmentInput = ref(null)
const tempAttachment = ref(null)

// 使用语音输入 composable
const {
  isRecording,
  hasPermission,
  recognizedText,
  startRecording,
  stopRecording,
  requestPermission
} = useVoiceInput()

const selectedVideo = ref(null)
const conversionMessage = ref('')
const conversionOutput = ref('')
const conversionLoading = ref(false)

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

const switchModule = (module) => {
  currentModule.value = module
}

const selectModel = (model) => {
  selectedModel.value = model
}

const formatDate = (isoString) => {
  if (!isoString) return ''
  try {
    return new Date(isoString).toLocaleString()
  } catch (err) {
    return isoString
  }
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
    await nextTick()
    scrollToBottom()
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

const scrollToBottom = async () => {
  await nextTick()
  if (chatWindow.value) {
    chatWindow.value.scrollTop = chatWindow.value.scrollHeight
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
  tempAttachment.value = null // Clear attachment after sending
  loading.value = true
  scrollToBottom()

  try {
    const res = await fetch(`${API_BASE}/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        text,
        model: selectedModel.value,
        sessionId: currentSessionId.value,
        deviceInfo: DEVICE_INFO,
        // Backend support for attachments would go here
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
    scrollToBottom()
  }
}

const handleAttachmentClick = () => {
  attachmentInput.value?.click()
}

const handleAttachmentChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    tempAttachment.value = file
  }
  event.target.value = '' // Reset input
}

const toggleVoiceRecording = async () => {
  // 检查麦克风权限
  if (!hasPermission.value) {
    try {
      await requestPermission()
    } catch (error) {
      console.error('麦克风权限被拒绝:', error)
      alert('需要麦克风权限才能使用语音输入功能')
      return
    }
  }

  if (isRecording.value) {
    // 停止录音
    try {
      const result = await stopRecording(
        1, // userId - 临时使用固定值,实际应该从用户状态获取
        currentSessionId.value || 1, // sessionId
        'zh-CN' // language
      )
      
      if (result && result.recognizedText) {
        chatInput.value = result.recognizedText
      }
    } catch (error) {
      console.error('停止录音失败:', error)
      alert('语音识别失败,请重试')
    }
  } else {
    // 开始录音
    try {
      await startRecording()
      chatInput.value = '正在聆听...'
    } catch (error) {
      console.error('开始录音失败:', error)
      alert('无法启动录音,请检查麦克风权限')
    }
  }
}

const handleVideoSelect = (event) => {
  const [file] = event.target.files
  selectedVideo.value = file || null
  conversionMessage.value = ''
  conversionOutput.value = ''
}

const convertVideo = async () => {
  if (!selectedVideo.value) return
  conversionLoading.value = true
  conversionMessage.value = ''
  conversionOutput.value = ''

  const formData = new FormData()
  formData.append('file', selectedVideo.value)

  try {
    const res = await fetch(`${API_BASE}/video/convert`, {
      method: 'POST',
      body: formData
    })
    if (!res.ok) throw new Error('视频转换失败')
    const data = await res.json()
    conversionMessage.value = data.message
    conversionOutput.value = data.outputPath
  } catch (err) {
    console.error(err)
    conversionMessage.value = '转换失败，请查看后端日志。'
  } finally {
    conversionLoading.value = false
  }
}

onMounted(() => {
  fetchSessions()
})
</script>

<style scoped>
:root {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.app-shell {
  display: flex;
  height: 96vh;
  width: 90%;
  max-width: 1600px;
  margin: 2vh auto;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 0 20px rgba(0,0,0,0.05);
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

@media (max-width: 768px) {
  .app-shell {
    width: 100%;
    height: 100vh;
    margin: 0;
    border-radius: 0;
    border: none;
  }
  
  .module-nav {
    display: none; /* Mobile nav strategy needed if hiding sidebar */
  }
  
  .session-sidebar {
    position: absolute;
    z-index: 100;
    height: 100%;
    transform: translateX(-100%);
    transition: transform 0.3s;
  }
  
  .session-sidebar.open {
    transform: translateX(0);
  }

  .video-card {
    padding: 1.5rem;
    width: 95%;
  }
}

.module-nav {
  width: 70px;
  background: #202123;
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem 0;
  gap: 1rem;
}

.nav-logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: white;
  color: #202123;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.module-btn {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.module-btn span {
  font-size: 0.65rem;
}

.module-btn.active {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.chat-module {
  flex: 1;
  display: flex;
  position: relative;
}

.session-sidebar {
  width: 300px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.session-sidebar .sidebar-header h3 {
  margin: 0;
}

.sidebar-subtitle {
  margin: 0;
  color: #6b7280;
  font-size: 0.85rem;
}

.new-chat-btn {
  background: #0ea5e9;
  border: none;
  color: white;
  padding: 0.5rem 0.9rem;
  border-radius: 999px;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 0.3rem;
  cursor: pointer;
}

.model-panel {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.panel-title {
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  color: #6b7280;
}

.model-option {
  display: flex;
  justify-content: space-between;
  border: 1px solid transparent;
  border-radius: 10px;
  padding: 0.6rem;
  background: #f9fafb;
  cursor: pointer;
}

.model-option.active {
  border-color: #0ea5e9;
  background: #e0f2fe;
}

.model-option-main {
  display: flex;
  flex-direction: column;
}

.model-name {
  font-weight: 600;
}

.model-desc {
  font-size: 0.8rem;
  color: #6b7280;
}

.model-pill-indicator {
  background: #cffafe;
  color: #0f766e;
  border-radius: 999px;
  padding: 0.1rem 0.6rem;
  font-size: 0.75rem;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.session-item {
  padding: 0.9rem;
  border-radius: 12px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: border 0.2s, background 0.2s;
}

.session-item:hover {
  border-color: #e5e7eb;
}

.session-item.active {
  border-color: #0ea5e9;
  background: #e0f2fe;
}

.session-title {
  font-weight: 600;
  margin-bottom: 0.3rem;
}

.session-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  color: #6b7280;
}

.session-overlay {
  display: none;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
}

.message-wrapper {
  margin-bottom: 1rem;
}

.video-module {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-card {
  background: #fff;
  padding: 2.5rem;
  border-radius: 16px;
  width: min(500px, 90%);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
}

.subtitle {
  color: #64748b;
}

.upload-area {
  border: 2px dashed #cbd5f5;
  border-radius: 12px;
  padding: 1.5rem;
  text-align: center;
  margin: 1.5rem 0;
}

.hidden-input {
  display: none;
}

.upload-btn,
.convert-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 0.9rem;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  font-size: 1rem;
}

.upload-btn {
  background: #f8fafc;
  color: #0f172a;
  border: 1px solid #e2e8f0;
}

.convert-btn {
  background: #2563eb;
  color: #fff;
  margin-top: 1rem;
}

.result-box {
  margin-top: 1.5rem;
  padding: 1rem;
  border-radius: 12px;
  background: #f1f5f9;
  }

.result-box code {
    display: block;
  margin-top: 0.5rem;
  word-break: break-all;
  font-size: 0.9rem;
  }

.thought-bubble {
  margin-bottom: 0.8rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.thought-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.8rem;
  background: #f1f5f9;
  cursor: pointer;
  font-size: 0.8rem;
  color: #64748b;
  font-weight: 500;
  user-select: none;
}

.thought-header:hover {
  background: #e2e8f0;
}

.thought-icon {
  font-size: 1rem;
}

.thought-header svg {
  margin-left: auto;
  transition: transform 0.2s;
}

.thought-header svg.rotated {
  transform: rotate(180deg);
}

.thought-content {
  padding: 0.8rem;
  font-size: 0.85rem;
  color: #475569;
  line-height: 1.5;
  white-space: pre-wrap;
  border-top: 1px solid #e2e8f0;
}

.video-card .file-name {
  margin-top: 0.8rem;
  color: #0f172a;
  font-size: 0.9rem;
  }

</style>
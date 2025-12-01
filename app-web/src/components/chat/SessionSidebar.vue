<template>
  <aside :class="['session-sidebar', { open: isOpen }]">
    <div class="sidebar-header">
      <h3>历史对话</h3>
      <button class="new-chat-btn" @click="$emit('create-session')" :disabled="sessionsLoading">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
      </button>
    </div>

    <!-- 搜索框 -->
    <div class="search-box">
      <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8" />
        <path d="m21 21-4.35-4.35" />
      </svg>
      <input 
        v-model="searchQuery"
        type="text"
        placeholder="搜索..."
        class="search-input"
      />
      <button v-if="searchQuery" class="clear-search" @click="searchQuery = ''">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
    </div>

    <div class="session-list" v-if="filteredSessions.length">
      <div
        v-for="session in filteredSessions"
        :key="session.id"
        :class="['session-item', { active: session.id === currentSessionId }]"
        @click="$emit('load-session', session.id)"
      >
        <div class="session-title">{{ session.title || '未命名会话' }}</div>
        <div class="session-meta">
          <span>{{ session.model || '未指定模型' }}</span>
          <span>{{ formatDate(session.updatedAt) }}</span>
        </div>
      </div>
    </div>
    <div class="session-empty" v-else>
      <LoadingState 
        v-if="sessionsLoading" 
        type="skeleton" 
        skeleton-type="list-item"
        :skeleton-count="5"
      />
      <p v-else-if="searchQuery || filterModel || filterDate">未找到匹配的会话</p>
      <p v-else>暂无会话，点击上方按钮新建</p>
    </div>
  </aside>
</template>

<script setup>
import { ref, computed } from 'vue'
import LoadingState from '../common/LoadingState.vue'

const MODEL_OPTIONS = [
  { key: 'doubao', name: '豆包 Doubao', desc: '火山引擎 · 豆包模型' },
  { key: 'deepseek', name: 'DeepSeek', desc: 'DeepSeek 官方模型' }
]

const props = defineProps({
  sessions: {
    type: Array,
    default: () => []
  },
  currentSessionId: {
    type: Number,
    default: null
  },
  isOpen: {
    type: Boolean,
    default: false
  },
  sessionsLoading: {
    type: Boolean,
    default: false
  },
  selectedModel: {
    type: String,
    required: true
  }
})

defineEmits(['close', 'load-session', 'create-session', 'select-model'])

// 搜索和过滤状态
const searchQuery = ref('')
const filterModel = ref('')
const filterDate = ref('')

// 过滤后的会话列表
const filteredSessions = computed(() => {
  let result = props.sessions

  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(session => 
      (session.title || '').toLowerCase().includes(query)
    )
  }

  // 模型过滤
  if (filterModel.value) {
    result = result.filter(session => session.model === filterModel.value)
  }

  // 日期过滤
  if (filterDate.value) {
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    
    result = result.filter(session => {
      if (!session.updatedAt) return false
      const sessionDate = new Date(session.updatedAt)
      
      switch (filterDate.value) {
        case 'today':
          return sessionDate >= today
        case 'week':
          const weekAgo = new Date(today)
          weekAgo.setDate(weekAgo.getDate() - 7)
          return sessionDate >= weekAgo
        case 'month':
          const monthAgo = new Date(today)
          monthAgo.setMonth(monthAgo.getMonth() - 1)
          return sessionDate >= monthAgo
        default:
          return true
      }
    })
  }

  return result
})

const formatDate = (isoString) => {
  if (!isoString) return ''
  try {
    const date = new Date(isoString)
    const now = new Date()
    const diff = now - date
    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    
    if (days === 0) {
      return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    } else if (days === 1) {
      return '昨天'
    } else if (days < 7) {
      return `${days}天前`
    } else {
      return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
    }
  } catch (err) {
    return isoString
  }
}
</script>

<style scoped>
.session-sidebar {
  width: 300px;
  background: var(--bg-color, #fff);
  border-right: 1px solid var(--border-color, #e5e7eb);
  padding: var(--spacing-lg, 1.5rem);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg, 1.5rem);
  flex-shrink: 0;
  overflow-y: auto;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

/* 移动端：固定定位的侧边栏 */
@media (max-width: 767px) {
  .session-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 100;
    height: 100vh;
    width: 85%;
    max-width: 320px;
    transform: translateX(-100%);
    box-shadow: none;
  }
  
  .session-sidebar.open {
    transform: translateX(0);
    box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  }
}

/* 平板设备：稍窄的侧边栏 */
@media (min-width: 768px) and (max-width: 1023px) {
  .session-sidebar {
    width: 260px;
    padding: var(--spacing-md, 1rem);
    gap: var(--spacing-md, 1rem);
  }
}

/* 大屏幕：更宽的侧边栏 */
@media (min-width: 1440px) {
  .session-sidebar {
    width: 340px;
  }
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md, 1rem);
  flex-shrink: 0;
}

.sidebar-header h3 {
  margin: 0;
  color: var(--text-primary, #111827);
  font-size: max(16px, 1.125rem); /* 确保最小字体大小 */
}

.sidebar-subtitle {
  margin: 0;
  color: var(--text-secondary, #6b7280);
  font-size: max(13px, 0.85rem);
}

@media (max-width: 767px) {
  .sidebar-header h3 {
    font-size: max(14px, 1rem);
  }

  .sidebar-subtitle {
    font-size: max(12px, 0.8rem);
  }
}

/* 搜索框 */
.search-box {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--spacing-xs, 0.25rem);
  padding: var(--spacing-sm, 0.5rem) var(--spacing-md, 0.75rem);
  background: var(--bg-secondary, #fff);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: var(--radius-md, 10px);
  transition: all 0.2s ease;
}

.search-box:focus-within {
  border-color: var(--accent-color, #10a37f);
  box-shadow: 0 0 0 3px rgba(16, 163, 127, 0.1);
}

.search-icon {
  flex-shrink: 0;
  color: var(--text-secondary, #6b7280);
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: max(14px, 0.875rem);
  color: var(--text-primary, #111827);
  min-width: 0;
}

.search-input::placeholder {
  color: var(--text-secondary, #6b7280);
}

.clear-search {
  flex-shrink: 0;
  background: transparent;
  border: none;
  color: var(--text-secondary, #6b7280);
  cursor: pointer;
  padding: var(--spacing-xs, 0.25rem);
  border-radius: var(--radius-sm, 6px);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.clear-search:hover {
  background: var(--hover-bg, rgba(0, 0, 0, 0.05));
  color: var(--text-primary, #111827);
}

/* 过滤栏 */
.filter-bar {
  display: flex;
  gap: var(--spacing-sm, 0.5rem);
}

.filter-select {
  flex: 1;
  padding: var(--spacing-xs, 0.375rem) var(--spacing-sm, 0.5rem);
  background: var(--bg-secondary, #fff);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: var(--radius-sm, 6px);
  font-size: max(13px, 0.8125rem);
  color: var(--text-primary, #111827);
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 0;
}

.filter-select:hover {
  border-color: var(--accent-color, #10a37f);
}

.filter-select:focus {
  outline: none;
  border-color: var(--accent-color, #10a37f);
  box-shadow: 0 0 0 3px rgba(16, 163, 127, 0.1);
}

@media (max-width: 767px) {
  .search-box {
    padding: var(--spacing-xs, 0.375rem) var(--spacing-sm, 0.5rem);
  }

  .search-input {
    font-size: max(14px, 0.8125rem);
  }

  .filter-bar {
    flex-direction: column;
  }

  .filter-select {
    width: 100%;
  }
}

.new-chat-btn {
  background: var(--accent-color, #0ea5e9);
  border: none;
  color: white;
  padding: var(--spacing-sm, 0.5rem) var(--spacing-md, 0.9rem);
  border-radius: var(--radius-full, 999px);
  font-size: max(14px, 0.85rem);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-xs, 0.3rem);
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 44px; /* 触摸目标大小 */
}

.new-chat-btn:hover {
  background: var(--accent-hover, #0284c7);
  transform: translateY(-1px);
}

.new-chat-btn:active {
  transform: translateY(0);
}

.new-chat-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

@media (max-width: 767px) {
  .new-chat-btn {
    padding: var(--spacing-sm, 0.5rem) var(--spacing-md, 1rem);
    font-size: max(14px, 0.9rem);
  }
}

.model-panel {
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: var(--radius-md, 12px);
  padding: var(--spacing-md, 1rem);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm, 0.5rem);
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .model-panel {
    padding: var(--spacing-sm, 0.75rem);
  }
}

.panel-title {
  font-size: max(12px, 0.8rem);
  letter-spacing: 0.08em;
  color: var(--text-secondary, #6b7280);
  text-transform: uppercase;
  font-weight: 600;
}

.model-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid transparent;
  border-radius: var(--radius-md, 10px);
  padding: var(--spacing-sm, 0.6rem);
  background: var(--user-bubble, #f9fafb);
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 44px; /* 触摸目标大小 */
}

.model-option:hover {
  border-color: var(--border-color, #e5e7eb);
  transform: translateX(2px);
}

.model-option:active {
  transform: scale(0.98);
}

.model-option.active {
  border-color: var(--accent-color, #0ea5e9);
  background: #e0f2fe;
}

.model-option-main {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  flex: 1;
  min-width: 0;
}

.model-name {
  font-weight: 600;
  color: var(--text-primary, #111827);
  font-size: max(14px, 0.9rem);
}

.model-desc {
  font-size: max(12px, 0.8rem);
  color: var(--text-secondary, #6b7280);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.model-pill-indicator {
  background: #cffafe;
  color: #0f766e;
  border-radius: var(--radius-full, 999px);
  padding: 0.1rem var(--spacing-sm, 0.6rem);
  font-size: max(11px, 0.75rem);
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm, 0.5rem);
  min-height: 0;
  -webkit-overflow-scrolling: touch; /* 平滑滚动 */
}

.session-item {
  padding: var(--spacing-md, 0.9rem);
  border-radius: var(--radius-md, 12px);
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 44px; /* 触摸目标大小 */
}

.session-item:hover {
  border-color: var(--border-color, #e5e7eb);
  transform: translateX(2px);
}

.session-item:active {
  transform: scale(0.98);
}

.session-item.active {
  border-color: var(--accent-color, #0ea5e9);
  background: #e0f2fe;
}

.session-title {
  font-weight: 600;
  margin-bottom: 0.3rem;
  color: var(--text-primary, #111827);
  font-size: max(14px, 0.9rem);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  display: flex;
  justify-content: space-between;
  font-size: max(12px, 0.8rem);
  color: var(--text-secondary, #6b7280);
  gap: var(--spacing-xs, 0.5rem);
}

.session-meta span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

@media (max-width: 767px) {
  .session-item {
    padding: var(--spacing-sm, 0.75rem);
  }

  .session-meta {
    flex-direction: column;
    gap: 0.25rem;
  }
}

.session-empty {
  text-align: center;
  padding: var(--spacing-xl, 2rem) var(--spacing-md, 1rem);
  color: var(--text-secondary, #6b7280);
  font-size: max(14px, 0.9rem);
}

/* 滚动条样式 */
.session-list::-webkit-scrollbar {
  width: 6px;
}

.session-list::-webkit-scrollbar-track {
  background: transparent;
}

.session-list::-webkit-scrollbar-thumb {
  background: var(--border-color, #e5e7eb);
  border-radius: 3px;
}

.session-list::-webkit-scrollbar-thumb:hover {
  background: var(--text-secondary, #6b7280);
}
</style>

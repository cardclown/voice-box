<template>
  <aside :class="['session-sidebar', { open: isOpen }]">
    <div class="sidebar-header">
      <div>
        <h3>Chat History</h3>
        <p class="sidebar-subtitle">存储在数据库的历史对话</p>
      </div>
      <button class="new-chat-btn" @click="$emit('create-session')" :disabled="sessionsLoading">
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
        @click="$emit('select-model', option.key)"
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
      <p>{{ sessionsLoading ? '加载中...' : '暂无会话，点击上方按钮新建' }}</p>
    </div>
  </aside>
</template>

<script setup>
const MODEL_OPTIONS = [
  { key: 'doubao', name: '豆包 Doubao', desc: '火山引擎 · 豆包模型' },
  { key: 'deepseek', name: 'DeepSeek', desc: 'DeepSeek 官方模型' }
]

defineProps({
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

const formatDate = (isoString) => {
  if (!isoString) return ''
  try {
    return new Date(isoString).toLocaleString()
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
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

@media (max-width: 768px) {
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
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sidebar-header h3 {
  margin: 0;
  color: var(--text-primary, #111827);
}

.sidebar-subtitle {
  margin: 0;
  color: var(--text-secondary, #6b7280);
  font-size: 0.85rem;
}

.new-chat-btn {
  background: var(--accent-color, #0ea5e9);
  border: none;
  color: white;
  padding: 0.5rem 0.9rem;
  border-radius: 999px;
  font-size: 0.85rem;
  display: flex;
  align-items: center;
  gap: 0.3rem;
  cursor: pointer;
  transition: background 0.2s;
}

.new-chat-btn:hover {
  background: var(--accent-hover, #0284c7);
}

.new-chat-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.model-panel {
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 12px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.panel-title {
  font-size: 0.8rem;
  letter-spacing: 0.08em;
  color: var(--text-secondary, #6b7280);
  text-transform: uppercase;
}

.model-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid transparent;
  border-radius: 10px;
  padding: 0.6rem;
  background: var(--user-bubble, #f9fafb);
  cursor: pointer;
  transition: all 0.2s;
}

.model-option:hover {
  border-color: var(--border-color, #e5e7eb);
}

.model-option.active {
  border-color: var(--accent-color, #0ea5e9);
  background: #e0f2fe;
}

.model-option-main {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.model-name {
  font-weight: 600;
  color: var(--text-primary, #111827);
}

.model-desc {
  font-size: 0.8rem;
  color: var(--text-secondary, #6b7280);
}

.model-pill-indicator {
  background: #cffafe;
  color: #0f766e;
  border-radius: 999px;
  padding: 0.1rem 0.6rem;
  font-size: 0.75rem;
  font-weight: 500;
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
  border-color: var(--border-color, #e5e7eb);
}

.session-item.active {
  border-color: var(--accent-color, #0ea5e9);
  background: #e0f2fe;
}

.session-title {
  font-weight: 600;
  margin-bottom: 0.3rem;
  color: var(--text-primary, #111827);
}

.session-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  color: var(--text-secondary, #6b7280);
}

.session-empty {
  text-align: center;
  padding: 2rem 1rem;
  color: var(--text-secondary, #6b7280);
}
</style>

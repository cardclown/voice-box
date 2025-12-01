<template>
  <div 
    :class="['message-row', message.sender, 'fade-in']"
    @mouseenter="showActions = true"
    @mouseleave="showActions = false"
  >
    <!-- AI 头像（左侧） -->
    <div v-if="message.sender === 'ai'" class="avatar ai-avatar">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10" />
        <circle cx="12" cy="12" r="3" />
      </svg>
    </div>

    <!-- 消息内容 -->
    <div class="message-content">
      <div class="sender-name">{{ message.sender === 'user' ? '我' : 'VoiceBox' }}</div>
      <div class="bubble">
        <div class="bubble-text" v-html="renderedText"></div>
        <!-- 流式响应光标 -->
        <span v-if="message.isStreaming" class="streaming-cursor">▊</span>
        
        <!-- 消息操作按钮 -->
        <div v-if="showActions && !message.isStreaming" class="message-actions">
          <button @click="copyMessage" class="action-btn" title="复制">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
            </svg>
          </button>
          <button v-if="message.sender === 'ai'" @click="regenerate" class="action-btn" title="重新生成">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 4 23 10 17 10"></polyline>
              <polyline points="1 20 1 14 7 14"></polyline>
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
            </svg>
          </button>
          <button v-if="message.sender === 'user'" @click.stop="editMessage" class="action-btn" title="重新编辑">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
          </button>
        </div>
        
        <div v-if="message.attachment" class="attachment-info">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
          </svg>
          {{ message.attachment.name }}
        </div>
      </div>
    </div>

    <!-- 用户头像（右侧） -->
    <div v-if="message.sender === 'user'" class="avatar user-avatar">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { renderMarkdown, containsMarkdown } from '../../utils/markdown'
import { showSuccess, showError } from '../../composables/useToast'

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['regenerate', 'edit', 'delete'])

const showActions = ref(false)

// 渲染消息内容（支持 Markdown）
const renderedText = computed(() => {
  const text = props.message.text || ''
  
  // 检测是否包含 Markdown 语法
  if (containsMarkdown(text)) {
    return renderMarkdown(text)
  }
  
  // 普通文本，转义 HTML 并保留换行
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
})

// 复制消息
async function copyMessage() {
  try {
    await navigator.clipboard.writeText(props.message.text)
    showSuccess('已复制到剪贴板')
  } catch (error) {
    console.error('Copy error:', error)
    showError('复制失败')
  }
}

// 重新生成（AI消息）
function regenerate() {
  console.log('Regenerate message:', props.message)
  emit('regenerate', props.message)
  showSuccess('正在重新生成...')
}

// 重新编辑（用户消息）- 将内容填充到输入框
function editMessage() {
  console.log('Edit message:', props.message)
  emit('edit', props.message)
  showSuccess('内容已填充到输入框')
}
</script>

<style scoped>
/* 淡入动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-in {
  animation: fadeIn 0.3s ease-out;
}

.message-row {
  display: flex;
  gap: var(--spacing-md, 1rem);
  align-items: flex-start;
  padding: var(--spacing-sm, 0.5rem) 0;
  max-width: 100%;
}

/* 用户消息：右对齐 */
.message-row.user {
  flex-direction: row-reverse;
  justify-content: flex-start;
}

/* AI 消息：左对齐 */
.message-row.ai {
  justify-content: flex-start;
}

/* 头像样式 */
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.avatar:hover {
  transform: scale(1.05);
}

.ai-avatar {
  background: linear-gradient(135deg, var(--accent-color, #10a37f) 0%, #0d8a6a 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(16, 163, 127, 0.2);
}

.user-avatar {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);
}

/* 消息内容容器 */
.message-content {
  flex: 1;
  max-width: 75%;
  min-width: 0; /* 防止溢出 */
  display: flex;
  flex-direction: column;
}

/* 用户消息内容右对齐 */
.message-row.user .message-content {
  align-items: flex-end;
}

/* AI 消息内容左对齐 */
.message-row.ai .message-content {
  align-items: flex-start;
}

/* 发送者名称 */
.sender-name {
  font-size: max(12px, 0.75rem);
  font-weight: 600;
  margin-bottom: var(--spacing-xs, 0.25rem);
  color: var(--text-secondary, #6b7280);
  padding: 0 var(--spacing-xs, 0.25rem);
}

/* 消息气泡 */
.bubble {
  padding: var(--spacing-md, 0.875rem) var(--spacing-lg, 1.125rem);
  border-radius: var(--radius-lg, 16px);
  line-height: 1.6;
  word-wrap: break-word;
  word-break: break-word;
  transition: all 0.2s ease;
  position: relative;
  max-width: 100%;
}

/* 用户消息气泡 */
.message-row.user .bubble {
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  color: white;
  border-bottom-right-radius: var(--radius-sm, 4px);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);
}

.message-row.user .bubble:hover {
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
  transform: translateY(-1px);
}

/* AI 消息气泡 */
.message-row.ai .bubble {
  background: var(--ai-bubble-bg, #f9fafb);
  color: var(--text-primary, #111827);
  border: 1px solid var(--border-color, #e5e7eb);
  border-bottom-left-radius: var(--radius-sm, 4px);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.message-row.ai .bubble:hover {
  border-color: var(--accent-color, #10a37f);
  box-shadow: 0 2px 8px rgba(16, 163, 127, 0.1);
}

/* 气泡文本 */
.bubble-text {
  font-size: max(14px, 0.9375rem);
  white-space: pre-wrap;
  position: relative;
}

/* 流式响应光标 */
.streaming-cursor {
  display: inline-block;
  margin-left: 2px;
  animation: blink 1s step-end infinite;
  color: var(--accent-color, #10a37f);
  font-weight: bold;
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

/* 消息操作按钮 */
.message-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs, 0.375rem);
  margin-top: var(--spacing-sm, 0.5rem);
  padding-top: var(--spacing-sm, 0.5rem);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  animation: fadeIn 0.2s ease-out;
  align-items: center;
}

.message-row.user .message-actions {
  border-top-color: rgba(255, 255, 255, 0.2);
  justify-content: flex-end;
}

.message-row.ai .message-actions {
  justify-content: flex-start;
}

.action-btn {
  background: rgba(0, 0, 0, 0.05);
  border: none;
  padding: var(--spacing-xs, 0.5rem);
  border-radius: var(--radius-sm, 6px);
  cursor: pointer;
  color: var(--text-secondary, #6b7280);
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  min-height: 32px;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}

.action-btn:hover {
  background: rgba(0, 0, 0, 0.1);
  color: var(--text-primary, #111827);
  transform: scale(1.08);
  z-index: 2;
}

.action-btn:active {
  transform: scale(0.92);
}

.action-btn:focus {
  outline: 2px solid var(--accent-color, #10a37f);
  outline-offset: 2px;
}

.action-btn-danger:hover {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.message-row.user .action-btn {
  background: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
}

.message-row.user .action-btn:hover {
  background: rgba(255, 255, 255, 0.35);
  color: white;
}

/* 移动端按钮优化 */
@media (max-width: 767px) {
  .message-actions {
    gap: var(--spacing-sm, 0.5rem);
  }

  .action-btn {
    min-width: 44px;
    min-height: 44px;
    padding: var(--spacing-sm, 0.625rem);
  }

  .action-btn svg {
    width: 16px;
    height: 16px;
  }
}

/* 附件信息 */
.attachment-info {
  margin-top: var(--spacing-sm, 0.5rem);
  padding-top: var(--spacing-sm, 0.5rem);
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  font-size: max(13px, 0.8125rem);
  display: flex;
  align-items: center;
  gap: var(--spacing-xs, 0.25rem);
  opacity: 0.9;
}

.message-row.ai .attachment-info {
  border-top-color: var(--border-color, #e5e7eb);
  color: var(--text-secondary, #6b7280);
}

/* 移动端优化 */
@media (max-width: 767px) {
  .message-row {
    gap: var(--spacing-sm, 0.5rem);
  }

  .message-content {
    max-width: 85%;
  }

  .avatar {
    width: 28px;
    height: 28px;
  }

  .avatar svg {
    width: 16px;
    height: 16px;
  }

  .bubble {
    padding: var(--spacing-sm, 0.75rem) var(--spacing-md, 1rem);
    border-radius: var(--radius-md, 14px);
  }

  .bubble-text {
    font-size: max(14px, 0.875rem);
  }

  .sender-name {
    font-size: max(11px, 0.7rem);
  }
}

/* 平板设备优化 */
@media (min-width: 768px) and (max-width: 1023px) {
  .message-content {
    max-width: 80%;
  }
}

/* 大屏幕优化 */
@media (min-width: 1440px) {
  .message-content {
    max-width: 70%;
  }

  .bubble {
    padding: var(--spacing-md, 1rem) var(--spacing-lg, 1.25rem);
  }
}

/* 打印样式 */
@media print {
  .message-row {
    page-break-inside: avoid;
  }

  .avatar {
    display: none;
  }

  .bubble {
    box-shadow: none !important;
    border: 1px solid #ccc !important;
  }
}
</style>

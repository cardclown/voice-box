<template>
  <div class="input-wrapper">
    <!-- Attachment Preview -->
    <div v-if="tempAttachment" class="attachment-preview">
      <div class="file-icon">📎</div>
      <span class="file-name">{{ tempAttachment.name }}</span>
      <button class="remove-file" @click="$emit('remove-attachment')">×</button>
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

      <textarea 
        ref="textareaRef"
        :value="modelValue" 
        @input="handleInput"
        @keydown="handleKeyDown"
        :placeholder="isRecording ? '正在聆听...' : '输入消息... (Enter 发送, Shift+Enter 换行)'"
        :disabled="loading || isRecording"
        rows="1"
        class="auto-resize-textarea"
      />
      
      <!-- Voice Input Button -->
      <button 
        class="action-btn" 
        :class="{ recording: isRecording }" 
        @click="$emit('toggle-recording')"
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

      <button 
        class="send-btn" 
        @click="$emit('send')" 
        :disabled="loading || (!modelValue.trim() && !tempAttachment)"
        :title="loading ? '正在发送...' : '发送消息'"
      >
        <!-- 加载中显示旋转图标 -->
        <svg 
          v-if="loading" 
          class="loading-icon"
          width="16" 
          height="16" 
          viewBox="0 0 24 24" 
          fill="none" 
          stroke="currentColor" 
          stroke-width="2"
        >
          <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
        </svg>
        <!-- 正常状态显示发送图标 -->
        <svg 
          v-else
          width="16" 
          height="16" 
          viewBox="0 0 24 24" 
          fill="none" 
          stroke="currentColor" 
          stroke-width="2" 
          stroke-linecap="round" 
          stroke-linejoin="round"
        >
          <line x1="22" y1="2" x2="11" y2="13" />
          <polygon points="22 2 15 22 11 13 2 9 22 2" />
        </svg>
      </button>
    </div>
    <p class="footer-text">VoiceBox 可能出现错误，请自行核实重要内容。</p>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  },
  isRecording: {
    type: Boolean,
    default: false
  },
  tempAttachment: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'send', 'toggle-recording', 'attachment-change', 'remove-attachment'])

const attachmentInput = ref(null)
const textareaRef = ref(null)
const MAX_LINES = 5
const LINE_HEIGHT = 24 // 像素

const handleAttachmentClick = () => {
  attachmentInput.value?.click()
}

const handleAttachmentChange = (event) => {
  const file = event.target.files[0]
  if (file) {
    emit('attachment-change', file)
  }
  event.target.value = ''
}

const handleInput = (event) => {
  emit('update:modelValue', event.target.value)
  adjustTextareaHeight()
}

const handleKeyDown = (event) => {
  // Enter 发送，Shift+Enter 换行
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    emit('send')
  }
}

const adjustTextareaHeight = async () => {
  await nextTick()
  const textarea = textareaRef.value
  if (!textarea) return

  // 重置高度以获取正确的 scrollHeight
  textarea.style.height = 'auto'
  
  // 计算新高度
  const scrollHeight = textarea.scrollHeight
  const maxHeight = LINE_HEIGHT * MAX_LINES
  
  if (scrollHeight > maxHeight) {
    textarea.style.height = `${maxHeight}px`
    textarea.style.overflowY = 'auto'
  } else {
    textarea.style.height = `${scrollHeight}px`
    textarea.style.overflowY = 'hidden'
  }
}

const resetTextareaHeight = () => {
  const textarea = textareaRef.value
  if (!textarea) return
  
  textarea.style.height = 'auto'
  textarea.style.overflowY = 'hidden'
}

// 监听 modelValue 变化（例如发送后清空）
watch(() => props.modelValue, (newValue) => {
  if (!newValue) {
    resetTextareaHeight()
  } else {
    adjustTextareaHeight()
  }
})

// 暴露方法供父组件调用
defineExpose({
  resetHeight: resetTextareaHeight
})
</script>

<style scoped>
.input-wrapper {
  padding: var(--spacing-md, 1rem) var(--spacing-lg, 1.5rem);
  border-top: 1px solid var(--border-color, #e5e7eb);
  background: var(--bg-color, #fff);
  flex-shrink: 0;
}

/* 附件预览 */
.attachment-preview {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm, 0.5rem);
  padding: var(--spacing-sm, 0.75rem);
  margin-bottom: var(--spacing-sm, 0.75rem);
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border-radius: var(--radius-md, 10px);
  border: 1px solid var(--border-color, #e5e7eb);
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.file-icon {
  font-size: 1.25rem;
}

.file-name {
  flex: 1;
  font-size: max(14px, 0.9rem);
  color: var(--text-primary, #111827);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.remove-file {
  background: rgba(0, 0, 0, 0.05);
  border: none;
  font-size: 1.25rem;
  cursor: pointer;
  color: var(--text-secondary, #6b7280);
  padding: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.remove-file:hover {
  background: rgba(0, 0, 0, 0.1);
  color: var(--text-primary, #111827);
  transform: rotate(90deg);
}

/* 输入框容器 */
.input-box {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm, 0.5rem);
  padding: var(--spacing-sm, 0.75rem) var(--spacing-md, 1rem);
  border: 2px solid var(--border-color, #e5e7eb);
  border-radius: var(--radius-xl, 24px);
  background: var(--bg-color, #fff);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.input-box:focus-within {
  border-color: var(--accent-color, #10a37f);
  box-shadow: 0 4px 12px rgba(16, 163, 127, 0.15);
}

.hidden-input {
  display: none;
}

/* 操作按钮 */
.action-btn {
  background: transparent;
  border: none;
  color: var(--text-secondary, #6b7280);
  cursor: pointer;
  padding: var(--spacing-xs, 0.375rem);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md, 8px);
  transition: all 0.2s ease;
  min-width: 32px;
  min-height: 32px;
}

.action-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-primary, #111827);
  transform: scale(1.05);
}

.action-btn:active {
  transform: scale(0.95);
}

.action-btn.recording {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

.action-btn.recording:hover {
  background: rgba(239, 68, 68, 0.15);
}

/* 录音指示器 */
.recording-indicator {
  display: flex;
  gap: 3px;
  align-items: center;
}

.recording-indicator .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 1.2s ease-in-out infinite;
}

.recording-indicator .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.recording-indicator .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}

/* 输入框 - Textarea */
.auto-resize-textarea {
  flex: 1;
  border: none;
  outline: none;
  font-size: max(14px, 1rem);
  font-family: inherit;
  background: transparent;
  color: var(--text-primary, #111827);
  min-width: 0;
  resize: none;
  line-height: 1.5;
  max-height: 120px; /* 5 行的最大高度 */
  overflow-y: hidden;
  transition: height 0.1s ease;
}

.auto-resize-textarea::placeholder {
  color: var(--text-secondary, #6b7280);
}

.auto-resize-textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 滚动条样式（当超过最大行数时） */
.auto-resize-textarea::-webkit-scrollbar {
  width: 4px;
}

.auto-resize-textarea::-webkit-scrollbar-track {
  background: transparent;
}

.auto-resize-textarea::-webkit-scrollbar-thumb {
  background: var(--border-color, #e5e7eb);
  border-radius: 2px;
}

.auto-resize-textarea::-webkit-scrollbar-thumb:hover {
  background: var(--text-secondary, #6b7280);
}

/* 发送按钮 */
.send-btn {
  background: linear-gradient(135deg, var(--accent-color, #10a37f) 0%, #0d8a6a 100%);
  border: none;
  color: white;
  padding: var(--spacing-sm, 0.5rem);
  border-radius: var(--radius-md, 10px);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  min-width: 40px;
  min-height: 40px;
  box-shadow: 0 2px 8px rgba(16, 163, 127, 0.2);
}

.send-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #0d8a6a 0%, #0a6b52 100%);
  box-shadow: 0 4px 12px rgba(16, 163, 127, 0.3);
  transform: translateY(-2px);
}

.send-btn:active:not(:disabled) {
  transform: translateY(0);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

/* 加载图标旋转动画 */
.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 底部文本 */
.footer-text {
  margin: var(--spacing-sm, 0.5rem) 0 0 0;
  font-size: max(12px, 0.75rem);
  color: var(--text-secondary, #6b7280);
  text-align: center;
  line-height: 1.4;
}

/* 移动端优化 */
@media (max-width: 767px) {
  .input-wrapper {
    padding: var(--spacing-sm, 0.75rem) var(--spacing-md, 1rem);
    /* 为移动端底部导航预留空间 */
    padding-bottom: calc(60px + var(--spacing-sm, 0.75rem));
  }

  .input-box {
    padding: var(--spacing-xs, 0.5rem) var(--spacing-sm, 0.75rem);
    border-radius: var(--radius-lg, 20px);
  }

  .input-box input {
    font-size: max(14px, 0.9375rem);
  }

  .action-btn {
    min-width: 36px;
    min-height: 36px;
  }

  .send-btn {
    min-width: 36px;
    min-height: 36px;
    padding: var(--spacing-xs, 0.375rem);
  }

  .footer-text {
    font-size: max(11px, 0.7rem);
  }
}

/* 平板设备优化 */
@media (min-width: 768px) and (max-width: 1023px) {
  .input-wrapper {
    padding: var(--spacing-md, 1rem);
  }
}

/* 大屏幕优化 */
@media (min-width: 1440px) {
  .input-wrapper {
    padding: var(--spacing-lg, 1.5rem) var(--spacing-xl, 2rem);
  }

  .input-box {
    padding: var(--spacing-md, 1rem) var(--spacing-lg, 1.25rem);
  }
}

/* 横屏模式优化 */
@media (orientation: landscape) and (max-height: 500px) {
  .input-wrapper {
    padding: var(--spacing-xs, 0.5rem) var(--spacing-md, 1rem);
  }

  .input-box {
    padding: var(--spacing-xs, 0.5rem);
  }

  .footer-text {
    display: none; /* 横屏时隐藏以节省空间 */
  }
}
</style>

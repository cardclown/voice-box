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

      <input 
        :value="modelValue" 
        @input="$emit('update:modelValue', $event.target.value)"
        @keyup.enter="$emit('send')" 
        :placeholder="isRecording ? '正在聆听...' : '输入消息...'"
        :disabled="loading || isRecording"
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

      <button class="send-btn" @click="$emit('send')" :disabled="loading || (!modelValue.trim() && !tempAttachment)">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13" />
          <polygon points="22 2 15 22 11 13 2 9 22 2" />
        </svg>
      </button>
    </div>
    <p class="footer-text">VoiceBox 可能出现错误，请自行核实重要内容。</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
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
</script>

<style scoped>
.input-wrapper {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--border-color, #e5e7eb);
  background: var(--bg-color, #fff);
}

.attachment-preview {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  margin-bottom: 0.5rem;
  background: var(--user-bubble, #f3f4f6);
  border-radius: 8px;
}

.file-icon {
  font-size: 1.25rem;
}

.file-name {
  flex: 1;
  font-size: 0.9rem;
  color: var(--text-primary, #111827);
}

.remove-file {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--text-secondary, #6b7280);
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-file:hover {
  color: var(--text-primary, #111827);
}

.input-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 12px;
  background: var(--bg-color, #fff);
}

.hidden-input {
  display: none;
}

.action-btn {
  background: transparent;
  border: none;
  color: var(--text-secondary, #6b7280);
  cursor: pointer;
  padding: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.action-btn:hover {
  color: var(--text-primary, #111827);
}

.action-btn.recording {
  color: #ef4444;
}

.recording-indicator {
  display: flex;
  gap: 2px;
}

.recording-indicator .dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 1s infinite;
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
  }
  50% {
    opacity: 1;
  }
}

.input-box input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 1rem;
  background: transparent;
  color: var(--text-primary, #111827);
}

.input-box input::placeholder {
  color: var(--text-secondary, #6b7280);
}

.send-btn {
  background: var(--accent-color, #10a37f);
  border: none;
  color: white;
  padding: 0.5rem;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: var(--accent-hover, #0d8a6a);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.footer-text {
  margin: 0.5rem 0 0 0;
  font-size: 0.75rem;
  color: var(--text-secondary, #6b7280);
  text-align: center;
}
</style>

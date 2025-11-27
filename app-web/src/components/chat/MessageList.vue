<template>
  <div class="messages-container" ref="chatWindow">
    <div v-if="messages.length === 0" class="welcome-screen">
      <div class="welcome-icon">✨</div>
      <h2>开始新的对话</h2>
      <p>选择模型，输入内容，左侧会自动保存所有历史。</p>
    </div>

    <div v-for="(msg, index) in messages" :key="`msg-${index}`" class="message-wrapper">
      <MessageItem :message="msg" />
    </div>

    <div v-if="loading" class="message-wrapper">
      <TypingIndicator />
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import MessageItem from './MessageItem.vue'
import TypingIndicator from './TypingIndicator.vue'

defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const chatWindow = ref(null)

const scrollToBottom = async () => {
  await nextTick()
  if (chatWindow.value) {
    chatWindow.value.scrollTop = chatWindow.value.scrollHeight
  }
}

defineExpose({
  scrollToBottom
})
</script>

<style scoped>
.messages-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: var(--spacing-lg, 1.5rem);
  background: var(--bg-color, #fff);
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch; /* iOS 平滑滚动 */
}

/* 滚动条样式 */
.messages-container::-webkit-scrollbar {
  width: 8px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background: var(--border-color, #e5e7eb);
  border-radius: 4px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: var(--text-secondary, #6b7280);
}

/* 欢迎屏幕 */
.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: var(--text-secondary, #6b7280);
  padding: var(--spacing-xl, 2rem);
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.welcome-icon {
  font-size: 4rem;
  margin-bottom: var(--spacing-lg, 1.5rem);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.welcome-screen h2 {
  margin: 0 0 var(--spacing-md, 0.75rem) 0;
  color: var(--text-primary, #111827);
  font-size: max(20px, 1.5rem);
  font-weight: 600;
}

.welcome-screen p {
  margin: 0;
  max-width: 500px;
  font-size: max(14px, 1rem);
  line-height: 1.6;
}

/* 消息包装器 */
.message-wrapper {
  margin-bottom: var(--spacing-lg, 1.25rem);
}

.message-wrapper:last-child {
  margin-bottom: var(--spacing-xl, 2rem);
}

/* 移动端优化 */
@media (max-width: 767px) {
  .messages-container {
    padding: var(--spacing-md, 1rem);
    /* 为移动端底部导航预留空间 */
    padding-bottom: calc(60px + var(--spacing-md, 1rem));
  }

  .welcome-screen {
    padding: var(--spacing-lg, 1.5rem);
  }

  .welcome-icon {
    font-size: 3rem;
    margin-bottom: var(--spacing-md, 1rem);
  }

  .welcome-screen h2 {
    font-size: max(18px, 1.25rem);
  }

  .welcome-screen p {
    font-size: max(14px, 0.9rem);
  }

  .message-wrapper {
    margin-bottom: var(--spacing-md, 1rem);
  }
}

/* 平板设备优化 */
@media (min-width: 768px) and (max-width: 1023px) {
  .messages-container {
    padding: var(--spacing-lg, 1.25rem);
  }
}

/* 大屏幕优化 */
@media (min-width: 1440px) {
  .messages-container {
    padding: var(--spacing-xl, 2rem) var(--spacing-xl, 3rem);
  }

  .message-wrapper {
    margin-bottom: var(--spacing-xl, 1.5rem);
  }
}

/* 横屏模式优化 */
@media (orientation: landscape) and (max-height: 500px) {
  .messages-container {
    padding: var(--spacing-sm, 0.75rem);
  }

  .welcome-icon {
    font-size: 2rem;
    margin-bottom: var(--spacing-sm, 0.5rem);
  }

  .welcome-screen h2 {
    font-size: max(16px, 1.125rem);
  }

  .welcome-screen p {
    font-size: max(13px, 0.875rem);
  }
}
</style>

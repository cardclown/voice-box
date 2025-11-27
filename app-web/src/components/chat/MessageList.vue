<template>
  <div class="messages-container" ref="chatWindow">
    <div v-if="messages.length === 0" class="welcome-screen">
      <div class="welcome-icon">✨</div>
      <h2>开始新的对话</h2>
      <p>选择模型，输入内容，左侧会自动保存所有历史。</p>
    </div>

    <div v-for="(msg, index) in messages" :key="index" class="message-wrapper">
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
  padding: 1.5rem;
  background: var(--bg-color, #fff);
}

.welcome-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: var(--text-secondary, #6b7280);
}

.welcome-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.welcome-screen h2 {
  margin: 0 0 0.5rem 0;
  color: var(--text-primary, #111827);
}

.welcome-screen p {
  margin: 0;
  max-width: 400px;
}

.message-wrapper {
  margin-bottom: 1rem;
}
</style>

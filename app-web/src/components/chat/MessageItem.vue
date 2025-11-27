<template>
  <div :class="['message-row', message.sender]">
    <div v-if="message.sender === 'ai'" class="avatar ai-avatar">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 14a4 4 0 1 1 4-4 4 4 0 0 1-4 4z" />
      </svg>
    </div>
    <div class="message-content">
      <div class="sender-name">{{ message.sender === 'user' ? '我' : 'VoiceBox' }}</div>
      <div class="bubble">{{ message.text }}</div>
      <div v-if="message.attachment" class="attachment-info">
        📎 {{ message.attachment.name }}
      </div>
    </div>
    <div v-if="message.sender === 'user'" class="avatar user-avatar">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </svg>
    </div>
  </div>
</template>

<script setup>
defineProps({
  message: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.message-row {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-avatar {
  background: var(--accent-color, #10a37f);
  color: white;
}

.user-avatar {
  background: var(--user-bubble, #f3f4f6);
  color: var(--text-primary, #111827);
}

.message-content {
  flex: 1;
  max-width: 70%;
}

.message-row.user .message-content {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.sender-name {
  font-size: 0.75rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
  color: var(--text-secondary, #6b7280);
}

.bubble {
  padding: 0.75rem 1rem;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
}

.message-row.user .bubble {
  background: var(--user-bubble, #f3f4f6);
  color: var(--text-primary, #111827);
}

.message-row.ai .bubble {
  background: var(--ai-bubble, transparent);
  color: var(--text-primary, #111827);
}

.attachment-info {
  margin-top: 0.5rem;
  font-size: 0.85rem;
  color: var(--text-secondary, #6b7280);
}

@media (max-width: 768px) {
  .message-content {
    max-width: 85%;
  }
}
</style>

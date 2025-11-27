<template>
  <aside class="module-nav">
    <div class="nav-logo">VB</div>
    <button
      class="module-btn"
      :class="{ active: currentModule === 'chat' }"
      @click="$emit('switch-module', 'chat')"
    >
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>
      <span>Chat</span>
    </button>
    <button
      class="module-btn"
      :class="{ active: currentModule === 'video' }"
      @click="$emit('switch-module', 'video')"
    >
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polygon points="23 7 16 12 23 17 23 7" />
        <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
      </svg>
      <span>Video</span>
    </button>
  </aside>
</template>

<script setup>
defineProps({
  currentModule: {
    type: String,
    required: true
  }
})

defineEmits(['switch-module'])
</script>

<style scoped>
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

@media (max-width: 768px) {
  .module-nav {
    display: none;
  }
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

.module-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.module-btn.active {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}
</style>

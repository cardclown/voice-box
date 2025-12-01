<template>
  <aside class="module-nav" :class="{ 'mobile-hidden': isMobile }">
    <div class="nav-logo">VB</div>
    <nav class="nav-buttons">
      <button
        class="module-btn"
        :class="{ active: currentModule === 'chat' }"
        @click="handleModuleSwitch('chat')"
        :aria-label="'切换到聊天模块'"
        :aria-current="currentModule === 'chat' ? 'page' : undefined"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
        <span>Chat</span>
      </button>
      <button
        class="module-btn"
        :class="{ active: currentModule === 'video' }"
        @click="handleModuleSwitch('video')"
        :aria-label="'切换到视频模块'"
        :aria-current="currentModule === 'video' ? 'page' : undefined"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <polygon points="23 7 16 12 23 17 23 7" />
          <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
        </svg>
        <span>Video</span>
      </button>
      <button
        class="module-btn"
        :class="{ active: currentModule === 'voice' }"
        @click="handleModuleSwitch('voice')"
        :aria-label="'切换到语音交互模块'"
        :aria-current="currentModule === 'voice' ? 'page' : undefined"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
          <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
          <line x1="12" y1="19" x2="12" y2="23"/>
          <line x1="8" y1="23" x2="16" y2="23"/>
        </svg>
        <span>Voice</span>
      </button>
      <button
        class="module-btn"
        :class="{ active: currentModule === 'emotional' }"
        @click="handleModuleSwitch('emotional')"
        :aria-label="'切换到情感语音模块'"
        :aria-current="currentModule === 'emotional' ? 'page' : undefined"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z"/>
          <circle cx="8" cy="10" r="1.5"/>
          <circle cx="16" cy="10" r="1.5"/>
          <path d="M8 15s1.5 2 4 2 4-2 4-2"/>
        </svg>
        <span>Emotion</span>
      </button>
    </nav>
  </aside>

  <!-- 移动端底部导航栏 -->
  <nav class="mobile-nav" v-if="isMobile">
    <button
      class="mobile-nav-btn"
      :class="{ active: currentModule === 'chat' }"
      @click="handleModuleSwitch('chat')"
      :aria-label="'切换到聊天模块'"
    >
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>
      <span>Chat</span>
    </button>
    <button
      class="mobile-nav-btn"
      :class="{ active: currentModule === 'video' }"
      @click="handleModuleSwitch('video')"
      :aria-label="'切换到视频模块'"
    >
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <polygon points="23 7 16 12 23 17 23 7" />
        <rect x="1" y="5" width="15" height="14" rx="2" ry="2" />
      </svg>
      <span>Video</span>
    </button>
    <button
      class="mobile-nav-btn"
      :class="{ active: currentModule === 'voice' }"
      @click="handleModuleSwitch('voice')"
      :aria-label="'切换到语音交互模块'"
    >
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
        <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
        <line x1="12" y1="19" x2="12" y2="23"/>
        <line x1="8" y1="23" x2="16" y2="23"/>
      </svg>
      <span>Voice</span>
    </button>
    <button
      class="mobile-nav-btn"
      :class="{ active: currentModule === 'emotional' }"
      @click="handleModuleSwitch('emotional')"
      :aria-label="'切换到情感语音模块'"
    >
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z"/>
        <circle cx="8" cy="10" r="1.5"/>
        <circle cx="16" cy="10" r="1.5"/>
        <path d="M8 15s1.5 2 4 2 4-2 4-2"/>
      </svg>
      <span>Emotion</span>
    </button>
  </nav>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  currentModule: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['switch-module'])

const isMobile = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

const handleModuleSwitch = (module) => {
  emit('switch-module', module)
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<style scoped>
/* 桌面端侧边栏 */
.module-nav {
  width: 70px;
  background: var(--nav-bg, #202123);
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-md, 1rem) 0;
  gap: var(--spacing-md, 1rem);
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.module-nav.mobile-hidden {
  display: none;
}

.nav-logo {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md, 12px);
  background: white;
  color: #202123;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1rem;
}

.nav-buttons {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm, 0.5rem);
  width: 100%;
  align-items: center;
}

.module-btn {
  width: 50px;
  height: 50px;
  border: none;
  border-radius: var(--radius-md, 12px);
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0.5rem;
}

.module-btn span {
  font-size: max(0.65rem, 10px); /* 确保最小字体大小 */
  line-height: 1;
}

.module-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  transform: translateY(-2px);
}

.module-btn:active {
  transform: translateY(0);
}

.module-btn.active {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

/* 移动端底部导航栏 */
.mobile-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: var(--nav-bg, #202123);
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 0 var(--spacing-md, 1rem);
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.mobile-nav-btn {
  flex: 1;
  max-width: 120px;
  height: 48px;
  border: none;
  border-radius: var(--radius-md, 12px);
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 0.5rem;
}

.mobile-nav-btn span {
  font-size: max(0.75rem, 12px); /* 确保最小字体大小 */
  line-height: 1;
}

.mobile-nav-btn:active {
  background: rgba(255, 255, 255, 0.1);
}

.mobile-nav-btn.active {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

/* 平板设备优化 */
@media (min-width: 768px) and (max-width: 1024px) {
  .module-nav {
    width: 60px;
    padding: var(--spacing-sm, 0.5rem) 0;
  }

  .nav-logo {
    width: 36px;
    height: 36px;
    font-size: 0.9rem;
  }

  .module-btn {
    width: 46px;
    height: 46px;
  }
}

/* 移动设备 */
@media (max-width: 767px) {
  .module-nav {
    display: none;
  }

  /* 为移动端底部导航栏预留空间 */
  :global(body) {
    padding-bottom: 60px;
  }
}

/* 大屏幕优化 */
@media (min-width: 1440px) {
  .module-nav {
    width: 80px;
  }

  .module-btn {
    width: 56px;
    height: 56px;
  }

  .module-btn span {
    font-size: 0.75rem;
  }
}
</style>

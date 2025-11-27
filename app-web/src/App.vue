<script setup>
import { ref, onMounted } from 'vue'
import AppShell from './components/layout/AppShell.vue'
import ChatContainer from './components/chat/ChatContainer.vue'
import VideoGenerator from './components/video/VideoGenerator.vue'
import ThemeToggle from './components/common/ThemeToggle.vue'
import { useThemeStore } from './stores/theme'

const currentModule = ref('chat')
const themeStore = useThemeStore()

const switchModule = (module) => {
  currentModule.value = module
}

onMounted(() => {
  themeStore.loadTheme()
})
</script>

<template>
  <div class="app-container">
    <!-- 主题切换按钮 -->
    <div class="theme-toggle-wrapper">
      <ThemeToggle />
    </div>

    <AppShell :current-module="currentModule" @switch-module="switchModule">
      <ChatContainer v-if="currentModule === 'chat'" />
      <VideoGenerator v-else-if="currentModule === 'video'" />
    </AppShell>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  position: fixed;
  top: 0;
  left: 0;
  background-color: var(--bg-color);
}

/* 主题切换按钮容器 */
.theme-toggle-wrapper {
  position: fixed;
  top: var(--spacing-md, 1rem);
  right: var(--spacing-md, 1rem);
  z-index: var(--z-fixed, 1030);
}

/* 移动端优化 */
@media (max-width: 767px) {
  .theme-toggle-wrapper {
    top: var(--spacing-sm, 0.5rem);
    right: var(--spacing-sm, 0.5rem);
  }
}

/* 平板设备 */
@media (min-width: 768px) and (max-width: 1023px) {
  .theme-toggle-wrapper {
    top: var(--spacing-md, 1rem);
    right: var(--spacing-md, 1rem);
  }
}

/* 大屏幕 */
@media (min-width: 1440px) {
  .theme-toggle-wrapper {
    top: var(--spacing-lg, 1.5rem);
    right: var(--spacing-lg, 1.5rem);
  }
}
</style>

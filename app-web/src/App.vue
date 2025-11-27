<script setup>
import { ref, onMounted } from 'vue'
import AppShell from './components/layout/AppShell.vue'
import ChatContainer from './components/chat/ChatContainer.vue'
import VideoGenerator from './components/video/VideoGenerator.vue'
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
</style>

import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUIStore = defineStore('ui', () => {
  const sessionSidebarOpen = ref(false)
  const currentModule = ref('chat')

  function toggleSessionSidebar() {
    sessionSidebarOpen.value = !sessionSidebarOpen.value
  }

  function openSessionSidebar() {
    sessionSidebarOpen.value = true
  }

  function closeSessionSidebar() {
    sessionSidebarOpen.value = false
  }

  function setCurrentModule(module) {
    currentModule.value = module
  }

  return {
    sessionSidebarOpen,
    currentModule,
    toggleSessionSidebar,
    openSessionSidebar,
    closeSessionSidebar,
    setCurrentModule
  }
})

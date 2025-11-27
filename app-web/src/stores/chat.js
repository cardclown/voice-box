import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref([])
  const selectedModel = ref('doubao')
  const loading = ref(false)

  const currentSession = computed(() => {
    return sessions.value.find(s => s.id === currentSessionId.value)
  })

  const currentSessionTitle = computed(() => {
    return currentSession.value?.title || '新的对话'
  })

  function setSessions(newSessions) {
    sessions.value = newSessions
  }

  function addSession(session) {
    sessions.value.unshift(session)
  }

  function setCurrentSessionId(id) {
    currentSessionId.value = id
  }

  function setMessages(newMessages) {
    messages.value = newMessages
  }

  function addMessage(message) {
    messages.value.push(message)
  }

  function setSelectedModel(model) {
    selectedModel.value = model
  }

  function setLoading(isLoading) {
    loading.value = isLoading
  }

  function clearMessages() {
    messages.value = []
  }

  return {
    sessions,
    currentSessionId,
    messages,
    selectedModel,
    loading,
    currentSession,
    currentSessionTitle,
    setSessions,
    addSession,
    setCurrentSessionId,
    setMessages,
    addMessage,
    setSelectedModel,
    setLoading,
    clearMessages
  }
})

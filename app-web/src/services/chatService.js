import api from './api.js'

export const chatService = {
  async getSessions() {
    return api.get('/chat/sessions')
  },

  async createSession(data) {
    return api.post('/chat/sessions', data)
  },

  async getSessionMessages(sessionId) {
    return api.get(`/chat/sessions/${sessionId}/messages`)
  },

  async sendMessage(data) {
    return api.post('/chat', data)
  },

  async deleteSession(sessionId) {
    return api.delete(`/chat/sessions/${sessionId}`)
  }
}

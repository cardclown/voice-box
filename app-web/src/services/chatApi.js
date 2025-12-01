/**
 * 聊天相关 API
 * 使用统一的 API 客户端
 */

import { get, post, del } from './apiClient.js'

/**
 * 获取设备信息
 */
function getDeviceInfo() {
  if (typeof navigator === 'undefined') return 'unknown-device'
  return `${navigator.userAgent} | ${window.screen.width}x${window.screen.height}`
}

/**
 * 获取会话列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 */
export async function fetchSessions(params = {}) {
  return get('/chat/sessions', params)
}

/**
 * 创建新会话
 * @param {Object} data - 会话数据
 * @param {string} data.title - 会话标题
 * @param {string} data.model - 模型名称
 */
export async function createSession(data) {
  return post('/chat/sessions', {
    title: data.title || 'New Chat',
    model: data.model || 'doubao',
    deviceInfo: getDeviceInfo(),
  })
}

/**
 * 获取会话消息
 * @param {number} sessionId - 会话 ID
 */
export async function fetchSessionMessages(sessionId) {
  return get(`/chat/sessions/${sessionId}/messages`)
}

/**
 * 删除会话
 * @param {number} sessionId - 会话 ID
 */
export async function deleteSession(sessionId) {
  return del(`/chat/sessions/${sessionId}`)
}

/**
 * 发送聊天消息（非流式）
 * @param {Object} data - 消息数据
 * @param {string} data.text - 消息内容
 * @param {string} data.model - 模型名称
 * @param {number} data.sessionId - 会话 ID
 * @param {boolean} data.hasAttachment - 是否有附件
 */
export async function sendMessage(data) {
  return post('/chat', {
    text: data.text,
    model: data.model || 'doubao',
    sessionId: data.sessionId,
    deviceInfo: getDeviceInfo(),
    hasAttachment: data.hasAttachment || false,
  })
}

/**
 * 搜索会话
 * @param {string} query - 搜索关键词
 * @param {Object} filters - 过滤条件
 */
export async function searchSessions(query, filters = {}) {
  return get('/chat/sessions/search', {
    q: query,
    ...filters,
  })
}

/**
 * 更新会话标题
 * @param {number} sessionId - 会话 ID
 * @param {string} title - 新标题
 */
export async function updateSessionTitle(sessionId, title) {
  return post(`/chat/sessions/${sessionId}/title`, { title })
}

/**
 * 删除消息
 * @param {number} messageId - 消息 ID
 */
export async function deleteMessage(messageId) {
  return del(`/chat/messages/${messageId}`)
}

/**
 * 重新生成 AI 回复
 * @param {number} messageId - 消息 ID
 */
export async function regenerateMessage(messageId) {
  return post(`/chat/messages/${messageId}/regenerate`)
}

// 默认导出
export default {
  fetchSessions,
  createSession,
  fetchSessionMessages,
  deleteSession,
  sendMessage,
  searchSessions,
  updateSessionTitle,
  deleteMessage,
  regenerateMessage,
}

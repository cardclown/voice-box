/**
 * 搜索相关 API
 * 支持会话搜索、消息内容搜索等
 */

import { get } from './apiClient.js'

/**
 * 搜索会话
 * @param {Object} params - 搜索参数
 * @param {string} params.query - 搜索关键词
 * @param {string} params.model - 模型筛选
 * @param {string} params.dateRange - 日期范围筛选
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 */
export async function searchSessions(params = {}) {
  return get('/chat/sessions/search', {
    q: params.query || '',
    model: params.model || '',
    dateRange: params.dateRange || '',
    page: params.page || 1,
    size: params.size || 20
  })
}

/**
 * 搜索消息内容
 * @param {Object} params - 搜索参数
 * @param {string} params.query - 搜索关键词
 * @param {number} params.sessionId - 会话ID（可选）
 * @param {string} params.sender - 发送者筛选（user/ai）
 * @param {number} params.page - 页码
 * @param {number} params.size - 每页数量
 */
export async function searchMessages(params = {}) {
  return get('/chat/messages/search', {
    q: params.query || '',
    sessionId: params.sessionId || '',
    sender: params.sender || '',
    page: params.page || 1,
    size: params.size || 20
  })
}

/**
 * 获取搜索建议
 * @param {string} query - 搜索关键词
 * @param {number} limit - 建议数量限制
 */
export async function getSearchSuggestions(query, limit = 5) {
  if (!query || query.length < 2) {
    return []
  }
  
  return get('/chat/search/suggestions', {
    q: query,
    limit
  })
}

/**
 * 获取热门搜索词
 * @param {number} limit - 数量限制
 */
export async function getPopularSearches(limit = 10) {
  return get('/chat/search/popular', { limit })
}

/**
 * 保存搜索历史
 * @param {string} query - 搜索关键词
 */
export async function saveSearchHistory(query) {
  if (!query || query.trim().length === 0) {
    return
  }
  
  // 保存到本地存储
  const history = getLocalSearchHistory()
  const newHistory = [query, ...history.filter(item => item !== query)].slice(0, 20)
  localStorage.setItem('voicebox_search_history', JSON.stringify(newHistory))
  
  // 可选：同时保存到服务器
  // return post('/chat/search/history', { query })
}

/**
 * 获取本地搜索历史
 */
export function getLocalSearchHistory() {
  try {
    const history = localStorage.getItem('voicebox_search_history')
    return history ? JSON.parse(history) : []
  } catch (error) {
    console.error('Failed to load search history:', error)
    return []
  }
}

/**
 * 清除搜索历史
 */
export function clearSearchHistory() {
  localStorage.removeItem('voicebox_search_history')
}

/**
 * 高亮搜索结果
 * @param {string} text - 原始文本
 * @param {string} query - 搜索关键词
 * @returns {string} 高亮后的 HTML
 */
export function highlightSearchResult(text, query) {
  if (!text || !query) {
    return text
  }
  
  // 转义特殊字符
  const escapedQuery = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escapedQuery})`, 'gi')
  
  return text.replace(regex, '<mark class="search-highlight">$1</mark>')
}

// 默认导出
export default {
  searchSessions,
  searchMessages,
  getSearchSuggestions,
  getPopularSearches,
  saveSearchHistory,
  getLocalSearchHistory,
  clearSearchHistory,
  highlightSearchResult
}

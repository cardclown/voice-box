/**
 * 搜索工具
 * 提供高亮、历史记录等功能
 */

const SEARCH_HISTORY_KEY = 'voicebox-search-history'
const MAX_HISTORY_SIZE = 10

/**
 * 高亮搜索关键词
 * @param {string} text - 原始文本
 * @param {string} query - 搜索关键词
 * @returns {string} 高亮后的 HTML
 */
export function highlightText(text, query) {
  if (!text || !query) return text
  
  const regex = new RegExp(`(${escapeRegex(query)})`, 'gi')
  return text.replace(regex, '<mark class="search-highlight">$1</mark>')
}

/**
 * 转义正则表达式特殊字符
 * @param {string} str - 字符串
 * @returns {string} 转义后的字符串
 */
function escapeRegex(str) {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * 获取搜索历史
 * @returns {Array<string>} 搜索历史数组
 */
export function getSearchHistory() {
  try {
    const history = localStorage.getItem(SEARCH_HISTORY_KEY)
    return history ? JSON.parse(history) : []
  } catch (error) {
    console.error('Failed to load search history:', error)
    return []
  }
}

/**
 * 保存搜索历史
 * @param {string} query - 搜索关键词
 */
export function saveSearchHistory(query) {
  if (!query || !query.trim()) return
  
  try {
    let history = getSearchHistory()
    
    // 移除重复项
    history = history.filter(item => item !== query)
    
    // 添加到开头
    history.unshift(query)
    
    // 限制数量
    if (history.length > MAX_HISTORY_SIZE) {
      history = history.slice(0, MAX_HISTORY_SIZE)
    }
    
    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(history))
  } catch (error) {
    console.error('Failed to save search history:', error)
  }
}

/**
 * 清除搜索历史
 */
export function clearSearchHistory() {
  try {
    localStorage.removeItem(SEARCH_HISTORY_KEY)
  } catch (error) {
    console.error('Failed to clear search history:', error)
  }
}

/**
 * 删除单条搜索历史
 * @param {string} query - 要删除的搜索关键词
 */
export function removeSearchHistory(query) {
  try {
    let history = getSearchHistory()
    history = history.filter(item => item !== query)
    localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(history))
  } catch (error) {
    console.error('Failed to remove search history:', error)
  }
}

/**
 * 模糊搜索
 * @param {string} text - 文本
 * @param {string} query - 查询
 * @returns {number} 匹配分数（0-1）
 */
export function fuzzyMatch(text, query) {
  if (!text || !query) return 0
  
  text = text.toLowerCase()
  query = query.toLowerCase()
  
  // 完全匹配
  if (text.includes(query)) return 1
  
  // 模糊匹配
  let score = 0
  let queryIndex = 0
  
  for (let i = 0; i < text.length && queryIndex < query.length; i++) {
    if (text[i] === query[queryIndex]) {
      score++
      queryIndex++
    }
  }
  
  return queryIndex === query.length ? score / text.length : 0
}

/**
 * 搜索会话
 * @param {Array} sessions - 会话列表
 * @param {string} query - 搜索关键词
 * @param {Object} filters - 过滤条件
 * @returns {Array} 过滤后的会话列表
 */
export function searchSessions(sessions, query, filters = {}) {
  let result = [...sessions]
  
  // 关键词搜索
  if (query) {
    const lowerQuery = query.toLowerCase()
    result = result.filter(session => {
      const title = (session.title || '').toLowerCase()
      const model = (session.model || '').toLowerCase()
      return title.includes(lowerQuery) || model.includes(lowerQuery)
    })
  }
  
  // 模型过滤
  if (filters.model) {
    result = result.filter(session => session.model === filters.model)
  }
  
  // 日期过滤
  if (filters.date) {
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    
    result = result.filter(session => {
      if (!session.updatedAt) return false
      const sessionDate = new Date(session.updatedAt)
      
      switch (filters.date) {
        case 'today':
          return sessionDate >= today
        case 'week':
          const weekAgo = new Date(today)
          weekAgo.setDate(weekAgo.getDate() - 7)
          return sessionDate >= weekAgo
        case 'month':
          const monthAgo = new Date(today)
          monthAgo.setMonth(monthAgo.getMonth() - 1)
          return sessionDate >= monthAgo
        default:
          return true
      }
    })
  }
  
  return result
}

/**
 * 搜索消息
 * @param {Array} messages - 消息列表
 * @param {string} query - 搜索关键词
 * @returns {Array} 匹配的消息
 */
export function searchMessages(messages, query) {
  if (!query) return messages
  
  const lowerQuery = query.toLowerCase()
  return messages.filter(msg => 
    (msg.text || '').toLowerCase().includes(lowerQuery)
  )
}

// 默认导出
export default {
  highlightText,
  getSearchHistory,
  saveSearchHistory,
  clearSearchHistory,
  removeSearchHistory,
  fuzzyMatch,
  searchSessions,
  searchMessages
}

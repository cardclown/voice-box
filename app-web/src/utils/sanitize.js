/**
 * HTML 内容清理工具
 * 防止 XSS 攻击
 */

import DOMPurify from 'dompurify'

// DOMPurify 配置
const DEFAULT_CONFIG = {
  ALLOWED_TAGS: [
    'p', 'br', 'strong', 'em', 'u', 's', 'code', 'pre',
    'a', 'ul', 'ol', 'li', 'blockquote', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'table', 'thead', 'tbody', 'tr', 'th', 'td',
    'img', 'span', 'div'
  ],
  ALLOWED_ATTR: [
    'href', 'target', 'rel', 'class', 'id',
    'src', 'alt', 'title', 'width', 'height'
  ],
  ALLOW_DATA_ATTR: false,
  ALLOW_UNKNOWN_PROTOCOLS: false,
  SAFE_FOR_TEMPLATES: true,
}

// 严格配置（仅允许基本文本格式）
const STRICT_CONFIG = {
  ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'code'],
  ALLOWED_ATTR: [],
  ALLOW_DATA_ATTR: false,
}

// 代码块配置（允许代码相关标签）
const CODE_CONFIG = {
  ALLOWED_TAGS: ['pre', 'code', 'span'],
  ALLOWED_ATTR: ['class'],
  ALLOW_DATA_ATTR: false,
}

/**
 * 清理 HTML 内容
 * @param {string} dirty - 待清理的 HTML
 * @param {Object} config - DOMPurify 配置（可选）
 * @returns {string} 清理后的 HTML
 */
export function sanitizeHtml(dirty, config = DEFAULT_CONFIG) {
  if (!dirty || typeof dirty !== 'string') {
    return ''
  }
  
  return DOMPurify.sanitize(dirty, config)
}

/**
 * 清理文本（移除所有 HTML 标签）
 * @param {string} dirty - 待清理的文本
 * @returns {string} 纯文本
 */
export function sanitizeText(dirty) {
  if (!dirty || typeof dirty !== 'string') {
    return ''
  }
  
  return DOMPurify.sanitize(dirty, {
    ALLOWED_TAGS: [],
    ALLOWED_ATTR: []
  })
}

/**
 * 清理 URL
 * @param {string} url - 待清理的 URL
 * @returns {string} 清理后的 URL
 */
export function sanitizeUrl(url) {
  if (!url || typeof url !== 'string') {
    return ''
  }
  
  // 只允许 http, https, mailto 协议
  const allowedProtocols = ['http:', 'https:', 'mailto:']
  
  try {
    const urlObj = new URL(url)
    if (allowedProtocols.includes(urlObj.protocol)) {
      return url
    }
  } catch (error) {
    // 无效的 URL
    return ''
  }
  
  return ''
}

/**
 * 清理消息内容（用于聊天消息）
 * @param {string} message - 消息内容
 * @returns {string} 清理后的消息
 */
export function sanitizeMessage(message) {
  if (!message || typeof message !== 'string') {
    return ''
  }
  
  // 使用严格配置，只允许基本格式
  return sanitizeHtml(message, STRICT_CONFIG)
}

/**
 * 清理代码块
 * @param {string} code - 代码内容
 * @returns {string} 清理后的代码
 */
export function sanitizeCode(code) {
  if (!code || typeof code !== 'string') {
    return ''
  }
  
  return sanitizeHtml(code, CODE_CONFIG)
}

/**
 * 转义 HTML 特殊字符
 * @param {string} text - 文本内容
 * @returns {string} 转义后的文本
 */
export function escapeHtml(text) {
  if (!text || typeof text !== 'string') {
    return ''
  }
  
  const map = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#039;'
  }
  
  return text.replace(/[&<>"']/g, (char) => map[char])
}

/**
 * 反转义 HTML 特殊字符
 * @param {string} text - 转义后的文本
 * @returns {string} 原始文本
 */
export function unescapeHtml(text) {
  if (!text || typeof text !== 'string') {
    return ''
  }
  
  const map = {
    '&amp;': '&',
    '&lt;': '<',
    '&gt;': '>',
    '&quot;': '"',
    '&#039;': "'"
  }
  
  return text.replace(/&amp;|&lt;|&gt;|&quot;|&#039;/g, (entity) => map[entity])
}

/**
 * 检查内容是否包含潜在的 XSS 攻击
 * @param {string} content - 待检查的内容
 * @returns {boolean} 是否包含潜在攻击
 */
export function containsXss(content) {
  if (!content || typeof content !== 'string') {
    return false
  }
  
  // 检查常见的 XSS 模式
  const xssPatterns = [
    /<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi,
    /javascript:/gi,
    /on\w+\s*=/gi, // onclick, onload 等
    /<iframe/gi,
    /<object/gi,
    /<embed/gi,
    /eval\(/gi,
    /expression\(/gi,
  ]
  
  return xssPatterns.some(pattern => pattern.test(content))
}

/**
 * 配置 DOMPurify 钩子
 */
export function configureDOMPurify() {
  // 添加钩子：在清理后添加 target="_blank" 和 rel="noopener noreferrer" 到外部链接
  DOMPurify.addHook('afterSanitizeAttributes', (node) => {
    // 处理链接
    if (node.tagName === 'A') {
      const href = node.getAttribute('href')
      if (href && (href.startsWith('http://') || href.startsWith('https://'))) {
        node.setAttribute('target', '_blank')
        node.setAttribute('rel', 'noopener noreferrer')
      }
    }
    
    // 处理图片：添加 loading="lazy"
    if (node.tagName === 'IMG') {
      node.setAttribute('loading', 'lazy')
    }
  })
}

// 初始化配置
configureDOMPurify()

// 默认导出
export default {
  sanitizeHtml,
  sanitizeText,
  sanitizeUrl,
  sanitizeMessage,
  sanitizeCode,
  escapeHtml,
  unescapeHtml,
  containsXss,
  configureDOMPurify
}

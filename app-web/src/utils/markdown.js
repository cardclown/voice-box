/**
 * Markdown 渲染工具
 * 支持代码高亮和安全的 HTML 渲染
 */

import { marked } from 'marked'
import hljs from 'highlight.js'
import { sanitizeHtml } from './sanitize'

// 配置 marked
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (err) {
        console.error('Highlight error:', err)
      }
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true, // 支持 GFM 换行
  gfm: true, // 启用 GitHub Flavored Markdown
  pedantic: false,
  sanitize: false, // 我们使用 DOMPurify 来清理
})

// 自定义渲染器
const renderer = new marked.Renderer()

// 自定义代码块渲染
renderer.code = function(code, language) {
  const validLang = language && hljs.getLanguage(language) ? language : 'plaintext'
  const highlighted = hljs.highlight(code, { language: validLang }).value
  
  return `
    <div class="code-block">
      <div class="code-header">
        <span class="code-language">${validLang}</span>
        <button class="code-copy-btn" onclick="copyCode(this)" title="复制代码">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
          </svg>
        </button>
      </div>
      <pre><code class="hljs language-${validLang}">${highlighted}</code></pre>
    </div>
  `
}

// 自定义行内代码渲染
renderer.codespan = function(code) {
  return `<code class="inline-code">${code}</code>`
}

// 自定义链接渲染（添加安全属性）
renderer.link = function(href, title, text) {
  const titleAttr = title ? ` title="${title}"` : ''
  const isExternal = href.startsWith('http://') || href.startsWith('https://')
  const target = isExternal ? ' target="_blank" rel="noopener noreferrer"' : ''
  
  return `<a href="${href}"${titleAttr}${target}>${text}</a>`
}

// 自定义表格渲染
renderer.table = function(header, body) {
  return `
    <div class="table-wrapper">
      <table class="markdown-table">
        <thead>${header}</thead>
        <tbody>${body}</tbody>
      </table>
    </div>
  `
}

// 自定义引用块渲染
renderer.blockquote = function(quote) {
  return `<blockquote class="markdown-blockquote">${quote}</blockquote>`
}

marked.use({ renderer })

/**
 * 渲染 Markdown 为 HTML
 * @param {string} markdown - Markdown 文本
 * @param {boolean} sanitize - 是否清理 HTML（默认 true）
 * @returns {string} HTML 字符串
 */
export function renderMarkdown(markdown, sanitize = true) {
  if (!markdown || typeof markdown !== 'string') {
    return ''
  }
  
  try {
    // 渲染 Markdown
    let html = marked.parse(markdown)
    
    // 清理 HTML（防止 XSS）
    if (sanitize) {
      html = sanitizeHtml(html, {
        ALLOWED_TAGS: [
          'p', 'br', 'strong', 'em', 'u', 's', 'code', 'pre',
          'a', 'ul', 'ol', 'li', 'blockquote',
          'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
          'table', 'thead', 'tbody', 'tr', 'th', 'td',
          'div', 'span', 'hr', 'img'
        ],
        ALLOWED_ATTR: [
          'href', 'target', 'rel', 'class', 'id', 'title',
          'src', 'alt', 'width', 'height', 'onclick'
        ],
        ALLOW_DATA_ATTR: false,
      })
    }
    
    return html
  } catch (error) {
    console.error('Markdown render error:', error)
    return markdown // 渲染失败时返回原始文本
  }
}

/**
 * 检测文本是否包含 Markdown 语法
 * @param {string} text - 文本内容
 * @returns {boolean} 是否包含 Markdown
 */
export function containsMarkdown(text) {
  if (!text || typeof text !== 'string') {
    return false
  }
  
  // 检测常见的 Markdown 模式
  const markdownPatterns = [
    /^#{1,6}\s/m, // 标题
    /\*\*.*\*\*/,  // 粗体
    /__.*__/,      // 粗体
    /\*.*\*/,      // 斜体
    /_.*_/,        // 斜体
    /```[\s\S]*```/, // 代码块
    /`[^`]+`/,     // 行内代码
    /^\s*[-*+]\s/m, // 无序列表
    /^\s*\d+\.\s/m, // 有序列表
    /\[.*\]\(.*\)/, // 链接
    /!\[.*\]\(.*\)/, // 图片
    /^\s*>/m,      // 引用
    /^\s*\|.*\|/m, // 表格
  ]
  
  return markdownPatterns.some(pattern => pattern.test(text))
}

/**
 * 提取代码块
 * @param {string} markdown - Markdown 文本
 * @returns {Array} 代码块数组
 */
export function extractCodeBlocks(markdown) {
  if (!markdown || typeof markdown !== 'string') {
    return []
  }
  
  const codeBlockRegex = /```(\w+)?\n([\s\S]*?)```/g
  const blocks = []
  let match
  
  while ((match = codeBlockRegex.exec(markdown)) !== null) {
    blocks.push({
      language: match[1] || 'plaintext',
      code: match[2].trim()
    })
  }
  
  return blocks
}

/**
 * 复制代码到剪贴板（全局函数，供 HTML 调用）
 */
if (typeof window !== 'undefined') {
  window.copyCode = function(button) {
    const codeBlock = button.closest('.code-block')
    const code = codeBlock.querySelector('code').textContent
    
    navigator.clipboard.writeText(code).then(() => {
      // 显示复制成功提示
      const originalHTML = button.innerHTML
      button.innerHTML = `
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
      `
      button.classList.add('copied')
      
      setTimeout(() => {
        button.innerHTML = originalHTML
        button.classList.remove('copied')
      }, 2000)
    }).catch(err => {
      console.error('复制失败:', err)
    })
  }
}

// 默认导出
export default {
  renderMarkdown,
  containsMarkdown,
  extractCodeBlocks
}

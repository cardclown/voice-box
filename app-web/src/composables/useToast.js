/**
 * Toast 通知系统
 * 提供全局的消息提示功能
 */

import { reactive } from 'vue'

// 全局 Toast 状态
export const toastState = reactive({
  toasts: [],
  nextId: 1
})

// Toast 配置
const TOAST_CONFIG = {
  duration: 3000, // 默认显示时长（毫秒）
  maxToasts: 5, // 最多同时显示的 Toast 数量
}

/**
 * 添加 Toast
 * @param {Object} options - Toast 选项
 * @param {string} options.message - 消息内容
 * @param {string} options.type - 类型：success, error, warning, info
 * @param {string} options.description - 描述信息（可选）
 * @param {number} options.duration - 显示时长（可选）
 * @param {Function} options.onClose - 关闭回调（可选）
 */
function addToast(options) {
  const toast = {
    id: toastState.nextId++,
    message: options.message || '',
    type: options.type || 'info',
    description: options.description || '',
    duration: options.duration ?? TOAST_CONFIG.duration,
    onClose: options.onClose || null,
  }
  
  // 如果超过最大数量，移除最早的 Toast
  if (toastState.toasts.length >= TOAST_CONFIG.maxToasts) {
    const oldestToast = toastState.toasts[0]
    removeToast(oldestToast.id)
  }
  
  toastState.toasts.push(toast)
  
  // 自动移除
  if (toast.duration > 0) {
    setTimeout(() => {
      removeToast(toast.id)
    }, toast.duration)
  }
  
  return toast.id
}

/**
 * 移除 Toast
 * @param {number} id - Toast ID
 */
export function removeToast(id) {
  const index = toastState.toasts.findIndex(t => t.id === id)
  if (index > -1) {
    const toast = toastState.toasts[index]
    toastState.toasts.splice(index, 1)
    
    // 调用关闭回调
    if (toast.onClose) {
      toast.onClose()
    }
  }
}

/**
 * 清除所有 Toast
 */
export function clearAllToasts() {
  toastState.toasts.forEach(toast => {
    if (toast.onClose) {
      toast.onClose()
    }
  })
  toastState.toasts = []
}

/**
 * 显示成功消息
 * @param {string} message - 消息内容
 * @param {string} description - 描述信息（可选）
 * @param {number} duration - 显示时长（可选）
 */
export function showSuccess(message, description = '', duration) {
  return addToast({
    message,
    description,
    type: 'success',
    duration
  })
}

/**
 * 显示错误消息
 * @param {string} message - 消息内容
 * @param {string} description - 描述信息（可选）
 * @param {number} duration - 显示时长（可选）
 */
export function showError(message, description = '', duration) {
  return addToast({
    message,
    description,
    type: 'error',
    duration: duration ?? 5000 // 错误消息默认显示更长时间
  })
}

/**
 * 显示警告消息
 * @param {string} message - 消息内容
 * @param {string} description - 描述信息（可选）
 * @param {number} duration - 显示时长（可选）
 */
export function showWarning(message, description = '', duration) {
  return addToast({
    message,
    description,
    type: 'warning',
    duration
  })
}

/**
 * 显示信息消息
 * @param {string} message - 消息内容
 * @param {string} description - 描述信息（可选）
 * @param {number} duration - 显示时长（可选）
 */
export function showInfo(message, description = '', duration) {
  return addToast({
    message,
    description,
    type: 'info',
    duration
  })
}

/**
 * 显示加载中消息
 * @param {string} message - 消息内容
 * @returns {number} Toast ID，用于后续更新或关闭
 */
export function showLoading(message = '加载中...') {
  return addToast({
    message,
    type: 'info',
    duration: 0 // 不自动关闭
  })
}

/**
 * 更新 Toast
 * @param {number} id - Toast ID
 * @param {Object} options - 更新选项
 */
export function updateToast(id, options) {
  const toast = toastState.toasts.find(t => t.id === id)
  if (toast) {
    if (options.message !== undefined) toast.message = options.message
    if (options.type !== undefined) toast.type = options.type
    if (options.description !== undefined) toast.description = options.description
    
    // 如果设置了新的 duration，重新设置自动关闭
    if (options.duration !== undefined && options.duration > 0) {
      setTimeout(() => {
        removeToast(id)
      }, options.duration)
    }
  }
}

/**
 * Promise Toast
 * 根据 Promise 状态自动显示不同的消息
 * @param {Promise} promise - Promise 对象
 * @param {Object} messages - 消息配置
 * @param {string} messages.loading - 加载中消息
 * @param {string} messages.success - 成功消息
 * @param {string} messages.error - 错误消息
 */
export async function showPromiseToast(promise, messages = {}) {
  const loadingId = showLoading(messages.loading || '处理中...')
  
  try {
    const result = await promise
    removeToast(loadingId)
    showSuccess(messages.success || '操作成功')
    return result
  } catch (error) {
    removeToast(loadingId)
    showError(messages.error || '操作失败', error.message)
    throw error
  }
}

/**
 * useToast Hook
 * 在组件中使用 Toast
 */
export function useToast() {
  return {
    showSuccess,
    showError,
    showWarning,
    showInfo,
    showLoading,
    updateToast,
    removeToast,
    clearAllToasts,
    showPromiseToast,
    toasts: toastState.toasts
  }
}

// 默认导出
export default {
  showSuccess,
  showError,
  showWarning,
  showInfo,
  showLoading,
  updateToast,
  removeToast,
  clearAllToasts,
  showPromiseToast,
  useToast
}

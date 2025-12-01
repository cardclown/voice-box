/**
 * 统一的 API 客户端
 * 提供请求拦截、错误处理、重试机制等功能
 */

// API 配置
const API_CONFIG = {
  baseURL: import.meta.env.VITE_API_BASE || 'http://localhost:10088/api',
  timeout: 30000, // 30秒超时
  retryCount: 3, // 重试次数
  retryDelay: 1000, // 重试延迟（毫秒）
}

/**
 * API 错误类
 */
export class ApiError extends Error {
  constructor(message, status, data) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
  }
}

/**
 * 请求拦截器列表
 */
const requestInterceptors = []

/**
 * 响应拦截器列表
 */
const responseInterceptors = []

/**
 * 添加请求拦截器
 * @param {Function} interceptor - 拦截器函数
 */
export function addRequestInterceptor(interceptor) {
  requestInterceptors.push(interceptor)
}

/**
 * 添加响应拦截器
 * @param {Function} interceptor - 拦截器函数
 */
export function addResponseInterceptor(interceptor) {
  responseInterceptors.push(interceptor)
}

/**
 * 延迟函数
 * @param {number} ms - 延迟毫秒数
 */
function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 执行请求拦截器
 * @param {Object} config - 请求配置
 */
async function executeRequestInterceptors(config) {
  let modifiedConfig = { ...config }
  
  for (const interceptor of requestInterceptors) {
    try {
      modifiedConfig = await interceptor(modifiedConfig)
    } catch (error) {
      console.error('Request interceptor error:', error)
    }
  }
  
  return modifiedConfig
}

/**
 * 执行响应拦截器
 * @param {Response} response - 响应对象
 */
async function executeResponseInterceptors(response) {
  let modifiedResponse = response
  
  for (const interceptor of responseInterceptors) {
    try {
      modifiedResponse = await interceptor(modifiedResponse)
    } catch (error) {
      console.error('Response interceptor error:', error)
    }
  }
  
  return modifiedResponse
}

/**
 * 核心请求函数
 * @param {string} url - 请求 URL
 * @param {Object} options - 请求选项
 * @param {number} retryCount - 剩余重试次数
 */
async function request(url, options = {}, retryCount = API_CONFIG.retryCount) {
  // 构建完整 URL
  const fullUrl = url.startsWith('http') ? url : `${API_CONFIG.baseURL}${url}`
  
  // 默认配置
  const defaultOptions = {
    headers: {
      'Content-Type': 'application/json',
    },
    timeout: API_CONFIG.timeout,
  }
  
  // 合并配置
  let config = {
    ...defaultOptions,
    ...options,
    headers: {
      ...defaultOptions.headers,
      ...options.headers,
    },
  }
  
  // 执行请求拦截器
  config = await executeRequestInterceptors(config)
  
  // 创建 AbortController 用于超时控制
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), config.timeout)
  
  try {
    // 发起请求
    const response = await fetch(fullUrl, {
      ...config,
      signal: controller.signal,
    })
    
    clearTimeout(timeoutId)
    
    // 执行响应拦截器
    const interceptedResponse = await executeResponseInterceptors(response)
    
    // 检查响应状态
    if (!interceptedResponse.ok) {
      const errorData = await interceptedResponse.json().catch(() => ({}))
      throw new ApiError(
        errorData.message || `HTTP ${interceptedResponse.status}: ${interceptedResponse.statusText}`,
        interceptedResponse.status,
        errorData
      )
    }
    
    // 解析响应
    const contentType = interceptedResponse.headers.get('content-type')
    if (contentType && contentType.includes('application/json')) {
      return await interceptedResponse.json()
    } else {
      return await interceptedResponse.text()
    }
    
  } catch (error) {
    clearTimeout(timeoutId)
    
    // 处理超时错误
    if (error.name === 'AbortError') {
      throw new ApiError('请求超时', 408, { timeout: config.timeout })
    }
    
    // 处理网络错误
    if (error instanceof TypeError && error.message === 'Failed to fetch') {
      throw new ApiError('网络连接失败，请检查网络设置', 0, {})
    }
    
    // 如果是 ApiError 且状态码为 5xx 或网络错误，尝试重试
    if (retryCount > 0 && (error.status >= 500 || error.status === 0)) {
      console.log(`请求失败，${API_CONFIG.retryDelay}ms 后重试... (剩余 ${retryCount} 次)`)
      await delay(API_CONFIG.retryDelay)
      return request(url, options, retryCount - 1)
    }
    
    throw error
  }
}

/**
 * GET 请求
 * @param {string} url - 请求 URL
 * @param {Object} params - 查询参数
 * @param {Object} options - 其他选项
 */
export async function get(url, params = {}, options = {}) {
  // 构建查询字符串
  const queryString = new URLSearchParams(params).toString()
  const fullUrl = queryString ? `${url}?${queryString}` : url
  
  return request(fullUrl, {
    method: 'GET',
    ...options,
  })
}

/**
 * POST 请求
 * @param {string} url - 请求 URL
 * @param {Object} data - 请求体数据
 * @param {Object} options - 其他选项
 */
export async function post(url, data = {}, options = {}) {
  return request(url, {
    method: 'POST',
    body: JSON.stringify(data),
    ...options,
  })
}

/**
 * PUT 请求
 * @param {string} url - 请求 URL
 * @param {Object} data - 请求体数据
 * @param {Object} options - 其他选项
 */
export async function put(url, data = {}, options = {}) {
  return request(url, {
    method: 'PUT',
    body: JSON.stringify(data),
    ...options,
  })
}

/**
 * DELETE 请求
 * @param {string} url - 请求 URL
 * @param {Object} options - 其他选项
 */
export async function del(url, options = {}) {
  return request(url, {
    method: 'DELETE',
    ...options,
  })
}

/**
 * PATCH 请求
 * @param {string} url - 请求 URL
 * @param {Object} data - 请求体数据
 * @param {Object} options - 其他选项
 */
export async function patch(url, data = {}, options = {}) {
  return request(url, {
    method: 'PATCH',
    body: JSON.stringify(data),
    ...options,
  })
}

/**
 * 上传文件
 * @param {string} url - 请求 URL
 * @param {FormData} formData - 表单数据
 * @param {Function} onProgress - 进度回调
 * @param {Object} options - 其他选项
 */
export async function upload(url, formData, onProgress = null, options = {}) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    const fullUrl = url.startsWith('http') ? url : `${API_CONFIG.baseURL}${url}`
    
    // 监听上传进度
    if (onProgress) {
      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
          const percentComplete = (e.loaded / e.total) * 100
          onProgress(percentComplete, e.loaded, e.total)
        }
      })
    }
    
    // 监听完成
    xhr.addEventListener('load', () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const response = JSON.parse(xhr.responseText)
          resolve(response)
        } catch (error) {
          resolve(xhr.responseText)
        }
      } else {
        reject(new ApiError(`上传失败: ${xhr.statusText}`, xhr.status, {}))
      }
    })
    
    // 监听错误
    xhr.addEventListener('error', () => {
      reject(new ApiError('上传失败：网络错误', 0, {}))
    })
    
    // 监听超时
    xhr.addEventListener('timeout', () => {
      reject(new ApiError('上传超时', 408, {}))
    })
    
    // 设置超时
    xhr.timeout = options.timeout || API_CONFIG.timeout
    
    // 发起请求
    xhr.open('POST', fullUrl)
    
    // 设置自定义请求头
    if (options.headers) {
      Object.entries(options.headers).forEach(([key, value]) => {
        xhr.setRequestHeader(key, value)
      })
    }
    
    xhr.send(formData)
  })
}

/**
 * 批量请求
 * @param {Array} requests - 请求数组
 */
export async function batch(requests) {
  return Promise.all(requests.map(req => request(req.url, req.options)))
}

/**
 * 取消令牌
 */
export class CancelToken {
  constructor() {
    this.controller = new AbortController()
  }
  
  get signal() {
    return this.controller.signal
  }
  
  cancel(reason = 'Request cancelled') {
    this.controller.abort(reason)
  }
}

/**
 * 创建取消令牌
 */
export function createCancelToken() {
  return new CancelToken()
}

// 默认导出
export default {
  get,
  post,
  put,
  del,
  patch,
  upload,
  batch,
  request,
  addRequestInterceptor,
  addResponseInterceptor,
  createCancelToken,
  ApiError,
}

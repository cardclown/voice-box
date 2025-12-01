# API 客户端使用文档

## 概述

`apiClient.js` 提供了一个统一的 HTTP 请求客户端，具有以下特性：

- ✅ 自动重试（失败时最多重试 3 次）
- ✅ 超时控制（默认 30 秒）
- ✅ 请求/响应拦截器
- ✅ 统一错误处理
- ✅ 取消请求支持
- ✅ 文件上传进度
- ✅ 环境变量配置

## 基本使用

### 1. GET 请求

```javascript
import { get } from '@/services/apiClient'

// 简单 GET 请求
const data = await get('/chat/sessions')

// 带查询参数
const data = await get('/chat/sessions', {
  page: 1,
  size: 20,
  model: 'doubao'
})
```

### 2. POST 请求

```javascript
import { post } from '@/services/apiClient'

const response = await post('/chat', {
  text: 'Hello',
  model: 'doubao',
  sessionId: 123
})
```

### 3. PUT/PATCH/DELETE 请求

```javascript
import { put, patch, del } from '@/services/apiClient'

// PUT
await put('/chat/sessions/123', { title: 'New Title' })

// PATCH
await patch('/chat/sessions/123', { title: 'New Title' })

// DELETE
await del('/chat/sessions/123')
```

### 4. 文件上传

```javascript
import { upload } from '@/services/apiClient'

const formData = new FormData()
formData.append('file', file)

const response = await upload(
  '/video/convert',
  formData,
  (percent, loaded, total) => {
    console.log(`上传进度: ${percent.toFixed(2)}%`)
  }
)
```

## 高级功能

### 1. 请求拦截器

```javascript
import { addRequestInterceptor } from '@/services/apiClient'

// 添加认证 Token
addRequestInterceptor((config) => {
  const token = localStorage.getItem('auth_token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// 添加请求日志
addRequestInterceptor((config) => {
  console.log('[API Request]', config.method, config.url)
  return config
})
```

### 2. 响应拦截器

```javascript
import { addResponseInterceptor } from '@/services/apiClient'

// 处理特殊响应
addResponseInterceptor((response) => {
  console.log('[API Response]', response.status)
  return response
})

// 刷新 Token
addResponseInterceptor(async (response) => {
  if (response.status === 401) {
    // Token 过期，尝试刷新
    await refreshToken()
  }
  return response
})
```

### 3. 取消请求

```javascript
import { get, createCancelToken } from '@/services/apiClient'

const cancelToken = createCancelToken()

// 发起请求
const promise = get('/chat/sessions', {}, {
  signal: cancelToken.signal
})

// 取消请求
cancelToken.cancel('用户取消了请求')

// 处理取消
try {
  await promise
} catch (error) {
  if (error.name === 'AbortError') {
    console.log('请求已取消')
  }
}
```

### 4. 批量请求

```javascript
import { batch } from '@/services/apiClient'

const results = await batch([
  { url: '/chat/sessions', options: { method: 'GET' } },
  { url: '/user/profile', options: { method: 'GET' } },
  { url: '/settings', options: { method: 'GET' } }
])

const [sessions, profile, settings] = results
```

## 错误处理

### ApiError 对象

所有 API 错误都会抛出 `ApiError` 对象：

```javascript
import { get, ApiError } from '@/services/apiClient'

try {
  const data = await get('/chat/sessions')
} catch (error) {
  if (error instanceof ApiError) {
    console.log('错误消息:', error.message)
    console.log('HTTP 状态码:', error.status)
    console.log('错误数据:', error.data)
    
    // 根据状态码处理
    switch (error.status) {
      case 401:
        // 未授权，跳转登录
        router.push('/login')
        break
      case 404:
        // 资源不存在
        showToast('资源不存在', 'error')
        break
      case 500:
        // 服务器错误
        showToast('服务器错误，请稍后重试', 'error')
        break
      default:
        showToast(error.message, 'error')
    }
  }
}
```

### 常见错误状态码

| 状态码 | 说明 | 处理建议 |
|--------|------|----------|
| 0 | 网络错误 | 检查网络连接 |
| 400 | 请求参数错误 | 检查请求参数 |
| 401 | 未授权 | 跳转登录页 |
| 403 | 禁止访问 | 提示权限不足 |
| 404 | 资源不存在 | 提示资源不存在 |
| 408 | 请求超时 | 提示超时并重试 |
| 500 | 服务器错误 | 提示服务器错误 |
| 503 | 服务不可用 | 提示服务维护中 |

## 环境配置

### 开发环境 (.env.development)

```env
VITE_API_BASE=http://localhost:10088/api
VITE_DEBUG=true
```

### 生产环境 (.env.production)

```env
VITE_API_BASE=https://api.voicebox.com/api
VITE_DEBUG=false
```

### 访问环境变量

```javascript
const apiBase = import.meta.env.VITE_API_BASE
const isDebug = import.meta.env.VITE_DEBUG === 'true'
```

## 最佳实践

### 1. 创建专用 API 模块

不要在组件中直接使用 `apiClient`，而是创建专用的 API 模块：

```javascript
// services/chatApi.js
import { get, post } from './apiClient'

export async function fetchSessions() {
  return get('/chat/sessions')
}

export async function sendMessage(data) {
  return post('/chat', data)
}
```

```vue
<!-- 在组件中使用 -->
<script setup>
import { fetchSessions, sendMessage } from '@/services/chatApi'

const sessions = await fetchSessions()
</script>
```

### 2. 统一错误处理

创建一个错误处理工具：

```javascript
// utils/errorHandler.js
import { ApiError } from '@/services/apiClient'
import { showToast } from '@/composables/useToast'

export function handleApiError(error) {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 401:
        showToast('请先登录', 'error')
        // 跳转登录
        break
      case 403:
        showToast('权限不足', 'error')
        break
      case 404:
        showToast('资源不存在', 'error')
        break
      case 500:
        showToast('服务器错误', 'error')
        break
      default:
        showToast(error.message, 'error')
    }
  } else {
    showToast('未知错误', 'error')
    console.error(error)
  }
}
```

```vue
<!-- 使用 -->
<script setup>
import { fetchSessions } from '@/services/chatApi'
import { handleApiError } from '@/utils/errorHandler'

async function loadSessions() {
  try {
    const data = await fetchSessions()
    sessions.value = data
  } catch (error) {
    handleApiError(error)
  }
}
</script>
```

### 3. 使用 Composable 封装

```javascript
// composables/useApi.js
import { ref } from 'vue'
import { handleApiError } from '@/utils/errorHandler'

export function useApi(apiFn) {
  const data = ref(null)
  const loading = ref(false)
  const error = ref(null)
  
  async function execute(...args) {
    loading.value = true
    error.value = null
    
    try {
      data.value = await apiFn(...args)
      return data.value
    } catch (err) {
      error.value = err
      handleApiError(err)
      throw err
    } finally {
      loading.value = false
    }
  }
  
  return {
    data,
    loading,
    error,
    execute
  }
}
```

```vue
<!-- 使用 -->
<script setup>
import { fetchSessions } from '@/services/chatApi'
import { useApi } from '@/composables/useApi'

const { data: sessions, loading, execute: loadSessions } = useApi(fetchSessions)

onMounted(() => {
  loadSessions()
})
</script>

<template>
  <div v-if="loading">加载中...</div>
  <div v-else>
    <div v-for="session in sessions" :key="session.id">
      {{ session.title }}
    </div>
  </div>
</template>
```

## 调试

### 启用请求日志

```javascript
import { addRequestInterceptor, addResponseInterceptor } from '@/services/apiClient'

if (import.meta.env.VITE_DEBUG === 'true') {
  // 请求日志
  addRequestInterceptor((config) => {
    console.log('[API Request]', {
      url: config.url,
      method: config.method,
      headers: config.headers,
      body: config.body
    })
    return config
  })
  
  // 响应日志
  addResponseInterceptor((response) => {
    console.log('[API Response]', {
      url: response.url,
      status: response.status,
      statusText: response.statusText
    })
    return response
  })
}
```

## 性能优化

### 1. 请求去重

```javascript
const pendingRequests = new Map()

addRequestInterceptor((config) => {
  const key = `${config.method}:${config.url}`
  
  if (pendingRequests.has(key)) {
    // 如果有相同的请求正在进行，返回该请求的 Promise
    return pendingRequests.get(key)
  }
  
  const promise = fetch(config.url, config)
  pendingRequests.set(key, promise)
  
  promise.finally(() => {
    pendingRequests.delete(key)
  })
  
  return config
})
```

### 2. 响应缓存

```javascript
const cache = new Map()

export async function getCached(url, params = {}, ttl = 60000) {
  const key = `${url}:${JSON.stringify(params)}`
  const cached = cache.get(key)
  
  if (cached && Date.now() - cached.timestamp < ttl) {
    return cached.data
  }
  
  const data = await get(url, params)
  cache.set(key, { data, timestamp: Date.now() })
  
  return data
}
```

## 总结

API 客户端提供了一个健壮、易用的 HTTP 请求解决方案。通过合理使用拦截器、错误处理和环境配置，可以大大提升开发效率和代码质量。

/**
 * 流式响应服务
 * 处理 Server-Sent Events (SSE) 流式数据
 */

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:10088/api'

/**
 * 创建流式聊天请求
 * @param {Object} params - 请求参数
 * @param {string} params.text - 用户消息
 * @param {string} params.model - 模型名称
 * @param {number} params.sessionId - 会话ID
 * @param {string} params.deviceInfo - 设备信息
 * @param {Function} onToken - 接收到新 token 时的回调
 * @param {Function} onComplete - 流式响应完成时的回调
 * @param {Function} onError - 发生错误时的回调
 * @returns {Object} 包含 abort 方法的控制对象
 */
export function createStreamingChat({ text, model, sessionId, deviceInfo }, onToken, onComplete, onError) {
  const controller = new AbortController()
  const signal = controller.signal

  // 构建请求体
  const requestBody = {
    text,
    model,
    sessionId,
    deviceInfo
  }

  // 发起流式请求
  fetch(`${API_BASE}/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream'
    },
    body: JSON.stringify(requestBody),
    signal
  })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      try {
        let currentEvent = null
        
        while (true) {
          const { done, value } = await reader.read()

          if (done) {
            break
          }

          // 解码数据块
          buffer += decoder.decode(value, { stream: true })

          // 处理完整的 SSE 消息
          const lines = buffer.split('\n')
          buffer = lines.pop() || '' // 保留不完整的行

          for (const line of lines) {
            if (!line) continue
            
            // 解析事件名称（注意：Spring Boot SSE 格式是 "event:name" 没有空格）
            if (line.startsWith('event:')) {
              currentEvent = line.slice(6).trim()
              continue
            }
            
            // 解析数据（注意：Spring Boot SSE 格式是 "data:content" 没有空格）
            if (line.startsWith('data:')) {
              const data = line.slice(5).trim()

              // 检查是否是结束标记
              if (data === '[DONE]') {
                if (onComplete) {
                  onComplete()
                }
                return
              }

              try {
                const parsed = JSON.parse(data)

                // 根据事件类型处理
                if (currentEvent === 'session') {
                  // 会话信息
                  if (onToken && parsed.sessionId) {
                    onToken('', parsed)
                  }
                } else if (currentEvent === 'delta') {
                  // 新的 token
                  if (onToken && parsed.text) {
                    onToken(parsed.text, parsed)
                  }
                } else if (currentEvent === 'error') {
                  // 错误信息
                  throw new Error(parsed.message || parsed.error || 'Unknown error')
                } else {
                  // 无事件名称的情况，尝试自动识别
                  if (parsed.text) {
                    if (onToken) {
                      onToken(parsed.text, parsed)
                    }
                  } else if (parsed.sessionId) {
                    if (onToken) {
                      onToken('', parsed)
                    }
                  } else if (parsed.error) {
                    throw new Error(parsed.error)
                  }
                }
                
                // 重置事件名称
                currentEvent = null
              } catch (parseError) {
                console.warn('Failed to parse SSE data:', data, parseError)
              }
            }
          }
        }

        // 流结束
        if (onComplete) {
          onComplete()
        }
      } catch (readError) {
        if (readError.name === 'AbortError') {
          console.log('Stream aborted by user')
        } else {
          console.error('Stream reading error:', readError)
          if (onError) {
            onError(readError)
          }
        }
      }
    })
    .catch((fetchError) => {
      if (fetchError.name === 'AbortError') {
        console.log('Fetch aborted by user')
      } else {
        console.error('Fetch error:', fetchError)
        if (onError) {
          onError(fetchError)
        }
      }
    })

  // 返回控制对象
  return {
    abort: () => {
      controller.abort()
    }
  }
}

/**
 * 使用 EventSource 的备用实现（如果后端支持）
 * @param {Object} params - 请求参数
 * @param {Function} onToken - 接收到新 token 时的回调
 * @param {Function} onComplete - 流式响应完成时的回调
 * @param {Function} onError - 发生错误时的回调
 * @returns {Object} 包含 close 方法的控制对象
 */
export function createEventSourceChat({ text, model, sessionId, deviceInfo }, onToken, onComplete, onError) {
  // 构建 URL 参数
  const params = new URLSearchParams({
    text,
    model,
    deviceInfo
  })

  if (sessionId) {
    params.append('sessionId', sessionId)
  }

  const url = `${API_BASE}/chat/stream?${params.toString()}`
  const eventSource = new EventSource(url)

  eventSource.onmessage = (event) => {
    const data = event.data

    if (data === '[DONE]') {
      eventSource.close()
      if (onComplete) {
        onComplete()
      }
      return
    }

    try {
      const parsed = JSON.parse(data)

      if (parsed.text) {
        // 后端发送的格式：{text: "..."}
        if (onToken) {
          onToken(parsed.text, parsed)
        }
      } else if (parsed.token) {
        // 备用格式：{token: "..."}
        if (onToken) {
          onToken(parsed.token, parsed)
        }
      } else if (parsed.error) {
        throw new Error(parsed.error)
      }
    } catch (parseError) {
      console.warn('Failed to parse event data:', data, parseError)
    }
  }

  eventSource.onerror = (error) => {
    console.error('EventSource error:', error)
    eventSource.close()
    if (onError) {
      onError(error)
    }
  }

  return {
    close: () => {
      eventSource.close()
    }
  }
}

/**
 * 检查浏览器是否支持流式 API
 * @returns {boolean}
 */
export function isStreamingSupported() {
  return (
    typeof ReadableStream !== 'undefined' &&
    typeof TextDecoder !== 'undefined' &&
    typeof AbortController !== 'undefined'
  )
}

/**
 * 检查浏览器是否支持 EventSource
 * @returns {boolean}
 */
export function isEventSourceSupported() {
  return typeof EventSource !== 'undefined'
}

export default {
  createStreamingChat,
  createEventSourceChat,
  isStreamingSupported,
  isEventSourceSupported
}

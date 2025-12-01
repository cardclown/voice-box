import apiClient from './apiClient'

/**
 * 情感语音API服务
 */
class EmotionalVoiceService {
  /**
   * 分析语音情感
   * @param {FormData} formData - 包含音频文件的表单数据
   * @param {string} userId - 用户ID（可选）
   * @returns {Promise<Object>} 分析结果
   */
  async analyzeVoice(formData, userId = null) {
    try {
      const url = userId 
        ? `/api/emotional-voice/analyze?userId=${encodeURIComponent(userId)}`
        : '/api/emotional-voice/analyze'

      const response = await apiClient.post(url, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        timeout: 30000
      })

      return response.data
    } catch (error) {
      console.error('语音分析失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 分析文本情感
   * @param {string} text - 要分析的文本
   * @param {string} userId - 用户ID（可选）
   * @returns {Promise<Object>} 分析结果
   */
  async analyzeText(text, userId = null) {
    try {
      const data = { text }
      if (userId) {
        data.userId = userId
      }

      const response = await apiClient.post('/api/emotional-voice/analyze-text', data)
      return response.data
    } catch (error) {
      console.error('文本分析失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 情感化语音合成
   * @param {string} userId - 用户ID
   * @param {Object} params - 合成参数
   * @returns {Promise<Object>} 合成结果
   */
  async synthesizeWithEmotion(userId, params) {
    try {
      const response = await apiClient.post(
        `/api/emotional-voice/synthesize/${encodeURIComponent(userId)}`,
        params,
        { timeout: 60000 }
      )
      return response.data
    } catch (error) {
      console.error('情感语音合成失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 批量语音合成
   * @param {string} userId - 用户ID
   * @param {Object} params - 批量合成参数
   * @returns {Promise<Object>} 批量合成结果
   */
  async batchSynthesize(userId, params) {
    try {
      const response = await apiClient.post(
        `/api/emotional-voice/synthesize/batch/${encodeURIComponent(userId)}`,
        params,
        { timeout: 120000 }
      )
      return response.data
    } catch (error) {
      console.error('批量语音合成失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 获取用户情感画像
   * @param {string} userId - 用户ID
   * @returns {Promise<Object>} 用户画像数据
   */
  async getProfile(userId) {
    try {
      const response = await apiClient.get(
        `/emotional-voice/profile/${encodeURIComponent(userId)}`
      )
      return response
    } catch (error) {
      console.error('获取用户画像失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 更新用户画像
   * @param {string} userId - 用户ID
   * @param {Object} data - 画像数据
   * @returns {Promise<Object>} 更新结果
   */
  async updateProfile(userId, data) {
    try {
      const response = await apiClient.post(
        `/api/emotional-voice/profile/${encodeURIComponent(userId)}`,
        data
      )
      return response.data
    } catch (error) {
      console.error('更新用户画像失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 清除用户画像
   * @param {string} userId - 用户ID
   * @returns {Promise<Object>} 删除结果
   */
  async clearProfile(userId) {
    try {
      const response = await apiClient.delete(
        `/api/emotional-voice/profile/${encodeURIComponent(userId)}`
      )
      return response.data
    } catch (error) {
      console.error('清除用户画像失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 获取情感统计数据
   * @param {string} userId - 用户ID
   * @param {Object} options - 查询选项
   * @returns {Promise<Object>} 统计数据
   */
  async getStatistics(userId, options = {}) {
    try {
      const params = new URLSearchParams()
      
      if (options.startDate) {
        params.append('startDate', options.startDate)
      }
      if (options.endDate) {
        params.append('endDate', options.endDate)
      }
      if (options.groupBy) {
        params.append('groupBy', options.groupBy)
      }

      const url = `/api/emotional-voice/statistics/${encodeURIComponent(userId)}?${params.toString()}`
      const response = await apiClient.get(url)
      return response.data
    } catch (error) {
      console.error('获取统计数据失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 获取情绪历史记录
   * @param {string} userId - 用户ID
   * @param {Object} options - 查询选项
   * @returns {Promise<Object>} 历史记录
   */
  async getHistory(userId, options = {}) {
    try {
      const params = new URLSearchParams()
      
      if (options.page) {
        params.append('page', options.page)
      }
      if (options.pageSize) {
        params.append('pageSize', options.pageSize)
      }
      if (options.emotion) {
        params.append('emotion', options.emotion)
      }
      if (options.startDate) {
        params.append('startDate', options.startDate)
      }
      if (options.endDate) {
        params.append('endDate', options.endDate)
      }

      const url = `/api/emotional-voice/history/${encodeURIComponent(userId)}?${params.toString()}`
      const response = await apiClient.get(url)
      return response.data
    } catch (error) {
      console.error('获取历史记录失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 删除历史记录
   * @param {string} userId - 用户ID
   * @param {string} recordId - 记录ID
   * @returns {Promise<Object>} 删除结果
   */
  async deleteHistory(userId, recordId) {
    try {
      const response = await apiClient.delete(
        `/api/emotional-voice/history/${encodeURIComponent(userId)}/${encodeURIComponent(recordId)}`
      )
      return response.data
    } catch (error) {
      console.error('删除历史记录失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 获取推荐的合成参数
   * @param {string} userId - 用户ID
   * @param {string} emotion - 目标情绪（可选）
   * @returns {Promise<Object>} 推荐参数
   */
  async getRecommendedParams(userId, emotion = null) {
    try {
      const url = emotion 
        ? `/api/emotional-voice/synthesize/params/${encodeURIComponent(userId)}?emotion=${encodeURIComponent(emotion)}`
        : `/api/emotional-voice/synthesize/params/${encodeURIComponent(userId)}`

      const response = await apiClient.get(url)
      return response.data
    } catch (error) {
      console.error('获取推荐参数失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 获取可用的音色列表
   * @param {string} gender - 性别筛选（可选）
   * @returns {Promise<Object>} 音色列表
   */
  async getVoiceList(gender = null) {
    try {
      const url = gender 
        ? `/api/emotional-voice/voices?gender=${encodeURIComponent(gender)}`
        : '/api/emotional-voice/voices'

      const response = await apiClient.get(url)
      return response.data
    } catch (error) {
      console.error('获取音色列表失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 检查服务健康状态
   * @returns {Promise<Object>} 健康状态
   */
  async checkHealth() {
    try {
      const response = await apiClient.get('/api/emotional-voice/health')
      return response.data
    } catch (error) {
      console.error('健康检查失败:', error)
      throw this._handleError(error)
    }
  }

  /**
   * 处理错误
   * @private
   */
  _handleError(error) {
    if (error.response) {
      // 服务器返回错误
      const message = error.response.data?.message || '服务器错误'
      return new Error(message)
    } else if (error.request) {
      // 请求发送但没有响应
      return new Error('网络连接失败，请检查网络')
    } else {
      // 其他错误
      return new Error(error.message || '未知错误')
    }
  }
}

// 导出单例
export default new EmotionalVoiceService()

import { defineStore } from 'pinia'
import emotionalVoiceService from '../services/emotionalVoiceService'

/**
 * 情感语音状态管理
 */
export const useEmotionalVoiceStore = defineStore('emotionalVoice', {
  state: () => ({
    // 当前用户ID
    currentUserId: null,
    
    // 当前情绪状态
    currentEmotion: null,
    
    // 用户画像
    userProfile: null,
    
    // 历史记录
    history: [],
    historyTotal: 0,
    historyPage: 1,
    historyPageSize: 10,
    
    // 统计数据
    statistics: null,
    
    // 加载状态
    loading: {
      profile: false,
      history: false,
      statistics: false,
      analyzing: false,
      synthesizing: false
    },
    
    // 错误信息
    error: null,
    
    // 缓存时间戳
    cacheTimestamps: {
      profile: null,
      statistics: null
    },
    
    // 缓存有效期（毫秒）
    cacheDuration: 5 * 60 * 1000 // 5分钟
  }),

  getters: {
    /**
     * 是否有用户画像
     */
    hasProfile: (state) => {
      return state.userProfile !== null
    },

    /**
     * 主导情绪
     */
    dominantEmotion: (state) => {
      if (!state.userProfile || !state.userProfile.emotionDistribution) {
        return null
      }
      const distribution = state.userProfile.emotionDistribution
      if (distribution.length === 0) return null
      return distribution[0].emotion
    },

    /**
     * 情绪分布
     */
    emotionDistribution: (state) => {
      return state.userProfile?.emotionDistribution || []
    },

    /**
     * 性格特征
     */
    personalityTraits: (state) => {
      return state.userProfile?.personality || null
    },

    /**
     * 情感标签
     */
    emotionalTags: (state) => {
      return state.userProfile?.tags || []
    },

    /**
     * 是否正在加载
     */
    isLoading: (state) => {
      return Object.values(state.loading).some(v => v)
    },

    /**
     * 画像缓存是否有效
     */
    isProfileCacheValid: (state) => {
      if (!state.cacheTimestamps.profile) return false
      const now = Date.now()
      return (now - state.cacheTimestamps.profile) < state.cacheDuration
    },

    /**
     * 统计缓存是否有效
     */
    isStatisticsCacheValid: (state) => {
      if (!state.cacheTimestamps.statistics) return false
      const now = Date.now()
      return (now - state.cacheTimestamps.statistics) < state.cacheDuration
    }
  },

  actions: {
    /**
     * 设置当前用户
     */
    setCurrentUser(userId) {
      if (this.currentUserId !== userId) {
        this.currentUserId = userId
        this.clearCache()
      }
    },

    /**
     * 设置当前情绪
     */
    setCurrentEmotion(emotion) {
      this.currentEmotion = emotion
    },

    /**
     * 分析语音
     */
    async analyzeVoice(formData) {
      this.loading.analyzing = true
      this.error = null

      try {
        const result = await emotionalVoiceService.analyzeVoice(
          formData,
          this.currentUserId
        )

        // 更新当前情绪
        if (result.emotion) {
          this.currentEmotion = result.emotion
        }

        // 添加到历史记录
        if (result.record) {
          this.history.unshift(result.record)
          this.historyTotal++
        }

        // 使画像缓存失效
        this.cacheTimestamps.profile = null

        return result
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading.analyzing = false
      }
    },

    /**
     * 分析文本
     */
    async analyzeText(text) {
      this.loading.analyzing = true
      this.error = null

      try {
        const result = await emotionalVoiceService.analyzeText(
          text,
          this.currentUserId
        )

        // 更新当前情绪
        if (result.emotion) {
          this.currentEmotion = result.emotion
        }

        return result
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading.analyzing = false
      }
    },

    /**
     * 情感化语音合成
     */
    async synthesizeVoice(params) {
      this.loading.synthesizing = true
      this.error = null

      try {
        const result = await emotionalVoiceService.synthesizeWithEmotion(
          this.currentUserId,
          params
        )
        return result
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading.synthesizing = false
      }
    },

    /**
     * 获取用户画像
     */
    async fetchProfile(forceRefresh = false) {
      if (!this.currentUserId) {
        throw new Error('未设置用户ID')
      }

      // 检查缓存
      if (!forceRefresh && this.isProfileCacheValid && this.userProfile) {
        return this.userProfile
      }

      this.loading.profile = true
      this.error = null

      try {
        const result = await emotionalVoiceService.getProfile(this.currentUserId)
        
        // 如果画像不存在，创建默认画像
        if (!result.profile || result.status === 'not_found') {
          console.warn('用户画像不存在，使用默认画像')
          this.userProfile = {
            userId: this.currentUserId,
            personality: null,
            emotionDistribution: [],
            tags: [],
            profileCompleteness: 0
          }
        } else {
          this.userProfile = result.profile
        }
        
        this.cacheTimestamps.profile = Date.now()
        return this.userProfile
      } catch (error) {
        console.error('获取用户画像失败:', error)
        this.error = error.message
        
        // 即使失败也创建默认画像，避免页面崩溃
        this.userProfile = {
          userId: this.currentUserId,
          personality: null,
          emotionDistribution: [],
          tags: [],
          profileCompleteness: 0
        }
        
        return this.userProfile
      } finally {
        this.loading.profile = false
      }
    },

    /**
     * 更新用户画像
     */
    async updateProfile(data) {
      if (!this.currentUserId) {
        throw new Error('未设置用户ID')
      }

      this.loading.profile = true
      this.error = null

      try {
        const result = await emotionalVoiceService.updateProfile(
          this.currentUserId,
          data
        )
        
        // 刷新画像
        await this.fetchProfile(true)
        
        return result
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading.profile = false
      }
    },

    /**
     * 清除用户画像
     */
    async clearProfile() {
      if (!this.currentUserId) {
        throw new Error('未设置用户ID')
      }

      this.loading.profile = true
      this.error = null

      try {
        const result = await emotionalVoiceService.clearProfile(this.currentUserId)
        this.userProfile = null
        this.cacheTimestamps.profile = null
        return result
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading.profile = false
      }
    },

    /**
     * 获取历史记录
     */
    async fetchHistory(options = {}) {
      if (!this.currentUserId) {
        throw new Error('未设置用户ID')
      }

      this.loading.history = true
      this.error = null

      try {
        const result = await emotionalVoiceService.getHistory(
          this.currentUserId,
          {
            page: options.page || this.historyPage,
            pageSize: options.pageSize || this.historyPageSize,
            ...options
          }
        )

        if (options.append) {
          this.history.push(...result.records)
        } else {
          this.history = result.records
        }
        
        this.historyTotal = result.total
        this.historyPage = result.page
        
        return result
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading.history = false
      }
    },

    /**
     * 删除历史记录
     */
    async deleteHistoryItem(recordId) {
      if (!this.currentUserId) {
        throw new Error('未设置用户ID')
      }

      this.error = null

      try {
        await emotionalVoiceService.deleteHistory(this.currentUserId, recordId)
        
        // 从本地列表中移除
        const index = this.history.findIndex(item => item.id === recordId)
        if (index !== -1) {
          this.history.splice(index, 1)
          this.historyTotal--
        }
      } catch (error) {
        this.error = error.message
        throw error
      }
    },

    /**
     * 清空历史记录
     */
    clearHistory() {
      this.history = []
      this.historyTotal = 0
      this.historyPage = 1
    },

    /**
     * 获取统计数据
     */
    async fetchStatistics(options = {}, forceRefresh = false) {
      if (!this.currentUserId) {
        throw new Error('未设置用户ID')
      }

      // 检查缓存
      if (!forceRefresh && this.isStatisticsCacheValid && this.statistics) {
        return this.statistics
      }

      this.loading.statistics = true
      this.error = null

      try {
        const result = await emotionalVoiceService.getStatistics(
          this.currentUserId,
          options
        )
        this.statistics = result.statistics
        this.cacheTimestamps.statistics = Date.now()
        return this.statistics
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading.statistics = false
      }
    },

    /**
     * 清除缓存
     */
    clearCache() {
      this.userProfile = null
      this.statistics = null
      this.history = []
      this.historyTotal = 0
      this.historyPage = 1
      this.cacheTimestamps.profile = null
      this.cacheTimestamps.statistics = null
    },

    /**
     * 清除错误
     */
    clearError() {
      this.error = null
    },

    /**
     * 重置状态
     */
    reset() {
      this.currentUserId = null
      this.currentEmotion = null
      this.clearCache()
      this.clearError()
    }
  }
})

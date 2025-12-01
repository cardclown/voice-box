import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useEmotionalVoiceStore } from '../stores/emotionalVoiceStore'
import { storeToRefs } from 'pinia'

/**
 * 情感语音Composable
 * 提供情感语音分析和合成的功能
 */
export function useEmotionalVoice(userId = null) {
  const store = useEmotionalVoiceStore()
  
  // 从store获取响应式状态
  const {
    currentEmotion,
    userProfile,
    history,
    statistics,
    loading,
    error
  } = storeToRefs(store)

  // 本地状态
  const isRecording = ref(false)
  const recordingTime = ref(0)
  const audioLevel = ref(0)

  // 录音相关
  let mediaRecorder = null
  let audioContext = null
  let analyser = null
  let recordingTimer = null
  let audioChunks = []

  /**
   * 初始化
   */
  const initialize = async (uid = null) => {
    if (uid || userId) {
      store.setCurrentUser(uid || userId)
      try {
        await store.fetchProfile()
      } catch (err) {
        console.error('初始化失败:', err)
      }
    }
  }

  /**
   * 开始录音
   */
  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      })

      // 创建音频上下文
      audioContext = new (window.AudioContext || window.webkitAudioContext)()
      analyser = audioContext.createAnalyser()
      const microphone = audioContext.createMediaStreamSource(stream)
      
      analyser.fftSize = 256
      microphone.connect(analyser)

      // 创建录音器
      mediaRecorder = new MediaRecorder(stream)
      audioChunks = []

      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunks.push(event.data)
        }
      }

      mediaRecorder.onstop = async () => {
        const audioBlob = new Blob(audioChunks, { type: 'audio/wav' })
        await analyzeAudio(audioBlob)
      }

      // 开始录音
      mediaRecorder.start()
      isRecording.value = true
      recordingTime.value = 0

      // 开始计时
      recordingTimer = setInterval(() => {
        recordingTime.value++
      }, 1000)

      // 开始音频可视化
      startAudioVisualization()

    } catch (err) {
      console.error('录音启动失败:', err)
      throw new Error('无法访问麦克风，请检查权限设置')
    }
  }

  /**
   * 停止录音
   */
  const stopRecording = () => {
    if (mediaRecorder && isRecording.value) {
      mediaRecorder.stop()
      isRecording.value = false

      if (recordingTimer) {
        clearInterval(recordingTimer)
        recordingTimer = null
      }

      // 停止所有音轨
      if (mediaRecorder.stream) {
        mediaRecorder.stream.getTracks().forEach(track => track.stop())
      }
    }
  }

  /**
   * 音频可视化
   */
  const startAudioVisualization = () => {
    if (!analyser) return

    const bufferLength = analyser.frequencyBinCount
    const dataArray = new Uint8Array(bufferLength)

    const updateLevel = () => {
      if (!isRecording.value) return

      analyser.getByteFrequencyData(dataArray)
      const average = dataArray.reduce((sum, value) => sum + value, 0) / bufferLength
      audioLevel.value = average / 255

      requestAnimationFrame(updateLevel)
    }

    updateLevel()
  }

  /**
   * 分析音频
   */
  const analyzeAudio = async (audioBlob, text = null) => {
    const formData = new FormData()
    formData.append('audio', audioBlob, 'recording.wav')
    
    if (text) {
      formData.append('text', text)
    }

    try {
      const result = await store.analyzeVoice(formData)
      return result
    } catch (err) {
      console.error('音频分析失败:', err)
      throw err
    }
  }

  /**
   * 分析文本
   */
  const analyzeText = async (text) => {
    try {
      const result = await store.analyzeText(text)
      return result
    } catch (err) {
      console.error('文本分析失败:', err)
      throw err
    }
  }

  /**
   * 情感化语音合成
   */
  const synthesizeVoice = async (text, options = {}) => {
    try {
      const params = {
        text,
        emotion: options.emotion || currentEmotion.value?.type,
        intensity: options.intensity || currentEmotion.value?.intensity,
        voiceId: options.voiceId,
        speed: options.speed,
        pitch: options.pitch,
        ...options
      }

      const result = await store.synthesizeVoice(params)
      return result
    } catch (err) {
      console.error('语音合成失败:', err)
      throw err
    }
  }

  /**
   * 刷新用户画像
   */
  const refreshProfile = async () => {
    try {
      await store.fetchProfile(true)
    } catch (err) {
      console.error('刷新画像失败:', err)
      throw err
    }
  }

  /**
   * 刷新历史记录
   */
  const refreshHistory = async (options = {}) => {
    try {
      await store.fetchHistory(options)
    } catch (err) {
      console.error('刷新历史失败:', err)
      throw err
    }
  }

  /**
   * 刷新统计数据
   */
  const refreshStatistics = async (options = {}) => {
    try {
      await store.fetchStatistics(options, true)
    } catch (err) {
      console.error('刷新统计失败:', err)
      throw err
    }
  }

  /**
   * 删除历史记录项
   */
  const deleteHistoryItem = async (recordId) => {
    try {
      await store.deleteHistoryItem(recordId)
    } catch (err) {
      console.error('删除历史记录失败:', err)
      throw err
    }
  }

  /**
   * 清空历史记录
   */
  const clearHistory = () => {
    store.clearHistory()
  }

  /**
   * 清除错误
   */
  const clearError = () => {
    store.clearError()
  }

  /**
   * 清理资源
   */
  const cleanup = () => {
    if (isRecording.value) {
      stopRecording()
    }

    if (recordingTimer) {
      clearInterval(recordingTimer)
    }

    if (audioContext) {
      audioContext.close()
    }
  }

  // 计算属性
  const hasProfile = computed(() => store.hasProfile)
  const dominantEmotion = computed(() => store.dominantEmotion)
  const emotionDistribution = computed(() => store.emotionDistribution)
  const personalityTraits = computed(() => store.personalityTraits)
  const emotionalTags = computed(() => store.emotionalTags)
  const isLoading = computed(() => store.isLoading)

  // 生命周期
  onMounted(() => {
    if (userId) {
      initialize(userId)
    }
  })

  onUnmounted(() => {
    cleanup()
  })

  // 监听用户ID变化
  watch(() => userId, (newUserId) => {
    if (newUserId) {
      initialize(newUserId)
    }
  })

  return {
    // 状态
    currentEmotion,
    userProfile,
    history,
    statistics,
    loading,
    error,
    isRecording,
    recordingTime,
    audioLevel,

    // 计算属性
    hasProfile,
    dominantEmotion,
    emotionDistribution,
    personalityTraits,
    emotionalTags,
    isLoading,

    // 方法
    initialize,
    startRecording,
    stopRecording,
    analyzeAudio,
    analyzeText,
    synthesizeVoice,
    refreshProfile,
    refreshHistory,
    refreshStatistics,
    deleteHistoryItem,
    clearHistory,
    clearError,
    cleanup
  }
}

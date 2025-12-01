<template>
  <div class="voice-interaction-page">
    <div class="page-header">
      <h1>语音交互</h1>
      <p class="subtitle">通过语音与AI进行自然对话</p>
    </div>

    <div class="voice-content">
      <!-- 语音设置区域 -->
      <div class="settings-section">
        <VoiceSettings />
      </div>

      <!-- 语音输入区域 -->
      <div class="input-section">
        <div class="section-title">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
            <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
            <line x1="12" y1="19" x2="12" y2="23"/>
            <line x1="8" y1="23" x2="16" y2="23"/>
          </svg>
          <h2>语音输入</h2>
        </div>
        <VoiceInput 
          :user-id="1"
          :session-id="1"
          @message-sent="handleMessageSent"
          @error="handleError"
        />
        
        <!-- 识别结果显示 -->
        <div v-if="recognizedText" class="recognized-text">
          <div class="text-label">识别结果：</div>
          <div class="text-content">{{ recognizedText }}</div>
        </div>
      </div>

      <!-- 语音播放区域 -->
      <div class="player-section">
        <div class="section-title">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="5 3 19 12 5 21 5 3"/>
          </svg>
          <h2>语音播放</h2>
        </div>
        
        <!-- 流式语音播放器 -->
        <div v-if="currentAudioUrl" class="player-container">
          <StreamingVoicePlayer 
            :audio-url="currentAudioUrl"
            :auto-play="true"
            @play="handlePlay"
            @pause="handlePause"
            @ended="handleEnded"
          />
        </div>
        
        <!-- 历史语音消息 -->
        <div v-if="voiceHistory.length > 0" class="history-section">
          <h3>历史语音</h3>
          <div class="history-list">
            <div 
              v-for="item in voiceHistory" 
              :key="item.id"
              class="history-item"
            >
              <div class="history-info">
                <span class="history-time">{{ formatTime(item.timestamp) }}</span>
                <span class="history-duration">{{ formatDuration(item.duration) }}</span>
              </div>
              <VoicePlayer 
                :audio-url="item.audioUrl"
                :duration="item.duration"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 语音监控区域 -->
      <div class="monitoring-section">
        <div class="section-title">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
          </svg>
          <h2>实时监控</h2>
        </div>
        <div class="metrics-grid">
          <div class="metric-card">
            <div class="metric-label">识别准确率</div>
            <div class="metric-value">{{ metrics.accuracy }}%</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">平均响应时间</div>
            <div class="metric-value">{{ metrics.responseTime }}ms</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">今日使用次数</div>
            <div class="metric-value">{{ metrics.usageCount }}</div>
          </div>
          <div class="metric-card">
            <div class="metric-label">服务状态</div>
            <div class="metric-value" :class="metrics.serviceStatus">
              {{ metrics.serviceStatus === 'healthy' ? '正常' : '异常' }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import VoiceInput from '../components/voice/VoiceInput.vue'
import VoicePlayer from '../components/voice/VoicePlayer.vue'
import StreamingVoicePlayer from '../components/voice/StreamingVoicePlayer.vue'
import VoiceSettings from '../components/voice/VoiceSettings.vue'
import { useVoiceMonitoring } from '../composables/useVoiceMonitoring'

const recognizedText = ref('')
const currentAudioUrl = ref('')
const voiceHistory = ref([])

// 使用语音监控
const { overallMetrics, healthStatus, refreshAllData } = useVoiceMonitoring()

// 创建 metrics 计算属性，提供默认值
const metrics = computed(() => ({
  accuracy: overallMetrics.value?.successRate || 0,
  responseTime: overallMetrics.value?.avgResponseTime || 0,
  usageCount: overallMetrics.value?.totalRequests || 0,
  errorRate: overallMetrics.value?.failureRate || 0,
  serviceStatus: healthStatus.value?.status === 'HEALTHY' ? 'healthy' : 'unhealthy'
}))

// 处理消息发送
const handleMessageSent = (data) => {
  console.log('消息发送:', data)
  recognizedText.value = data.text
  
  // 可以在这里调用TTS服务生成语音回复
  // 或者发送到聊天API
}

// 处理错误
const handleError = (error) => {
  console.error('语音输入错误:', error)
  alert('语音输入出错: ' + error.message)
}

// 处理播放事件
const handlePlay = () => {
  console.log('开始播放')
}

const handlePause = () => {
  console.log('暂停播放')
}

const handleEnded = () => {
  console.log('播放结束')
  currentAudioUrl.value = ''
}

// 格式化时间
const formatTime = (date) => {
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  
  return date.toLocaleDateString()
}

// 格式化时长
const formatDuration = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 组件挂载时获取监控数据
onMounted(() => {
  // 获取监控数据（添加容错处理）
  refreshAllData().catch(err => {
    console.error('获取监控数据失败:', err)
    // 不阻止页面渲染，使用默认值
  })
  
  // 每30秒刷新一次监控数据
  setInterval(() => {
    refreshAllData().catch(err => {
      console.error('刷新监控数据失败:', err)
    })
  }, 30000)
})
</script>

<style scoped>
.voice-interaction-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-color);
  overflow: hidden;
}

.page-header {
  padding: var(--spacing-lg) var(--spacing-xl);
  border-bottom: 1px solid var(--border-color);
  background: var(--card-bg);
}

.page-header h1 {
  font-size: 1.75rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 var(--spacing-xs) 0;
}

.subtitle {
  font-size: 0.95rem;
  color: var(--text-secondary);
  margin: 0;
}

.voice-content {
  flex: 1;
  overflow-y: auto;
  padding: var(--spacing-xl);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xl);
}

/* 区域样式 */
.settings-section,
.input-section,
.player-section,
.monitoring-section {
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  padding: var(--spacing-lg);
  border: 1px solid var(--border-color);
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-bottom: var(--spacing-lg);
  color: var(--text-primary);
}

.section-title svg {
  color: var(--primary-color);
}

.section-title h2 {
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0;
}

.section-title h3 {
  font-size: 1rem;
  font-weight: 500;
  margin: 0;
}

/* 识别结果 */
.recognized-text {
  margin-top: var(--spacing-lg);
  padding: var(--spacing-md);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--primary-color);
}

.text-label {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: var(--spacing-xs);
}

.text-content {
  font-size: 1rem;
  color: var(--text-primary);
  line-height: 1.6;
}

/* 播放器容器 */
.player-container {
  margin-bottom: var(--spacing-lg);
}

/* 历史记录 */
.history-section {
  margin-top: var(--spacing-xl);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--border-color);
}

.history-section h3 {
  font-size: 1rem;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0 0 var(--spacing-md) 0;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.history-item {
  padding: var(--spacing-md);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
}

.history-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
  font-size: 0.875rem;
  color: var(--text-secondary);
}

/* 监控指标 */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--spacing-md);
}

.metric-card {
  padding: var(--spacing-lg);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  text-align: center;
}

.metric-label {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin-bottom: var(--spacing-sm);
}

.metric-value {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary);
}

.metric-value.healthy {
  color: var(--success-color);
}

.metric-value.unhealthy {
  color: var(--error-color);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    padding: var(--spacing-md);
  }

  .page-header h1 {
    font-size: 1.5rem;
  }

  .voice-content {
    padding: var(--spacing-md);
    gap: var(--spacing-md);
  }

  .settings-section,
  .input-section,
  .player-section,
  .monitoring-section {
    padding: var(--spacing-md);
  }

  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>

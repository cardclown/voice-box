<template>
  <div class="emotion-history">
    <!-- 历史记录头部 -->
    <div class="history-header">
      <h3 class="history-title">情绪历史</h3>
      <div class="history-actions">
        <button class="action-button" @click="refreshHistory">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
          </svg>
          刷新
        </button>
        <button class="action-button" @click="clearHistory">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
          </svg>
          清空
        </button>
      </div>
    </div>

    <!-- 筛选器 -->
    <div class="history-filters">
      <select v-model="filterEmotion" class="filter-select">
        <option value="">全部情绪</option>
        <option value="HAPPY">开心</option>
        <option value="SAD">悲伤</option>
        <option value="ANGRY">愤怒</option>
        <option value="CALM">平静</option>
        <option value="ANXIOUS">焦虑</option>
      </select>
      
      <select v-model="sortBy" class="filter-select">
        <option value="time-desc">时间降序</option>
        <option value="time-asc">时间升序</option>
        <option value="confidence-desc">置信度降序</option>
      </select>
    </div>

    <!-- 历史记录列表 -->
    <div v-if="filteredHistory.length > 0" class="history-list">
      <div 
        v-for="item in paginatedHistory" 
        :key="item.id"
        class="history-item"
      >
        <div class="item-header">
          <div class="item-emotion">
            <span class="emotion-icon">{{ getEmotionEmoji(item.emotion) }}</span>
            <span class="emotion-name">{{ getEmotionName(item.emotion) }}</span>
            <span class="emotion-confidence">{{ Math.round(item.confidence * 100) }}%</span>
          </div>
          <div class="item-time">{{ formatTime(item.timestamp) }}</div>
        </div>
        
        <div v-if="item.text" class="item-text">{{ item.text }}</div>
        
        <div v-if="item.tags && item.tags.length > 0" class="item-tags">
          <span 
            v-for="tag in item.tags.slice(0, 5)" 
            :key="tag"
            class="item-tag"
          >
            {{ tag }}
          </span>
          <span v-if="item.tags.length > 5" class="item-tag-more">
            +{{ item.tags.length - 5 }}
          </span>
        </div>
        
        <div class="item-actions">
          <button 
            v-if="item.audioUrl" 
            class="item-action-button"
            @click="playAudio(item)"
          >
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path v-if="!isPlaying(item.id)" d="M8 5v14l11-7z"/>
              <path v-else d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>
            </svg>
            {{ isPlaying(item.id) ? '暂停' : '播放' }}
          </button>
          
          <button class="item-action-button" @click="viewDetails(item)">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
            </svg>
            详情
          </button>
          
          <button class="item-action-button delete" @click="deleteItem(item)">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
            </svg>
            删除
          </button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="history-empty">
      <div class="empty-icon">📝</div>
      <p class="empty-text">暂无历史记录</p>
      <p class="empty-hint">开始语音分析后将自动记录</p>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="history-pagination">
      <button 
        class="pagination-button"
        :disabled="currentPage === 1"
        @click="currentPage--"
      >
        上一页
      </button>
      <span class="pagination-info">
        第 {{ currentPage }} / {{ totalPages }} 页
      </span>
      <button 
        class="pagination-button"
        :disabled="currentPage === totalPages"
        @click="currentPage++"
      >
        下一页
      </button>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="selectedItem" class="detail-modal" @click="closeDetails">
      <div class="detail-modal-content" @click.stop>
        <div class="detail-modal-header">
          <h3 class="detail-modal-title">情绪详情</h3>
          <button class="detail-modal-close" @click="closeDetails">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
        <div class="detail-modal-body">
          <div class="detail-section">
            <h4 class="detail-section-title">基本信息</h4>
            <div class="detail-item">
              <span class="detail-label">情绪:</span>
              <span class="detail-value">
                {{ getEmotionEmoji(selectedItem.emotion) }} {{ getEmotionName(selectedItem.emotion) }}
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">置信度:</span>
              <span class="detail-value">{{ Math.round(selectedItem.confidence * 100) }}%</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">时间:</span>
              <span class="detail-value">{{ formatFullTime(selectedItem.timestamp) }}</span>
            </div>
          </div>
          
          <div v-if="selectedItem.text" class="detail-section">
            <h4 class="detail-section-title">文本内容</h4>
            <p class="detail-text">{{ selectedItem.text }}</p>
          </div>
          
          <div v-if="selectedItem.tags && selectedItem.tags.length > 0" class="detail-section">
            <h4 class="detail-section-title">情感标签</h4>
            <div class="detail-tags">
              <span 
                v-for="tag in selectedItem.tags" 
                :key="tag"
                class="detail-tag"
              >
                {{ tag }}
              </span>
            </div>
          </div>
          
          <div v-if="selectedItem.features" class="detail-section">
            <h4 class="detail-section-title">语音特征</h4>
            <div class="detail-features">
              <div v-if="selectedItem.features.pitch" class="feature-item">
                <span class="feature-label">音高:</span>
                <span class="feature-value">{{ selectedItem.features.pitch.toFixed(1) }} Hz</span>
              </div>
              <div v-if="selectedItem.features.volume" class="feature-item">
                <span class="feature-label">音量:</span>
                <span class="feature-value">{{ selectedItem.features.volume.toFixed(1) }} dB</span>
              </div>
              <div v-if="selectedItem.features.speed" class="feature-item">
                <span class="feature-label">语速:</span>
                <span class="feature-value">{{ selectedItem.features.speed.toFixed(1) }} 字/分</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 音频播放器 -->
    <audio ref="audioPlayer" @ended="onAudioEnded"></audio>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  history: {
    type: Array,
    default: () => []
  },
  pageSize: {
    type: Number,
    default: 10
  }
})

const emit = defineEmits(['refresh', 'clear', 'delete-item'])

// 筛选和排序
const filterEmotion = ref('')
const sortBy = ref('time-desc')

// 分页
const currentPage = ref(1)

// 选中的项
const selectedItem = ref(null)

// 音频播放
const audioPlayer = ref(null)
const playingItemId = ref(null)

// 情绪名称映射
const emotionNames = {
  HAPPY: '开心',
  SAD: '悲伤',
  ANGRY: '愤怒',
  FEAR: '恐惧',
  SURPRISE: '惊讶',
  DISGUST: '厌恶',
  CALM: '平静',
  EXCITED: '兴奋',
  ANXIOUS: '焦虑',
  NEUTRAL: '中性'
}

// 情绪表情映射
const emotionEmojis = {
  HAPPY: '😊',
  SAD: '😢',
  ANGRY: '😠',
  FEAR: '😨',
  SURPRISE: '😲',
  DISGUST: '🤢',
  CALM: '😌',
  EXCITED: '🤩',
  ANXIOUS: '😰',
  NEUTRAL: '😐'
}

// 获取情绪名称
const getEmotionName = (emotion) => {
  return emotionNames[emotion] || emotion || '未知'
}

// 获取情绪表情
const getEmotionEmoji = (emotion) => {
  return emotionEmojis[emotion] || '😐'
}

// 筛选后的历史记录
const filteredHistory = computed(() => {
  let result = [...props.history]
  
  // 按情绪筛选
  if (filterEmotion.value) {
    result = result.filter(item => item.emotion === filterEmotion.value)
  }
  
  // 排序
  result.sort((a, b) => {
    switch (sortBy.value) {
      case 'time-desc':
        return b.timestamp - a.timestamp
      case 'time-asc':
        return a.timestamp - b.timestamp
      case 'confidence-desc':
        return b.confidence - a.confidence
      default:
        return 0
    }
  })
  
  return result
})

// 分页后的历史记录
const paginatedHistory = computed(() => {
  const start = (currentPage.value - 1) * props.pageSize
  const end = start + props.pageSize
  return filteredHistory.value.slice(start, end)
})

// 总页数
const totalPages = computed(() => {
  return Math.ceil(filteredHistory.value.length / props.pageSize)
})

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  
  // 小于1分钟
  if (diff < 60000) {
    return '刚刚'
  }
  
  // 小于1小时
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }
  
  // 小于1天
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  
  // 小于7天
  if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)}天前`
  }
  
  // 超过7天，显示日期
  return date.toLocaleDateString('zh-CN', { 
    month: '2-digit', 
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化完整时间
const formatFullTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 刷新历史
const refreshHistory = () => {
  emit('refresh')
}

// 清空历史
const clearHistory = () => {
  if (confirm('确定要清空所有历史记录吗？此操作不可恢复。')) {
    emit('clear')
    currentPage.value = 1
  }
}

// 删除单项
const deleteItem = (item) => {
  if (confirm('确定要删除这条记录吗？')) {
    emit('delete-item', item)
  }
}

// 播放音频
const playAudio = (item) => {
  if (!audioPlayer.value || !item.audioUrl) return
  
  if (playingItemId.value === item.id) {
    // 暂停当前播放
    audioPlayer.value.pause()
    playingItemId.value = null
  } else {
    // 播放新音频
    audioPlayer.value.src = item.audioUrl
    audioPlayer.value.play()
    playingItemId.value = item.id
  }
}

// 检查是否正在播放
const isPlaying = (itemId) => {
  return playingItemId.value === itemId
}

// 音频播放结束
const onAudioEnded = () => {
  playingItemId.value = null
}

// 查看详情
const viewDetails = (item) => {
  selectedItem.value = item
}

// 关闭详情
const closeDetails = () => {
  selectedItem.value = null
}

// 监听筛选变化，重置页码
watch([filterEmotion, sortBy], () => {
  currentPage.value = 1
})

// 监听历史记录变化
watch(() => props.history, (newHistory) => {
  console.log('历史记录已更新:', newHistory.length)
}, { deep: true })

onMounted(() => {
  console.log('情绪历史组件已加载')
})

onUnmounted(() => {
  // 停止音频播放
  if (audioPlayer.value) {
    audioPlayer.value.pause()
  }
})
</script>

<style scoped>
.emotion-history {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.history-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.action-button {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background-color: white;
  color: var(--text-primary);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-button:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
  background-color: rgba(var(--primary-color-rgb), 0.05);
}

.action-button svg {
  width: 16px;
  height: 16px;
}

.history-filters {
  display: flex;
  gap: var(--spacing-sm);
}

.filter-select {
  flex: 1;
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  background-color: white;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.filter-select:focus {
  outline: none;
  border-color: var(--primary-color);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.history-item {
  padding: var(--spacing-md);
  background-color: white;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
}

.history-item:hover {
  box-shadow: var(--shadow-sm);
  border-color: var(--primary-color);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.item-emotion {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.emotion-icon {
  font-size: 1.5rem;
}

.emotion-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
}

.emotion-confidence {
  font-size: 0.875rem;
  color: var(--text-secondary);
  padding: 2px 8px;
  background-color: var(--bg-secondary);
  border-radius: var(--radius-sm);
}

.item-time {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.item-text {
  font-size: 0.875rem;
  color: var(--text-primary);
  line-height: 1.5;
  margin-bottom: var(--spacing-sm);
  padding: var(--spacing-sm);
  background-color: var(--bg-secondary);
  border-radius: var(--radius-sm);
}

.item-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-sm);
}

.item-tag {
  padding: 2px 8px;
  background-color: var(--primary-color);
  color: white;
  font-size: 0.75rem;
  border-radius: var(--radius-sm);
}

.item-tag-more {
  padding: 2px 8px;
  background-color: var(--text-secondary);
  color: white;
  font-size: 0.75rem;
  border-radius: var(--radius-sm);
}

.item-actions {
  display: flex;
  gap: var(--spacing-xs);
  padding-top: var(--spacing-sm);
  border-top: 1px solid var(--border-color);
}

.item-action-button {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) var(--spacing-sm);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background-color: white;
  color: var(--text-primary);
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.item-action-button:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.item-action-button.delete:hover {
  border-color: #e74c3c;
  color: #e74c3c;
}

.item-action-button svg {
  width: 14px;
  height: 14px;
}

.history-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-xl);
  text-align: center;
  background-color: var(--bg-secondary);
  border-radius: var(--radius-md);
  min-height: 200px;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: var(--spacing-md);
  opacity: 0.5;
}

.empty-text {
  font-size: 1rem;
  color: var(--text-primary);
  margin: 0 0 var(--spacing-xs) 0;
}

.empty-hint {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin: 0;
}

.history-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-md);
  padding-top: var(--spacing-md);
  border-top: 1px solid var(--border-color);
}

.pagination-button {
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background-color: white;
  color: var(--text-primary);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pagination-button:hover:not(:disabled) {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.pagination-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-info {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.detail-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: var(--spacing-md);
}

.detail-modal-content {
  background-color: white;
  border-radius: var(--radius-lg);
  max-width: 600px;
  width: 100%;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: var(--shadow-lg);
}

.detail-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
}

.detail-modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.detail-modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background-color: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.detail-modal-close:hover {
  background-color: var(--hover-color);
  color: var(--text-primary);
}

.detail-modal-close svg {
  width: 20px;
  height: 20px;
}

.detail-modal-body {
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.detail-section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  padding-bottom: var(--spacing-xs);
  border-bottom: 2px solid var(--primary-color);
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-xs) 0;
}

.detail-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.detail-value {
  font-size: 0.875rem;
  color: var(--text-primary);
}

.detail-text {
  font-size: 0.875rem;
  color: var(--text-primary);
  line-height: 1.6;
  padding: var(--spacing-md);
  background-color: var(--bg-secondary);
  border-radius: var(--radius-md);
  margin: 0;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
}

.detail-tag {
  padding: var(--spacing-xs) var(--spacing-sm);
  background-color: var(--primary-color);
  color: white;
  font-size: 0.875rem;
  border-radius: var(--radius-sm);
}

.detail-features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--spacing-sm);
}

.feature-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  padding: var(--spacing-md);
  background-color: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.feature-label {
  font-size: 0.75rem;
  color: var(--text-secondary);
}

.feature-value {
  font-size: 1rem;
  font-weight: 600;
  color: var(--primary-color);
}

/* 响应式设计 */
@media (max-width: 767px) {
  .history-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-sm);
  }
  
  .history-actions {
    width: 100%;
  }
  
  .action-button {
    flex: 1;
    justify-content: center;
  }
  
  .history-filters {
    flex-direction: column;
  }
  
  .item-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-xs);
  }
  
  .item-actions {
    flex-wrap: wrap;
  }
  
  .item-action-button {
    flex: 1;
    justify-content: center;
  }
  
  .detail-modal-content {
    max-height: 90vh;
  }
  
  .detail-features {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="emotion-feedback">
    <div v-if="emotionData" class="emotion-display">
      <div class="emotion-main">
        <div class="emotion-icon">
          <div :class="['emotion-avatar', emotionData.primaryEmotion?.toLowerCase()]">
            {{ getEmotionEmoji(emotionData.primaryEmotion) }}
          </div>
        </div>
        <div class="emotion-info">
          <h3 class="emotion-name">{{ getEmotionName(emotionData.primaryEmotion) }}</h3>
          <div class="emotion-confidence">
            置信度: {{ Math.round((emotionData.confidence || 0) * 100) }}%
          </div>
        </div>
      </div>
      
      <div class="emotion-intensity">
        <label class="intensity-label">情绪强度</label>
        <div class="intensity-bar">
          <div 
            class="intensity-fill"
            :style="{ 
              width: `${(emotionData.confidence || 0) * 100}%`,
              backgroundColor: getEmotionColor(emotionData.primaryEmotion)
            }"
          ></div>
        </div>
        <span class="intensity-value">{{ getIntensityLevel(emotionData.confidence) }}</span>
      </div>
    </div>
    
    <div v-else class="no-emotion">
      <div class="no-emotion-icon">🎤</div>
      <p class="no-emotion-text">开始录音或输入文本来分析情绪</p>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  emotionData: {
    type: Object,
    default: null
  }
})

const emotionNames = {
  HAPPY: '开心',
  SAD: '悲伤',
  ANGRY: '愤怒',
  CALM: '平静',
  ANXIOUS: '焦虑'
}

const emotionEmojis = {
  HAPPY: '😊',
  SAD: '😢',
  ANGRY: '😠',
  CALM: '😌',
  ANXIOUS: '😰'
}

const emotionColors = {
  HAPPY: '#f39c12',
  SAD: '#3498db',
  ANGRY: '#e74c3c',
  CALM: '#2ecc71',
  ANXIOUS: '#34495e'
}

const getEmotionName = (emotion) => {
  return emotionNames[emotion] || emotion || '未知'
}

const getEmotionEmoji = (emotion) => {
  return emotionEmojis[emotion] || '😐'
}

const getEmotionColor = (emotion) => {
  return emotionColors[emotion] || '#bdc3c7'
}

const getIntensityLevel = (confidence) => {
  if (!confidence) return '无'
  if (confidence < 0.3) return '弱'
  if (confidence < 0.6) return '中'
  if (confidence < 0.8) return '强'
  return '很强'
}
</script>

<style scoped>
.emotion-feedback {
  padding: var(--spacing-lg);
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
}

.emotion-main {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-md);
}

.emotion-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.5rem;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
}

.emotion-name {
  font-size: 1.5rem;
  margin: 0;
}

.emotion-intensity {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.intensity-bar {
  flex: 1;
  height: 8px;
  background-color: var(--bg-secondary);
  border-radius: 4px;
  overflow: hidden;
}

.intensity-fill {
  height: 100%;
  transition: width 0.5s ease;
}

.no-emotion {
  text-align: center;
  padding: var(--spacing-xl);
}

.no-emotion-icon {
  font-size: 3rem;
  margin-bottom: var(--spacing-md);
  opacity: 0.5;
}
</style>

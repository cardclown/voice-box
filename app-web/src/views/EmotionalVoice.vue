<template>
  <div class="emotional-voice-container">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <div class="error-icon">⚠️</div>
      <p class="error-message">{{ error }}</p>
      <button @click="loadProfile" class="retry-button">重试</button>
    </div>

    <!-- 正常内容 -->
    <template v-else>
      <div class="emotional-voice-header">
        <h1 class="page-title">情感语音交互</h1>
        <p class="page-subtitle">体验智能情感识别与个性化语音合成</p>
      </div>

      <div class="emotional-voice-content">
        <div class="left-panel">
          <div class="voice-input-section">
            <h2 class="section-title">语音输入</h2>
            <EmotionalVoiceInput 
              @voice-analyzed="handleVoiceAnalyzed"
              @error="handleError"
            />
          </div>

          <div class="emotion-feedback-section">
            <h2 class="section-title">实时情绪反馈</h2>
            <EmotionFeedback :emotion-data="currentEmotion" />
          </div>
        </div>

        <div class="right-panel">
          <div class="language-section">
            <h2 class="section-title">语言设置</h2>
            <LanguageSelector 
              :user-id="userId"
              @language-changed="handleLanguageChanged"
            />
          </div>
          
          <div class="tags-section">
            <h2 class="section-title">情感标签</h2>
            <TagVisualization :tags="userTags" />
          </div>

          <div class="statistics-section">
            <h2 class="section-title">情感统计</h2>
            <EmotionStatistics :user-id="userId" />
          </div>
        </div>
      </div>

      <div class="history-section">
        <h2 class="section-title">对话历史</h2>
        <EmotionHistory :user-id="userId" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import EmotionalVoiceInput from '../components/emotional/EmotionalVoiceInput.vue'
import EmotionFeedback from '../components/emotional/EmotionFeedback.vue'
import TagVisualization from '../components/emotional/TagVisualization.vue'
import EmotionStatistics from '../components/emotional/EmotionStatistics.vue'
import EmotionHistory from '../components/emotional/EmotionHistory.vue'
import LanguageSelector from '../components/emotional/LanguageSelector.vue'
import { useEmotionalVoiceStore } from '../stores/emotionalVoiceStore'

const store = useEmotionalVoiceStore()
const userId = ref(1)
const currentEmotion = ref(null)
const userTags = ref({})
const loading = ref(true)
const error = ref(null)

const handleVoiceAnalyzed = (result) => {
  currentEmotion.value = result
  store.setCurrentEmotion(result)
  if (result.tags) {
    userTags.value = result.tags
  }
}

const handleError = (err) => {
  console.error('语音分析错误:', err)
  error.value = err.message || '发生错误'
}

const handleLanguageChanged = (language) => {
  console.log('语言已切换:', language)
  // 重新加载用户画像以获取新语言的配置
  loadProfile()
}

const loadProfile = async () => {
  try {
    loading.value = true
    error.value = null
    
    // 设置当前用户
    store.setCurrentUser(userId.value)
    
    // 获取用户画像（已添加容错处理，不会抛出错误）
    await store.fetchProfile()
  } catch (err) {
    console.error('加载用户画像失败:', err)
    error.value = '加载用户画像失败，但您仍可以使用基本功能'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 加载用户画像
  loadProfile()
})
</script>

<style scoped>
.emotional-voice-container {
  padding: var(--spacing-lg);
  max-width: 1400px;
  margin: 0 auto;
  min-height: 400px;
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 1rem;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 错误状态 */
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  gap: 1rem;
}

.error-icon {
  font-size: 3rem;
}

.error-message {
  color: #e74c3c;
  font-size: 1.1rem;
}

.retry-button {
  padding: 0.5rem 1.5rem;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
}

.retry-button:hover {
  background: #2980b9;
}

.emotional-voice-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.page-title {
  font-size: 2rem;
  margin: 0 0 var(--spacing-sm) 0;
}

.page-subtitle {
  color: var(--text-secondary);
  margin: 0;
}

.emotional-voice-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
}

.section-title {
  font-size: 1.25rem;
  margin: 0 0 var(--spacing-md) 0;
}

.voice-input-section,
.emotion-feedback-section,
.language-section,
.tags-section,
.statistics-section,
.history-section {
  background: var(--bg-primary);
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.language-section {
  margin-bottom: var(--spacing-lg);
}

@media (max-width: 768px) {
  .emotional-voice-content {
    grid-template-columns: 1fr;
  }
}
</style>

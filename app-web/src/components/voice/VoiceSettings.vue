<template>
  <div class="voice-settings">
    <button 
      class="settings-button"
      @click="toggleSettings"
      title="语音设置"
    >
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="3"/>
        <path d="M12 1v6m0 6v6M5.64 5.64l4.24 4.24m4.24 4.24l4.24 4.24M1 12h6m6 0h6M5.64 18.36l4.24-4.24m4.24-4.24l4.24-4.24"/>
      </svg>
    </button>

    <transition name="modal">
      <div v-if="isOpen" class="settings-modal" @click.self="closeSettings">
        <div class="settings-content">
          <div class="settings-header">
            <h3>语音设置</h3>
            <button class="close-button" @click="closeSettings">×</button>
          </div>

          <div class="settings-body">
            <!-- 语言设置 -->
            <div class="setting-section">
              <label class="setting-label">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                  <path d="M12.87 15.07l-2.54-2.51.03-.03c1.74-1.94 2.98-4.17 3.71-6.53H17V4h-7V2H8v2H1v1.99h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-12zm-2.62 7l1.62-4.33L19.12 17h-3.24z"/>
                </svg>
                语言
              </label>
              <LanguageSelector 
                v-model="selectedLanguage"
                @change="handleLanguageChange"
              />
            </div>

            <!-- 自动播放设置 -->
            <div class="setting-section">
              <label class="setting-label">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                  <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/>
                </svg>
                自动播放AI回复
              </label>
              <label class="toggle-switch">
                <input 
                  type="checkbox" 
                  v-model="autoPlay"
                  @change="handleAutoPlayChange"
                />
                <span class="toggle-slider"></span>
              </label>
            </div>

            <!-- 语速设置 -->
            <div class="setting-section">
              <label class="setting-label">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                  <path d="M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z"/>
                </svg>
                语速
              </label>
              <div class="speed-control">
                <input 
                  type="range" 
                  v-model.number="speechRate"
                  @change="handleSpeechRateChange"
                  min="0.5" 
                  max="2" 
                  step="0.1"
                  class="speed-slider"
                />
                <span class="speed-value">{{ speechRate.toFixed(1) }}x</span>
              </div>
            </div>

            <!-- 音量设置 -->
            <div class="setting-section">
              <label class="setting-label">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                  <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"/>
                </svg>
                音量
              </label>
              <div class="volume-control">
                <input 
                  type="range" 
                  v-model.number="volume"
                  @change="handleVolumeChange"
                  min="0" 
                  max="100" 
                  step="1"
                  class="volume-slider"
                />
                <span class="volume-value">{{ volume }}%</span>
              </div>
            </div>
          </div>

          <div class="settings-footer">
            <button class="reset-button" @click="resetSettings">
              重置为默认
            </button>
            <button class="save-button" @click="saveSettings">
              保存设置
            </button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import LanguageSelector from './LanguageSelector.vue'
import { useLanguagePreference } from '@/composables/useLanguagePreference'

const props = defineProps({
  userId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['settings-changed'])

const { currentLanguage, setLanguage, initializeLanguage } = useLanguagePreference()

const isOpen = ref(false)
const selectedLanguage = ref('zh-CN')
const autoPlay = ref(true)
const speechRate = ref(1.0)
const volume = ref(80)

// 切换设置面板
const toggleSettings = () => {
  isOpen.value = !isOpen.value
}

// 关闭设置面板
const closeSettings = () => {
  isOpen.value = false
}

// 处理语言变化
const handleLanguageChange = (language) => {
  selectedLanguage.value = language
  setLanguage(language, props.userId)
  emit('settings-changed', { language })
}

// 处理自动播放变化
const handleAutoPlayChange = () => {
  saveToLocalStorage('autoPlay', autoPlay.value)
  emit('settings-changed', { autoPlay: autoPlay.value })
}

// 处理语速变化
const handleSpeechRateChange = () => {
  saveToLocalStorage('speechRate', speechRate.value)
  emit('settings-changed', { speechRate: speechRate.value })
}

// 处理音量变化
const handleVolumeChange = () => {
  saveToLocalStorage('volume', volume.value)
  emit('settings-changed', { volume: volume.value })
}

// 保存到本地存储
const saveToLocalStorage = (key, value) => {
  try {
    localStorage.setItem(`voicebox_${key}`, JSON.stringify(value))
  } catch (error) {
    console.error('保存设置失败:', error)
  }
}

// 从本地存储加载
const loadFromLocalStorage = (key, defaultValue) => {
  try {
    const saved = localStorage.getItem(`voicebox_${key}`)
    return saved ? JSON.parse(saved) : defaultValue
  } catch (error) {
    console.error('加载设置失败:', error)
    return defaultValue
  }
}

// 重置设置
const resetSettings = () => {
  selectedLanguage.value = 'zh-CN'
  autoPlay.value = true
  speechRate.value = 1.0
  volume.value = 80
  
  setLanguage('zh-CN', props.userId)
  saveToLocalStorage('autoPlay', true)
  saveToLocalStorage('speechRate', 1.0)
  saveToLocalStorage('volume', 80)
  
  emit('settings-changed', {
    language: 'zh-CN',
    autoPlay: true,
    speechRate: 1.0,
    volume: 80
  })
}

// 保存设置
const saveSettings = () => {
  closeSettings()
  // 设置已经在各个change事件中保存了
}

// 初始化设置
const initializeSettings = async () => {
  await initializeLanguage(props.userId)
  selectedLanguage.value = currentLanguage.value
  autoPlay.value = loadFromLocalStorage('autoPlay', true)
  speechRate.value = loadFromLocalStorage('speechRate', 1.0)
  volume.value = loadFromLocalStorage('volume', 80)
}

onMounted(() => {
  initializeSettings()
})
</script>

<style scoped>
.voice-settings {
  position: relative;
}

.settings-button {
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.settings-button:hover {
  background: #f3f4f6;
  color: #111827;
}

.settings-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 20px;
}

.settings-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.settings-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.close-button {
  background: none;
  border: none;
  font-size: 28px;
  color: #6b7280;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.close-button:hover {
  background: #f3f4f6;
  color: #111827;
}

.settings-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.setting-section {
  margin-bottom: 24px;
}

.setting-section:last-child {
  margin-bottom: 0;
}

.setting-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 12px;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 48px;
  height: 24px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #d1d5db;
  transition: 0.3s;
  border-radius: 24px;
}

.toggle-slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
}

input:checked + .toggle-slider {
  background-color: #3b82f6;
}

input:checked + .toggle-slider:before {
  transform: translateX(24px);
}

.speed-control,
.volume-control {
  display: flex;
  align-items: center;
  gap: 12px;
}

.speed-slider,
.volume-slider {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: #e5e7eb;
  outline: none;
  -webkit-appearance: none;
}

.speed-slider::-webkit-slider-thumb,
.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
}

.speed-slider::-moz-range-thumb,
.volume-slider::-moz-range-thumb {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #3b82f6;
  cursor: pointer;
  border: none;
}

.speed-value,
.volume-value {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  min-width: 48px;
  text-align: right;
}

.settings-footer {
  display: flex;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #e5e7eb;
}

.reset-button,
.save-button {
  flex: 1;
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.reset-button {
  background: #f3f4f6;
  color: #374151;
}

.reset-button:hover {
  background: #e5e7eb;
}

.save-button {
  background: #3b82f6;
  color: white;
}

.save-button:hover {
  background: #2563eb;
}

/* 模态动画 */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-active .settings-content,
.modal-leave-active .settings-content {
  transition: transform 0.3s ease;
}

.modal-enter-from .settings-content,
.modal-leave-to .settings-content {
  transform: scale(0.9);
}

/* 移动端优化 */
@media (max-width: 767px) {
  .settings-modal {
    padding: 0;
  }

  .settings-content {
    max-width: 100%;
    max-height: 100vh;
    border-radius: 0;
  }
}
</style>

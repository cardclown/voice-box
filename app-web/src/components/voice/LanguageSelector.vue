<template>
  <div class="language-selector">
    <button 
      class="language-button"
      @click="toggleDropdown"
      :title="'当前语言: ' + currentLanguageLabel"
    >
      <span class="language-icon">🌐</span>
      <span class="language-label">{{ currentLanguageLabel }}</span>
      <svg class="dropdown-icon" :class="{ open: isOpen }" viewBox="0 0 24 24" width="16" height="16">
        <path d="M7 10l5 5 5-5z" fill="currentColor"/>
      </svg>
    </button>

    <transition name="dropdown">
      <div v-if="isOpen" class="language-dropdown">
        <div class="dropdown-header">
          <span>选择语言</span>
          <button class="close-button" @click="closeDropdown">×</button>
        </div>
        <div class="language-list">
          <button
            v-for="lang in supportedLanguages"
            :key="lang.code"
            class="language-option"
            :class="{ active: modelValue === lang.code }"
            @click="selectLanguage(lang.code)"
          >
            <span class="language-flag">{{ lang.flag }}</span>
            <div class="language-info">
              <span class="language-name">{{ lang.name }}</span>
              <span class="language-native">{{ lang.nativeName }}</span>
            </div>
            <svg v-if="modelValue === lang.code" class="check-icon" viewBox="0 0 24 24" width="20" height="20">
              <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" fill="currentColor"/>
            </svg>
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: 'zh-CN'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const isOpen = ref(false)

// 支持的语言列表
const supportedLanguages = [
  {
    code: 'zh-CN',
    name: '中文（普通话）',
    nativeName: '简体中文',
    flag: '🇨🇳'
  },
  {
    code: 'en-US',
    name: '英语（美国）',
    nativeName: 'English (US)',
    flag: '🇺🇸'
  },
  {
    code: 'ja-JP',
    name: '日语',
    nativeName: '日本語',
    flag: '🇯🇵'
  },
  {
    code: 'ko-KR',
    name: '韩语',
    nativeName: '한국어',
    flag: '🇰🇷'
  }
]

// 当前语言标签
const currentLanguageLabel = computed(() => {
  const lang = supportedLanguages.find(l => l.code === props.modelValue)
  return lang ? lang.name : '中文（普通话）'
})

// 切换下拉菜单
const toggleDropdown = () => {
  isOpen.value = !isOpen.value
}

// 关闭下拉菜单
const closeDropdown = () => {
  isOpen.value = false
}

// 选择语言
const selectLanguage = (code) => {
  emit('update:modelValue', code)
  emit('change', code)
  closeDropdown()
}

// 点击外部关闭下拉菜单
const handleClickOutside = (event) => {
  const selector = event.target.closest('.language-selector')
  if (!selector && isOpen.value) {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.language-selector {
  position: relative;
  display: inline-block;
}

.language-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #374151;
  transition: all 0.2s ease;
}

.language-button:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.language-icon {
  font-size: 18px;
}

.language-label {
  font-weight: 500;
}

.dropdown-icon {
  transition: transform 0.2s ease;
}

.dropdown-icon.open {
  transform: rotate(180deg);
}

.language-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 280px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  font-weight: 600;
  font-size: 14px;
  color: #111827;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  color: #6b7280;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.close-button:hover {
  background: #f3f4f6;
  color: #111827;
}

.language-list {
  max-height: 320px;
  overflow-y: auto;
}

.language-option {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background 0.2s ease;
}

.language-option:hover {
  background: #f9fafb;
}

.language-option.active {
  background: #eff6ff;
}

.language-flag {
  font-size: 24px;
  flex-shrink: 0;
}

.language-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.language-name {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
}

.language-native {
  font-size: 12px;
  color: #6b7280;
}

.check-icon {
  flex-shrink: 0;
  color: #3b82f6;
}

/* 下拉动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 移动端优化 */
@media (max-width: 767px) {
  .language-dropdown {
    position: fixed;
    top: auto;
    bottom: 0;
    left: 0;
    right: 0;
    min-width: 100%;
    border-radius: 16px 16px 0 0;
    max-height: 60vh;
  }

  .language-list {
    max-height: calc(60vh - 60px);
  }

  .dropdown-enter-from,
  .dropdown-leave-to {
    transform: translateY(100%);
  }
}

/* 滚动条样式 */
.language-list::-webkit-scrollbar {
  width: 6px;
}

.language-list::-webkit-scrollbar-track {
  background: transparent;
}

.language-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.language-list::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}
</style>

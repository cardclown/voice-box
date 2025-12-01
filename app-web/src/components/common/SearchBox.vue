<template>
  <div :class="['search-box', { 'search-active': isActive }]">
    <div class="search-input-wrapper">
      <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8" />
        <path d="m21 21-4.35-4.35" />
      </svg>
      
      <input
        ref="searchInput"
        v-model="searchQuery"
        type="text"
        :placeholder="placeholder"
        class="search-input"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown="handleKeydown"
        @input="handleInput"
      />
      
      <button
        v-if="searchQuery"
        @click="clearSearch"
        class="clear-btn"
        title="清除"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
    </div>
    
    <!-- 搜索建议下拉框 -->
    <div v-if="showSuggestions" class="search-suggestions">
      <!-- 搜索历史 -->
      <div v-if="searchHistory.length > 0 && !searchQuery" class="suggestion-section">
        <div class="suggestion-header">
          <span>搜索历史</span>
          <button @click="clearHistory" class="clear-history-btn">清除</button>
        </div>
        <div
          v-for="(item, index) in searchHistory.slice(0, 5)"
          :key="`history-${index}`"
          :class="['suggestion-item', { active: selectedIndex === index }]"
          @click="selectSuggestion(item)"
          @mouseenter="selectedIndex = index"
        >
          <svg class="suggestion-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3" />
            <path d="M12 1v6m0 6v6m11-7h-6m-6 0H1" />
          </svg>
          <span class="suggestion-text">{{ item }}</span>
        </div>
      </div>
      
      <!-- 搜索建议 -->
      <div v-if="suggestions.length > 0" class="suggestion-section">
        <div v-if="searchQuery" class="suggestion-header">
          <span>搜索建议</span>
        </div>
        <div
          v-for="(item, index) in suggestions"
          :key="`suggestion-${index}`"
          :class="['suggestion-item', { active: selectedIndex === (searchHistory.length + index) }]"
          @click="selectSuggestion(item)"
          @mouseenter="selectedIndex = searchHistory.length + index"
        >
          <svg class="suggestion-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.35-4.35" />
          </svg>
          <span class="suggestion-text" v-html="highlightQuery(item)"></span>
        </div>
      </div>
      
      <!-- 热门搜索 -->
      <div v-if="popularSearches.length > 0 && !searchQuery" class="suggestion-section">
        <div class="suggestion-header">
          <span>热门搜索</span>
        </div>
        <div
          v-for="(item, index) in popularSearches.slice(0, 3)"
          :key="`popular-${index}`"
          :class="['suggestion-item', { active: selectedIndex === (searchHistory.length + index) }]"
          @click="selectSuggestion(item)"
          @mouseenter="selectedIndex = searchHistory.length + index"
        >
          <svg class="suggestion-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
            <rect x="8" y="2" width="8" height="4" rx="1" ry="1" />
          </svg>
          <span class="suggestion-text">{{ item }}</span>
        </div>
      </div>
      
      <!-- 无结果 -->
      <div v-if="searchQuery && suggestions.length === 0 && !loading" class="no-results">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.35-4.35" />
        </svg>
        <span>未找到相关建议</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'

const props = defineProps({
  placeholder: {
    type: String,
    default: '搜索会话...'
  },
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'search', 'clear'])

const searchInput = ref(null)
const searchQuery = ref(props.modelValue)
const isActive = ref(false)
const showSuggestions = ref(false)
const selectedIndex = ref(-1)
const loading = ref(false)
const suggestions = ref([])
const searchHistory = ref([])
const popularSearches = ref([])

// 防抖定时器
let debounceTimer = null

// 计算总建议数量
const totalSuggestions = computed(() => {
  return searchHistory.value.length + suggestions.value.length + popularSearches.value.length
})

// 处理输入
function handleInput() {
  emit('update:modelValue', searchQuery.value)
  
  // 清除之前的定时器
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  
  // 防抖获取建议
  debounceTimer = setTimeout(() => {
    fetchSuggestions()
  }, 300)
}

// 处理焦点
function handleFocus() {
  isActive.value = true
  showSuggestions.value = true
  selectedIndex.value = -1
  
  // 如果没有输入，显示历史和热门搜索
  if (!searchQuery.value) {
    loadHistoryAndPopular()
  }
}

// 处理失焦
function handleBlur() {
  // 延迟隐藏，允许点击建议
  setTimeout(() => {
    isActive.value = false
    showSuggestions.value = false
    selectedIndex.value = -1
  }, 200)
}

// 处理键盘事件
function handleKeydown(event) {
  if (!showSuggestions.value) return
  
  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      selectedIndex.value = Math.min(selectedIndex.value + 1, totalSuggestions.value - 1)
      break
    case 'ArrowUp':
      event.preventDefault()
      selectedIndex.value = Math.max(selectedIndex.value - 1, -1)
      break
    case 'Enter':
      event.preventDefault()
      if (selectedIndex.value >= 0) {
        const allSuggestions = [...searchHistory.value, ...suggestions.value, ...popularSearches.value]
        selectSuggestion(allSuggestions[selectedIndex.value])
      } else {
        performSearch()
      }
      break
    case 'Escape':
      showSuggestions.value = false
      searchInput.value.blur()
      break
  }
}

// 获取搜索建议（模拟）
async function fetchSuggestions() {
  if (!searchQuery.value || searchQuery.value.length < 2) {
    suggestions.value = []
    return
  }
  
  loading.value = true
  try {
    // 模拟 API 调用
    await new Promise(resolve => setTimeout(resolve, 100))
    suggestions.value = []
  } catch (error) {
    console.error('Failed to fetch suggestions:', error)
    suggestions.value = []
  } finally {
    loading.value = false
  }
}

// 加载历史和热门搜索
async function loadHistoryAndPopular() {
  searchHistory.value = getLocalSearchHistory()
  popularSearches.value = []
}

// 选择建议
function selectSuggestion(suggestion) {
  searchQuery.value = suggestion
  emit('update:modelValue', suggestion)
  showSuggestions.value = false
  performSearch()
}

// 执行搜索
function performSearch() {
  if (!searchQuery.value.trim()) return
  
  // 保存搜索历史
  saveSearchHistory(searchQuery.value)
  searchHistory.value = getLocalSearchHistory()
  
  // 触发搜索事件
  emit('search', searchQuery.value)
  
  // 隐藏建议
  showSuggestions.value = false
  searchInput.value.blur()
}

// 清除搜索
function clearSearch() {
  searchQuery.value = ''
  emit('update:modelValue', '')
  emit('clear')
  suggestions.value = []
  nextTick(() => {
    searchInput.value.focus()
  })
}

// 清除历史
function clearHistory() {
  clearSearchHistory()
  searchHistory.value = []
}

// 高亮查询词
function highlightQuery(text) {
  if (!text || !searchQuery.value) {
    return text
  }
  
  const escapedQuery = searchQuery.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escapedQuery})`, 'gi')
  
  return text.replace(regex, '<mark class="search-highlight">$1</mark>')
}

// 本地存储操作
function getLocalSearchHistory() {
  try {
    const history = localStorage.getItem('voicebox_search_history')
    return history ? JSON.parse(history) : []
  } catch (error) {
    console.error('Failed to load search history:', error)
    return []
  }
}

function saveSearchHistory(query) {
  if (!query || query.trim().length === 0) {
    return
  }
  
  const history = getLocalSearchHistory()
  const newHistory = [query, ...history.filter(item => item !== query)].slice(0, 20)
  localStorage.setItem('voicebox_search_history', JSON.stringify(newHistory))
}

function clearSearchHistory() {
  localStorage.removeItem('voicebox_search_history')
}

// 点击外部关闭
function handleClickOutside(event) {
  if (!event.target.closest('.search-box')) {
    showSuggestions.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  loadHistoryAndPopular()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
})

// 监听 modelValue 变化
watch(() => props.modelValue, (newValue) => {
  searchQuery.value = newValue
})
</script>

<style scoped>
.search-box {
  position: relative;
  width: 100%;
}

.search-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  background: var(--bg-secondary, #f9fafb);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: var(--radius-lg, 12px);
  transition: all 0.2s ease;
}

.search-box.search-active .search-input-wrapper {
  border-color: var(--accent-color, #10a37f);
  box-shadow: 0 0 0 3px rgba(16, 163, 127, 0.1);
}

.search-icon {
  position: absolute;
  left: var(--spacing-md, 1rem);
  color: var(--text-secondary, #6b7280);
  pointer-events: none;
  z-index: 1;
}

.search-input {
  width: 100%;
  padding: var(--spacing-md, 1rem) var(--spacing-xl, 2.5rem) var(--spacing-md, 1rem) var(--spacing-xl, 2.5rem);
  border: none;
  background: transparent;
  font-size: max(14px, 0.9375rem);
  color: var(--text-primary, #111827);
  outline: none;
}

.search-input::placeholder {
  color: var(--text-secondary, #6b7280);
}

.clear-btn {
  position: absolute;
  right: var(--spacing-md, 1rem);
  background: transparent;
  border: none;
  color: var(--text-secondary, #6b7280);
  cursor: pointer;
  padding: var(--spacing-xs, 0.25rem);
  border-radius: var(--radius-sm, 4px);
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.clear-btn:hover {
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-primary, #111827);
}

/* 搜索建议 */
.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: var(--radius-lg, 12px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  max-height: 300px;
  overflow-y: auto;
  margin-top: var(--spacing-xs, 0.25rem);
}

.suggestion-section {
  padding: var(--spacing-sm, 0.75rem) 0;
}

.suggestion-section:not(:last-child) {
  border-bottom: 1px solid var(--border-color, #e5e7eb);
}

.suggestion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--spacing-md, 1rem) var(--spacing-xs, 0.25rem);
  font-size: max(12px, 0.75rem);
  font-weight: 600;
  color: var(--text-secondary, #6b7280);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.clear-history-btn {
  background: transparent;
  border: none;
  color: var(--text-secondary, #6b7280);
  cursor: pointer;
  font-size: max(12px, 0.75rem);
  padding: var(--spacing-xs, 0.25rem);
  border-radius: var(--radius-sm, 4px);
  transition: color 0.2s ease;
}

.clear-history-btn:hover {
  color: var(--text-primary, #111827);
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm, 0.75rem);
  padding: var(--spacing-sm, 0.75rem) var(--spacing-md, 1rem);
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.suggestion-item:hover,
.suggestion-item.active {
  background: var(--hover-bg, rgba(0, 0, 0, 0.02));
}

.suggestion-icon {
  flex-shrink: 0;
  color: var(--text-secondary, #6b7280);
}

.suggestion-text {
  flex: 1;
  font-size: max(14px, 0.9375rem);
  color: var(--text-primary, #111827);
}

.no-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-sm, 0.75rem);
  padding: var(--spacing-xl, 2rem);
  color: var(--text-secondary, #6b7280);
  font-size: max(14px, 0.9375rem);
}

/* 搜索高亮 */
:deep(.search-highlight) {
  background: rgba(16, 163, 127, 0.2);
  color: var(--accent-color, #10a37f);
  font-weight: 600;
  padding: 0 0.1em;
  border-radius: 2px;
}

/* 深色模式 */
[data-theme="dark"] .search-input-wrapper {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
}

[data-theme="dark"] .search-input {
  color: #f3f4f6;
}

[data-theme="dark"] .search-input::placeholder {
  color: #9ca3af;
}

[data-theme="dark"] .search-suggestions {
  background: #1f2937;
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

[data-theme="dark"] .suggestion-item:hover,
[data-theme="dark"] .suggestion-item.active {
  background: rgba(255, 255, 255, 0.05);
}

[data-theme="dark"] .suggestion-text {
  color: #f3f4f6;
}

[data-theme="dark"] .suggestion-header {
  color: #9ca3af;
}

[data-theme="dark"] .suggestion-section:not(:last-child) {
  border-bottom-color: rgba(255, 255, 255, 0.1);
}

/* 移动端优化 */
@media (max-width: 767px) {
  .search-input {
    padding: var(--spacing-sm, 0.75rem) var(--spacing-lg, 2rem) var(--spacing-sm, 0.75rem) var(--spacing-lg, 2rem);
    font-size: max(16px, 1rem); /* 防止 iOS 缩放 */
  }
  
  .search-suggestions {
    max-height: 250px;
  }
}
</style>

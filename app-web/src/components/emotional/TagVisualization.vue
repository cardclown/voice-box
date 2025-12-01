<template>
  <div class="tag-visualization">
    <!-- 标签云 -->
    <div v-if="tags && tags.length > 0" class="tags-container">
      <div class="tags-cloud">
        <div 
          v-for="tag in processedTags" 
          :key="tag.text"
          :class="['tag-item', `tag-${tag.category}`, `size-${tag.sizeClass}`]"
          :style="{
            fontSize: `${tag.fontSize}px`,
            color: tag.color,
            transform: `rotate(${tag.rotation}deg)`
          }"
          @click="selectTag(tag)"
          @mouseenter="highlightTag(tag)"
          @mouseleave="unhighlightTag()"
        >
          {{ tag.text }}
        </div>
      </div>
    </div>

    <!-- 无标签状态 -->
    <div v-else class="no-tags">
      <div class="no-tags-icon">🏷️</div>
      <p class="no-tags-text">暂无情感标签</p>
      <p class="no-tags-hint">开始语音分析后将自动生成标签</p>
    </div>

    <!-- 标签统计 -->
    <div v-if="tags && tags.length > 0" class="tag-stats">
      <h4 class="stats-title">标签统计</h4>
      <div class="stats-grid">
        <div class="stat-item">
          <span class="stat-label">总标签数</span>
          <span class="stat-value">{{ tags.length }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">情绪标签</span>
          <span class="stat-value">{{ getTagsByCategory('emotion').length }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">性格标签</span>
          <span class="stat-value">{{ getTagsByCategory('personality').length }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">语气标签</span>
          <span class="stat-value">{{ getTagsByCategory('tone').length }}</span>
        </div>
      </div>
    </div>

    <!-- 标签分类视图 -->
    <div v-if="tags && tags.length > 0" class="tag-categories">
      <h4 class="categories-title">分类标签</h4>
      <div class="category-tabs">
        <button 
          v-for="category in availableCategories"
          :key="category"
          :class="['category-tab', { active: selectedCategory === category }]"
          @click="selectedCategory = category"
        >
          {{ getCategoryName(category) }}
          <span class="category-count">({{ getTagsByCategory(category).length }})</span>
        </button>
      </div>
      
      <div class="category-content">
        <div class="category-tags">
          <div 
            v-for="tag in getTagsByCategory(selectedCategory)"
            :key="tag.text || tag"
            :class="['category-tag', `category-${selectedCategory}`]"
          >
            <span class="tag-text">{{ typeof tag === 'string' ? tag : tag.text }}</span>
            <span v-if="tag.confidence" class="tag-confidence">
              {{ Math.round(tag.confidence * 100) }}%
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 标签详情弹窗 -->
    <div v-if="selectedTag" class="tag-modal" @click="closeTagModal">
      <div class="tag-modal-content" @click.stop>
        <div class="tag-modal-header">
          <h3 class="tag-modal-title">{{ selectedTag.text }}</h3>
          <button class="tag-modal-close" @click="closeTagModal">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
        <div class="tag-modal-body">
          <div class="tag-detail-item">
            <span class="detail-label">类别:</span>
            <span class="detail-value">{{ getCategoryName(selectedTag.category) }}</span>
          </div>
          <div v-if="selectedTag.confidence" class="tag-detail-item">
            <span class="detail-label">置信度:</span>
            <span class="detail-value">{{ Math.round(selectedTag.confidence * 100) }}%</span>
          </div>
          <div v-if="selectedTag.description" class="tag-detail-item">
            <span class="detail-label">描述:</span>
            <span class="detail-value">{{ selectedTag.description }}</span>
          </div>
          <div v-if="selectedTag.relatedTags" class="tag-detail-item">
            <span class="detail-label">相关标签:</span>
            <div class="related-tags">
              <span 
                v-for="relatedTag in selectedTag.relatedTags"
                :key="relatedTag"
                class="related-tag"
              >
                {{ relatedTag }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  tags: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['tag-selected'])

// 选中的分类
const selectedCategory = ref('all')

// 选中的标签
const selectedTag = ref(null)

// 高亮的标签
const highlightedTag = ref(null)

// 标签分类映射
const categoryNames = {
  all: '全部',
  emotion: '情绪',
  personality: '性格',
  tone: '语气',
  content: '内容',
  style: '风格'
}

// 标签颜色映射
const categoryColors = {
  emotion: ['#e74c3c', '#f39c12', '#f1c40f', '#2ecc71', '#3498db'],
  personality: ['#9b59b6', '#8e44ad', '#e67e22', '#d35400', '#34495e'],
  tone: ['#1abc9c', '#16a085', '#27ae60', '#2ecc71', '#95a5a6'],
  content: ['#3498db', '#2980b9', '#5dade2', '#85c1e9', '#aed6f1'],
  style: ['#f39c12', '#e67e22', '#d68910', '#b7950b', '#a04000']
}

// 处理标签数据
const processedTags = computed(() => {
  if (!props.tags || props.tags.length === 0) return []
  
  return props.tags.map((tag, index) => {
    const tagObj = typeof tag === 'string' ? { text: tag, category: 'content' } : tag
    const category = tagObj.category || 'content'
    const colors = categoryColors[category] || categoryColors.content
    
    // 根据置信度或索引计算大小
    const confidence = tagObj.confidence || (1 - index / props.tags.length)
    const fontSize = 12 + confidence * 20
    const sizeClass = confidence > 0.7 ? 'large' : confidence > 0.4 ? 'medium' : 'small'
    
    // 随机旋转角度
    const rotation = (Math.random() - 0.5) * 10
    
    // 选择颜色
    const color = colors[index % colors.length]
    
    return {
      ...tagObj,
      fontSize,
      sizeClass,
      rotation,
      color,
      category
    }
  })
})

// 可用的分类
const availableCategories = computed(() => {
  const categories = new Set(['all'])
  props.tags.forEach(tag => {
    const category = typeof tag === 'string' ? 'content' : (tag.category || 'content')
    categories.add(category)
  })
  return Array.from(categories)
})

// 获取分类名称
const getCategoryName = (category) => {
  return categoryNames[category] || category
}

// 根据分类获取标签
const getTagsByCategory = (category) => {
  if (category === 'all') return props.tags
  
  return props.tags.filter(tag => {
    const tagCategory = typeof tag === 'string' ? 'content' : (tag.category || 'content')
    return tagCategory === category
  })
}

// 选择标签
const selectTag = (tag) => {
  selectedTag.value = tag
  emit('tag-selected', tag)
}

// 关闭标签详情
const closeTagModal = () => {
  selectedTag.value = null
}

// 高亮标签
const highlightTag = (tag) => {
  highlightedTag.value = tag
}

// 取消高亮
const unhighlightTag = () => {
  highlightedTag.value = null
}

// 监听标签变化
watch(() => props.tags, (newTags) => {
  console.log('标签已更新:', newTags)
}, { deep: true })
</script>

<style scoped>
.tag-visualization {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.tags-container {
  min-height: 200px;
}

.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
  justify-content: center;
  align-items: center;
  padding: var(--spacing-lg);
  background-color: var(--bg-secondary);
  border-radius: var(--radius-md);
}

.tag-item {
  display: inline-block;
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
  white-space: nowrap;
  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.tag-item:hover {
  transform: scale(1.1) !important;
  box-shadow: var(--shadow-md);
  z-index: 10;
}

.tag-item.size-large {
  font-weight: 600;
}

.tag-item.size-medium {
  font-weight: 500;
}

.tag-item.size-small {
  font-weight: 400;
  opacity: 0.8;
}

.no-tags {
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

.no-tags-icon {
  font-size: 3rem;
  margin-bottom: var(--spacing-md);
  opacity: 0.5;
}

.no-tags-text {
  font-size: 1rem;
  color: var(--text-primary);
  margin: 0 0 var(--spacing-xs) 0;
}

.no-tags-hint {
  font-size: 0.875rem;
  color: var(--text-secondary);
  margin: 0;
}

.tag-stats {
  border-top: 1px solid var(--border-color);
  padding-top: var(--spacing-lg);
}

.stats-title,
.categories-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 var(--spacing-md) 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: var(--spacing-md);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
  padding: var(--spacing-md);
  background-color: var(--bg-secondary);
  border-radius: var(--radius-md);
  text-align: center;
}

.stat-label {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--primary-color);
}

.tag-categories {
  border-top: 1px solid var(--border-color);
  padding-top: var(--spacing-lg);
}

.category-tabs {
  display: flex;
  gap: var(--spacing-xs);
  margin-bottom: var(--spacing-md);
  overflow-x: auto;
  padding-bottom: var(--spacing-xs);
}

.category-tab {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-sm) var(--spacing-md);
  border: 2px solid var(--border-color);
  border-radius: var(--radius-md);
  background-color: transparent;
  color: var(--text-primary);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.category-tab:hover {
  border-color: var(--primary-color);
  background-color: rgba(var(--primary-color-rgb), 0.1);
}

.category-tab.active {
  border-color: var(--primary-color);
  background-color: var(--primary-color);
  color: white;
}

.category-count {
  font-size: 0.75rem;
  opacity: 0.8;
}

.category-content {
  background-color: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
}

.category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.category-tag {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-xs) var(--spacing-md);
  background-color: white;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.category-tag:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.category-tag.category-emotion {
  border-color: #e74c3c;
  color: #e74c3c;
}

.category-tag.category-personality {
  border-color: #9b59b6;
  color: #9b59b6;
}

.category-tag.category-tone {
  border-color: #1abc9c;
  color: #1abc9c;
}

.category-tag.category-content {
  border-color: #3498db;
  color: #3498db;
}

.category-tag.category-style {
  border-color: #f39c12;
  color: #f39c12;
}

.tag-text {
  font-weight: 500;
}

.tag-confidence {
  font-size: 0.75rem;
  opacity: 0.7;
}

.tag-modal {
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

.tag-modal-content {
  background-color: white;
  border-radius: var(--radius-lg);
  max-width: 500px;
  width: 100%;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: var(--shadow-lg);
}

.tag-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-lg);
  border-bottom: 1px solid var(--border-color);
}

.tag-modal-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.tag-modal-close {
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

.tag-modal-close:hover {
  background-color: var(--hover-color);
  color: var(--text-primary);
}

.tag-modal-close svg {
  width: 20px;
  height: 20px;
}

.tag-modal-body {
  padding: var(--spacing-lg);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.tag-detail-item {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.detail-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.detail-value {
  font-size: 1rem;
  color: var(--text-primary);
}

.related-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
}

.related-tag {
  padding: var(--spacing-xs) var(--spacing-sm);
  background-color: var(--bg-secondary);
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  color: var(--text-primary);
}

/* 响应式设计 */
@media (max-width: 767px) {
  .tags-cloud {
    padding: var(--spacing-md);
  }
  
  .tag-item {
    font-size: 0.875rem !important;
  }
  
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .category-tabs {
    flex-wrap: nowrap;
  }
  
  .tag-modal-content {
    max-height: 90vh;
  }
}
</style>

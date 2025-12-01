<template>
  <div v-if="history.length > 0" class="search-history">
    <div class="history-header">
      <span class="history-title">搜索历史</span>
      <button @click="clearAll" class="clear-btn">清除</button>
    </div>
    <div class="history-list">
      <div
        v-for="(item, index) in history"
        :key="index"
        class="history-item"
        @click="$emit('select', item)"
      >
        <svg class="history-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </svg>
        <span class="history-text">{{ item }}</span>
        <button @click.stop="removeItem(item)" class="remove-btn">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSearchHistory, clearSearchHistory, removeSearchHistory } from '../../utils/search'

defineEmits(['select'])

const history = ref([])

function loadHistory() {
  history.value = getSearchHistory()
}

function clearAll() {
  clearSearchHistory()
  history.value = []
}

function removeItem(query) {
  removeSearchHistory(query)
  loadHistory()
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.search-history {
  margin-top: var(--spacing-sm, 0.5rem);
  padding: var(--spacing-sm, 0.75rem);
  background: var(--bg-secondary, #f9fafb);
  border-radius: var(--radius-md, 8px);
  border: 1px solid var(--border-color, #e5e7eb);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm, 0.5rem);
}

.history-title {
  font-size: max(12px, 0.75rem);
  font-weight: 600;
  color: var(--text-secondary, #6b7280);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.clear-btn {
  background: transparent;
  border: none;
  color: var(--text-secondary, #6b7280);
  font-size: max(12px, 0.75rem);
  cursor: pointer;
  padding: var(--spacing-xs, 0.25rem);
  border-radius: var(--radius-sm, 4px);
  transition: all 0.2s ease;
}

.clear-btn:hover {
  color: var(--accent-color, #10a37f);
  background: rgba(16, 163, 127, 0.1);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs, 0.25rem);
}

.history-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm, 0.5rem);
  padding: var(--spacing-xs, 0.5rem);
  border-radius: var(--radius-sm, 6px);
  cursor: pointer;
  transition: all 0.2s ease;
}

.history-item:hover {
  background: rgba(0, 0, 0, 0.05);
}

.history-icon {
  flex-shrink: 0;
  color: var(--text-secondary, #6b7280);
}

.history-text {
  flex: 1;
  font-size: max(13px, 0.875rem);
  color: var(--text-primary, #111827);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.remove-btn {
  flex-shrink: 0;
  background: transparent;
  border: none;
  color: var(--text-secondary, #6b7280);
  cursor: pointer;
  padding: var(--spacing-xs, 0.25rem);
  border-radius: var(--radius-sm, 4px);
  opacity: 0;
  transition: all 0.2s ease;
}

.history-item:hover .remove-btn {
  opacity: 1;
}

.remove-btn:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}

/* 深色模式 */
[data-theme="dark"] .search-history {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
}

[data-theme="dark"] .history-item:hover {
  background: rgba(255, 255, 255, 0.05);
}
</style>

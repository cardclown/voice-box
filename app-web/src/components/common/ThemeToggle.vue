<template>
  <button 
    class="theme-toggle"
    @click="toggleTheme"
    :aria-label="currentTheme === 'light' ? '切换到深色模式' : '切换到浅色模式'"
    :title="currentTheme === 'light' ? '切换到深色模式' : '切换到浅色模式'"
  >
    <!-- 太阳图标（浅色模式） -->
    <svg 
      v-if="currentTheme === 'light'" 
      class="theme-icon sun-icon"
      width="20" 
      height="20" 
      viewBox="0 0 24 24" 
      fill="none" 
      stroke="currentColor" 
      stroke-width="2" 
      stroke-linecap="round" 
      stroke-linejoin="round"
    >
      <circle cx="12" cy="12" r="5" />
      <line x1="12" y1="1" x2="12" y2="3" />
      <line x1="12" y1="21" x2="12" y2="23" />
      <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
      <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
      <line x1="1" y1="12" x2="3" y2="12" />
      <line x1="21" y1="12" x2="23" y2="12" />
      <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
      <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
    </svg>

    <!-- 月亮图标（深色模式） -->
    <svg 
      v-else 
      class="theme-icon moon-icon"
      width="20" 
      height="20" 
      viewBox="0 0 24 24" 
      fill="none" 
      stroke="currentColor" 
      stroke-width="2" 
      stroke-linecap="round" 
      stroke-linejoin="round"
    >
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
    </svg>

    <span class="theme-label">{{ currentTheme === 'light' ? '浅色' : '深色' }}</span>
  </button>
</template>

<script setup>
import { computed } from 'vue'
import { useThemeStore } from '../../stores/theme'

const themeStore = useThemeStore()

const currentTheme = computed(() => themeStore.currentTheme)

const toggleTheme = () => {
  themeStore.toggleTheme()
}
</script>

<style scoped>
.theme-toggle {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm, 0.5rem);
  padding: var(--spacing-sm, 0.5rem) var(--spacing-md, 1rem);
  background: var(--bg-secondary, #fff);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: var(--radius-full, 999px);
  color: var(--text-primary, #111827);
  cursor: pointer;
  transition: all var(--transition-base, 200ms) ease;
  font-size: max(14px, 0.875rem);
  font-weight: 500;
  min-height: 40px;
  box-shadow: var(--shadow-sm);
}

.theme-toggle:hover {
  background: var(--bg-color, #f9fafb);
  border-color: var(--accent-color, #10a37f);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.theme-toggle:active {
  transform: translateY(0);
}

.theme-icon {
  flex-shrink: 0;
  transition: transform var(--transition-base, 200ms) ease;
}

.sun-icon {
  color: #f59e0b;
  animation: rotate 20s linear infinite;
}

.moon-icon {
  color: #6366f1;
  animation: float 3s ease-in-out infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-3px);
  }
}

.theme-label {
  white-space: nowrap;
}

/* 移动端优化 */
@media (max-width: 767px) {
  .theme-toggle {
    padding: var(--spacing-xs, 0.375rem) var(--spacing-sm, 0.75rem);
    min-height: 36px;
  }

  .theme-label {
    display: none; /* 移动端只显示图标 */
  }

  .theme-icon {
    width: 18px;
    height: 18px;
  }
}

/* 紧凑模式（可选） */
.theme-toggle.compact {
  padding: var(--spacing-xs, 0.375rem);
  min-width: 36px;
  justify-content: center;
}

.theme-toggle.compact .theme-label {
  display: none;
}

/* 焦点样式（可访问性） */
.theme-toggle:focus {
  outline: 2px solid var(--accent-color, #10a37f);
  outline-offset: 2px;
}

.theme-toggle:focus:not(:focus-visible) {
  outline: none;
}

/* 深色模式特定样式 */
[data-theme="dark"] .theme-toggle {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

[data-theme="dark"] .theme-toggle:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}
</style>

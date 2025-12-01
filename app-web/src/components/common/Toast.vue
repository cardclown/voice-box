<template>
  <Teleport to="body">
    <TransitionGroup name="toast" tag="div" class="toast-container">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        :class="['toast', `toast-${toast.type}`]"
        @click="removeToast(toast.id)"
      >
        <div class="toast-icon">
          <svg v-if="toast.type === 'success'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
            <polyline points="22 4 12 14.01 9 11.01" />
          </svg>
          <svg v-else-if="toast.type === 'error'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="15" y1="9" x2="9" y2="15" />
            <line x1="9" y1="9" x2="15" y2="15" />
          </svg>
          <svg v-else-if="toast.type === 'warning'" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" />
          </svg>
        </div>
        <div class="toast-content">
          <div class="toast-message">{{ toast.message }}</div>
          <div v-if="toast.description" class="toast-description">{{ toast.description }}</div>
        </div>
        <button class="toast-close" @click.stop="removeToast(toast.id)">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="18" y1="6" x2="6" y2="18" />
            <line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<script setup>
import { toastState, removeToast } from '../../composables/useToast'

const toasts = toastState.toasts
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: var(--spacing-lg, 1.5rem);
  right: var(--spacing-lg, 1.5rem);
  z-index: var(--z-toast, 9999);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm, 0.75rem);
  pointer-events: none;
  max-width: 420px;
}

@media (max-width: 767px) {
  .toast-container {
    top: var(--spacing-md, 1rem);
    right: var(--spacing-md, 1rem);
    left: var(--spacing-md, 1rem);
    max-width: none;
  }
}

.toast {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md, 1rem);
  padding: var(--spacing-md, 1rem) var(--spacing-lg, 1.25rem);
  background: white;
  border-radius: var(--radius-lg, 12px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(0, 0, 0, 0.05);
  pointer-events: auto;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 300px;
}

.toast:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(0, 0, 0, 0.05);
}

@media (max-width: 767px) {
  .toast {
    min-width: 0;
  }
}

/* Toast 类型样式 */
.toast-success {
  border-left: 4px solid #10b981;
}

.toast-success .toast-icon {
  color: #10b981;
}

.toast-error {
  border-left: 4px solid #ef4444;
}

.toast-error .toast-icon {
  color: #ef4444;
}

.toast-warning {
  border-left: 4px solid #f59e0b;
}

.toast-warning .toast-icon {
  color: #f59e0b;
}

.toast-info {
  border-left: 4px solid #3b82f6;
}

.toast-info .toast-icon {
  color: #3b82f6;
}

/* Toast 图标 */
.toast-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
}

/* Toast 内容 */
.toast-content {
  flex: 1;
  min-width: 0;
}

.toast-message {
  font-size: max(14px, 0.9375rem);
  font-weight: 600;
  color: var(--text-primary, #111827);
  line-height: 1.4;
  margin-bottom: 0.25rem;
}

.toast-description {
  font-size: max(13px, 0.8125rem);
  color: var(--text-secondary, #6b7280);
  line-height: 1.4;
}

/* 关闭按钮 */
.toast-close {
  flex-shrink: 0;
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  color: var(--text-secondary, #6b7280);
  transition: color 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 4px;
}

.toast-close:hover {
  color: var(--text-primary, #111827);
  background: rgba(0, 0, 0, 0.05);
}

/* 动画 */
.toast-enter-active {
  animation: toast-in 0.3s ease-out;
}

.toast-leave-active {
  animation: toast-out 0.2s ease-in;
}

@keyframes toast-in {
  from {
    opacity: 0;
    transform: translateX(100%) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

@keyframes toast-out {
  from {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateX(100%) scale(0.95);
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .toast {
    background: #1f2937;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.1);
  }
  
  .toast-message {
    color: #f3f4f6;
  }
  
  .toast-description {
    color: #9ca3af;
  }
  
  .toast-close {
    color: #9ca3af;
  }
  
  .toast-close:hover {
    color: #f3f4f6;
    background: rgba(255, 255, 255, 0.1);
  }
}

/* 主题支持 */
[data-theme="dark"] .toast {
  background: #1f2937;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.1);
}

[data-theme="dark"] .toast-message {
  color: #f3f4f6;
}

[data-theme="dark"] .toast-description {
  color: #9ca3af;
}

[data-theme="dark"] .toast-close {
  color: #9ca3af;
}

[data-theme="dark"] .toast-close:hover {
  color: #f3f4f6;
  background: rgba(255, 255, 255, 0.1);
}
</style>

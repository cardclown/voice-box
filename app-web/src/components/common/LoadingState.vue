<template>
  <div :class="['loading-state', `loading-${type}`, { fullscreen }]">
    <!-- 简单加载器 -->
    <template v-if="type === 'spinner'">
      <div class="spinner-container">
        <div class="spinner"></div>
        <p v-if="message" class="loading-message">{{ message }}</p>
      </div>
    </template>
    
    <!-- 点状加载器 -->
    <template v-else-if="type === 'dots'">
      <div class="dots-container">
        <div class="dots">
          <div class="dot"></div>
          <div class="dot"></div>
          <div class="dot"></div>
        </div>
        <p v-if="message" class="loading-message">{{ message }}</p>
      </div>
    </template>
    
    <!-- 进度条 -->
    <template v-else-if="type === 'progress'">
      <div class="progress-container">
        <p v-if="message" class="loading-message">{{ message }}</p>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: `${progress}%` }"></div>
        </div>
        <div v-if="showPercentage" class="progress-text">{{ Math.round(progress) }}%</div>
      </div>
    </template>
    
    <!-- 脉冲加载器 -->
    <template v-else-if="type === 'pulse'">
      <div class="pulse-container">
        <div class="pulse-circle"></div>
        <p v-if="message" class="loading-message">{{ message }}</p>
      </div>
    </template>
    
    <!-- 骨架屏 -->
    <template v-else-if="type === 'skeleton'">
      <div class="skeleton-container">
        <Skeleton v-for="n in skeletonCount" :key="n" :type="skeletonType" :lines="skeletonLines" />
      </div>
    </template>
    
    <!-- 自定义内容 -->
    <template v-else>
      <slot>
        <div class="spinner-container">
          <div class="spinner"></div>
          <p v-if="message" class="loading-message">{{ message }}</p>
        </div>
      </slot>
    </template>
  </div>
</template>

<script setup>
import Skeleton from './Skeleton.vue'

const props = defineProps({
  type: {
    type: String,
    default: 'spinner',
    validator: (value) => ['spinner', 'dots', 'progress', 'pulse', 'skeleton', 'custom'].includes(value)
  },
  message: {
    type: String,
    default: ''
  },
  progress: {
    type: Number,
    default: 0,
    validator: (value) => value >= 0 && value <= 100
  },
  showPercentage: {
    type: Boolean,
    default: false
  },
  fullscreen: {
    type: Boolean,
    default: false
  },
  skeletonType: {
    type: String,
    default: 'text'
  },
  skeletonCount: {
    type: Number,
    default: 3
  },
  skeletonLines: {
    type: Number,
    default: 3
  }
})
</script>

<style scoped>
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-xl, 2rem);
}

.loading-state.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  z-index: 9999;
  backdrop-filter: blur(4px);
}

[data-theme="dark"] .loading-state.fullscreen {
  background: rgba(0, 0, 0, 0.9);
}

/* 通用容器 */
.spinner-container,
.dots-container,
.progress-container,
.pulse-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md, 1rem);
}

.loading-message {
  margin: 0;
  font-size: max(14px, 0.9375rem);
  color: var(--text-secondary, #6b7280);
  text-align: center;
}

/* 旋转加载器 */
.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border-color, #e5e7eb);
  border-top: 3px solid var(--accent-color, #10a37f);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 点状加载器 */
.dots {
  display: flex;
  gap: var(--spacing-xs, 0.25rem);
}

.dot {
  width: 8px;
  height: 8px;
  background: var(--accent-color, #10a37f);
  border-radius: 50%;
  animation: dot-bounce 1.4s ease-in-out infinite both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
.dot:nth-child(3) { animation-delay: 0s; }

@keyframes dot-bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 进度条 */
.progress-bar {
  width: 200px;
  height: 4px;
  background: var(--border-color, #e5e7eb);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--accent-color, #10a37f);
  border-radius: 2px;
  transition: width 0.3s ease;
  position: relative;
}

.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent
  );
  animation: progress-shine 2s ease-in-out infinite;
}

@keyframes progress-shine {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.progress-text {
  font-size: max(12px, 0.75rem);
  font-weight: 600;
  color: var(--text-secondary, #6b7280);
}

/* 脉冲加载器 */
.pulse-circle {
  width: 40px;
  height: 40px;
  background: var(--accent-color, #10a37f);
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(16, 163, 127, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 10px rgba(16, 163, 127, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(16, 163, 127, 0);
  }
}

/* 骨架屏容器 */
.skeleton-container {
  width: 100%;
  max-width: 600px;
}

/* 响应式 */
@media (max-width: 767px) {
  .loading-state {
    padding: var(--spacing-lg, 1.5rem);
  }
  
  .spinner {
    width: 28px;
    height: 28px;
  }
  
  .pulse-circle {
    width: 36px;
    height: 36px;
  }
  
  .progress-bar {
    width: 150px;
  }
}
</style>

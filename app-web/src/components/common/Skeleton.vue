<template>
  <div :class="['skeleton', `skeleton-${type}`, { animated }]" :style="customStyle">
    <!-- 文本骨架 -->
    <template v-if="type === 'text'">
      <div v-for="line in lines" :key="line" class="skeleton-line" :style="getLineStyle(line)"></div>
    </template>
    
    <!-- 头像骨架 -->
    <div v-else-if="type === 'avatar'" class="skeleton-avatar"></div>
    
    <!-- 按钮骨架 -->
    <div v-else-if="type === 'button'" class="skeleton-button"></div>
    
    <!-- 图片骨架 -->
    <div v-else-if="type === 'image'" class="skeleton-image">
      <svg class="skeleton-image-icon" viewBox="0 0 24 24" fill="none">
        <path d="M21 19V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 1 2 2h14a2 2 0 0 0 2-2z" stroke="currentColor" stroke-width="2"/>
        <polyline points="8.5 8.5 12 12 21 3" stroke="currentColor" stroke-width="2"/>
        <circle cx="8.5" cy="8.5" r="1.5" stroke="currentColor" stroke-width="2"/>
      </svg>
    </div>
    
    <!-- 列表项骨架 -->
    <template v-else-if="type === 'list-item'">
      <div class="skeleton-list-item">
        <div class="skeleton-avatar"></div>
        <div class="skeleton-list-content">
          <div class="skeleton-line" style="width: 70%"></div>
          <div class="skeleton-line" style="width: 50%; height: 12px"></div>
        </div>
      </div>
    </template>
    
    <!-- 消息骨架 -->
    <template v-else-if="type === 'message'">
      <div class="skeleton-message">
        <div v-if="!isUser" class="skeleton-avatar"></div>
        <div class="skeleton-message-content">
          <div class="skeleton-line" style="width: 90%"></div>
          <div class="skeleton-line" style="width: 70%"></div>
          <div class="skeleton-line" style="width: 50%"></div>
        </div>
        <div v-if="isUser" class="skeleton-avatar"></div>
      </div>
    </template>
    
    <!-- 自定义内容 -->
    <slot v-else></slot>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'text',
    validator: (value) => [
      'text', 'avatar', 'button', 'image', 'list-item', 'message', 'custom'
    ].includes(value)
  },
  lines: {
    type: Number,
    default: 3
  },
  animated: {
    type: Boolean,
    default: true
  },
  width: {
    type: [String, Number],
    default: null
  },
  height: {
    type: [String, Number],
    default: null
  },
  isUser: {
    type: Boolean,
    default: false
  }
})

// 自定义样式
const customStyle = computed(() => {
  const style = {}
  if (props.width) {
    style.width = typeof props.width === 'number' ? `${props.width}px` : props.width
  }
  if (props.height) {
    style.height = typeof props.height === 'number' ? `${props.height}px` : props.height
  }
  return style
})

// 获取行样式
function getLineStyle(lineIndex) {
  const widths = ['100%', '80%', '60%', '90%', '70%']
  return {
    width: widths[lineIndex % widths.length]
  }
}
</script>

<style scoped>
.skeleton {
  display: block;
}

.skeleton.animated .skeleton-line,
.skeleton.animated .skeleton-avatar,
.skeleton.animated .skeleton-button,
.skeleton.animated .skeleton-image {
  animation: skeleton-loading 1.5s ease-in-out infinite;
}

@keyframes skeleton-loading {
  0% {
    background-position: -200px 0;
  }
  100% {
    background-position: calc(200px + 100%) 0;
  }
}

/* 基础骨架样式 */
.skeleton-line {
  height: 16px;
  background: linear-gradient(
    90deg,
    var(--skeleton-bg, #f0f0f0) 25%,
    var(--skeleton-highlight, #e0e0e0) 50%,
    var(--skeleton-bg, #f0f0f0) 75%
  );
  background-size: 200px 100%;
  border-radius: var(--radius-sm, 4px);
  margin-bottom: var(--spacing-xs, 0.5rem);
}

.skeleton-line:last-child {
  margin-bottom: 0;
}

.skeleton-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(
    90deg,
    var(--skeleton-bg, #f0f0f0) 25%,
    var(--skeleton-highlight, #e0e0e0) 50%,
    var(--skeleton-bg, #f0f0f0) 75%
  );
  background-size: 200px 100%;
  flex-shrink: 0;
}

.skeleton-button {
  height: 36px;
  background: linear-gradient(
    90deg,
    var(--skeleton-bg, #f0f0f0) 25%,
    var(--skeleton-highlight, #e0e0e0) 50%,
    var(--skeleton-bg, #f0f0f0) 75%
  );
  background-size: 200px 100%;
  border-radius: var(--radius-md, 8px);
  width: 100px;
}

.skeleton-image {
  width: 100%;
  height: 200px;
  background: linear-gradient(
    90deg,
    var(--skeleton-bg, #f0f0f0) 25%,
    var(--skeleton-highlight, #e0e0e0) 50%,
    var(--skeleton-bg, #f0f0f0) 75%
  );
  background-size: 200px 100%;
  border-radius: var(--radius-md, 8px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.skeleton-image-icon {
  width: 48px;
  height: 48px;
  color: var(--skeleton-icon, #c0c0c0);
}

/* 列表项骨架 */
.skeleton-list-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md, 1rem);
  padding: var(--spacing-md, 1rem);
}

.skeleton-list-content {
  flex: 1;
}

/* 消息骨架 */
.skeleton-message {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-md, 1rem);
  padding: var(--spacing-md, 1rem);
}

.skeleton-message-content {
  flex: 1;
  max-width: 70%;
}

/* 深色模式 */
[data-theme="dark"] .skeleton-line,
[data-theme="dark"] .skeleton-avatar,
[data-theme="dark"] .skeleton-button,
[data-theme="dark"] .skeleton-image {
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.05) 25%,
    rgba(255, 255, 255, 0.1) 50%,
    rgba(255, 255, 255, 0.05) 75%
  );
  background-size: 200px 100%;
}

[data-theme="dark"] .skeleton-image-icon {
  color: rgba(255, 255, 255, 0.2);
}

/* 响应式 */
@media (max-width: 767px) {
  .skeleton-avatar {
    width: 32px;
    height: 32px;
  }
  
  .skeleton-line {
    height: 14px;
  }
  
  .skeleton-button {
    height: 32px;
  }
  
  .skeleton-image {
    height: 150px;
  }
  
  .skeleton-image-icon {
    width: 36px;
    height: 36px;
  }
}
</style>

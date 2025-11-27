<template>
  <div class="app-shell">
    <ModuleNav
      :current-module="currentModule"
      @switch-module="$emit('switch-module', $event)"
    />
    <main class="app-content">
      <slot></slot>
    </main>
  </div>
</template>

<script setup>
import ModuleNav from './ModuleNav.vue'

defineProps({
  currentModule: {
    type: String,
    required: true
  }
})

defineEmits(['switch-module'])
</script>

<style scoped>
.app-shell {
  display: flex;
  height: 100vh;
  width: 100%;
  max-width: 100%;
  margin: 0;
  background: var(--bg-color, #fff);
  overflow: hidden;
  transition: all 0.3s ease;
}

.app-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0; /* 防止 flex 子元素溢出 */
}

/* 桌面设备 - 添加适当的边距和圆角 */
@media (min-width: 1025px) {
  .app-shell {
    height: 98vh;
    width: 98%;
    max-width: 1800px;
    margin: 1vh auto;
    border-radius: var(--radius-lg, 12px);
    box-shadow: 0 4px 24px rgba(0,0,0,0.08);
    border: 1px solid var(--border-color, #e5e7eb);
  }
}

/* 平板设备 (768px - 1024px) */
@media (min-width: 768px) and (max-width: 1024px) {
  .app-shell {
    height: 100vh;
    width: 100%;
    border-radius: 0;
  }
}

/* 移动设备 (< 768px) */
@media (max-width: 767px) {
  .app-shell {
    height: 100vh;
    width: 100%;
    border-radius: 0;
  }
}

/* 确保最小字体大小 */
.app-shell * {
  min-font-size: 14px;
}
</style>

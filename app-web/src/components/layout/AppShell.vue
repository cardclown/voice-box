<template>
  <div class="app-shell">
    <ModuleNav
      :current-module="currentModule"
      @switch-module="$emit('switch-module', $event)"
    />
    <slot></slot>
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
  height: 96vh;
  width: 90%;
  max-width: 1600px;
  margin: 2vh auto;
  background: var(--bg-color, #fff);
  border-radius: 20px;
  box-shadow: 0 0 20px rgba(0,0,0,0.05);
  overflow: hidden;
  border: 1px solid var(--border-color, #e5e7eb);
}

@media (max-width: 768px) {
  .app-shell {
    width: 100%;
    height: 100vh;
    margin: 0;
    border-radius: 0;
    border: none;
  }
}
</style>

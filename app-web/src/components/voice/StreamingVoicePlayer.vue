<template>
  <div class="streaming-voice-player">
    <!-- 播放控制 -->
    <div class="player-controls">
      <button 
        v-if="!isStreaming"
        @click="handleStart"
        class="btn-start"
        :disabled="!text || text.trim().length === 0"
      >
        <i class="icon-play"></i>
        开始播放
      </button>
      
      <button 
        v-else
        @click="handleStop"
        class="btn-stop"
      >
        <i class="icon-stop"></i>
        停止播放
      </button>
    </div>
    
    <!-- 播放进度 -->
    <div v-if="isStreaming || totalSegments > 0" class="player-progress">
      <div class="progress-bar">
        <div 
          class="progress-fill" 
          :style="{ width: getProgress() + '%' }"
        ></div>
      </div>
      <div class="progress-text">
        {{ currentSegment }} / {{ totalSegments }} 段
      </div>
    </div>
    
    <!-- 错误提示 -->
    <div v-if="error" class="player-error">
      <i class="icon-error"></i>
      {{ error }}
    </div>
    
    <!-- 音频队列状态（调试用） -->
    <div v-if="showDebug && audioQueue.length > 0" class="player-debug">
      队列中: {{ audioQueue.length }} 段
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useStreamingVoice } from '@/composables/useStreamingVoice';

const props = defineProps({
  text: {
    type: String,
    required: true
  },
  userId: {
    type: Number,
    required: true
  },
  language: {
    type: String,
    default: 'zh-CN'
  },
  showDebug: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['started', 'stopped', 'error']);

const {
  isStreaming,
  currentSegment,
  totalSegments,
  error,
  startStreaming,
  stopStreaming,
  getProgress
} = useStreamingVoice();

const handleStart = async () => {
  try {
    await startStreaming(props.text, props.userId, props.language);
    emit('started');
  } catch (err) {
    emit('error', err);
  }
};

const handleStop = async () => {
  await stopStreaming();
  emit('stopped');
};
</script>

<style scoped>
.streaming-voice-player {
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: 8px;
}

.player-controls {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.btn-start,
.btn-stop {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-start {
  background: var(--primary-color);
  color: white;
}

.btn-start:hover:not(:disabled) {
  background: var(--primary-hover);
}

.btn-start:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-stop {
  background: var(--danger-color);
  color: white;
}

.btn-stop:hover {
  background: var(--danger-hover);
}

.player-progress {
  margin-bottom: 8px;
}

.progress-bar {
  height: 4px;
  background: var(--border-color);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 4px;
}

.progress-fill {
  height: 100%;
  background: var(--primary-color);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: var(--text-secondary);
  text-align: center;
}

.player-error {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px;
  background: var(--danger-bg);
  color: var(--danger-color);
  border-radius: 4px;
  font-size: 13px;
}

.player-debug {
  margin-top: 8px;
  padding: 6px;
  background: var(--warning-bg);
  color: var(--warning-color);
  border-radius: 4px;
  font-size: 12px;
  text-align: center;
}

.icon-play::before {
  content: '▶';
}

.icon-stop::before {
  content: '⏹';
}

.icon-error::before {
  content: '⚠';
}
</style>

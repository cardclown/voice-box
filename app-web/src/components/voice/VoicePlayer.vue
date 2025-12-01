<template>
  <div class="voice-player">
    <!-- 播放按钮 -->
    <button 
      class="play-button"
      :class="{ 'playing': isPlaying }"
      @click="togglePlay"
      :disabled="loading"
    >
      <svg v-if="!isPlaying" class="icon" viewBox="0 0 24 24">
        <path d="M8 5v14l11-7z"/>
      </svg>
      <svg v-else class="icon" viewBox="0 0 24 24">
        <path d="M6 4h4v16H6V4zm8 0h4v16h-4V4z"/>
      </svg>
    </button>

    <!-- 进度条 -->
    <div class="progress-container" @click="seek">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        <div class="progress-handle" :style="{ left: progressPercent + '%' }"></div>
      </div>
      <div class="time-display">
        <span class="current-time">{{ formattedCurrentTime }}</span>
        <span class="duration">{{ formattedDuration }}</span>
      </div>
    </div>

    <!-- 音频元素 -->
    <audio 
      ref="audioElement"
      :src="audioUrl"
      @loadedmetadata="onLoadedMetadata"
      @timeupdate="onTimeUpdate"
      @ended="onEnded"
      @error="onError"
    ></audio>
  </div>
</template>

<script>
import { ref, computed, watch, onUnmounted } from 'vue';
import { useVoicePlayer } from '@/composables/useVoicePlayer';

export default {
  name: 'VoicePlayer',
  props: {
    fileId: {
      type: String,
      required: true
    },
    autoPlay: {
      type: Boolean,
      default: false
    }
  },
  emits: ['play', 'pause', 'ended', 'error'],
  setup(props, { emit }) {
    const audioElement = ref(null);
    const {
      isPlaying,
      currentTime,
      duration,
      loading,
      play,
      pause,
      seek: seekTo,
      stop
    } = useVoicePlayer(audioElement);

    // 音频URL
    const audioUrl = computed(() => {
      return `/api/voice/audio/${props.fileId}`;
    });

    // 进度百分比
    const progressPercent = computed(() => {
      if (duration.value === 0) return 0;
      return (currentTime.value / duration.value) * 100;
    });

    // 格式化时间
    const formatTime = (seconds) => {
      const mins = Math.floor(seconds / 60);
      const secs = Math.floor(seconds % 60);
      return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    const formattedCurrentTime = computed(() => formatTime(currentTime.value));
    const formattedDuration = computed(() => formatTime(duration.value));

    // 切换播放/暂停
    const togglePlay = async () => {
      try {
        if (isPlaying.value) {
          await pause();
          emit('pause');
        } else {
          await play();
          emit('play');
        }
      } catch (error) {
        console.error('播放控制失败:', error);
        emit('error', error);
      }
    };

    // 拖动进度条
    const seek = (event) => {
      const progressBar = event.currentTarget;
      const rect = progressBar.getBoundingClientRect();
      const percent = (event.clientX - rect.left) / rect.width;
      const newTime = percent * duration.value;
      seekTo(newTime);
    };

    // 音频加载完成
    const onLoadedMetadata = () => {
      console.log('音频加载完成，时长:', duration.value);
      if (props.autoPlay) {
        play();
      }
    };

    // 时间更新
    const onTimeUpdate = () => {
      // 由composable处理
    };

    // 播放结束
    const onEnded = () => {
      emit('ended');
    };

    // 播放错误
    const onError = (error) => {
      console.error('音频播放错误:', error);
      emit('error', error);
    };

    // 监听fileId变化
    watch(() => props.fileId, () => {
      stop();
    });

    // 清理
    onUnmounted(() => {
      stop();
    });

    return {
      audioElement,
      audioUrl,
      isPlaying,
      loading,
      progressPercent,
      formattedCurrentTime,
      formattedDuration,
      togglePlay,
      seek,
      onLoadedMetadata,
      onTimeUpdate,
      onEnded,
      onError
    };
  }
};
</script>

<style scoped>
.voice-player {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  background: #f5f5f5;
  border-radius: 8px;
  max-width: 400px;
}

.play-button {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: var(--color-primary, #4CAF50);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.play-button:hover:not(:disabled) {
  background: var(--color-primary-dark, #45a049);
  transform: scale(1.05);
}

.play-button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.play-button.playing {
  background: #ff9800;
}

.icon {
  width: 20px;
  height: 20px;
  fill: currentColor;
}

.progress-container {
  flex: 1;
  cursor: pointer;
}

.progress-bar {
  position: relative;
  height: 4px;
  background: #ddd;
  border-radius: 2px;
  margin-bottom: 4px;
}

.progress-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  background: var(--color-primary, #4CAF50);
  border-radius: 2px;
  transition: width 0.1s linear;
}

.progress-handle {
  position: absolute;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 12px;
  height: 12px;
  background: var(--color-primary, #4CAF50);
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: left 0.1s linear;
}

.time-display {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #666;
  font-family: monospace;
}

audio {
  display: none;
}
</style>

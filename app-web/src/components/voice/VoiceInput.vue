<template>
  <div class="voice-input">
    <!-- 录音按钮 -->
    <button 
      class="voice-button"
      :class="{ 'recording': isRecording, 'disabled': !hasPermission }"
      @click="toggleRecording"
      :disabled="!hasPermission"
      :title="buttonTitle"
    >
      <svg v-if="!isRecording" class="icon" viewBox="0 0 24 24">
        <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
        <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
      </svg>
      <svg v-else class="icon pulse" viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="8"/>
      </svg>
    </button>

    <!-- 录音状态显示 -->
    <div v-if="isRecording" class="recording-status">
      <div class="waveform">
        <span v-for="i in 5" :key="i" class="wave-bar" :style="{ animationDelay: `${i * 0.1}s` }"></span>
      </div>
      <span class="duration">{{ formattedDuration }}</span>
    </div>

    <!-- 识别结果编辑 -->
    <div v-if="recognizedText" class="recognized-text">
      <textarea 
        v-model="editableText"
        class="text-editor"
        placeholder="识别的文本..."
        @keydown.enter.ctrl="sendMessage"
      ></textarea>
      <div class="actions">
        <button class="btn-send" @click="sendMessage">发送</button>
        <button class="btn-cancel" @click="cancelRecording">取消</button>
      </div>
    </div>

    <!-- 权限提示 -->
    <div v-if="showPermissionPrompt" class="permission-prompt">
      <p>需要麦克风权限才能使用语音输入</p>
      <button @click="requestPermission">授予权限</button>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useVoiceInput } from '@/composables/useVoiceInput';
import { useLanguagePreference } from '@/composables/useLanguagePreference';

export default {
  name: 'VoiceInput',
  props: {
    userId: {
      type: Number,
      required: true
    },
    sessionId: {
      type: Number,
      required: true
    },
    language: {
      type: String,
      default: ''  // 空字符串表示使用用户偏好
    },
    maxDuration: {
      type: Number,
      default: 300 // 5分钟
    }
  },
  emits: ['message-sent', 'error'],
  setup(props, { emit }) {
    // 语言偏好管理
    const { currentLanguage, getSTTLanguageCode } = useLanguagePreference();
    
    // 使用的语言（优先使用props，否则使用用户偏好）
    const effectiveLanguage = computed(() => {
      return props.language || currentLanguage.value;
    });
    
    // 获取STT语言代码
    const sttLanguage = computed(() => {
      return getSTTLanguageCode(effectiveLanguage.value);
    });
    const {
      isRecording,
      hasPermission,
      recognizedText,
      duration,
      startRecording,
      stopRecording,
      requestPermission: requestMicPermission
    } = useVoiceInput();

    const editableText = ref('');
    const showPermissionPrompt = ref(false);

    // 格式化时长显示
    const formattedDuration = computed(() => {
      const minutes = Math.floor(duration.value / 60);
      const seconds = duration.value % 60;
      return `${minutes}:${seconds.toString().padStart(2, '0')}`;
    });

    // 按钮标题
    const buttonTitle = computed(() => {
      if (!hasPermission.value) return '需要麦克风权限';
      if (isRecording.value) return '点击停止录音';
      return '点击开始录音';
    });

    // 切换录音状态
    const toggleRecording = async () => {
      if (!hasPermission.value) {
        showPermissionPrompt.value = true;
        return;
      }

      if (isRecording.value) {
        try {
          const result = await stopRecording(props.userId, props.sessionId, sttLanguage.value);
          if (result && result.recognizedText) {
            editableText.value = result.recognizedText;
          }
        } catch (error) {
          console.error('停止录音失败:', error);
          emit('error', error);
        }
      } else {
        try {
          await startRecording();
        } catch (error) {
          console.error('开始录音失败:', error);
          emit('error', error);
        }
      }
    };

    // 请求权限
    const requestPermission = async () => {
      try {
        await requestMicPermission();
        showPermissionPrompt.value = false;
      } catch (error) {
        console.error('权限请求失败:', error);
        emit('error', error);
      }
    };

    // 发送消息
    const sendMessage = () => {
      if (editableText.value.trim()) {
        emit('message-sent', {
          text: editableText.value,
          isVoice: true
        });
        editableText.value = '';
      }
    };

    // 取消录音
    const cancelRecording = () => {
      editableText.value = '';
    };

    // 检查录音时长限制
    const checkDuration = () => {
      if (isRecording.value && duration.value >= props.maxDuration) {
        toggleRecording();
        emit('error', new Error(`录音时长超过限制（${props.maxDuration}秒）`));
      }
    };

    // 定时检查时长
    let durationCheckInterval;
    onMounted(() => {
      durationCheckInterval = setInterval(checkDuration, 1000);
    });

    onUnmounted(() => {
      if (durationCheckInterval) {
        clearInterval(durationCheckInterval);
      }
    });

    return {
      isRecording,
      hasPermission,
      recognizedText,
      editableText,
      formattedDuration,
      buttonTitle,
      showPermissionPrompt,
      toggleRecording,
      requestPermission,
      sendMessage,
      cancelRecording
    };
  }
};
</script>

<style scoped>
.voice-input {
  position: relative;
}

.voice-button {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  background: var(--color-primary, #4CAF50);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.voice-button:hover:not(.disabled) {
  background: var(--color-primary-dark, #45a049);
  transform: scale(1.05);
}

.voice-button.recording {
  background: #f44336;
  animation: pulse 1.5s ease-in-out infinite;
}

.voice-button.disabled {
  background: #ccc;
  cursor: not-allowed;
}

.icon {
  width: 24px;
  height: 24px;
  fill: currentColor;
}

.icon.pulse {
  animation: pulse 1s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

.recording-status {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.waveform {
  display: flex;
  align-items: center;
  gap: 3px;
  height: 24px;
}

.wave-bar {
  width: 3px;
  height: 100%;
  background: var(--color-primary, #4CAF50);
  border-radius: 2px;
  animation: wave 1s ease-in-out infinite;
}

@keyframes wave {
  0%, 100% {
    transform: scaleY(0.3);
  }
  50% {
    transform: scaleY(1);
  }
}

.duration {
  font-size: 14px;
  color: #666;
  font-family: monospace;
}

.recognized-text {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 8px;
  width: 300px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 12px;
  z-index: 10;
}

.text-editor {
  width: 100%;
  min-height: 80px;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  resize: vertical;
  font-family: inherit;
}

.text-editor:focus {
  outline: none;
  border-color: var(--color-primary, #4CAF50);
}

.actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.btn-send,
.btn-cancel {
  flex: 1;
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-send {
  background: var(--color-primary, #4CAF50);
  color: white;
}

.btn-send:hover {
  background: var(--color-primary-dark, #45a049);
}

.btn-cancel {
  background: #f5f5f5;
  color: #666;
}

.btn-cancel:hover {
  background: #e0e0e0;
}

.permission-prompt {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 8px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 10;
}

.permission-prompt p {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #666;
}

.permission-prompt button {
  padding: 8px 16px;
  background: var(--color-primary, #4CAF50);
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.permission-prompt button:hover {
  background: var(--color-primary-dark, #45a049);
}
</style>

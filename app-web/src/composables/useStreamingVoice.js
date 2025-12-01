import { ref, onUnmounted } from 'vue';

/**
 * 流式语音播放 Composable
 */
export function useStreamingVoice() {
  const isStreaming = ref(false);
  const currentSegment = ref(0);
  const totalSegments = ref(0);
  const audioQueue = ref([]);
  const currentAudio = ref(null);
  const error = ref(null);
  
  let eventSource = null;
  let sessionId = null;
  
  /**
   * 开始流式播放
   */
  const startStreaming = async (text, userId, language = 'zh-CN') => {
    try {
      isStreaming.value = true;
      error.value = null;
      currentSegment.value = 0;
      totalSegments.value = 0;
      audioQueue.value = [];
      
      // 创建EventSource连接
      const url = `/api/voice/stream/synthesize?text=${encodeURIComponent(text)}&userId=${userId}&language=${language}`;
      eventSource = new EventSource(url);
      
      // 监听开始事件
      eventSource.addEventListener('start', (event) => {
        const data = JSON.parse(event.data);
        totalSegments.value = data.totalSegments;
        console.log('流式TTS开始:', data);
      });
      
      // 监听音频段事件
      eventSource.addEventListener('segment', (event) => {
        const data = JSON.parse(event.data);
        console.log('收到音频段:', data);
        
        // 添加到播放队列
        audioQueue.value.push({
          index: data.index,
          url: data.audioUrl,
          text: data.text,
          duration: data.duration
        });
        
        // 如果当前没有播放，开始播放
        if (!currentAudio.value) {
          playNextSegment();
        }
      });
      
      // 监听完成事件
      eventSource.addEventListener('complete', (event) => {
        const data = JSON.parse(event.data);
        console.log('流式TTS完成:', data);
        
        // 保存完整音频URL
        sessionId = data.audioUrl;
        
        // 关闭连接
        if (eventSource) {
          eventSource.close();
          eventSource = null;
        }
      });
      
      // 监听错误事件
      eventSource.addEventListener('error', (event) => {
        const data = JSON.parse(event.data);
        console.error('流式TTS错误:', data);
        error.value = data.message;
        stopStreaming();
      });
      
      // 监听连接错误
      eventSource.onerror = (err) => {
        console.error('SSE连接错误:', err);
        error.value = '连接失败';
        stopStreaming();
      };
      
    } catch (err) {
      console.error('启动流式播放失败:', err);
      error.value = err.message;
      isStreaming.value = false;
    }
  };
  
  /**
   * 播放下一个音频段
   */
  const playNextSegment = () => {
    if (audioQueue.value.length === 0) {
      // 队列为空，检查是否已完成
      if (!isStreaming.value) {
        currentAudio.value = null;
      }
      return;
    }
    
    const segment = audioQueue.value.shift();
    currentSegment.value = segment.index;
    
    // 创建音频元素
    const audio = new Audio(segment.url);
    currentAudio.value = audio;
    
    // 监听播放结束
    audio.onended = () => {
      playNextSegment();
    };
    
    // 监听错误
    audio.onerror = (err) => {
      console.error('音频播放错误:', err);
      // 继续播放下一段
      playNextSegment();
    };
    
    // 开始播放
    audio.play().catch(err => {
      console.error('播放失败:', err);
      playNextSegment();
    });
  };
  
  /**
   * 停止流式播放
   */
  const stopStreaming = async () => {
    isStreaming.value = false;
    
    // 停止当前音频
    if (currentAudio.value) {
      currentAudio.value.pause();
      currentAudio.value = null;
    }
    
    // 清空队列
    audioQueue.value = [];
    
    // 关闭SSE连接
    if (eventSource) {
      eventSource.close();
      eventSource = null;
    }
    
    // 通知后端停止
    if (sessionId) {
      try {
        await fetch(`/api/voice/stream/stop?sessionId=${sessionId}`, {
          method: 'POST'
        });
      } catch (err) {
        console.error('停止流式播放失败:', err);
      }
    }
  };
  
  /**
   * 暂停播放
   */
  const pauseStreaming = () => {
    if (currentAudio.value) {
      currentAudio.value.pause();
    }
  };
  
  /**
   * 恢复播放
   */
  const resumeStreaming = () => {
    if (currentAudio.value) {
      currentAudio.value.play().catch(err => {
        console.error('恢复播放失败:', err);
      });
    }
  };
  
  /**
   * 获取播放进度
   */
  const getProgress = () => {
    if (totalSegments.value === 0) return 0;
    return (currentSegment.value / totalSegments.value) * 100;
  };
  
  // 组件卸载时清理
  onUnmounted(() => {
    stopStreaming();
  });
  
  return {
    isStreaming,
    currentSegment,
    totalSegments,
    error,
    startStreaming,
    stopStreaming,
    pauseStreaming,
    resumeStreaming,
    getProgress
  };
}

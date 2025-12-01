import { ref, watch, onUnmounted } from 'vue';

/**
 * 语音播放 Composable
 * 处理音频播放控制和状态管理
 */
export function useVoicePlayer(audioElementRef) {
  const isPlaying = ref(false);
  const currentTime = ref(0);
  const duration = ref(0);
  const loading = ref(false);

  /**
   * 播放音频
   */
  const play = async () => {
    if (!audioElementRef.value) {
      throw new Error('音频元素未就绪');
    }

    try {
      loading.value = true;
      await audioElementRef.value.play();
      isPlaying.value = true;
      console.log('开始播放');
    } catch (error) {
      console.error('播放失败:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 暂停音频
   */
  const pause = () => {
    if (!audioElementRef.value) return;

    audioElementRef.value.pause();
    isPlaying.value = false;
    console.log('暂停播放');
  };

  /**
   * 停止音频
   */
  const stop = () => {
    if (!audioElementRef.value) return;

    audioElementRef.value.pause();
    audioElementRef.value.currentTime = 0;
    isPlaying.value = false;
    currentTime.value = 0;
    console.log('停止播放');
  };

  /**
   * 跳转到指定时间
   */
  const seek = (time) => {
    if (!audioElementRef.value) return;

    audioElementRef.value.currentTime = Math.max(0, Math.min(time, duration.value));
    currentTime.value = audioElementRef.value.currentTime;
  };

  /**
   * 设置音量
   */
  const setVolume = (volume) => {
    if (!audioElementRef.value) return;

    audioElementRef.value.volume = Math.max(0, Math.min(1, volume));
  };

  /**
   * 设置播放速度
   */
  const setPlaybackRate = (rate) => {
    if (!audioElementRef.value) return;

    audioElementRef.value.playbackRate = Math.max(0.5, Math.min(2, rate));
  };

  // 监听音频元素事件
  watch(audioElementRef, (newElement) => {
    if (!newElement) return;

    // 时间更新
    newElement.addEventListener('timeupdate', () => {
      currentTime.value = newElement.currentTime;
    });

    // 加载元数据
    newElement.addEventListener('loadedmetadata', () => {
      duration.value = newElement.duration;
      console.log('音频时长:', duration.value);
    });

    // 播放结束
    newElement.addEventListener('ended', () => {
      isPlaying.value = false;
      currentTime.value = 0;
      console.log('播放结束');
    });

    // 播放错误
    newElement.addEventListener('error', (error) => {
      console.error('音频错误:', error);
      isPlaying.value = false;
      loading.value = false;
    });

    // 等待数据
    newElement.addEventListener('waiting', () => {
      loading.value = true;
    });

    // 可以播放
    newElement.addEventListener('canplay', () => {
      loading.value = false;
    });
  }, { immediate: true });

  // 清理
  onUnmounted(() => {
    stop();
  });

  return {
    isPlaying,
    currentTime,
    duration,
    loading,
    play,
    pause,
    stop,
    seek,
    setVolume,
    setPlaybackRate
  };
}

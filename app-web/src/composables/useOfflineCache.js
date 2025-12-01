import { ref, onMounted, onUnmounted } from 'vue';

/**
 * 离线缓存 Composable
 * 用于在网络中断时缓存语音文件
 */
export function useOfflineCache() {
  const isOnline = ref(navigator.onLine);
  const pendingUploads = ref([]);
  const cacheSize = ref(0);
  
  const CACHE_KEY = 'voiceOfflineCache';
  const MAX_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
  
  /**
   * 监听网络状态
   */
  const handleOnline = () => {
    isOnline.value = true;
    console.log('网络已恢复，开始上传缓存的语音文件');
    uploadPendingFiles();
  };
  
  const handleOffline = () => {
    isOnline.value = false;
    console.log('网络已断开，语音文件将被缓存');
  };
  
  onMounted(() => {
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    loadPendingUploads();
  });
  
  onUnmounted(() => {
    window.removeEventListener('online', handleOnline);
    window.removeEventListener('offline', handleOffline);
  });
  
  /**
   * 缓存语音文件
   */
  const cacheAudioFile = async (audioBlob, metadata = {}) => {
    try {
      // 检查缓存大小
      if (cacheSize.value + audioBlob.size > MAX_CACHE_SIZE) {
        throw new Error('缓存空间不足');
      }
      
      // 转换为Base64
      const base64Data = await blobToBase64(audioBlob);
      
      // 创建缓存项
      const cacheItem = {
        id: generateId(),
        data: base64Data,
        size: audioBlob.size,
        type: audioBlob.type,
        metadata: {
          ...metadata,
          cachedAt: new Date().toISOString()
        }
      };
      
      // 添加到待上传列表
      pendingUploads.value.push(cacheItem);
      cacheSize.value += audioBlob.size;
      
      // 保存到localStorage
      savePendingUploads();
      
      console.log('语音文件已缓存:', cacheItem.id);
      return cacheItem.id;
      
    } catch (error) {
      console.error('缓存语音文件失败:', error);
      throw error;
    }
  };
  
  /**
   * 上传待处理的文件
   */
  const uploadPendingFiles = async () => {
    if (!isOnline.value || pendingUploads.value.length === 0) {
      return;
    }
    
    console.log(`开始上传${pendingUploads.value.length}个缓存文件`);
    
    const uploadPromises = pendingUploads.value.map(async (item) => {
      try {
        // 转换回Blob
        const blob = await base64ToBlob(item.data, item.type);
        
        // 上传文件
        await uploadAudioFile(blob, item.metadata);
        
        // 上传成功，从列表中移除
        removeCacheItem(item.id);
        
        console.log('缓存文件上传成功:', item.id);
        return { success: true, id: item.id };
        
      } catch (error) {
        console.error('缓存文件上传失败:', item.id, error);
        return { success: false, id: item.id, error };
      }
    });
    
    const results = await Promise.allSettled(uploadPromises);
    
    // 保存更新后的列表
    savePendingUploads();
    
    return results;
  };
  
  /**
   * 上传音频文件到服务器
   */
  const uploadAudioFile = async (blob, metadata) => {
    const formData = new FormData();
    formData.append('file', blob, 'audio.mp3');
    formData.append('userId', metadata.userId || 1);
    formData.append('sessionId', metadata.sessionId || 1);
    formData.append('language', metadata.language || 'zh-CN');
    
    const response = await fetch('/api/voice/upload', {
      method: 'POST',
      body: formData
    });
    
    if (!response.ok) {
      throw new Error(`上传失败: ${response.statusText}`);
    }
    
    return await response.json();
  };
  
  /**
   * 移除缓存项
   */
  const removeCacheItem = (id) => {
    const index = pendingUploads.value.findIndex(item => item.id === id);
    if (index !== -1) {
      const item = pendingUploads.value[index];
      cacheSize.value -= item.size;
      pendingUploads.value.splice(index, 1);
    }
  };
  
  /**
   * 清除所有缓存
   */
  const clearCache = () => {
    pendingUploads.value = [];
    cacheSize.value = 0;
    localStorage.removeItem(CACHE_KEY);
  };
  
  /**
   * 加载待上传列表
   */
  const loadPendingUploads = () => {
    try {
      const cached = localStorage.getItem(CACHE_KEY);
      if (cached) {
        const items = JSON.parse(cached);
        pendingUploads.value = items;
        cacheSize.value = items.reduce((sum, item) => sum + item.size, 0);
        
        console.log(`加载了${items.length}个缓存文件，总大小: ${formatSize(cacheSize.value)}`);
        
        // 如果在线，尝试上传
        if (isOnline.value) {
          uploadPendingFiles();
        }
      }
    } catch (error) {
      console.error('加载缓存失败:', error);
    }
  };
  
  /**
   * 保存待上传列表
   */
  const savePendingUploads = () => {
    try {
      localStorage.setItem(CACHE_KEY, JSON.stringify(pendingUploads.value));
    } catch (error) {
      console.error('保存缓存失败:', error);
      
      // 如果存储空间不足，清除旧的缓存
      if (error.name === 'QuotaExceededError') {
        if (pendingUploads.value.length > 0) {
          pendingUploads.value.shift(); // 移除最旧的
          savePendingUploads(); // 重试
        }
      }
    }
  };
  
  /**
   * Blob转Base64
   */
  const blobToBase64 = (blob) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onloadend = () => resolve(reader.result);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  };
  
  /**
   * Base64转Blob
   */
  const base64ToBlob = async (base64Data, type) => {
    const response = await fetch(base64Data);
    return await response.blob();
  };
  
  /**
   * 生成唯一ID
   */
  const generateId = () => {
    return `${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  };
  
  /**
   * 格式化文件大小
   */
  const formatSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };
  
  /**
   * 获取缓存统计
   */
  const getCacheStats = () => {
    return {
      count: pendingUploads.value.length,
      size: cacheSize.value,
      sizeFormatted: formatSize(cacheSize.value),
      maxSize: MAX_CACHE_SIZE,
      maxSizeFormatted: formatSize(MAX_CACHE_SIZE),
      usage: (cacheSize.value / MAX_CACHE_SIZE * 100).toFixed(2) + '%'
    };
  };
  
  return {
    isOnline,
    pendingUploads,
    cacheSize,
    cacheAudioFile,
    uploadPendingFiles,
    clearCache,
    getCacheStats
  };
}

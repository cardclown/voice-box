import { ref } from 'vue';
import { useToast } from './useToast';

/**
 * 语音错误处理 Composable
 */
export function useVoiceError() {
  const lastError = ref(null);
  const toast = useToast();
  
  /**
   * 错误类型映射
   */
  const ERROR_TYPES = {
    PERMISSION_DENIED: {
      title: '权限被拒绝',
      message: '请授予麦克风权限以使用语音功能',
      action: '在浏览器设置中允许麦克风权限',
      severity: 'warning'
    },
    FILE_TOO_LARGE: {
      title: '文件过大',
      message: '音频文件过大，请录制较短的语音（最大10MB）',
      action: '录制较短的语音或压缩音频文件',
      severity: 'error'
    },
    UNSUPPORTED_FORMAT: {
      title: '格式不支持',
      message: '不支持的音频格式，请使用MP3、WAV或OGG格式',
      action: '使用支持的音频格式',
      severity: 'error'
    },
    DURATION_EXCEEDED: {
      title: '时长超限',
      message: '录音时长超过限制（最长5分钟）',
      action: '分段录制或使用文字输入',
      severity: 'warning'
    },
    STT_SERVICE_ERROR: {
      title: '语音识别失败',
      message: '语音识别暂时不可用，请稍后重试或使用文字输入',
      action: '稍后重试或使用文字输入',
      severity: 'error',
      retryable: true
    },
    TTS_SERVICE_ERROR: {
      title: '语音合成失败',
      message: '语音合成暂时不可用，将仅显示文本回复',
      action: '稍后重试或查看文本回复',
      severity: 'warning',
      retryable: true
    },
    STORAGE_ERROR: {
      title: '存储失败',
      message: '保存语音文件失败，请检查存储空间',
      action: '清理存储空间或联系管理员',
      severity: 'error',
      retryable: true
    },
    TIMEOUT: {
      title: '网络超时',
      message: '网络连接超时，请检查网络连接后重试',
      action: '检查网络连接后重试',
      severity: 'error',
      retryable: true
    },
    DEVICE_ERROR: {
      title: '设备故障',
      message: '麦克风设备故障，请检查设备连接',
      action: '检查麦克风连接或使用其他设备',
      severity: 'error'
    },
    INTERNAL_ERROR: {
      title: '处理失败',
      message: '处理失败，请重试',
      action: '刷新页面或联系技术支持',
      severity: 'error',
      retryable: true
    }
  };
  
  /**
   * 处理错误
   */
  const handleError = (error, context = {}) => {
    console.error('语音错误:', error, context);
    
    // 解析错误
    const errorInfo = parseError(error);
    
    // 保存最后的错误
    lastError.value = {
      ...errorInfo,
      context,
      timestamp: new Date()
    };
    
    // 显示错误提示
    showErrorToast(errorInfo);
    
    // 记录错误（可选：发送到服务器）
    logError(errorInfo, context);
    
    return errorInfo;
  };
  
  /**
   * 解析错误
   */
  const parseError = (error) => {
    // 如果是API响应错误
    if (error.response && error.response.data) {
      const data = error.response.data;
      const errorCode = data.errorCode || 'INTERNAL_ERROR';
      const errorType = ERROR_TYPES[errorCode] || ERROR_TYPES.INTERNAL_ERROR;
      
      return {
        code: errorCode,
        message: data.userMessage || data.errorMessage || errorType.message,
        action: data.suggestedAction || errorType.action,
        severity: errorType.severity,
        retryable: data.retryable !== undefined ? data.retryable : errorType.retryable,
        httpStatus: data.httpStatus || error.response.status
      };
    }
    
    // 如果是浏览器错误
    if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
      return {
        code: 'PERMISSION_DENIED',
        ...ERROR_TYPES.PERMISSION_DENIED
      };
    }
    
    if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
      return {
        code: 'DEVICE_ERROR',
        ...ERROR_TYPES.DEVICE_ERROR
      };
    }
    
    if (error.name === 'NetworkError' || error.message?.includes('network')) {
      return {
        code: 'TIMEOUT',
        ...ERROR_TYPES.TIMEOUT
      };
    }
    
    // 默认错误
    return {
      code: 'INTERNAL_ERROR',
      message: error.message || ERROR_TYPES.INTERNAL_ERROR.message,
      ...ERROR_TYPES.INTERNAL_ERROR
    };
  };
  
  /**
   * 显示错误提示
   */
  const showErrorToast = (errorInfo) => {
    const toastOptions = {
      duration: errorInfo.retryable ? 5000 : 3000,
      type: errorInfo.severity
    };
    
    // 显示主要错误消息
    toast.show(errorInfo.message, toastOptions);
    
    // 如果有建议操作，延迟显示
    if (errorInfo.action) {
      setTimeout(() => {
        toast.show(`建议: ${errorInfo.action}`, {
          ...toastOptions,
          type: 'info'
        });
      }, 500);
    }
  };
  
  /**
   * 记录错误
   */
  const logError = (errorInfo, context) => {
    // 构建错误日志
    const logEntry = {
      timestamp: new Date().toISOString(),
      errorCode: errorInfo.code,
      message: errorInfo.message,
      context,
      userAgent: navigator.userAgent,
      url: window.location.href
    };
    
    // 保存到本地存储（用于调试）
    try {
      const logs = JSON.parse(localStorage.getItem('voiceErrorLogs') || '[]');
      logs.push(logEntry);
      
      // 只保留最近100条
      if (logs.length > 100) {
        logs.shift();
      }
      
      localStorage.setItem('voiceErrorLogs', JSON.stringify(logs));
    } catch (e) {
      console.error('保存错误日志失败:', e);
    }
    
    // TODO: 发送到服务器（可选）
    // sendErrorToServer(logEntry);
  };
  
  /**
   * 清除错误
   */
  const clearError = () => {
    lastError.value = null;
  };
  
  /**
   * 获取错误日志
   */
  const getErrorLogs = () => {
    try {
      return JSON.parse(localStorage.getItem('voiceErrorLogs') || '[]');
    } catch (e) {
      return [];
    }
  };
  
  /**
   * 清除错误日志
   */
  const clearErrorLogs = () => {
    localStorage.removeItem('voiceErrorLogs');
  };
  
  /**
   * 处理权限错误
   */
  const handlePermissionError = () => {
    return handleError(new Error('Permission denied'), {
      type: 'permission',
      source: 'microphone'
    });
  };
  
  /**
   * 处理网络错误
   */
  const handleNetworkError = (error) => {
    return handleError(error, {
      type: 'network',
      online: navigator.onLine
    });
  };
  
  /**
   * 处理服务错误
   */
  const handleServiceError = (error, serviceName) => {
    return handleError(error, {
      type: 'service',
      service: serviceName
    });
  };
  
  /**
   * 判断是否可以重试
   */
  const canRetry = () => {
    return lastError.value?.retryable === true;
  };
  
  return {
    lastError,
    handleError,
    clearError,
    getErrorLogs,
    clearErrorLogs,
    handlePermissionError,
    handleNetworkError,
    handleServiceError,
    canRetry
  };
}

/**
 * Toast Composable (简化版，如果项目中已有可以使用现有的)
 */
function useToast() {
  return {
    show: (message, options = {}) => {
      // 这里应该使用项目中的Toast组件
      // 临时使用console.log
      console.log(`[Toast ${options.type || 'info'}]:`, message);
      
      // 如果有全局Toast组件，可以这样调用：
      // window.$toast?.show(message, options);
    }
  };
}

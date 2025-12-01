import { ref, onUnmounted } from 'vue';
import apiClient from '@/services/apiClient';

/**
 * 语音输入 Composable
 * 处理录音、权限管理和语音识别
 */
export function useVoiceInput() {
  const isRecording = ref(false);
  const hasPermission = ref(false);
  const recognizedText = ref('');
  const duration = ref(0);
  
  let mediaRecorder = null;
  let audioChunks = [];
  let durationTimer = null;
  let audioStream = null;

  /**
   * 请求麦克风权限
   */
  const requestPermission = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      hasPermission.value = true;
      // 立即停止流，只是为了获取权限
      stream.getTracks().forEach(track => track.stop());
      return true;
    } catch (error) {
      console.error('麦克风权限被拒绝:', error);
      hasPermission.value = false;
      throw new Error('麦克风权限被拒绝');
    }
  };

  /**
   * 开始录音
   */
  const startRecording = async () => {
    try {
      // 获取音频流
      audioStream = await navigator.mediaDevices.getUserMedia({ 
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          sampleRate: 16000
        } 
      });

      // 创建MediaRecorder
      mediaRecorder = new MediaRecorder(audioStream, {
        mimeType: 'audio/webm;codecs=opus'
      });

      audioChunks = [];

      // 监听数据
      mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          audioChunks.push(event.data);
        }
      };

      // 开始录音
      mediaRecorder.start();
      isRecording.value = true;
      duration.value = 0;

      // 启动计时器
      durationTimer = setInterval(() => {
        duration.value++;
      }, 1000);

      console.log('开始录音');
    } catch (error) {
      console.error('开始录音失败:', error);
      throw error;
    }
  };

  /**
   * 停止录音并上传
   */
  const stopRecording = async (userId, sessionId, language = 'zh-CN') => {
    return new Promise((resolve, reject) => {
      if (!mediaRecorder || !isRecording.value) {
        reject(new Error('未在录音中'));
        return;
      }

      mediaRecorder.onstop = async () => {
        try {
          // 停止计时器
          if (durationTimer) {
            clearInterval(durationTimer);
            durationTimer = null;
          }

          // 停止音频流
          if (audioStream) {
            audioStream.getTracks().forEach(track => track.stop());
            audioStream = null;
          }

          // 创建音频Blob
          const audioBlob = new Blob(audioChunks, { type: 'audio/webm' });
          console.log('录音完成，大小:', audioBlob.size, 'bytes');

          // 上传并识别
          const result = await uploadAndRecognize(audioBlob, userId, sessionId, language);
          
          recognizedText.value = result.recognizedText || '';
          isRecording.value = false;

          resolve(result);
        } catch (error) {
          console.error('处理录音失败:', error);
          isRecording.value = false;
          reject(error);
        }
      };

      mediaRecorder.stop();
    });
  };

  /**
   * 上传音频并识别
   */
  const uploadAndRecognize = async (audioBlob, userId, sessionId, language) => {
    try {
      // 创建FormData
      const formData = new FormData();
      formData.append('file', audioBlob, 'recording.webm');  // 后端期望的参数名是'file'
      formData.append('userId', userId);
      formData.append('sessionId', sessionId);
      formData.append('language', language);

      // 上传到后端
      const response = await apiClient.post('/api/voice/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });

      if (response.data.success) {
        return {
          recognizedText: response.data.recognizedText || '',
          fileId: response.data.fileId,
          duration: response.data.duration
        };
      } else {
        throw new Error(response.data.errorMessage || '语音识别失败');
      }
    } catch (error) {
      console.error('上传音频失败:', error);
      throw error;
    }
  };

  /**
   * 取消录音
   */
  const cancelRecording = () => {
    if (mediaRecorder && isRecording.value) {
      mediaRecorder.stop();
      isRecording.value = false;
      recognizedText.value = '';
      
      if (durationTimer) {
        clearInterval(durationTimer);
        durationTimer = null;
      }

      if (audioStream) {
        audioStream.getTracks().forEach(track => track.stop());
        audioStream = null;
      }
    }
  };

  // 检查初始权限状态
  navigator.permissions?.query({ name: 'microphone' }).then(result => {
    hasPermission.value = result.state === 'granted';
    result.onchange = () => {
      hasPermission.value = result.state === 'granted';
    };
  }).catch(() => {
    // 某些浏览器不支持permissions API
    hasPermission.value = false;
  });

  // 清理
  onUnmounted(() => {
    cancelRecording();
  });

  return {
    isRecording,
    hasPermission,
    recognizedText,
    duration,
    startRecording,
    stopRecording,
    cancelRecording,
    requestPermission
  };
}

<template>
  <div class="video-module">
    <div class="video-card">
      <h2>视频转换接口</h2>
      <p class="subtitle">上传视频文件，后端会执行示例转换流程并返回结果。</p>
        
      <div class="upload-area">
        <input ref="videoInput" type="file" accept="video/*" class="hidden-input" @change="handleVideoSelect" />
        <button class="upload-btn" @click="videoInput?.click()">选择视频文件</button>
        <p v-if="selectedVideo" class="file-name">已选择：{{ selectedVideo.name }}</p>
      </div>

      <button class="convert-btn" @click="convertVideo" :disabled="!selectedVideo || conversionLoading">
        {{ conversionLoading ? '转换中...' : '开始转换' }}
      </button>

      <div class="result-box" v-if="conversionMessage">
        <p>{{ conversionMessage }}</p>
        <p v-if="conversionOutput">
          输出路径：
          <code>{{ conversionOutput }}</code>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const API_BASE = 'http://localhost:10088/api'

const videoInput = ref(null)
const selectedVideo = ref(null)
const conversionMessage = ref('')
const conversionOutput = ref('')
const conversionLoading = ref(false)

const handleVideoSelect = (event) => {
  const [file] = event.target.files
  selectedVideo.value = file || null
  conversionMessage.value = ''
  conversionOutput.value = ''
}

const convertVideo = async () => {
  if (!selectedVideo.value) return
  conversionLoading.value = true
  conversionMessage.value = ''
  conversionOutput.value = ''

  const formData = new FormData()
  formData.append('file', selectedVideo.value)

  try {
    const res = await fetch(`${API_BASE}/video/convert`, {
      method: 'POST',
      body: formData
    })
    if (!res.ok) throw new Error('视频转换失败')
    const data = await res.json()
    conversionMessage.value = data.message
    conversionOutput.value = data.outputPath
  } catch (err) {
    console.error(err)
    conversionMessage.value = '转换失败，请查看后端日志。'
  } finally {
    conversionLoading.value = false
  }
}
</script>

<style scoped>
.video-module {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-color, #f9fafb);
}

.video-card {
  background: var(--bg-color, #fff);
  padding: 2.5rem;
  border-radius: 16px;
  width: min(500px, 90%);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
}

.video-card h2 {
  margin: 0 0 0.5rem 0;
  color: var(--text-primary, #111827);
}

.subtitle {
  color: var(--text-secondary, #64748b);
  margin: 0 0 1.5rem 0;
}

.upload-area {
  border: 2px dashed var(--border-color, #cbd5f5);
  border-radius: 12px;
  padding: 1.5rem;
  text-align: center;
  margin: 1.5rem 0;
}

.hidden-input {
  display: none;
}

.upload-btn,
.convert-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 0.9rem;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.2s;
}

.upload-btn {
  background: var(--user-bubble, #f8fafc);
  color: var(--text-primary, #0f172a);
  border: 1px solid var(--border-color, #e2e8f0);
}

.upload-btn:hover {
  background: #e2e8f0;
}

.convert-btn {
  background: var(--accent-color, #2563eb);
  color: #fff;
  margin-top: 1rem;
}

.convert-btn:hover:not(:disabled) {
  background: var(--accent-hover, #1d4ed8);
}

.convert-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.file-name {
  margin-top: 0.8rem;
  color: var(--text-primary, #0f172a);
  font-size: 0.9rem;
}

.result-box {
  margin-top: 1.5rem;
  padding: 1rem;
  border-radius: 12px;
  background: var(--user-bubble, #f1f5f9);
}

.result-box p {
  margin: 0.5rem 0;
  color: var(--text-primary, #111827);
}

.result-box code {
  display: block;
  margin-top: 0.5rem;
  word-break: break-all;
  font-size: 0.9rem;
  color: var(--text-secondary, #6b7280);
}

@media (max-width: 768px) {
  .video-card {
    padding: 1.5rem;
    width: 95%;
  }
}
</style>

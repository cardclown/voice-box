<template>
  <div class="video-generator">
    <h2>Generate Video from Folder</h2>
    <p class="description">
      Enter the absolute path of the folder on the server containing your images, 
      or upload a folder (if small enough). 
      For large collections, please use the server path.
    </p>

    <div class="form-group">
      <label>Server Folder Path:</label>
      <input v-model="folderPath" type="text" placeholder="/path/to/images" class="input-field" />
    </div>

    <div class="divider">OR</div>

    <div class="form-group">
      <label>Upload Folder:</label>
      <input type="file" webkitdirectory directory multiple @change="handleFileUpload" class="file-input" />
      <p v-if="files.length" class="file-info">Selected {{ files.length }} files</p>
    </div>

    <button @click="generateVideo" :disabled="loading" class="btn">
      {{ loading ? 'Generating...' : 'Generate & Download Video' }}
    </button>

    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const folderPath = ref('')
const files = ref([])
const loading = ref(false)
const error = ref('')

const handleFileUpload = (event) => {
  files.value = Array.from(event.target.files)
  if (files.value.length > 0) {
    folderPath.value = ''
  }
}

const generateVideo = async () => {
  loading.value = true
  error.value = ''
  
  try {
    let response;
    
    if (files.value.length > 0) {
      const formData = new FormData()
      files.value.forEach(file => {
        formData.append('files', file)
      })
      
      response = await axios.post('/api/video/generate-upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        responseType: 'blob'
      })
    } else if (folderPath.value) {
      response = await axios.post('/api/video/generate-path', null, {
        params: { path: folderPath.value },
        responseType: 'blob'
      })
    } else {
      error.value = 'Please provide a path or select a folder.'
      loading.value = false
      return
    }

    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'video_timelapse.zip')
    document.body.appendChild(link)
    link.click()
    link.remove()

  } catch (err) {
    console.error(err)
    error.value = 'Error generating video. Check console for details.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.video-generator {
  padding: 20px;
  background: white;
  max-width: 800px;
  margin: 0 auto;
}

.description {
  color: #666;
  margin-bottom: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.label {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
}

.input-field {
  width: 100%;
  padding: 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.divider {
  text-align: center;
  margin: 20px 0;
  color: #999;
  font-weight: bold;
}

.btn {
  background-color: #4CAF50;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  width: 100%;
}

.btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.error {
  color: red;
  margin-top: 10px;
}
</style>


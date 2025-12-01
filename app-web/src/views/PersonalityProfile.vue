<template>
  <div class="personality-profile-page">
    <div class="page-header">
      <h1>我的个性画像</h1>
      <p class="subtitle">基于大五人格模型的智能分析</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>正在加载您的个性画像...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <p class="error-message">{{ error }}</p>
      <button @click="loadProfile" class="retry-btn">重试</button>
    </div>

    <!-- 数据不足提示 -->
    <div v-else-if="!profile.available" class="insufficient-data">
      <div class="icon">📊</div>
      <h2>需要更多数据</h2>
      <p>{{ profile.message }}</p>
      <div class="progress-info">
        <p>当前消息数：{{ profile.currentMessages }} / {{ profile.minMessages }}</p>
        <div class="progress-bar">
          <div class="progress" :style="{ width: progressPercentage + '%' }"></div>
        </div>
        <p class="progress-text">{{ progressPercentage.toFixed(0) }}% 完成</p>
      </div>
      <button @click="$router.push('/chat')" class="chat-btn">开始对话</button>
    </div>

    <!-- 完整画像展示 -->
    <div v-else class="profile-content">
      <!-- 概览卡片 -->
      <div class="overview-card card">
        <div class="card-header">
          <h2>个性概览</h2>
          <span class="confidence-badge" :class="confidenceClass">
            置信度: {{ (profile.confidence * 100).toFixed(0) }}%
          </span>
        </div>
        <div class="personality-type">
          <div class="type-icon">🎭</div>
          <div class="type-info">
            <h3>{{ profile.personalityType }}</h3>
            <p>您的个性类型</p>
          </div>
        </div>
      </div>

      <!-- 大五人格维度 -->
      <div class="dimensions-card card">
        <div class="card-header">
          <h2>性格维度分析</h2>
          <button @click="showDimensionInfo = !showDimensionInfo" class="info-btn">
            {{ showDimensionInfo ? '隐藏说明' : '查看说明' }}
          </button>
        </div>
        
        <div v-if="showDimensionInfo" class="dimension-info">
          <p>基于大五人格模型（OCEAN）的科学分析</p>
        </div>

        <div class="dimensions-list">
          <div v-for="dim in dimensions" :key="dim.key" class="dimension-item">
            <div class="dim-header">
              <span class="dim-icon">{{ dim.icon }}</span>
              <span class="dim-name">{{ dim.name }}</span>
              <span class="dim-value">{{ dim.value.toFixed(0) }}%</span>
            </div>
            <div class="dim-bar">
              <div class="dim-progress" :style="{ width: dim.value + '%', backgroundColor: dim.color }"></div>
            </div>
            <p class="dim-description">{{ dim.description }}</p>
          </div>
        </div>
      </div>

      <!-- 偏好设置 -->
      <div class="preferences-card card">
        <div class="card-header">
          <h2>对话偏好</h2>
        </div>
        <div class="preferences-grid">
          <div class="pref-item">
            <div class="pref-icon">📏</div>
            <div class="pref-content">
              <h4>回答长度</h4>
              <p class="pref-value">{{ getPreferenceLabel('responseLength', profile.preferences.responseLength) }}</p>
            </div>
          </div>
          <div class="pref-item">
            <div class="pref-icon">💬</div>
            <div class="pref-content">
              <h4>语言风格</h4>
              <p class="pref-value">{{ getPreferenceLabel('languageStyle', profile.preferences.languageStyle) }}</p>
            </div>
          </div>
          <div class="pref-item">
            <div class="pref-icon">🤝</div>
            <div class="pref-content">
              <h4>互动风格</h4>
              <p class="pref-value">{{ getPreferenceLabel('interactionStyle', profile.preferences.interactionStyle) }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 个性化建议 -->
      <div v-if="profile.tips && profile.tips.length > 0" class="tips-card card">
        <div class="card-header">
          <h2>个性化建议</h2>
        </div>
        <div class="tips-list">
          <div v-for="(tip, index) in profile.tips" :key="index" class="tip-item">
            <span class="tip-icon">💡</span>
            <p>{{ tip }}</p>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="actions">
        <button @click="refreshProfile" class="action-btn primary">
          <span class="btn-icon">🔄</span>
          刷新画像
        </button>
        <button @click="$router.push('/personality/settings')" class="action-btn">
          <span class="btn-icon">⚙️</span>
          偏好设置
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PersonalityProfile',
  data() {
    return {
      loading: true,
      error: null,
      profile: {
        available: false,
        message: '加载中...'
      },
      showDimensionInfo: false
    };
  },
  computed: {
    progressPercentage() {
      if (!this.profile.currentMessages || !this.profile.minMessages) return 0;
      return Math.min(100, (this.profile.currentMessages / this.profile.minMessages) * 100);
    },
    confidenceClass() {
      const confidence = this.profile.confidence || 0;
      if (confidence >= 0.7) return 'high';
      if (confidence >= 0.5) return 'medium';
      return 'low';
    },
    dimensions() {
      if (!this.profile.available || !this.profile.personality) return [];
      
      return [
        {
          key: 'openness',
          name: '开放性',
          icon: '🌟',
          value: this.profile.personality.openness * 100,
          color: '#FF6B6B',
          description: '对新想法和创新的接受程度'
        },
        {
          key: 'conscientiousness',
          name: '尽责性',
          icon: '📋',
          value: this.profile.personality.conscientiousness * 100,
          color: '#4ECDC4',
          description: '对细节和准确性的重视程度'
        },
        {
          key: 'extraversion',
          name: '外向性',
          icon: '🎉',
          value: this.profile.personality.extraversion * 100,
          color: '#45B7D1',
          description: '社交互动的活跃程度'
        },
        {
          key: 'agreeableness',
          name: '宜人性',
          icon: '🤗',
          value: this.profile.personality.agreeableness * 100,
          color: '#96CEB4',
          description: '友好和合作的倾向'
        },
        {
          key: 'neuroticism',
          name: '神经质',
          icon: '😰',
          value: this.profile.personality.neuroticism * 100,
          color: '#FFEAA7',
          description: '情绪稳定性'
        }
      ];
    }
  },
  methods: {
    async loadProfile() {
      this.loading = true;
      this.error = null;
      
      try {
        // 获取当前用户ID（从store或localStorage）
        const userId = this.$store?.state?.user?.id || localStorage.getItem('userId') || 1;
        
        const response = await fetch(`/api/chat-integration/suggestions/${userId}`);
        const data = await response.json();
        
        if (data.success) {
          this.profile = data.data;
        } else {
          this.error = data.message || '加载失败';
        }
      } catch (err) {
        console.error('加载用户画像失败:', err);
        this.error = '网络错误，请稍后重试';
      } finally {
        this.loading = false;
      }
    },
    async refreshProfile() {
      try {
        const userId = this.$store?.state?.user?.id || localStorage.getItem('userId') || 1;
        
        // 触发重新分析
        await fetch(`/api/personality/analyze/${userId}`, { method: 'POST' });
        
        // 重新加载画像
        await this.loadProfile();
        
        this.$message?.success('画像已更新');
      } catch (err) {
        console.error('刷新画像失败:', err);
        this.$message?.error('刷新失败，请稍后重试');
      }
    },
    getPreferenceLabel(type, value) {
      const labels = {
        responseLength: {
          concise: '简洁',
          balanced: '适中',
          detailed: '详细'
        },
        languageStyle: {
          formal: '正式',
          balanced: '自然',
          casual: '轻松'
        },
        interactionStyle: {
          active: '主动',
          balanced: '平衡',
          passive: '被动'
        }
      };
      
      return labels[type]?.[value] || value;
    }
  },
  mounted() {
    this.loadProfile();
  }
};
</script>

<style scoped>
.personality-profile-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-header h1 {
  font-size: 32px;
  color: #2c3e50;
  margin-bottom: 10px;
}

.subtitle {
  color: #7f8c8d;
  font-size: 16px;
}

/* 加载状态 */
.loading-container {
  text-align: center;
  padding: 60px 20px;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 错误状态 */
.error-container {
  text-align: center;
  padding: 60px 20px;
}

.error-message {
  color: #e74c3c;
  margin-bottom: 20px;
}

.retry-btn {
  padding: 10px 30px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}

/* 数据不足 */
.insufficient-data {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.insufficient-data .icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.progress-info {
  max-width: 400px;
  margin: 30px auto;
}

.progress-bar {
  height: 20px;
  background: #ecf0f1;
  border-radius: 10px;
  overflow: hidden;
  margin: 15px 0;
}

.progress {
  height: 100%;
  background: linear-gradient(90deg, #3498db, #2ecc71);
  transition: width 0.3s ease;
}

.progress-text {
  color: #7f8c8d;
  font-size: 14px;
}

.chat-btn {
  padding: 12px 40px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  margin-top: 20px;
}

/* 卡片样式 */
.card {
  background: white;
  border-radius: 10px;
  padding: 25px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h2 {
  font-size: 20px;
  color: #2c3e50;
  margin: 0;
}

/* 概览卡片 */
.confidence-badge {
  padding: 5px 15px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: bold;
}

.confidence-badge.high {
  background: #d4edda;
  color: #155724;
}

.confidence-badge.medium {
  background: #fff3cd;
  color: #856404;
}

.confidence-badge.low {
  background: #f8d7da;
  color: #721c24;
}

.personality-type {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  color: white;
}

.type-icon {
  font-size: 48px;
}

.type-info h3 {
  font-size: 24px;
  margin: 0 0 5px 0;
}

.type-info p {
  margin: 0;
  opacity: 0.9;
}

/* 维度分析 */
.dimension-info {
  padding: 15px;
  background: #f8f9fa;
  border-radius: 5px;
  margin-bottom: 20px;
}

.dimensions-list {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.dimension-item {
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.dim-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.dim-icon {
  font-size: 24px;
}

.dim-name {
  flex: 1;
  font-weight: bold;
  color: #2c3e50;
}

.dim-value {
  font-weight: bold;
  color: #3498db;
}

.dim-bar {
  height: 12px;
  background: #e0e0e0;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 8px;
}

.dim-progress {
  height: 100%;
  transition: width 0.5s ease;
  border-radius: 6px;
}

.dim-description {
  font-size: 14px;
  color: #7f8c8d;
  margin: 0;
}

/* 偏好设置 */
.preferences-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.pref-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.pref-icon {
  font-size: 32px;
}

.pref-content h4 {
  margin: 0 0 5px 0;
  color: #2c3e50;
  font-size: 14px;
}

.pref-value {
  margin: 0;
  color: #3498db;
  font-weight: bold;
}

/* 建议列表 */
.tips-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 15px;
  background: #fff3cd;
  border-radius: 8px;
  border-left: 4px solid #ffc107;
}

.tip-icon {
  font-size: 20px;
}

.tip-item p {
  margin: 0;
  color: #856404;
  flex: 1;
}

/* 操作按钮 */
.actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 30px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 30px;
  border: 2px solid #3498db;
  background: white;
  color: #3498db;
  border-radius: 5px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.3s;
}

.action-btn.primary {
  background: #3498db;
  color: white;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3);
}

.info-btn {
  padding: 5px 15px;
  background: #ecf0f1;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 14px;
}

/* 响应式 */
@media (max-width: 768px) {
  .personality-profile-page {
    padding: 10px;
  }
  
  .page-header h1 {
    font-size: 24px;
  }
  
  .personality-type {
    flex-direction: column;
    text-align: center;
  }
  
  .preferences-grid {
    grid-template-columns: 1fr;
  }
  
  .actions {
    flex-direction: column;
  }
  
  .action-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>

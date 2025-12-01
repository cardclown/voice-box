<template>
  <div class="language-selector">
    <div class="selector-header">
      <span class="icon">🌐</span>
      <span class="title">{{ t('language') }}</span>
    </div>
    
    <div class="language-options">
      <button
        v-for="lang in languages"
        :key="lang.language"
        :class="['language-option', { active: currentLanguage === lang.language }]"
        @click="selectLanguage(lang.language)"
        :disabled="loading"
      >
        <span class="flag">{{ getFlag(lang.language) }}</span>
        <span class="name">{{ lang.displayName }}</span>
        <span v-if="currentLanguage === lang.language" class="check">✓</span>
      </button>
    </div>
    
    <div v-if="loading" class="loading">
      <span class="spinner"></span>
      <span>{{ t('switching') }}...</span>
    </div>
    
    <div v-if="error" class="error">
      {{ error }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import emotionalVoiceService from '@/services/emotionalVoiceService';

const props = defineProps({
  userId: {
    type: Number,
    required: true
  }
});

const emit = defineEmits(['language-changed']);

// 简单的翻译函数，不依赖 vue-i18n
const t = (key) => {
  const translations = {
    'language': '语言选择',
    'switching': '切换中',
    'language.select': '选择语言',
    'language.current': '当前语言',
    'language.zh-CN': '中文',
    'language.en-US': 'English'
  };
  return translations[key] || key;
};

const languages = ref([]);
const currentLanguage = ref('zh-CN');
const loading = ref(false);
const error = ref('');

// 获取语言对应的旗帜emoji
const getFlag = (lang) => {
  const flags = {
    'zh-CN': '🇨🇳',
    'en-US': '🇺🇸'
  };
  return flags[lang] || '🌐';
};

// 加载支持的语言列表
const loadLanguages = async () => {
  try {
    // 使用默认语言列表
    languages.value = [
      { language: 'zh-CN', displayName: '中文', flag: '🇨🇳' },
      { language: 'en-US', displayName: 'English', flag: '🇺🇸' }
    ];
  } catch (err) {
    console.error('加载语言列表失败:', err);
  }
};

// 加载用户当前语言
const loadUserLanguage = async () => {
  try {
    // 使用默认语言
    currentLanguage.value = 'zh-CN';
  } catch (err) {
    console.error('加载用户语言失败:', err);
  }
};

// 选择语言
const selectLanguage = async (lang) => {
  if (lang === currentLanguage.value) return;
  
  loading.value = true;
  error.value = '';
  
  try {
    console.log('切换语言到:', lang);
    // 直接切换语言，不调用后端API
    currentLanguage.value = lang;
    emit('language-changed', lang);
    console.log('语言切换完成:', lang);
  } catch (err) {
    console.error('切换语言失败:', err);
    error.value = '切换语言失败';
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadLanguages();
  loadUserLanguage();
});
</script>

<style scoped>
.language-selector {
  background: var(--bg-secondary);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.selector-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.selector-header .icon {
  font-size: 20px;
}

.language-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.language-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-primary);
  border: 2px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
}

.language-option:hover:not(:disabled) {
  background: var(--bg-hover);
  border-color: var(--primary-color);
}

.language-option.active {
  background: var(--primary-light);
  border-color: var(--primary-color);
  color: var(--primary-color);
  font-weight: 600;
}

.language-option:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.language-option .flag {
  font-size: 24px;
}

.language-option .name {
  flex: 1;
}

.language-option .check {
  color: var(--primary-color);
  font-weight: bold;
}

.loading {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 12px;
  background: var(--bg-info);
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid var(--primary-color);
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error {
  margin-top: 12px;
  padding: 12px;
  background: var(--bg-error);
  border-radius: 8px;
  color: var(--text-error);
  font-size: 14px;
}
</style>

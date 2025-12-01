import { ref, watch, onMounted } from 'vue'

/**
 * 语言偏好管理
 * 
 * 功能：
 * - 保存和加载用户语言偏好
 * - 自动检测浏览器语言
 * - 同步到服务器
 */

const STORAGE_KEY = 'voicebox_language_preference'

// 支持的语言代码
const SUPPORTED_LANGUAGES = ['zh-CN', 'en-US', 'ja-JP', 'ko-KR']

// 默认语言
const DEFAULT_LANGUAGE = 'zh-CN'

// 全局语言状态（跨组件共享）
const currentLanguage = ref(DEFAULT_LANGUAGE)

export function useLanguagePreference() {
  /**
   * 获取浏览器语言
   */
  const getBrowserLanguage = () => {
    const browserLang = navigator.language || navigator.userLanguage
    
    // 精确匹配
    if (SUPPORTED_LANGUAGES.includes(browserLang)) {
      return browserLang
    }
    
    // 模糊匹配（例如 zh 匹配 zh-CN）
    const langPrefix = browserLang.split('-')[0]
    const matched = SUPPORTED_LANGUAGES.find(lang => 
      lang.startsWith(langPrefix)
    )
    
    return matched || DEFAULT_LANGUAGE
  }

  /**
   * 从本地存储加载语言偏好
   */
  const loadLanguagePreference = () => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (saved && SUPPORTED_LANGUAGES.includes(saved)) {
        return saved
      }
    } catch (error) {
      console.error('加载语言偏好失败:', error)
    }
    
    // 如果没有保存的偏好，使用浏览器语言
    return getBrowserLanguage()
  }

  /**
   * 保存语言偏好到本地存储
   */
  const saveLanguagePreference = (language) => {
    try {
      localStorage.setItem(STORAGE_KEY, language)
    } catch (error) {
      console.error('保存语言偏好失败:', error)
    }
  }

  /**
   * 保存语言偏好到服务器
   */
  const saveLanguageToServer = async (userId, language) => {
    if (!userId) return

    try {
      const response = await fetch('/api/user/preferences', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId,
          preferenceKey: 'voice_language',
          preferenceValue: language
        })
      })

      if (!response.ok) {
        console.warn('保存语言偏好到服务器失败')
      }
    } catch (error) {
      console.error('保存语言偏好到服务器失败:', error)
    }
  }

  /**
   * 从服务器加载语言偏好
   */
  const loadLanguageFromServer = async (userId) => {
    if (!userId) return null

    try {
      const response = await fetch(`/api/user/preferences/${userId}/voice_language`)
      
      if (response.ok) {
        const data = await response.json()
        if (data.preferenceValue && SUPPORTED_LANGUAGES.includes(data.preferenceValue)) {
          return data.preferenceValue
        }
      }
    } catch (error) {
      console.error('从服务器加载语言偏好失败:', error)
    }

    return null
  }

  /**
   * 设置语言
   */
  const setLanguage = (language, userId = null) => {
    if (!SUPPORTED_LANGUAGES.includes(language)) {
      console.warn(`不支持的语言: ${language}`)
      return
    }

    currentLanguage.value = language
    saveLanguagePreference(language)

    // 如果提供了用户ID，同步到服务器
    if (userId) {
      saveLanguageToServer(userId, language)
    }
  }

  /**
   * 初始化语言偏好
   */
  const initializeLanguage = async (userId = null) => {
    // 优先从服务器加载
    if (userId) {
      const serverLanguage = await loadLanguageFromServer(userId)
      if (serverLanguage) {
        currentLanguage.value = serverLanguage
        saveLanguagePreference(serverLanguage)
        return
      }
    }

    // 其次从本地存储加载
    const localLanguage = loadLanguagePreference()
    currentLanguage.value = localLanguage
  }

  /**
   * 获取语言名称
   */
  const getLanguageName = (code) => {
    const names = {
      'zh-CN': '中文（普通话）',
      'en-US': 'English (US)',
      'ja-JP': '日本語',
      'ko-KR': '한국어'
    }
    return names[code] || code
  }

  /**
   * 获取STT语言模型代码
   * 不同的STT服务可能使用不同的语言代码格式
   */
  const getSTTLanguageCode = (language) => {
    // 豆包STT使用的语言代码
    const sttCodes = {
      'zh-CN': 'zh-CN',
      'en-US': 'en-US',
      'ja-JP': 'ja-JP',
      'ko-KR': 'ko-KR'
    }
    return sttCodes[language] || language
  }

  /**
   * 获取TTS语言引擎代码
   */
  const getTTSLanguageCode = (language) => {
    // 豆包TTS使用的语言代码
    const ttsCodes = {
      'zh-CN': 'zh-CN',
      'en-US': 'en-US',
      'ja-JP': 'ja-JP',
      'ko-KR': 'ko-KR'
    }
    return ttsCodes[language] || language
  }

  /**
   * 获取默认音色（根据语言）
   */
  const getDefaultVoice = (language) => {
    const defaultVoices = {
      'zh-CN': 'zh_female_qingxin',
      'en-US': 'en_female_clara',
      'ja-JP': 'ja_female_aoi',
      'ko-KR': 'ko_female_sora'
    }
    return defaultVoices[language] || defaultVoices['zh-CN']
  }

  // 监听语言变化
  watch(currentLanguage, (newLang) => {
    console.log('语言已切换:', getLanguageName(newLang))
  })

  // 组件挂载时初始化
  onMounted(() => {
    if (currentLanguage.value === DEFAULT_LANGUAGE) {
      currentLanguage.value = loadLanguagePreference()
    }
  })

  return {
    currentLanguage,
    supportedLanguages: SUPPORTED_LANGUAGES,
    setLanguage,
    initializeLanguage,
    getLanguageName,
    getSTTLanguageCode,
    getTTSLanguageCode,
    getDefaultVoice,
    getBrowserLanguage
  }
}

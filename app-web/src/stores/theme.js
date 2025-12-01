import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const THEME_STORAGE_KEY = 'voicebox-theme'
const VALID_THEMES = ['light', 'dark']
const DEFAULT_THEME = 'light'

export const useThemeStore = defineStore('theme', () => {
  const currentTheme = ref(DEFAULT_THEME)
  const isLoaded = ref(false)

  // 计算属性
  const isDark = computed(() => currentTheme.value === 'dark')
  const isLight = computed(() => currentTheme.value === 'light')

  /**
   * 设置主题
   * @param {string} theme - 主题名称 ('light' 或 'dark')
   */
  function setTheme(theme) {
    // 验证主题值
    if (!VALID_THEMES.includes(theme)) {
      console.warn(`Invalid theme: ${theme}. Using default theme: ${DEFAULT_THEME}`)
      theme = DEFAULT_THEME
    }

    currentTheme.value = theme
    applyTheme(theme)
    saveTheme(theme)
  }

  /**
   * 应用主题到 DOM
   * @param {string} theme - 主题名称
   */
  function applyTheme(theme) {
    // 检查 document 是否可用（测试环境兼容）
    if (typeof document === 'undefined' || !document.documentElement) {
      return
    }
    
    // 设置 data-theme 属性
    document.documentElement.setAttribute('data-theme', theme)
    
    // 设置 class（备用方案）
    if (document.documentElement.classList) {
      document.documentElement.classList.remove('light', 'dark')
      document.documentElement.classList.add(theme)
    }
    
    // 更新 meta theme-color（移动端浏览器地址栏颜色）
    updateMetaThemeColor(theme)
  }

  /**
   * 更新移动端浏览器主题颜色
   * @param {string} theme - 主题名称
   */
  function updateMetaThemeColor(theme) {
    // 检查 document 是否可用（测试环境兼容）
    if (typeof document === 'undefined' || !document.querySelector) {
      return
    }
    
    const metaThemeColor = document.querySelector('meta[name="theme-color"]')
    const color = theme === 'dark' ? '#1a1a1a' : '#f9fafb'
    
    if (metaThemeColor) {
      metaThemeColor.setAttribute('content', color)
    } else if (document.head && document.createElement) {
      const meta = document.createElement('meta')
      meta.name = 'theme-color'
      meta.content = color
      document.head.appendChild(meta)
    }
  }

  /**
   * 保存主题到 localStorage
   * @param {string} theme - 主题名称
   */
  function saveTheme(theme) {
    try {
      localStorage.setItem(THEME_STORAGE_KEY, theme)
    } catch (error) {
      console.error('Failed to save theme to localStorage:', error)
    }
  }

  /**
   * 从 localStorage 加载主题
   */
  function loadTheme() {
    try {
      // 1. 尝试从 localStorage 读取
      const savedTheme = localStorage.getItem(THEME_STORAGE_KEY)
      
      if (savedTheme && VALID_THEMES.includes(savedTheme)) {
        setTheme(savedTheme)
        isLoaded.value = true
        return
      }

      // 2. 检测系统偏好
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      const systemTheme = prefersDark ? 'dark' : 'light'
      
      setTheme(systemTheme)
      isLoaded.value = true
    } catch (error) {
      console.error('Failed to load theme:', error)
      setTheme(DEFAULT_THEME)
      isLoaded.value = true
    }
  }

  /**
   * 切换主题（浅色 ↔ 深色）
   */
  function toggleTheme() {
    const newTheme = currentTheme.value === 'light' ? 'dark' : 'light'
    setTheme(newTheme)
  }

  /**
   * 监听系统主题变化
   */
  function watchSystemTheme() {
    try {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      
      const handleChange = (e) => {
        // 只在用户没有手动设置主题时才跟随系统
        const savedTheme = localStorage.getItem(THEME_STORAGE_KEY)
        if (!savedTheme) {
          const systemTheme = e.matches ? 'dark' : 'light'
          setTheme(systemTheme)
        }
      }

      // 现代浏览器
      if (mediaQuery.addEventListener) {
        mediaQuery.addEventListener('change', handleChange)
      } else {
        // 旧版浏览器
        mediaQuery.addListener(handleChange)
      }

      return () => {
        if (mediaQuery.removeEventListener) {
          mediaQuery.removeEventListener('change', handleChange)
        } else {
          mediaQuery.removeListener(handleChange)
        }
      }
    } catch (error) {
      console.error('Failed to watch system theme:', error)
    }
  }

  /**
   * 重置主题到默认值
   */
  function resetTheme() {
    localStorage.removeItem(THEME_STORAGE_KEY)
    setTheme(DEFAULT_THEME)
  }

  return {
    // State
    currentTheme,
    isLoaded,
    
    // Computed
    isDark,
    isLight,
    
    // Actions
    setTheme,
    loadTheme,
    toggleTheme,
    watchSystemTheme,
    resetTheme
  }
})

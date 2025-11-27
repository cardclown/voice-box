import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useThemeStore } from '../theme'

describe('Theme Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    // Mock localStorage
    global.localStorage = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn()
    }
    // Mock document
    global.document = {
      documentElement: {
        setAttribute: vi.fn()
      }
    }
  })

  it('should initialize with light theme', () => {
    const store = useThemeStore()
    expect(store.currentTheme).toBe('light')
  })

  it('should set theme and persist to localStorage', () => {
    const store = useThemeStore()
    store.setTheme('dark')
    
    expect(store.currentTheme).toBe('dark')
    expect(localStorage.setItem).toHaveBeenCalledWith('voicebox-theme', 'dark')
    expect(document.documentElement.setAttribute).toHaveBeenCalledWith('data-theme', 'dark')
  })

  it('should toggle between light and dark themes', () => {
    const store = useThemeStore()
    expect(store.currentTheme).toBe('light')
    
    store.toggleTheme()
    expect(store.currentTheme).toBe('dark')
    
    store.toggleTheme()
    expect(store.currentTheme).toBe('light')
  })

  it('should load theme from localStorage', () => {
    localStorage.getItem.mockReturnValue('dark')
    const store = useThemeStore()
    store.loadTheme()
    
    expect(store.currentTheme).toBe('dark')
  })
})

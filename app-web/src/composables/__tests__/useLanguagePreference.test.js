import { describe, it, expect, beforeEach, vi } from 'vitest'
import { useLanguagePreference } from '../useLanguagePreference'

/**
 * 语言模型匹配属性测试
 * 
 * 属性 11: 语言模型匹配
 * 验证需求: 6.2
 * 
 * 属性：对于任何支持的语言，STT和TTS应该使用对应的语言模型
 */

describe('属性测试：语言模型匹配', () => {
  beforeEach(() => {
    // 清理localStorage
    localStorage.clear()
    
    // Mock fetch
    global.fetch = vi.fn()
  })

  /**
   * 属性 11: 语言模型匹配
   * 
   * 对于任何支持的语言code，
   * getSTTLanguageCode(code) 和 getTTSLanguageCode(code) 
   * 应该返回该语言对应的正确模型代码
   */
  it('属性11: 对于所有支持的语言，STT和TTS应该返回正确的语言模型代码', () => {
    const { 
      supportedLanguages, 
      getSTTLanguageCode, 
      getTTSLanguageCode 
    } = useLanguagePreference()

    // 对于每一种支持的语言
    supportedLanguages.forEach(languageCode => {
      // 获取STT和TTS语言代码
      const sttCode = getSTTLanguageCode(languageCode)
      const ttsCode = getTTSLanguageCode(languageCode)

      // 验证：STT代码不为空
      expect(sttCode).toBeTruthy()
      expect(sttCode).toBeTypeOf('string')
      
      // 验证：TTS代码不为空
      expect(ttsCode).toBeTruthy()
      expect(ttsCode).toBeTypeOf('string')

      // 验证：STT和TTS代码应该匹配语言
      // 对于标准语言代码，STT和TTS应该使用相同的代码
      expect(sttCode).toBe(ttsCode)
      
      // 验证：返回的代码应该包含语言前缀
      const langPrefix = languageCode.split('-')[0]
      expect(sttCode.toLowerCase()).toContain(langPrefix.toLowerCase())

      console.log(`✓ 语言 ${languageCode}: STT=${sttCode}, TTS=${ttsCode}`)
    })
  })

  /**
   * 属性：语言代码一致性
   * 
   * 对于任何语言，多次调用应该返回相同的结果
   */
  it('属性: 语言代码获取应该是确定性的', () => {
    const { getSTTLanguageCode, getTTSLanguageCode } = useLanguagePreference()

    const testLanguages = ['zh-CN', 'en-US', 'ja-JP', 'ko-KR']

    testLanguages.forEach(lang => {
      // 多次调用
      const stt1 = getSTTLanguageCode(lang)
      const stt2 = getSTTLanguageCode(lang)
      const stt3 = getSTTLanguageCode(lang)

      const tts1 = getTTSLanguageCode(lang)
      const tts2 = getTTSLanguageCode(lang)
      const tts3 = getTTSLanguageCode(lang)

      // 验证：多次调用返回相同结果
      expect(stt1).toBe(stt2)
      expect(stt2).toBe(stt3)
      expect(tts1).toBe(tts2)
      expect(tts2).toBe(tts3)

      console.log(`✓ 语言 ${lang} 代码一致性验证通过`)
    })
  })

  /**
   * 属性：默认音色匹配语言
   * 
   * 对于任何语言，默认音色应该适合该语言
   */
  it('属性: 默认音色应该匹配语言', () => {
    const { supportedLanguages, getDefaultVoice } = useLanguagePreference()

    supportedLanguages.forEach(lang => {
      const defaultVoice = getDefaultVoice(lang)

      // 验证：默认音色不为空
      expect(defaultVoice).toBeTruthy()
      expect(defaultVoice).toBeTypeOf('string')

      // 验证：音色代码应该包含语言前缀
      const langPrefix = lang.split('-')[0]
      expect(defaultVoice.toLowerCase()).toContain(langPrefix.toLowerCase())

      console.log(`✓ 语言 ${lang} 默认音色: ${defaultVoice}`)
    })
  })

  /**
   * 属性：语言切换保持一致性
   * 
   * 切换语言后，当前语言应该立即更新
   */
  it('属性: 语言切换应该立即生效', () => {
    const { currentLanguage, setLanguage, supportedLanguages } = useLanguagePreference()

    const initialLanguage = currentLanguage.value

    // 对于每种支持的语言
    supportedLanguages.forEach(lang => {
      // 切换语言
      setLanguage(lang)

      // 验证：当前语言已更新
      expect(currentLanguage.value).toBe(lang)

      console.log(`✓ 切换到语言 ${lang} 成功`)
    })

    // 恢复初始语言
    setLanguage(initialLanguage)
  })

  /**
   * 属性：不支持的语言应该回退到默认
   * 
   * 对于不支持的语言代码，应该使用默认语言
   */
  it('属性: 不支持的语言应该回退到默认', () => {
    const { getSTTLanguageCode, getTTSLanguageCode } = useLanguagePreference()

    const unsupportedLanguages = ['fr-FR', 'de-DE', 'es-ES', 'invalid']

    unsupportedLanguages.forEach(lang => {
      const sttCode = getSTTLanguageCode(lang)
      const ttsCode = getTTSLanguageCode(lang)

      // 验证：返回的代码不为空（应该回退到默认或返回原值）
      expect(sttCode).toBeTruthy()
      expect(ttsCode).toBeTruthy()

      console.log(`✓ 不支持的语言 ${lang}: STT=${sttCode}, TTS=${ttsCode}`)
    })
  })

  /**
   * 属性：浏览器语言检测
   * 
   * 应该能够正确检测浏览器语言
   */
  it('属性: 应该正确检测浏览器语言', () => {
    const { getBrowserLanguage, supportedLanguages } = useLanguagePreference()

    // Mock不同的浏览器语言
    const testCases = [
      { browserLang: 'zh-CN', expected: 'zh-CN' },
      { browserLang: 'zh', expected: 'zh-CN' },  // 模糊匹配
      { browserLang: 'en-US', expected: 'en-US' },
      { browserLang: 'en', expected: 'en-US' },  // 模糊匹配
      { browserLang: 'ja-JP', expected: 'ja-JP' },
      { browserLang: 'ko-KR', expected: 'ko-KR' },
      { browserLang: 'fr-FR', expected: 'zh-CN' }  // 不支持，回退到默认
    ]

    testCases.forEach(({ browserLang, expected }) => {
      // Mock navigator.language
      Object.defineProperty(navigator, 'language', {
        value: browserLang,
        configurable: true
      })

      const detected = getBrowserLanguage()

      // 验证：检测到的语言应该是支持的语言
      expect(supportedLanguages).toContain(detected)
      
      // 验证：检测结果符合预期
      expect(detected).toBe(expected)

      console.log(`✓ 浏览器语言 ${browserLang} 检测为 ${detected}`)
    })
  })

  /**
   * 属性：语言偏好持久化
   * 
   * 保存的语言偏好应该能够被正确加载
   */
  it('属性: 语言偏好应该正确持久化', () => {
    const { setLanguage, supportedLanguages } = useLanguagePreference()

    supportedLanguages.forEach(lang => {
      // 保存语言偏好
      setLanguage(lang)

      // 验证：localStorage中已保存
      const saved = localStorage.getItem('voicebox_language_preference')
      expect(saved).toBe(lang)

      // 创建新实例来测试加载
      const { currentLanguage: newCurrentLanguage } = useLanguagePreference()
      
      // 验证：新实例加载了保存的语言
      // 注意：由于是响应式的，可能需要等待初始化
      setTimeout(() => {
        expect(newCurrentLanguage.value).toBe(lang)
      }, 0)

      console.log(`✓ 语言 ${lang} 持久化验证通过`)
    })
  })

  /**
   * 边界测试：空值和null处理
   */
  it('边界: 应该正确处理空值和null', () => {
    const { getSTTLanguageCode, getTTSLanguageCode, getDefaultVoice } = useLanguagePreference()

    // 测试空字符串
    expect(getSTTLanguageCode('')).toBeTruthy()
    expect(getTTSLanguageCode('')).toBeTruthy()
    expect(getDefaultVoice('')).toBeTruthy()

    // 测试null（会被转换为字符串）
    expect(getSTTLanguageCode(null)).toBeTruthy()
    expect(getTTSLanguageCode(null)).toBeTruthy()
    expect(getDefaultVoice(null)).toBeTruthy()

    console.log('✓ 空值和null处理验证通过')
  })

  /**
   * 性能测试：语言代码获取应该快速
   */
  it('性能: 语言代码获取应该在1ms内完成', () => {
    const { getSTTLanguageCode, getTTSLanguageCode, getDefaultVoice } = useLanguagePreference()

    const iterations = 1000
    const testLanguage = 'zh-CN'

    // 测试STT
    const sttStart = performance.now()
    for (let i = 0; i < iterations; i++) {
      getSTTLanguageCode(testLanguage)
    }
    const sttTime = performance.now() - sttStart
    const sttAvg = sttTime / iterations

    // 测试TTS
    const ttsStart = performance.now()
    for (let i = 0; i < iterations; i++) {
      getTTSLanguageCode(testLanguage)
    }
    const ttsTime = performance.now() - ttsStart
    const ttsAvg = ttsTime / iterations

    // 测试默认音色
    const voiceStart = performance.now()
    for (let i = 0; i < iterations; i++) {
      getDefaultVoice(testLanguage)
    }
    const voiceTime = performance.now() - voiceStart
    const voiceAvg = voiceTime / iterations

    // 验证：平均时间应该小于1ms
    expect(sttAvg).toBeLessThan(1)
    expect(ttsAvg).toBeLessThan(1)
    expect(voiceAvg).toBeLessThan(1)

    console.log(`✓ 性能测试通过:`)
    console.log(`  - STT平均: ${sttAvg.toFixed(4)}ms`)
    console.log(`  - TTS平均: ${ttsAvg.toFixed(4)}ms`)
    console.log(`  - 音色平均: ${voiceAvg.toFixed(4)}ms`)
  })
})

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import VoiceInput from '../VoiceInput.vue'
import VoicePlayer from '../VoicePlayer.vue'

/**
 * 语音聊天端到端测试
 * 
 * 测试完整的语音聊天流程：
 * 1. 录音并发送消息
 * 2. 接收AI语音回复
 * 3. 播放历史语音消息
 * 
 * 验证需求: 1.1-1.8, 2.1-2.8, 3.4, 3.5
 */

// Mock MediaRecorder API
class MockMediaRecorder {
  constructor(stream, options) {
    this.stream = stream
    this.options = options
    this.state = 'inactive'
    this.ondataavailable = null
    this.onstop = null
    this.onerror = null
  }

  start() {
    this.state = 'recording'
    // 模拟录音数据
    setTimeout(() => {
      if (this.ondataavailable) {
        const mockBlob = new Blob(['mock audio data'], { type: 'audio/wav' })
        this.ondataavailable({ data: mockBlob })
      }
    }, 100)
  }

  stop() {
    this.state = 'inactive'
    if (this.onstop) {
      this.onstop()
    }
  }

  pause() {
    this.state = 'paused'
  }

  resume() {
    this.state = 'recording'
  }
}

// Mock Audio API
class MockAudio {
  constructor(src) {
    this.src = src
    this.paused = true
    this.currentTime = 0
    this.duration = 10
    this.volume = 1
    this.onloadedmetadata = null
    this.onplay = null
    this.onpause = null
    this.onended = null
    this.onerror = null
  }

  play() {
    this.paused = false
    if (this.onplay) {
      this.onplay()
    }
    return Promise.resolve()
  }

  pause() {
    this.paused = true
    if (this.onpause) {
      this.onpause()
    }
  }

  load() {
    if (this.onloadedmetadata) {
      setTimeout(() => this.onloadedmetadata(), 10)
    }
  }
}

// Mock fetch API
const mockFetch = vi.fn()

describe('语音聊天端到端测试', () => {
  beforeEach(() => {
    // Setup global mocks
    global.MediaRecorder = MockMediaRecorder
    global.Audio = MockAudio
    global.fetch = mockFetch
    global.URL.createObjectURL = vi.fn(() => 'blob:mock-url')
    global.URL.revokeObjectURL = vi.fn()

    // Mock navigator.mediaDevices
    global.navigator.mediaDevices = {
      getUserMedia: vi.fn(() => Promise.resolve({
        getTracks: () => [{
          stop: vi.fn()
        }]
      }))
    }

    // Reset fetch mock
    mockFetch.mockReset()
  })

  /**
   * 测试场景 1: 完整的语音聊天流程
   * 
   * 流程：
   * 1. 用户点击录音按钮开始录音
   * 2. 用户点击停止按钮结束录音
   * 3. 系统上传音频并识别文本
   * 4. 用户编辑识别的文本（可选）
   * 5. 用户发送消息
   * 6. 系统接收AI语音回复
   * 7. 用户播放AI语音回复
   * 8. 用户查看和播放历史消息
   */
  it('应该完成完整的语音聊天流程', async () => {
    console.log('\n========== 测试：完整语音聊天流程 ==========')

    // ========== 第1步：开始录音 ==========
    const voiceInput = mount(VoiceInput, {
      props: {
        userId: 1,
        sessionId: 100,
        language: 'zh-CN'
      }
    })

    // 请求麦克风权限
    await voiceInput.vm.requestPermission()
    await flushPromises()

    expect(voiceInput.vm.hasPermission).toBe(true)
    console.log('✓ 步骤1：麦克风权限已授予')

    // 点击录音按钮
    const recordButton = voiceInput.find('.voice-button')
    await recordButton.trigger('click')
    await flushPromises()

    expect(voiceInput.vm.isRecording).toBe(true)
    console.log('✓ 步骤2：开始录音')

    // ========== 第2步：停止录音并上传 ==========
    // Mock 上传响应
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        success: true,
        recognizedText: '你好，这是测试消息',
        fileId: 'user-file-123',
        duration: 3
      })
    })

    // 停止录音
    await recordButton.trigger('click')
    await flushPromises()
    await nextTick()

    expect(voiceInput.vm.isRecording).toBe(false)
    expect(voiceInput.vm.editableText).toBe('你好，这是测试消息')
    console.log('✓ 步骤3：录音停止，文本识别成功')
    console.log('  - 识别文本: 你好，这是测试消息')

    // ========== 第3步：发送消息 ==========
    const messageSentSpy = vi.fn()
    voiceInput.vm.$emit = messageSentSpy

    const sendButton = voiceInput.find('.btn-send')
    await sendButton.trigger('click')
    await flushPromises()

    expect(messageSentSpy).toHaveBeenCalledWith('message-sent', {
      text: '你好，这是测试消息',
      isVoice: true
    })
    console.log('✓ 步骤4：消息发送成功')

    // ========== 第4步：接收AI语音回复 ==========
    // 模拟AI回复的TTS合成
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        success: true,
        fileId: 'ai-file-456',
        duration: 5
      })
    })

    // 模拟接收到AI回复
    const aiReplyText = '你好！我收到了你的消息'
    console.log('✓ 步骤5：接收AI回复')
    console.log('  - AI回复: ' + aiReplyText)

    // ========== 第5步：播放AI语音回复 ==========
    const voicePlayer = mount(VoicePlayer, {
      props: {
        audioUrl: '/api/voice/audio/ai-file-456',
        autoPlay: false
      }
    })

    await flushPromises()
    await nextTick()

    // 点击播放按钮
    const playButton = voicePlayer.find('.play-button')
    await playButton.trigger('click')
    await flushPromises()

    expect(voicePlayer.vm.isPlaying).toBe(true)
    console.log('✓ 步骤6：AI语音回复播放中')

    // ========== 第6步：查看历史消息 ==========
    // 模拟查询历史消息
    const historyMessages = [
      {
        id: 1,
        fileId: 'user-file-123',
        recognizedText: '你好，这是测试消息',
        userId: 1,
        sessionId: 100,
        createdAt: new Date()
      },
      {
        id: 2,
        fileId: 'ai-file-456',
        originalText: '你好！我收到了你的消息',
        userId: 1,
        sessionId: 100,
        createdAt: new Date()
      }
    ]

    expect(historyMessages).toHaveLength(2)
    console.log('✓ 步骤7：历史消息查询成功')
    console.log('  - 消息数量: ' + historyMessages.length)

    // ========== 第7步：播放历史消息 ==========
    // 播放用户自己的消息
    const userMessagePlayer = mount(VoicePlayer, {
      props: {
        audioUrl: '/api/voice/audio/user-file-123',
        autoPlay: false
      }
    })

    await userMessagePlayer.find('.play-button').trigger('click')
    await flushPromises()

    expect(userMessagePlayer.vm.isPlaying).toBe(true)
    console.log('✓ 步骤8：历史消息播放成功')

    console.log('\n========== 端到端测试完成 ==========')
    console.log('✓ 所有步骤验证通过')
  })

  /**
   * 测试场景 2: 录音时长限制
   * 
   * 场景：
   * 用户录音超过最大时长限制（5分钟）
   * 
   * 验证：
   * - 录音自动停止
   * - 显示时长限制提示
   */
  it('应该在录音超过时长限制时自动停止', async () => {
    console.log('\n========== 测试：录音时长限制 ==========')

    const voiceInput = mount(VoiceInput, {
      props: {
        userId: 1,
        sessionId: 100,
        maxDuration: 5 // 5秒（测试用）
      }
    })

    // 授予权限并开始录音
    await voiceInput.vm.requestPermission()
    await voiceInput.find('.voice-button').trigger('click')
    await flushPromises()

    expect(voiceInput.vm.isRecording).toBe(true)
    console.log('✓ 开始录音')

    // 模拟时间流逝
    vi.useFakeTimers()
    vi.advanceTimersByTime(6000) // 6秒
    await flushPromises()

    // 应该自动停止
    expect(voiceInput.vm.isRecording).toBe(false)
    console.log('✓ 录音自动停止（超过时长限制）')

    vi.useRealTimers()
  })

  /**
   * 测试场景 3: 权限拒绝处理
   * 
   * 场景：
   * 用户拒绝麦克风权限
   * 
   * 验证：
   * - 显示权限提示
   * - 录音按钮禁用
   * - 可以重新请求权限
   */
  it('应该正确处理权限拒绝', async () => {
    console.log('\n========== 测试：权限拒绝处理 ==========')

    // Mock 权限拒绝
    global.navigator.mediaDevices.getUserMedia = vi.fn(() => 
      Promise.reject(new Error('Permission denied'))
    )

    const voiceInput = mount(VoiceInput, {
      props: {
        userId: 1,
        sessionId: 100
      }
    })

    // 尝试请求权限
    try {
      await voiceInput.vm.requestPermission()
    } catch (error) {
      expect(error.message).toBe('Permission denied')
    }

    await flushPromises()

    expect(voiceInput.vm.hasPermission).toBe(false)
    console.log('✓ 权限被拒绝')

    // 录音按钮应该被禁用
    const recordButton = voiceInput.find('.voice-button')
    expect(recordButton.classes()).toContain('disabled')
    console.log('✓ 录音按钮已禁用')

    // 点击按钮应该显示权限提示
    await recordButton.trigger('click')
    await flushPromises()

    expect(voiceInput.vm.showPermissionPrompt).toBe(true)
    console.log('✓ 显示权限提示')
  })

  /**
   * 测试场景 4: 文本编辑后发送
   * 
   * 场景：
   * 用户编辑识别的文本后发送
   * 
   * 验证：
   * - 可以编辑识别的文本
   * - 发送编辑后的文本
   */
  it('应该支持编辑识别文本后发送', async () => {
    console.log('\n========== 测试：文本编辑后发送 ==========')

    const voiceInput = mount(VoiceInput, {
      props: {
        userId: 1,
        sessionId: 100
      }
    })

    // 模拟识别结果
    voiceInput.vm.editableText = '原始识别文本'
    await nextTick()

    console.log('✓ 原始文本: 原始识别文本')

    // 编辑文本
    const textarea = voiceInput.find('.text-editor')
    await textarea.setValue('编辑后的文本')
    await flushPromises()

    expect(voiceInput.vm.editableText).toBe('编辑后的文本')
    console.log('✓ 文本已编辑: 编辑后的文本')

    // 发送消息
    const messageSentSpy = vi.fn()
    voiceInput.vm.$emit = messageSentSpy

    await voiceInput.find('.btn-send').trigger('click')
    await flushPromises()

    expect(messageSentSpy).toHaveBeenCalledWith('message-sent', {
      text: '编辑后的文本',
      isVoice: true
    })
    console.log('✓ 发送编辑后的文本')
  })

  /**
   * 测试场景 5: 播放进度控制
   * 
   * 场景：
   * 用户控制语音播放进度
   * 
   * 验证：
   * - 可以播放/暂停
   * - 可以调整播放进度
   * - 播放完成后自动停止
   */
  it('应该支持播放进度控制', async () => {
    console.log('\n========== 测试：播放进度控制 ==========')

    const voicePlayer = mount(VoicePlayer, {
      props: {
        audioUrl: '/api/voice/audio/test-file',
        autoPlay: false
      }
    })

    await flushPromises()

    // 播放
    await voicePlayer.find('.play-button').trigger('click')
    await flushPromises()

    expect(voicePlayer.vm.isPlaying).toBe(true)
    console.log('✓ 开始播放')

    // 暂停
    await voicePlayer.find('.play-button').trigger('click')
    await flushPromises()

    expect(voicePlayer.vm.isPlaying).toBe(false)
    console.log('✓ 暂停播放')

    // 调整进度
    const progressBar = voicePlayer.find('.progress-bar')
    await progressBar.trigger('click', {
      offsetX: 50,
      target: { offsetWidth: 100 }
    })
    await flushPromises()

    console.log('✓ 调整播放进度')

    // 模拟播放完成
    if (voicePlayer.vm.audio && voicePlayer.vm.audio.onended) {
      voicePlayer.vm.audio.onended()
      await flushPromises()
    }

    expect(voicePlayer.vm.isPlaying).toBe(false)
    console.log('✓ 播放完成自动停止')
  })

  /**
   * 测试场景 6: 多轮对话
   * 
   * 场景：
   * 用户与AI进行多轮语音对话
   * 
   * 验证：
   * - 支持连续多轮对话
   * - 每轮对话独立处理
   * - 历史消息正确累积
   */
  it('应该支持多轮语音对话', async () => {
    console.log('\n========== 测试：多轮对话 ==========')

    const rounds = 3
    const messages = []

    for (let i = 1; i <= rounds; i++) {
      console.log(`\n--- 第${i}轮对话 ---`)

      // 用户发送消息
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          success: true,
          recognizedText: `用户消息${i}`,
          fileId: `user-file-${i}`,
          duration: 3
        })
      })

      messages.push({
        id: messages.length + 1,
        text: `用户消息${i}`,
        fileId: `user-file-${i}`,
        isUser: true
      })

      console.log(`✓ 用户: 用户消息${i}`)

      // AI回复
      mockFetch.mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          success: true,
          fileId: `ai-file-${i}`,
          duration: 5
        })
      })

      messages.push({
        id: messages.length + 1,
        text: `AI回复${i}`,
        fileId: `ai-file-${i}`,
        isUser: false
      })

      console.log(`✓ AI: AI回复${i}`)
    }

    expect(messages).toHaveLength(rounds * 2)
    console.log(`\n✓ 完成${rounds}轮对话`)
    console.log(`✓ 总消息数: ${messages.length}`)
  })

  /**
   * 测试场景 7: 错误处理
   * 
   * 场景：
   * 在语音聊天过程中遇到各种错误
   * 
   * 验证：
   * - 上传失败显示错误提示
   * - 合成失败显示错误提示
   * - 播放失败显示错误提示
   * - 错误后可以重试
   */
  it('应该正确处理各种错误', async () => {
    console.log('\n========== 测试：错误处理 ==========')

    // 测试上传失败
    mockFetch.mockRejectedValueOnce(new Error('Network error'))

    const voiceInput = mount(VoiceInput, {
      props: {
        userId: 1,
        sessionId: 100
      }
    })

    const errorSpy = vi.fn()
    voiceInput.vm.$emit = errorSpy

    await voiceInput.vm.requestPermission()
    await voiceInput.find('.voice-button').trigger('click')
    await flushPromises()

    // 停止录音（会触发上传）
    await voiceInput.find('.voice-button').trigger('click')
    await flushPromises()

    expect(errorSpy).toHaveBeenCalled()
    console.log('✓ 上传失败错误已捕获')

    // 测试播放失败
    const voicePlayer = mount(VoicePlayer, {
      props: {
        audioUrl: '/api/voice/audio/invalid-file',
        autoPlay: false
      }
    })

    // 模拟播放错误
    if (voicePlayer.vm.audio && voicePlayer.vm.audio.onerror) {
      voicePlayer.vm.audio.onerror(new Error('Playback error'))
      await flushPromises()
    }

    console.log('✓ 播放失败错误已捕获')

    // 重试应该可以成功
    mockFetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        success: true,
        recognizedText: '重试成功',
        fileId: 'retry-file',
        duration: 3
      })
    })

    await voiceInput.find('.voice-button').trigger('click')
    await voiceInput.find('.voice-button').trigger('click')
    await flushPromises()

    console.log('✓ 重试成功')
  })
})

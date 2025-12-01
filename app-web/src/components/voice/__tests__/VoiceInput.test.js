import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';
import VoiceInput from '../VoiceInput.vue';

/**
 * VoiceInput 组件测试
 * 
 * Feature: voice-interaction, Property 3: 录音时长限制
 * Validates: Requirements 1.6
 */

describe('VoiceInput', () => {
  let wrapper;
  let mockMediaRecorder;
  let mockStream;

  beforeEach(() => {
    // Mock MediaRecorder
    mockMediaRecorder = {
      start: vi.fn(),
      stop: vi.fn(),
      ondataavailable: null,
      onstop: null,
      state: 'inactive'
    };

    // Mock getUserMedia
    mockStream = {
      getTracks: () => [{
        stop: vi.fn()
      }]
    };

    global.navigator.mediaDevices = {
      getUserMedia: vi.fn().mockResolvedValue(mockStream)
    };

    global.MediaRecorder = vi.fn(() => mockMediaRecorder);

    // Mock permissions API
    global.navigator.permissions = {
      query: vi.fn().mockResolvedValue({
        state: 'granted',
        onchange: null
      })
    };
  });

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
    vi.clearAllMocks();
  });

  /**
   * 属性 3: 录音时长限制
   * 
   * 对于任意录音会话，当录音时长达到maxDuration时，
   * 系统应该自动停止录音并触发错误事件
   * 
   * Validates: Requirements 1.6
   */
  describe('Property 3: 录音时长限制', () => {
    it('应该在达到最大时长时自动停止录音', async () => {
      const maxDuration = 5; // 5秒用于测试
      const errorSpy = vi.fn();

      wrapper = mount(VoiceInput, {
        props: {
          userId: 1,
          sessionId: 1,
          maxDuration
        }
      });

      wrapper.vm.$on('error', errorSpy);

      // 开始录音
      await wrapper.find('.voice-button').trigger('click');
      await nextTick();

      // 模拟时间流逝
      vi.useFakeTimers();
      
      // 快进到maxDuration
      vi.advanceTimersByTime(maxDuration * 1000);
      await nextTick();

      // 验证录音已停止
      expect(wrapper.vm.isRecording).toBe(false);
      
      // 验证触发了错误事件
      expect(errorSpy).toHaveBeenCalled();
      const error = errorSpy.mock.calls[0][0];
      expect(error.message).toContain('录音时长超过限制');

      vi.useRealTimers();
    });

    it('应该在不同的maxDuration值下正确限制时长', async () => {
      const testCases = [10, 30, 60, 120, 300]; // 不同的时长限制

      for (const maxDuration of testCases) {
        const errorSpy = vi.fn();

        wrapper = mount(VoiceInput, {
          props: {
            userId: 1,
            sessionId: 1,
            maxDuration
          }
        });

        wrapper.vm.$on('error', errorSpy);

        // 开始录音
        await wrapper.find('.voice-button').trigger('click');
        await nextTick();

        vi.useFakeTimers();
        
        // 快进到maxDuration - 1秒（应该还在录音）
        vi.advanceTimersByTime((maxDuration - 1) * 1000);
        await nextTick();
        expect(wrapper.vm.isRecording).toBe(true);

        // 再快进1秒（应该停止）
        vi.advanceTimersByTime(1000);
        await nextTick();
        expect(wrapper.vm.isRecording).toBe(false);

        vi.useRealTimers();
        wrapper.unmount();
      }
    });

    it('应该显示正确的录音时长', async () => {
      wrapper = mount(VoiceInput, {
        props: {
          userId: 1,
          sessionId: 1,
          maxDuration: 300
        }
      });

      // 开始录音
      await wrapper.find('.voice-button').trigger('click');
      await nextTick();

      vi.useFakeTimers();

      // 测试不同时长的显示
      const testDurations = [
        { seconds: 5, expected: '0:05' },
        { seconds: 30, expected: '0:30' },
        { seconds: 65, expected: '1:05' },
        { seconds: 125, expected: '2:05' }
      ];

      for (const { seconds, expected } of testDurations) {
        vi.advanceTimersByTime(seconds * 1000);
        await nextTick();
        
        const durationText = wrapper.find('.duration').text();
        expect(durationText).toBe(expected);
      }

      vi.useRealTimers();
    });
  });

  /**
   * 属性 7: 权限拒绝后功能禁用
   * 
   * 对于任意用户，当麦克风权限被拒绝时，
   * 录音按钮应该被禁用，并显示权限提示
   * 
   * Validates: Requirements 1.7, 9.2
   */
  describe('Property 7: 权限拒绝后功能禁用', () => {
    it('应该在权限被拒绝时禁用录音按钮', async () => {
      // Mock权限被拒绝
      global.navigator.permissions.query = vi.fn().mockResolvedValue({
        state: 'denied',
        onchange: null
      });

      wrapper = mount(VoiceInput, {
        props: {
          userId: 1,
          sessionId: 1
        }
      });

      await nextTick();

      // 验证按钮被禁用
      const button = wrapper.find('.voice-button');
      expect(button.classes()).toContain('disabled');
      expect(button.attributes('disabled')).toBeDefined();
    });

    it('应该在点击禁用按钮时显示权限提示', async () => {
      global.navigator.permissions.query = vi.fn().mockResolvedValue({
        state: 'denied',
        onchange: null
      });

      wrapper = mount(VoiceInput, {
        props: {
          userId: 1,
          sessionId: 1
        }
      });

      await nextTick();

      // 点击禁用的按钮
      await wrapper.find('.voice-button').trigger('click');
      await nextTick();

      // 验证显示权限提示
      expect(wrapper.find('.permission-prompt').exists()).toBe(true);
      expect(wrapper.find('.permission-prompt p').text()).toContain('麦克风权限');
    });

    it('应该在权限状态变化时更新按钮状态', async () => {
      let permissionState = 'denied';
      const permissionResult = {
        get state() { return permissionState; },
        onchange: null
      };

      global.navigator.permissions.query = vi.fn().mockResolvedValue(permissionResult);

      wrapper = mount(VoiceInput, {
        props: {
          userId: 1,
          sessionId: 1
        }
      });

      await nextTick();

      // 初始状态：禁用
      expect(wrapper.find('.voice-button').classes()).toContain('disabled');

      // 模拟权限被授予
      permissionState = 'granted';
      if (permissionResult.onchange) {
        permissionResult.onchange();
      }
      await nextTick();

      // 验证按钮启用
      expect(wrapper.find('.voice-button').classes()).not.toContain('disabled');
    });
  });

  /**
   * 属性 13: 文本编辑后发送
   * 
   * 对于任意识别的文本，用户应该能够编辑后发送，
   * 编辑后的文本应该被正确传递
   * 
   * Validates: Requirements 5.3
   */
  describe('Property 13: 文本编辑后发送', () => {
    it('应该允许编辑识别的文本', async () => {
      wrapper = mount(VoiceInput, {
        props: {
          userId: 1,
          sessionId: 1
        }
      });

      // 模拟识别结果
      wrapper.vm.recognizedText = '原始文本';
      wrapper.vm.editableText = '原始文本';
      await nextTick();

      // 验证显示文本编辑器
      expect(wrapper.find('.recognized-text').exists()).toBe(true);
      expect(wrapper.find('.text-editor').exists()).toBe(true);

      // 编辑文本
      const textarea = wrapper.find('.text-editor');
      await textarea.setValue('编辑后的文本');
      await nextTick();

      expect(wrapper.vm.editableText).toBe('编辑后的文本');
    });

    it('应该在发送时传递编辑后的文本', async () => {
      const messageSentSpy = vi.fn();

      wrapper = mount(VoiceInput, {
        props: {
          userId: 1,
          sessionId: 1
        }
      });

      wrapper.vm.$on('message-sent', messageSentSpy);

      // 模拟识别结果
      wrapper.vm.recognizedText = '原始文本';
      wrapper.vm.editableText = '编辑后的文本';
      await nextTick();

      // 点击发送按钮
      await wrapper.find('.btn-send').trigger('click');
      await nextTick();

      // 验证发送的是编辑后的文本
      expect(messageSentSpy).toHaveBeenCalledWith({
        text: '编辑后的文本',
        isVoice: true
      });
    });

    it('应该支持Ctrl+Enter快捷键发送', async () => {
      const messageSentSpy = vi.fn();

      wrapper = mount(VoiceInput, {
        props: {
          userId: 1,
          sessionId: 1
        }
      });

      wrapper.vm.$on('message-sent', messageSentSpy);

      // 模拟识别结果
      wrapper.vm.recognizedText = '测试文本';
      wrapper.vm.editableText = '测试文本';
      await nextTick();

      // 按Ctrl+Enter
      const textarea = wrapper.find('.text-editor');
      await textarea.trigger('keydown.enter', { ctrlKey: true });
      await nextTick();

      // 验证消息已发送
      expect(messageSentSpy).toHaveBeenCalled();
    });

    it('应该在取消时清空文本', async () => {
      wrapper = mount(VoiceInput, {
        props: {
          userId: 1,
          sessionId: 1
        }
      });

      // 模拟识别结果
      wrapper.vm.recognizedText = '测试文本';
      wrapper.vm.editableText = '测试文本';
      await nextTick();

      // 点击取消按钮
      await wrapper.find('.btn-cancel').trigger('click');
      await nextTick();

      // 验证文本已清空
      expect(wrapper.vm.editableText).toBe('');
    });
  });

  /**
   * 基础功能测试
   */
  describe('基础功能', () => {
    it('应该正确渲染组件', () => {
      wrapper = mount(VoiceInput, {
        props: {
          userId: 1,
          sessionId: 1
        }
      });

      expect(wrapper.find('.voice-input').exists()).toBe(true);
      expect(wrapper.find('.voice-button').exists()).toBe(true);
    });

    it('应该在录音时显示波形动画', async () => {
      wrapper = mount(VoiceInput, {
        props: {
          userId: 1,
          sessionId: 1
        }
      });

      // 开始录音
      await wrapper.find('.voice-button').trigger('click');
      await nextTick();

      // 验证显示录音状态
      expect(wrapper.find('.recording-status').exists()).toBe(true);
      expect(wrapper.find('.waveform').exists()).toBe(true);
      expect(wrapper.findAll('.wave-bar').length).toBe(5);
    });

    it('应该在录音时改变按钮样式', async () => {
      wrapper = mount(VoiceInput, {
        props: {
          userId: 1,
          sessionId: 1
        }
      });

      const button = wrapper.find('.voice-button');

      // 初始状态
      expect(button.classes()).not.toContain('recording');

      // 开始录音
      await button.trigger('click');
      await nextTick();

      // 录音状态
      expect(button.classes()).toContain('recording');
    });
  });
});

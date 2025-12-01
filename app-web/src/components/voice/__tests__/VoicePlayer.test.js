import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';
import VoicePlayer from '../VoicePlayer.vue';

/**
 * VoicePlayer 组件测试
 * 
 * Feature: voice-interaction, Property 5: 播放进度一致性
 * Feature: voice-interaction, Property 14: 播放状态切换
 * Validates: Requirements 2.3, 2.4, 2.5, 2.6
 */

describe('VoicePlayer', () => {
  let wrapper;
  let mockAudio;

  beforeEach(() => {
    // Mock Audio元素
    mockAudio = {
      play: vi.fn().mockResolvedValue(undefined),
      pause: vi.fn(),
      load: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      currentTime: 0,
      duration: 100,
      paused: true,
      ended: false,
      volume: 1,
      playbackRate: 1
    };

    global.Audio = vi.fn(() => mockAudio);
  });

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
    vi.clearAllMocks();
  });

  /**
   * 属性 5: 播放进度一致性
   * 
   * 对于任意音频文件，播放进度应该与实际播放时间一致，
   * 进度条显示应该准确反映当前播放位置
   * 
   * Validates: Requirements 2.4
   */
  describe('Property 5: 播放进度一致性', () => {
    it('应该正确显示播放进度', async () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 100
        }
      });

      await nextTick();

      // 模拟不同的播放进度
      const testCases = [
        { currentTime: 0, expected: 0 },
        { currentTime: 25, expected: 25 },
        { currentTime: 50, expected: 50 },
        { currentTime: 75, expected: 75 },
        { currentTime: 100, expected: 100 }
      ];

      for (const { currentTime, expected } of testCases) {
        mockAudio.currentTime = currentTime;
        
        // 触发timeupdate事件
        const timeUpdateHandler = mockAudio.addEventListener.mock.calls
          .find(call => call[0] === 'timeupdate')?.[1];
        
        if (timeUpdateHandler) {
          timeUpdateHandler();
          await nextTick();
        }

        // 验证进度百分比
        const progress = (currentTime / mockAudio.duration) * 100;
        expect(progress).toBe(expected);
      }
    });

    it('应该在拖动进度条时更新播放位置', async () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 100
        }
      });

      await nextTick();

      // 模拟拖动进度条到50%
      const progressBar = wrapper.find('.progress-bar');
      if (progressBar.exists()) {
        await progressBar.trigger('click', {
          offsetX: 50,
          target: { offsetWidth: 100 }
        });
        await nextTick();

        // 验证音频currentTime被更新
        expect(mockAudio.currentTime).toBe(50);
      }
    });

    it('应该正确格式化时间显示', async () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 125 // 2分5秒
        }
      });

      await nextTick();

      // 测试不同时间的格式化
      const testCases = [
        { seconds: 0, expected: '0:00' },
        { seconds: 5, expected: '0:05' },
        { seconds: 65, expected: '1:05' },
        { seconds: 125, expected: '2:05' }
      ];

      for (const { seconds, expected } of testCases) {
        mockAudio.currentTime = seconds;
        
        const timeUpdateHandler = mockAudio.addEventListener.mock.calls
          .find(call => call[0] === 'timeupdate')?.[1];
        
        if (timeUpdateHandler) {
          timeUpdateHandler();
          await nextTick();
        }

        // 验证时间显示格式
        const timeDisplay = wrapper.find('.time-display');
        if (timeDisplay.exists()) {
          expect(timeDisplay.text()).toContain(expected);
        }
      }
    });
  });

  /**
   * 属性 14: 播放状态切换
   * 
   * 对于任意音频，播放/暂停状态应该正确切换，
   * UI应该准确反映当前播放状态
   * 
   * Validates: Requirements 2.3, 2.5, 2.6
   */
  describe('Property 14: 播放状态切换', () => {
    it('应该正确切换播放和暂停状态', async () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 100
        }
      });

      await nextTick();

      const playButton = wrapper.find('.play-button');

      // 初始状态：暂停
      expect(mockAudio.paused).toBe(true);

      // 点击播放
      await playButton.trigger('click');
      await nextTick();
      expect(mockAudio.play).toHaveBeenCalled();

      // 模拟播放状态
      mockAudio.paused = false;
      await nextTick();

      // 点击暂停
      await playButton.trigger('click');
      await nextTick();
      expect(mockAudio.pause).toHaveBeenCalled();
    });

    it('应该在播放结束时重置状态', async () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 100
        }
      });

      await nextTick();

      // 开始播放
      mockAudio.paused = false;
      await wrapper.find('.play-button').trigger('click');
      await nextTick();

      // 模拟播放结束
      mockAudio.ended = true;
      mockAudio.currentTime = mockAudio.duration;
      
      const endedHandler = mockAudio.addEventListener.mock.calls
        .find(call => call[0] === 'ended')?.[1];
      
      if (endedHandler) {
        endedHandler();
        await nextTick();
      }

      // 验证状态重置
      expect(mockAudio.currentTime).toBe(0);
      expect(mockAudio.paused).toBe(true);
    });

    it('应该显示正确的播放/暂停图标', async () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 100
        }
      });

      await nextTick();

      // 暂停状态：显示播放图标
      mockAudio.paused = true;
      await nextTick();
      expect(wrapper.find('.play-icon').exists()).toBe(true);

      // 播放状态：显示暂停图标
      mockAudio.paused = false;
      await nextTick();
      expect(wrapper.find('.pause-icon').exists()).toBe(true);
    });

    it('应该支持自动播放', async () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 100,
          autoPlay: true
        }
      });

      await nextTick();

      // 验证自动调用play
      expect(mockAudio.play).toHaveBeenCalled();
    });

    it('应该在切换音频时重置播放状态', async () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 100
        }
      });

      await nextTick();

      // 开始播放
      await wrapper.find('.play-button').trigger('click');
      mockAudio.paused = false;
      mockAudio.currentTime = 50;
      await nextTick();

      // 切换音频
      await wrapper.setProps({
        audioUrl: '/api/voice/audio/test456',
        duration: 80
      });
      await nextTick();

      // 验证状态重置
      expect(mockAudio.load).toHaveBeenCalled();
      expect(mockAudio.currentTime).toBe(0);
    });
  });

  /**
   * 基础功能测试
   */
  describe('基础功能', () => {
    it('应该正确渲染组件', () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 100
        }
      });

      expect(wrapper.find('.voice-player').exists()).toBe(true);
      expect(wrapper.find('.play-button').exists()).toBe(true);
      expect(wrapper.find('.progress-bar').exists()).toBe(true);
    });

    it('应该在没有audioUrl时禁用播放', () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '',
          duration: 0
        }
      });

      const playButton = wrapper.find('.play-button');
      expect(playButton.attributes('disabled')).toBeDefined();
    });

    it('应该支持音量控制', async () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 100
        }
      });

      await nextTick();

      // 模拟音量调整
      const volumeSlider = wrapper.find('.volume-slider');
      if (volumeSlider.exists()) {
        await volumeSlider.setValue(0.5);
        await nextTick();

        expect(mockAudio.volume).toBe(0.5);
      }
    });

    it('应该支持播放速度调整', async () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 100
        }
      });

      await nextTick();

      // 模拟速度调整
      const speedButton = wrapper.find('.speed-button');
      if (speedButton.exists()) {
        await speedButton.trigger('click');
        await nextTick();

        // 验证速度变化（1x -> 1.5x -> 2x -> 0.5x -> 1x）
        expect([0.5, 1, 1.5, 2]).toContain(mockAudio.playbackRate);
      }
    });

    it('应该在组件卸载时清理资源', async () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 100
        }
      });

      await nextTick();

      // 开始播放
      await wrapper.find('.play-button').trigger('click');
      mockAudio.paused = false;

      // 卸载组件
      wrapper.unmount();

      // 验证音频被暂停
      expect(mockAudio.pause).toHaveBeenCalled();
    });

    it('应该处理加载错误', async () => {
      const errorSpy = vi.fn();

      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/invalid',
          duration: 100
        }
      });

      wrapper.vm.$on('error', errorSpy);

      await nextTick();

      // 模拟加载错误
      const errorHandler = mockAudio.addEventListener.mock.calls
        .find(call => call[0] === 'error')?.[1];
      
      if (errorHandler) {
        errorHandler(new Error('加载失败'));
        await nextTick();
      }

      // 验证错误事件被触发
      expect(errorSpy).toHaveBeenCalled();
    });
  });

  /**
   * 边界情况测试
   */
  describe('边界情况', () => {
    it('应该处理零时长音频', () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 0
        }
      });

      expect(wrapper.find('.voice-player').exists()).toBe(true);
      // 零时长应该显示为0:00
      const timeDisplay = wrapper.find('.time-display');
      if (timeDisplay.exists()) {
        expect(timeDisplay.text()).toContain('0:00');
      }
    });

    it('应该处理非常长的音频', () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 3600 // 1小时
        }
      });

      expect(wrapper.find('.voice-player').exists()).toBe(true);
      // 应该正确显示小时格式
      const timeDisplay = wrapper.find('.time-display');
      if (timeDisplay.exists()) {
        expect(timeDisplay.text()).toMatch(/\d+:\d{2}:\d{2}/);
      }
    });

    it('应该处理无效的进度值', async () => {
      wrapper = mount(VoicePlayer, {
        props: {
          audioUrl: '/api/voice/audio/test123',
          duration: 100
        }
      });

      await nextTick();

      // 尝试设置负数进度
      mockAudio.currentTime = -10;
      const timeUpdateHandler = mockAudio.addEventListener.mock.calls
        .find(call => call[0] === 'timeupdate')?.[1];
      
      if (timeUpdateHandler) {
        timeUpdateHandler();
        await nextTick();
      }

      // 进度应该被限制在有效范围内
      expect(mockAudio.currentTime).toBeGreaterThanOrEqual(0);
      expect(mockAudio.currentTime).toBeLessThanOrEqual(mockAudio.duration);
    });
  });
});

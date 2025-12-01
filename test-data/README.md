# 测试数据说明

这个目录包含用于端到端测试的测试数据文件。

## 文件说明

### sample-audio.wav
模拟的音频文件，用于测试语音分析功能。

**注意**: 实际测试时，你可以：
1. 使用真实的音频文件替换这个文件
2. 或者在测试中使用 Mock 数据

## 创建测试音频文件

如果需要创建真实的测试音频文件，可以：

### macOS
```bash
# 录制 5 秒音频
sox -d test-data/sample-audio.wav trim 0 5
```

### Linux
```bash
# 使用 arecord 录制
arecord -d 5 -f cd test-data/sample-audio.wav
```

### 或者使用在线工具
访问 https://online-voice-recorder.com/ 录制并下载音频文件

## 测试数据格式

### 音频文件要求
- 格式: WAV, MP3
- 采样率: 16000 Hz 或更高
- 时长: 1-30 秒
- 大小: < 10 MB

### 测试场景

1. **正常情绪测试**
   - happy-audio.wav: 开心的语音
   - sad-audio.wav: 悲伤的语音
   - calm-audio.wav: 平静的语音

2. **边界测试**
   - empty-audio.wav: 空音频文件
   - noise-audio.wav: 噪音音频
   - long-audio.wav: 超长音频（用于性能测试）

3. **错误测试**
   - invalid.txt: 非音频文件
   - corrupted.wav: 损坏的音频文件

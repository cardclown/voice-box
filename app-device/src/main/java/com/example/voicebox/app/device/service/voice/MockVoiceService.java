package com.example.voicebox.app.device.service.voice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * Mock语音服务 - 用于开发和测试
 * 当豆包服务不可用时，可以使用此服务进行开发
 * 注意：不使用@Profile，让它始终可用，通过配置开关控制是否使用
 */
@Slf4j
@Service
public class MockVoiceService {

    /**
     * Mock语音识别
     * 返回固定的识别结果
     */
    public CompletableFuture<String> speechToText(InputStream audioStream, String language) {
        log.info("Mock语音识别 - language: {}", language);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 模拟处理时间
                Thread.sleep(500);
                
                // 读取音频数据（虽然不处理，但要消费掉）
                byte[] buffer = new byte[1024];
                while (audioStream.read(buffer) != -1) {
                    // 读取但不处理
                }
                
                // 返回mock结果
                String mockText = "这是模拟的语音识别结果";
                log.info("Mock语音识别完成: {}", mockText);
                return mockText;
                
            } catch (Exception e) {
                log.error("Mock语音识别失败", e);
                throw new RuntimeException("Mock语音识别失败", e);
            }
        });
    }

    /**
     * Mock语音合成
     * 返回一个简单的音频数据（实际上是空数据）
     */
    public CompletableFuture<byte[]> textToSpeech(String text, String language, String voiceName) {
        log.info("Mock语音合成 - text: {}, language: {}, voice: {}", text, language, voiceName);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 模拟处理时间
                Thread.sleep(300);
                
                // 生成一个简单的mock音频数据
                // 实际应用中，这里可以返回一个预录制的音频文件
                byte[] mockAudio = generateMockAudio(text.length());
                
                log.info("Mock语音合成完成: {} bytes", mockAudio.length);
                return mockAudio;
                
            } catch (Exception e) {
                log.error("Mock语音合成失败", e);
                throw new RuntimeException("Mock语音合成失败", e);
            }
        });
    }

    /**
     * 生成mock音频数据
     * 实际上是一个简单的WAV文件头 + 静音数据
     */
    private byte[] generateMockAudio(int textLength) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // 简单的WAV文件头（44字节）
            // RIFF header
            baos.write("RIFF".getBytes());
            writeInt(baos, 36 + textLength * 100); // 文件大小
            baos.write("WAVE".getBytes());
            
            // fmt chunk
            baos.write("fmt ".getBytes());
            writeInt(baos, 16); // fmt chunk size
            writeShort(baos, 1); // audio format (PCM)
            writeShort(baos, 1); // number of channels (mono)
            writeInt(baos, 16000); // sample rate
            writeInt(baos, 32000); // byte rate
            writeShort(baos, 2); // block align
            writeShort(baos, 16); // bits per sample
            
            // data chunk
            baos.write("data".getBytes());
            writeInt(baos, textLength * 100); // data size
            
            // 静音数据（全0）
            byte[] silence = new byte[textLength * 100];
            baos.write(silence);
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("生成mock音频失败", e);
            return new byte[1000]; // 返回一个简单的字节数组
        }
    }

    private void writeInt(ByteArrayOutputStream baos, int value) {
        baos.write(value & 0xFF);
        baos.write((value >> 8) & 0xFF);
        baos.write((value >> 16) & 0xFF);
        baos.write((value >> 24) & 0xFF);
    }

    private void writeShort(ByteArrayOutputStream baos, int value) {
        baos.write(value & 0xFF);
        baos.write((value >> 8) & 0xFF);
    }
}

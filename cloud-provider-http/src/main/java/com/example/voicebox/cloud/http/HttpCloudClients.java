package com.example.voicebox.cloud.http;

import com.example.voicebox.cloud.AsrClient;
import com.example.voicebox.cloud.ChatClient;
import com.example.voicebox.cloud.EmotionAnalyzer;
import com.example.voicebox.cloud.TtsClient;
import com.example.voicebox.hardware.EmotionType;

import java.nio.charset.StandardCharsets;

/**
 * 提供一组基于 HTTP/云端实现的客户端工厂方法。
 *
 * 目前重点是 {@link #chatClient()} 使用 {@link HttpChatClient} 调用云端大模型对话接口。
 * ASR/TTS/Emotion 这里先给出简单占位实现，你可以按实际云服务替换为真正的 HTTP 调用。
 */
public final class HttpCloudClients {

    private HttpCloudClients() {
    }

    /**
     * 创建基于 HTTP 的 ChatClient。
     *
     * 你可以通过以下环境变量/系统属性配置 DeepSeek / 豆包 等提供商：
     * - VOICEBOX_CHAT_URL / voicebox.chat.url
     * - VOICEBOX_CHAT_API_KEY / voicebox.chat.apiKey
     * - VOICEBOX_CHAT_MODEL / voicebox.chat.model
     *
     * DeepSeek 示例（请以官方文档为准）：
     * - VOICEBOX_CHAT_URL=https://api.deepseek.com/v1/chat/completions
     * - VOICEBOX_CHAT_MODEL=deepseek-chat
     *
     * 豆包示例（请以官方文档为准）：
     * - VOICEBOX_CHAT_URL=https://ark.cn-beijing.volces.com/api/v3/chat/completions
     * - VOICEBOX_CHAT_MODEL=doubao-1.5-lite-32k
     */
    public static ChatClient chatClient() {
        try {
            return HttpChatClient.fromEnv();
        } catch (IllegalArgumentException e) {
            System.err.println("************************************************************");
            System.err.println("* [HttpCloudClients] Cloud Chat Config Missing!");
            System.err.println("* " + e.getMessage());
            System.err.println("* Please refer to 'env-example.properties' for configuration.");
            System.err.println("************************************************************");
            throw e;
        }
    }

    /**
     * 默认使用阿里云 ASR（通过可配置的 HTTP 网关，内部协议需参考阿里云官方文档）。
     * 如果相关配置缺失，则退回到一个简单的本地占位实现。
     */
    public static AsrClient asrClient() {
        try {
            return AliyunAsrClient.fromEnv();
        } catch (IllegalArgumentException e) {
            System.out.println("[HttpCloudClients] Aliyun ASR not configured properly, fallback to simple UTF-8 ASR: " + e.getMessage());
            return audioPcm -> new String(audioPcm, StandardCharsets.UTF_8);
        }
    }

    /**
     * 默认使用阿里云 TTS（通过可配置的 HTTP 网关，内部协议需参考阿里云官方文档）。
     * 如果相关配置缺失，则退回到一个简单的本地占位实现。
     */
    public static TtsClient ttsClient() {
        try {
            return AliyunTtsClient.fromEnv();
        } catch (IllegalArgumentException e) {
            System.out.println("[HttpCloudClients] Aliyun TTS not configured properly, fallback to simple UTF-8 TTS: " + e.getMessage());
            return (text, style) -> {
                String decorated = "[" + style + "] " + text;
                return decorated.getBytes(StandardCharsets.UTF_8);
            };
        }
    }

    /**
     * 简单情绪分析实现：根据关键词粗略判断情绪。
     * 实际使用时可以替换为云端情绪识别服务或使用大模型分析。
     */
    public static EmotionAnalyzer emotionAnalyzer() {
        return text -> {
            if (text == null || text.isEmpty()) {
                return EmotionType.NEUTRAL;
            }
            String t = text.toLowerCase();
            if (t.contains("开心") || t.contains("happy")) {
                return EmotionType.HAPPY;
            }
            if (t.contains("难过") || t.contains("sad")) {
                return EmotionType.SAD;
            }
            if (t.contains("生气") || t.contains("angry")) {
                return EmotionType.ANGRY;
            }
            return EmotionType.CALM;
        };
    }
}



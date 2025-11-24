package com.example.voicebox.cloud.sample;

import com.example.voicebox.cloud.AsrClient;
import com.example.voicebox.cloud.ChatClient;
import com.example.voicebox.cloud.ChatRequest;
import com.example.voicebox.cloud.ChatResponse;
import com.example.voicebox.cloud.EmotionAnalyzer;
import com.example.voicebox.cloud.TtsClient;
import com.example.voicebox.cloud.VoiceStyle;
import com.example.voicebox.hardware.EmotionType;

import java.nio.charset.StandardCharsets;

public final class DummyCloudClients {

    private DummyCloudClients() {}

    public static AsrClient asrClient() {
        return audioPcm -> "（假 ASR）你好，我是语音盒子";
    }

    public static ChatClient chatClient() {
        return request -> new ChatResponse("（假对话）你刚才说：" + request.getUserText());
    }

    public static TtsClient ttsClient() {
        return (text, style) -> ("[" + style + "] " + text).getBytes(StandardCharsets.UTF_8);
    }

    public static EmotionAnalyzer emotionAnalyzer() {
        return text -> {
            String t = text.toLowerCase();
            if (t.contains("开心") || t.contains("happy")) {
                return EmotionType.HAPPY;
            }
            if (t.contains("难过") || t.contains("sad")) {
                return EmotionType.SAD;
            }
            return EmotionType.CALM;
        };
    }
}

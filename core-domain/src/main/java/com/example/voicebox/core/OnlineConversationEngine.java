package com.example.voicebox.core;

import com.example.voicebox.cloud.AsrClient;
import com.example.voicebox.cloud.ChatClient;
import com.example.voicebox.cloud.ChatRequest;
import com.example.voicebox.cloud.ChatResponse;
import com.example.voicebox.cloud.EmotionAnalyzer;
import com.example.voicebox.cloud.TtsClient;
import com.example.voicebox.cloud.VoiceStyle;
import com.example.voicebox.hardware.EmotionType;

import java.util.Collections;

public class OnlineConversationEngine implements ConversationEngine {

    private final AsrClient asrClient;
    private final ChatClient chatClient;
    private final TtsClient ttsClient;
    private final EmotionAnalyzer emotionAnalyzer;

    public OnlineConversationEngine(AsrClient asrClient,
                                    ChatClient chatClient,
                                    TtsClient ttsClient,
                                    EmotionAnalyzer emotionAnalyzer) {
        this.asrClient = asrClient;
        this.chatClient = chatClient;
        this.ttsClient = ttsClient;
        this.emotionAnalyzer = emotionAnalyzer;
    }

    public ChatClient getChatClient() {
        return chatClient;
    }

    public TtsClient getTtsClient() {
        return ttsClient;
    }

    @Override
    public ConversationResult handleUserUtterance(byte[] audioPcm, DeviceContext context) {
        String text = asrClient.recognize(audioPcm);
        ChatResponse chat = chatClient.chat(new ChatRequest(Collections.emptyList(), text));
        EmotionType emotion = emotionAnalyzer.analyze(chat.getText());
        VoiceStyle style = mapEmotionToStyle(emotion);
        byte[] tts = ttsClient.synthesize(chat.getText(), style);
        return new ConversationResult(chat.getText(), emotion, tts);
    }

    private VoiceStyle mapEmotionToStyle(EmotionType emotion) {
        switch (emotion) {
            case HAPPY:
                return VoiceStyle.HAPPY;
            case SAD:
                return VoiceStyle.SAD;
            case CALM:
            case NEUTRAL:
                return VoiceStyle.CALM;
            case ANGRY:
                return VoiceStyle.SERIOUS;
            case SURPRISED:
            default:
                return VoiceStyle.DEFAULT;
        }
    }
}

package com.example.voicebox.core;

import com.example.voicebox.hardware.EmotionType;

public class ConversationResult {

    private final String text;
    private final EmotionType emotionType;
    private final byte[] ttsBytes;

    public ConversationResult(String text, EmotionType emotionType, byte[] ttsBytes) {
        this.text = text;
        this.emotionType = emotionType;
        this.ttsBytes = ttsBytes;
    }

    public String getText() {
        return text;
    }

    public EmotionType getEmotionType() {
        return emotionType;
    }

    public byte[] getTtsBytes() {
        return ttsBytes;
    }
}

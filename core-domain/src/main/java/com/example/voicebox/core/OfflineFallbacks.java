package com.example.voicebox.core;

import com.example.voicebox.hardware.EmotionType;

public final class OfflineFallbacks {

    private OfflineFallbacks() {}

    public static ConversationResult simpleResponse() {
        String text = "当前处于离线模式，我先陪你简单聊聊~";
        return new ConversationResult(text, EmotionType.CALM, new byte[0]);
    }
}

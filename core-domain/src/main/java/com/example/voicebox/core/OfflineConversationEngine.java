package com.example.voicebox.core;

public class OfflineConversationEngine implements ConversationEngine {

    @Override
    public ConversationResult handleUserUtterance(byte[] audioPcm, DeviceContext context) {
        return OfflineFallbacks.simpleResponse();
    }
}

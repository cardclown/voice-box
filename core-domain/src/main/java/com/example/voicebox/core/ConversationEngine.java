package com.example.voicebox.core;

public interface ConversationEngine {

    ConversationResult handleUserUtterance(byte[] audioPcm, DeviceContext context);
}

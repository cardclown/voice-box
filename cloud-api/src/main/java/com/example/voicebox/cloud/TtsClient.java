package com.example.voicebox.cloud;

public interface TtsClient {

    byte[] synthesize(String text, VoiceStyle style);
}

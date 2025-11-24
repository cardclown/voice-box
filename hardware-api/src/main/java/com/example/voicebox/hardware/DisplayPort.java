package com.example.voicebox.hardware;

public interface DisplayPort {

    void showEmotion(EmotionType emotionType);

    void showText(String text);

    void showState(DeviceState state);
}

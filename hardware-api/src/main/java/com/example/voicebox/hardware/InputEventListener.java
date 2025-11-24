package com.example.voicebox.hardware;

public interface InputEventListener {

    void onWakeButtonPressed();

    default void onVolumeUp() {}

    default void onVolumeDown() {}
}

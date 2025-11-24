package com.example.voicebox.hardware.mock;

import com.example.voicebox.hardware.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public final class MockHardware implements AudioInputPort, AudioOutputPort, DisplayPort, InputEventPort, NetworkStatusPort {

    private final AtomicReference<InputEventListener> listenerRef = new AtomicReference<>();

    @Override
    public void startRecording() {
        System.out.println("[MockHardware] startRecording called");
    }

    @Override
    public byte[] stopAndFetchPcm() {
        System.out.println("[MockHardware] stopAndFetchPcm called");
        return "你好，测试一下语音盒子".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void playPcm(byte[] data) {
        System.out.println("[MockHardware] playPcm: " + new String(data, StandardCharsets.UTF_8));
    }

    @Override
    public void playFile(String filePath) {
        System.out.println("[MockHardware] playFile: " + filePath);
    }

    @Override
    public void showEmotion(EmotionType emotionType) {
        System.out.println("[MockHardware] 表情: " + emotionType);
    }

    @Override
    public void showText(String text) {
        System.out.println("[MockHardware] 文本: " + text);
    }

    @Override
    public void showState(DeviceState state) {
        System.out.println("[MockHardware] 状态: " + state);
    }

    @Override
    public void registerListener(InputEventListener listener) {
        this.listenerRef.set(listener);
    }

    @Override
    public NetworkType currentNetwork() {
        return NetworkType.WIFI;
    }

    // helper method to simulate button press in tests
    public void simulateWakeButtonPress() {
        InputEventListener listener = listenerRef.get();
        if (listener != null) {
            listener.onWakeButtonPressed();
        }
    }
}

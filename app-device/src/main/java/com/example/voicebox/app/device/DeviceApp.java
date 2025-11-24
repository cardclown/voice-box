package com.example.voicebox.app.device;

import com.example.voicebox.core.util.ConfigLoader;
import com.example.voicebox.cloud.http.HttpCloudClients;
import com.example.voicebox.cloud.sample.DummyCloudClients;
import com.example.voicebox.core.*;
import com.example.voicebox.hardware.InputEventListener;
import com.example.voicebox.hardware.mock.MockHardware;
import com.example.voicebox.app.device.db.DatabaseLogger;

public class DeviceApp {

    public static void main(String[] args) {
        // 1. 加载本地配置文件 (config.properties)
        ConfigLoader.loadProperties();
        
        // 2. 初始化数据库 (建表等)
        DatabaseLogger.getInstance();

        // In real device, you would wire concrete Raspberry Pi implementations here.
        MockHardware hardware = new MockHardware();

        boolean useHttpCloud = Boolean.parseBoolean(
                System.getProperty("voicebox.useHttpCloud",
                        System.getenv("VOICEBOX_USE_HTTP_CLOUD") != null
                                ? System.getenv("VOICEBOX_USE_HTTP_CLOUD")
                                : "false")
        );

        OnlineConversationEngine onlineEngine;
        if (useHttpCloud) {
            System.out.println("[DeviceApp] Using HTTP-based cloud clients");
            onlineEngine = new OnlineConversationEngine(
                    HttpCloudClients.asrClient(),
                    HttpCloudClients.chatClient(),
                    HttpCloudClients.ttsClient(),
                    HttpCloudClients.emotionAnalyzer()
            );
        } else {
            System.out.println("[DeviceApp] Using dummy cloud clients");
            onlineEngine = new OnlineConversationEngine(
                DummyCloudClients.asrClient(),
                DummyCloudClients.chatClient(),
                DummyCloudClients.ttsClient(),
                DummyCloudClients.emotionAnalyzer()
        );
        }

        OfflineConversationEngine offlineEngine = new OfflineConversationEngine();

        ModeManager modeManager = new ModeManager(hardware, onlineEngine, offlineEngine);

        hardware.registerListener(new InputEventListener() {
            @Override
            public void onWakeButtonPressed() {
                System.out.println("[DeviceApp] Wake button pressed");
                hardware.showState(com.example.voicebox.hardware.DeviceState.LISTENING);
                hardware.startRecording();
                byte[] pcm = hardware.stopAndFetchPcm();
                ConversationResult result = modeManager.handle(pcm, new DeviceContext());
                hardware.showEmotion(result.getEmotionType());
                hardware.showText(result.getText());
                hardware.playPcm(result.getTtsBytes());
                
                // 记录到数据库
                DatabaseLogger.getInstance().logConversation(
                    result.getText(), 
                    result.getEmotionType().name()
                );
            }
        });

        // For now, simulate a single wake button press.
        hardware.simulateWakeButtonPress();
    }
}

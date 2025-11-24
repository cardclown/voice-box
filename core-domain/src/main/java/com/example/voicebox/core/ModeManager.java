package com.example.voicebox.core;

import com.example.voicebox.hardware.NetworkStatusPort;

public class ModeManager {

    private final NetworkStatusPort networkStatusPort;
    private final ConversationEngine onlineEngine;
    private final ConversationEngine offlineEngine;

    public ModeManager(NetworkStatusPort networkStatusPort,
                       ConversationEngine onlineEngine,
                       ConversationEngine offlineEngine) {
        this.networkStatusPort = networkStatusPort;
        this.onlineEngine = onlineEngine;
        this.offlineEngine = offlineEngine;
    }

    public ConversationResult handle(byte[] audioPcm, DeviceContext context) {
        if (networkStatusPort.isOnline()) {
            return onlineEngine.handleUserUtterance(audioPcm, context);
        }
        return offlineEngine.handleUserUtterance(audioPcm, context);
    }
}

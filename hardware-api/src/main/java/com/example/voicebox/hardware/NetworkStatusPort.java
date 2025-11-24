package com.example.voicebox.hardware;

public interface NetworkStatusPort {

    NetworkType currentNetwork();

    default boolean isOnline() {
        return currentNetwork() != NetworkType.OFFLINE;
    }
}

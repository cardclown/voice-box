package com.example.voicebox.hardware;

public interface AudioInputPort {
    void startRecording();

    /**
     * Stop recording and return raw PCM audio data.
     */
    byte[] stopAndFetchPcm();
}

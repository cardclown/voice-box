package com.example.voicebox.hardware;

public interface AudioOutputPort {

    void playPcm(byte[] data);

    void playFile(String filePath);
}

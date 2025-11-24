package com.example.voicebox.cloud;

public interface AsrClient {

    String recognize(byte[] audioPcm);
}

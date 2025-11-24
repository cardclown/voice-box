package com.example.voicebox.cloud;

import com.example.voicebox.hardware.EmotionType;

public interface EmotionAnalyzer {

    EmotionType analyze(String text);
}

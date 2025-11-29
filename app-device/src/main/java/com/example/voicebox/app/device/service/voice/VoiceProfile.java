package com.example.voicebox.app.device.service.voice;

/**
 * 语音配置类
 * 
 * @author VoiceBox Team
 * @since 1.5
 */
public class VoiceProfile {
    
    private String voiceName;       // 音色名称
    private Double speechRate;      // 语速 (0.5-2.0)
    private Double pitch;           // 音调 (0.5-2.0)
    private Double volume;          // 音量 (0.0-1.0)
    private String emotion;         // 情感 (neutral/happy/sad)
    
    public VoiceProfile() {
        this.voiceName = "xiaoyun";
        this.speechRate = 1.0;
        this.pitch = 1.0;
        this.volume = 0.8;
        this.emotion = "neutral";
    }
    
    public static VoiceProfile getDefault() {
        return new VoiceProfile();
    }
    
    // Getters and Setters
    
    public String getVoiceName() {
        return voiceName;
    }
    
    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
    }
    
    public Double getSpeechRate() {
        return speechRate;
    }
    
    public void setSpeechRate(Double speechRate) {
        this.speechRate = speechRate;
    }
    
    public Double getPitch() {
        return pitch;
    }
    
    public void setPitch(Double pitch) {
        this.pitch = pitch;
    }
    
    public Double getVolume() {
        return volume;
    }
    
    public void setVolume(Double volume) {
        this.volume = volume;
    }
    
    public String getEmotion() {
        return emotion;
    }
    
    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }
}

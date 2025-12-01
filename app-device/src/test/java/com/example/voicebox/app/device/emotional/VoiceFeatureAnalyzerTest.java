package com.example.voicebox.app.device.emotional;

import com.example.voicebox.app.device.service.emotional.VoiceFeatureAnalyzer;
import com.example.voicebox.app.device.service.emotional.VoiceFeatureAnalyzer.VoiceFeatures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VoiceFeatureAnalyzer 服务单元测试
 */
public class VoiceFeatureAnalyzerTest {
    
    private VoiceFeatureAnalyzer analyzer;
    
    @BeforeEach
    public void setUp() {
        analyzer = new VoiceFeatureAnalyzer();
    }
    
    @Test
    public void testExtractFeaturesFromByteArray() {
        byte[] audioData = new byte[1000];
        
        VoiceFeatures features = analyzer.extractFeatures(audioData);
        
        assertNotNull(features);
        assertNotNull(features.getPitchMean());
        assertNotNull(features.getVolumeMean());
        assertNotNull(features.getSpeedWpm());
        assertNotNull(features.getAudioQuality());
    }
    
    @Test
    public void testExtractFeaturesFromEmptyArray() {
        byte[] audioData = new byte[0];
        
        VoiceFeatures features = analyzer.extractFeatures(audioData);
        
        assertNotNull(features);
    }
    
    @Test
    public void testCalculateStatistics() {
        VoiceFeatures features = new VoiceFeatures();
        features.setPitchMax(250.0);
        features.setPitchMin(100.0);
        features.setVolumeMean(-15.0);
        features.setVolumeStd(5.0);
        features.setSpeedWpm(150);
        features.setPauseCount(3);
        features.setEnergyLevel(0.7);
        features.setArousalLevel(0.6);
        
        Map<String, Double> stats = analyzer.calculateStatistics(features);
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("pitch_range"));
        assertEquals(150.0, stats.get("pitch_range"));
        
        assertTrue(stats.containsKey("volume_dynamic_range"));
        assertEquals(10.0, stats.get("volume_dynamic_range"));
        
        assertTrue(stats.containsKey("speech_stability"));
        assertTrue(stats.get("speech_stability") > 0);
        
        assertTrue(stats.containsKey("emotional_intensity"));
        assertEquals(0.65, stats.get("emotional_intensity"), 0.01);
    }
    
    @Test
    public void testCalculateStatisticsWithNullValues() {
        VoiceFeatures features = new VoiceFeatures();
        
        Map<String, Double> stats = analyzer.calculateStatistics(features);
        
        assertNotNull(stats);
        assertTrue(stats.isEmpty());
    }
    
    @Test
    public void testValidateFeaturesWithValidData() {
        VoiceFeatures features = new VoiceFeatures();
        features.setPitchMean(180.0);
        features.setVolumeMean(-15.0);
        features.setSpeedWpm(150);
        features.setAudioQuality(0.85);
        
        assertTrue(analyzer.validateFeatures(features));
    }
    
    @Test
    public void testValidateFeaturesWithNull() {
        assertFalse(analyzer.validateFeatures(null));
    }
    
    @Test
    public void testValidateFeaturesWithMissingPitch() {
        VoiceFeatures features = new VoiceFeatures();
        features.setVolumeMean(-15.0);
        features.setSpeedWpm(150);
        features.setAudioQuality(0.85);
        
        assertFalse(analyzer.validateFeatures(features));
    }
    
    @Test
    public void testValidateFeaturesWithMissingVolume() {
        VoiceFeatures features = new VoiceFeatures();
        features.setPitchMean(180.0);
        features.setSpeedWpm(150);
        features.setAudioQuality(0.85);
        
        assertFalse(analyzer.validateFeatures(features));
    }
    
    @Test
    public void testValidateFeaturesWithMissingSpeed() {
        VoiceFeatures features = new VoiceFeatures();
        features.setPitchMean(180.0);
        features.setVolumeMean(-15.0);
        features.setAudioQuality(0.85);
        
        assertFalse(analyzer.validateFeatures(features));
    }
    
    @Test
    public void testValidateFeaturesWithMissingQuality() {
        VoiceFeatures features = new VoiceFeatures();
        features.setPitchMean(180.0);
        features.setVolumeMean(-15.0);
        features.setSpeedWpm(150);
        
        assertFalse(analyzer.validateFeatures(features));
    }
    
    @Test
    public void testVoiceFeaturesGettersAndSetters() {
        VoiceFeatures features = new VoiceFeatures();
        
        // 测试音高特征
        features.setPitchMean(180.0);
        assertEquals(180.0, features.getPitchMean());
        
        features.setPitchStd(25.0);
        assertEquals(25.0, features.getPitchStd());
        
        features.setPitchMin(120.0);
        assertEquals(120.0, features.getPitchMin());
        
        features.setPitchMax(250.0);
        assertEquals(250.0, features.getPitchMax());
        
        // 测试音量特征
        features.setVolumeMean(-15.0);
        assertEquals(-15.0, features.getVolumeMean());
        
        features.setVolumeStd(5.0);
        assertEquals(5.0, features.getVolumeStd());
        
        // 测试语速特征
        features.setSpeedWpm(150);
        assertEquals(150, features.getSpeedWpm());
        
        features.setPauseCount(3);
        assertEquals(3, features.getPauseCount());
        
        features.setPauseDuration(1.5);
        assertEquals(1.5, features.getPauseDuration());
        
        // 测试音色特征
        features.setTimbreBrightness(0.65);
        assertEquals(0.65, features.getTimbreBrightness());
        
        features.setTimbreWarmth(0.55);
        assertEquals(0.55, features.getTimbreWarmth());
        
        // 测试情感特征
        features.setEnergyLevel(0.7);
        assertEquals(0.7, features.getEnergyLevel());
        assertEquals(0.7, features.getEnergy());
        
        features.setArousalLevel(0.6);
        assertEquals(0.6, features.getArousalLevel());
        
        features.setValenceLevel(0.3);
        assertEquals(0.3, features.getValenceLevel());
        
        // 测试质量指标
        features.setAudioQuality(0.85);
        assertEquals(0.85, features.getAudioQuality());
        
        features.setNoiseLevel(0.15);
        assertEquals(0.15, features.getNoiseLevel());
    }
    
    @Test
    public void testGetSpeechRate() {
        VoiceFeatures features = new VoiceFeatures();
        
        // 测试 null 情况
        assertNull(features.getSpeechRate());
        
        // 测试正常情况
        features.setSpeedWpm(150);
        assertNotNull(features.getSpeechRate());
        assertTrue(features.getSpeechRate() > 0);
    }
    
    @Test
    public void testSetEnergy() {
        VoiceFeatures features = new VoiceFeatures();
        
        features.setEnergy(0.8);
        assertEquals(0.8, features.getEnergy());
        assertEquals(0.8, features.getEnergyLevel());
    }
}

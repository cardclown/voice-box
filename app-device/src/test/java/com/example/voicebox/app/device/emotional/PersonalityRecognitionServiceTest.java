package com.example.voicebox.app.device.emotional;

import com.example.voicebox.app.device.service.emotional.PersonalityRecognitionService;
import com.example.voicebox.app.device.service.emotional.PersonalityRecognitionService.PersonalityTraits;
import com.example.voicebox.app.device.service.emotional.VoiceFeatureAnalyzer;
import com.example.voicebox.app.device.service.emotional.VoiceFeatureAnalyzer.VoiceFeatures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PersonalityRecognitionService 单元测试
 * 测试外向型、内向型、理性/感性判断
 */
public class PersonalityRecognitionServiceTest {
    
    private PersonalityRecognitionService service;
    
    @BeforeEach
    public void setUp() {
        service = new PersonalityRecognitionService();
    }
    
    @Test
    public void testAnalyzePersonality_ExtrovertType() {
        // 创建外向型特征：语速快、音量大
        VoiceFeatures features = new VoiceFeatures();
        features.setSpeedWpm(180); // 快语速
        features.setVolumeMean(0.8); // 大音量
        features.setVolumeStd(0.15); // 音量变化大
        features.setPitchStd(30.0);
        features.setAudioQuality(0.9);
        
        String text = "太棒了！今天真开心！";
        
        PersonalityTraits traits = service.analyzePersonality(features, text);
        
        assertNotNull(traits);
        assertTrue(traits.getExtroversionScore() > 0.5, "应该识别为外向型");
        assertNotNull(traits.getPrimaryType());
        assertTrue(traits.getPrimaryType().contains("外向"));
        assertFalse(traits.getTags().isEmpty());
        assertTrue(traits.getConfidence() > 0);
    }
    
    @Test
    public void testAnalyzePersonality_IntrovertType() {
        // 创建内向型特征：语速慢、音量小
        VoiceFeatures features = new VoiceFeatures();
        features.setSpeedWpm(100); // 慢语速
        features.setVolumeMean(0.3); // 小音量
        features.setVolumeStd(0.05); // 音量变化小
        features.setPitchStd(15.0);
        features.setAudioQuality(0.8);
        
        String text = "嗯，我觉得还好吧。";
        
        PersonalityTraits traits = service.analyzePersonality(features, text);
        
        assertNotNull(traits);
        assertTrue(traits.getExtroversionScore() < 0.5, "应该识别为内向型");
        assertNotNull(traits.getPrimaryType());
        assertTrue(traits.getPrimaryType().contains("内向"));
    }
    
    @Test
    public void testAnalyzePersonality_RationalType() {
        // 创建理性型特征：语速平稳、音量稳定、逻辑词汇多
        VoiceFeatures features = new VoiceFeatures();
        features.setSpeedWpm(140); // 适中语速
        features.setVolumeMean(0.5); // 适中音量
        features.setVolumeStd(0.08); // 音量变化小
        features.setPitchStd(20.0); // 音高变化小
        features.setAudioQuality(0.85);
        
        String text = "因为这个原因，所以我认为应该这样做。";
        
        PersonalityTraits traits = service.analyzePersonality(features, text);
        
        assertNotNull(traits);
        assertTrue(traits.getRationalityScore() > 0.5, "应该识别为理性型");
        assertNotNull(traits.getPrimaryType());
        assertTrue(traits.getPrimaryType().contains("理性"));
    }
    
    @Test
    public void testAnalyzePersonality_EmotionalType() {
        // 创建感性型特征：音量变化大、情感词汇多
        VoiceFeatures features = new VoiceFeatures();
        features.setSpeedWpm(160);
        features.setVolumeMean(0.6);
        features.setVolumeStd(0.2); // 音量变化大
        features.setPitchStd(40.0); // 音高变化大
        features.setAudioQuality(0.8);
        
        String text = "好开心啊！太棒了！真的很喜欢！";
        
        PersonalityTraits traits = service.analyzePersonality(features, text);
        
        assertNotNull(traits);
        assertTrue(traits.getRationalityScore() < 0.6, "应该识别为感性型");
        assertNotNull(traits.getPrimaryType());
        assertTrue(traits.getPrimaryType().contains("感性"));
    }
    
    @Test
    public void testAnalyzePersonality_WithNullText() {
        VoiceFeatures features = new VoiceFeatures();
        features.setSpeedWpm(150);
        features.setVolumeMean(0.5);
        features.setVolumeStd(0.1);
        features.setPitchStd(25.0);
        features.setAudioQuality(0.8);
        
        PersonalityTraits traits = service.analyzePersonality(features, null);
        
        assertNotNull(traits);
        assertNotNull(traits.getPrimaryType());
        assertTrue(traits.getConfidence() > 0);
    }
    
    @Test
    public void testAnalyzePersonality_WithEmptyText() {
        VoiceFeatures features = new VoiceFeatures();
        features.setSpeedWpm(150);
        features.setVolumeMean(0.5);
        features.setVolumeStd(0.1);
        features.setPitchStd(25.0);
        features.setAudioQuality(0.8);
        
        PersonalityTraits traits = service.analyzePersonality(features, "");
        
        assertNotNull(traits);
        assertNotNull(traits.getPrimaryType());
    }
    
    @Test
    public void testAnalyzePersonalityBatch() {
        // 创建多个样本
        VoiceFeatures features1 = new VoiceFeatures();
        features1.setSpeedWpm(180);
        features1.setVolumeMean(0.8);
        features1.setVolumeStd(0.15);
        features1.setPitchStd(30.0);
        features1.setAudioQuality(0.9);
        
        VoiceFeatures features2 = new VoiceFeatures();
        features2.setSpeedWpm(170);
        features2.setVolumeMean(0.75);
        features2.setVolumeStd(0.12);
        features2.setPitchStd(28.0);
        features2.setAudioQuality(0.85);
        
        List<VoiceFeatures> featuresList = Arrays.asList(features1, features2);
        List<String> textList = Arrays.asList("太棒了！", "真开心！");
        
        PersonalityTraits traits = service.analyzePersonalityBatch(featuresList, textList);
        
        assertNotNull(traits);
        assertNotNull(traits.getPrimaryType());
        assertTrue(traits.getConfidence() > 0);
        assertFalse(traits.getTags().isEmpty());
    }
    
    @Test
    public void testAnalyzePersonalityBatch_WithNullTextList() {
        VoiceFeatures features = new VoiceFeatures();
        features.setSpeedWpm(150);
        features.setVolumeMean(0.5);
        features.setVolumeStd(0.1);
        features.setPitchStd(25.0);
        features.setAudioQuality(0.8);
        
        List<VoiceFeatures> featuresList = Arrays.asList(features);
        
        PersonalityTraits traits = service.analyzePersonalityBatch(featuresList, null);
        
        assertNotNull(traits);
        assertNotNull(traits.getPrimaryType());
    }
    
    @Test
    public void testAnalyzePersonalityBatch_EmptyList() {
        List<VoiceFeatures> featuresList = Arrays.asList();
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.analyzePersonalityBatch(featuresList, null);
        });
    }
    
    @Test
    public void testAnalyzePersonalityBatch_NullList() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.analyzePersonalityBatch(null, null);
        });
    }
    
    @Test
    public void testPersonalityTraits_GettersAndSetters() {
        PersonalityTraits traits = new PersonalityTraits();
        
        traits.setPrimaryType("理性外向型");
        assertEquals("理性外向型", traits.getPrimaryType());
        
        traits.setExtroversionScore(0.8);
        assertEquals(0.8, traits.getExtroversionScore());
        
        traits.setRationalityScore(0.7);
        assertEquals(0.7, traits.getRationalityScore());
        
        traits.setConfidence(0.9);
        assertEquals(0.9, traits.getConfidence());
        
        assertNotNull(traits.getDimensionScores());
        assertNotNull(traits.getTags());
    }
}

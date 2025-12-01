package com.example.voicebox.app.device.emotional;

import com.example.voicebox.app.device.domain.EmotionalVoiceMessage;
import com.example.voicebox.app.device.domain.EmotionalVoiceMessage.MessageType;
import com.example.voicebox.app.device.domain.EmotionalVoiceSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmotionalVoiceMessage 实体类单元测试
 */
public class EmotionalVoiceMessageTest {
    
    private EmotionalVoiceMessage message;
    
    @BeforeEach
    public void setUp() {
        message = new EmotionalVoiceMessage();
    }
    
    @Test
    public void testDefaultConstructor() {
        assertNotNull(message.getCreatedAt());
        assertNull(message.getProcessedAt());
    }
    
    @Test
    public void testGettersAndSetters() {
        // 测试 session
        EmotionalVoiceSession session = new EmotionalVoiceSession();
        message.setSession(session);
        assertEquals(session, message.getSession());
        
        // 测试 userId
        message.setUserId(123L);
        assertEquals(123L, message.getUserId());
        
        // 测试 messageType
        message.setMessageType(MessageType.USER_INPUT);
        assertEquals(MessageType.USER_INPUT, message.getMessageType());
        
        // 测试 content
        message.setContent("Hello world");
        assertEquals("Hello world", message.getContent());
        
        // 测试 audioFilePath
        message.setAudioFilePath("/path/to/audio.wav");
        assertEquals("/path/to/audio.wav", message.getAudioFilePath());
        
        // 测试 audioDurationSeconds
        message.setAudioDurationSeconds(30);
        assertEquals(30, message.getAudioDurationSeconds());
        
        // 测试 detectedEmotion
        message.setDetectedEmotion("HAPPY");
        assertEquals("HAPPY", message.getDetectedEmotion());
        
        // 测试 emotionConfidence
        message.setEmotionConfidence(0.95);
        assertEquals(0.95, message.getEmotionConfidence());
        
        // 测试 emotionIntensity
        message.setEmotionIntensity(0.8);
        assertEquals(0.8, message.getEmotionIntensity());
        
        // 测试 sentimentScore
        message.setSentimentScore(0.7);
        assertEquals(0.7, message.getSentimentScore());
        
        // 测试 voicePitchAvg
        message.setVoicePitchAvg(220.5);
        assertEquals(220.5, message.getVoicePitchAvg());
        
        // 测试 voiceSpeedWpm
        message.setVoiceSpeedWpm(150);
        assertEquals(150, message.getVoiceSpeedWpm());
        
        // 测试 voiceVolumeDb
        message.setVoiceVolumeDb(65.5);
        assertEquals(65.5, message.getVoiceVolumeDb());
        
        // 测试 voiceTone
        message.setVoiceTone("WARM");
        assertEquals("WARM", message.getVoiceTone());
        
        // 测试 isDirectedToSystem
        message.setIsDirectedToSystem(true);
        assertTrue(message.getIsDirectedToSystem());
        
        // 测试 triggerConfidence
        message.setTriggerConfidence(0.9);
        assertEquals(0.9, message.getTriggerConfidence());
        
        // 测试 speechDirection
        message.setSpeechDirection("TO_SYSTEM");
        assertEquals("TO_SYSTEM", message.getSpeechDirection());
        
        // 测试 responseStrategy
        message.setResponseStrategy("EMPATHETIC");
        assertEquals("EMPATHETIC", message.getResponseStrategy());
        
        // 测试 emotionalResponseType
        message.setEmotionalResponseType("SUPPORTIVE");
        assertEquals("SUPPORTIVE", message.getEmotionalResponseType());
        
        // 测试 voiceStyleUsed
        message.setVoiceStyleUsed("GENTLE");
        assertEquals("GENTLE", message.getVoiceStyleUsed());
    }
    
    @Test
    public void testMarkAsProcessed() {
        assertNull(message.getProcessedAt());
        message.markAsProcessed();
        assertNotNull(message.getProcessedAt());
    }
    
    @Test
    public void testShouldTriggerResponse_NotUserInput() {
        message.setMessageType(MessageType.SYSTEM_RESPONSE);
        message.setIsDirectedToSystem(true);
        message.setTriggerConfidence(0.9);
        
        assertFalse(message.shouldTriggerResponse());
    }
    
    @Test
    public void testShouldTriggerResponse_NotDirectedToSystem() {
        message.setMessageType(MessageType.USER_INPUT);
        message.setIsDirectedToSystem(false);
        message.setTriggerConfidence(0.9);
        
        assertFalse(message.shouldTriggerResponse());
    }
    
    @Test
    public void testShouldTriggerResponse_NullDirectedToSystem() {
        message.setMessageType(MessageType.USER_INPUT);
        message.setIsDirectedToSystem(null);
        message.setTriggerConfidence(0.9);
        
        assertFalse(message.shouldTriggerResponse());
    }
    
    @Test
    public void testShouldTriggerResponse_NullTriggerConfidence() {
        message.setMessageType(MessageType.USER_INPUT);
        message.setIsDirectedToSystem(true);
        message.setTriggerConfidence(null);
        
        assertFalse(message.shouldTriggerResponse());
    }
    
    @Test
    public void testShouldTriggerResponse_LowConfidence() {
        message.setMessageType(MessageType.USER_INPUT);
        message.setIsDirectedToSystem(true);
        message.setTriggerConfidence(0.8);
        
        assertFalse(message.shouldTriggerResponse());
    }
    
    @Test
    public void testShouldTriggerResponse_HighConfidence() {
        message.setMessageType(MessageType.USER_INPUT);
        message.setIsDirectedToSystem(true);
        message.setTriggerConfidence(0.9);
        
        assertTrue(message.shouldTriggerResponse());
    }
    
    @Test
    public void testShouldTriggerResponse_ExactThreshold() {
        message.setMessageType(MessageType.USER_INPUT);
        message.setIsDirectedToSystem(true);
        message.setTriggerConfidence(0.85);
        
        assertFalse(message.shouldTriggerResponse());
    }
    
    @Test
    public void testShouldTriggerResponse_JustAboveThreshold() {
        message.setMessageType(MessageType.USER_INPUT);
        message.setIsDirectedToSystem(true);
        message.setTriggerConfidence(0.86);
        
        assertTrue(message.shouldTriggerResponse());
    }
}

package com.example.voicebox.app.device.emotional;

import com.example.voicebox.app.device.domain.EmotionalVoiceSession;
import com.example.voicebox.app.device.domain.EmotionalVoiceSession.SessionType;
import com.example.voicebox.app.device.domain.EmotionalVoiceSession.MoodContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmotionalVoiceSession 实体类单元测试
 */
public class EmotionalVoiceSessionTest {
    
    private EmotionalVoiceSession session;
    
    @BeforeEach
    public void setUp() {
        session = new EmotionalVoiceSession();
    }
    
    @Test
    public void testDefaultConstructor() {
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getStartTime());
        assertEquals(0, session.getMessageCount());
        assertNull(session.getEndTime());
    }
    
    @Test
    public void testParameterizedConstructor() {
        EmotionalVoiceSession session2 = new EmotionalVoiceSession(123L, SessionType.CASUAL_CHAT);
        
        assertEquals(123L, session2.getUserId());
        assertEquals(SessionType.CASUAL_CHAT, session2.getSessionType());
        assertNotNull(session2.getCreatedAt());
        assertNotNull(session2.getStartTime());
        assertEquals(0, session2.getMessageCount());
    }
    
    @Test
    public void testGettersAndSetters() {
        // 测试 userId
        session.setUserId(123L);
        assertEquals(123L, session.getUserId());
        
        // 测试 sessionName
        session.setSessionName("Test Session");
        assertEquals("Test Session", session.getSessionName());
        
        // 测试 sessionType
        session.setSessionType(SessionType.EMOTIONAL_SUPPORT);
        assertEquals(SessionType.EMOTIONAL_SUPPORT, session.getSessionType());
        
        // 测试 moodContext
        session.setMoodContext(MoodContext.HAPPY);
        assertEquals(MoodContext.HAPPY, session.getMoodContext());
        
        // 测试 startTime
        LocalDateTime now = LocalDateTime.now();
        session.setStartTime(now);
        assertEquals(now, session.getStartTime());
        
        // 测试 endTime
        session.setEndTime(now);
        assertEquals(now, session.getEndTime());
        
        // 测试 totalDurationSeconds
        session.setTotalDurationSeconds(300);
        assertEquals(300, session.getTotalDurationSeconds());
        
        // 测试 messageCount
        session.setMessageCount(10);
        assertEquals(10, session.getMessageCount());
        
        // 测试 dominantEmotion
        session.setDominantEmotion("HAPPY");
        assertEquals("HAPPY", session.getDominantEmotion());
        
        // 测试 emotionIntensity
        session.setEmotionIntensity(0.8);
        assertEquals(0.8, session.getEmotionIntensity());
        
        // 测试 userSatisfactionScore
        session.setUserSatisfactionScore(4.5);
        assertEquals(4.5, session.getUserSatisfactionScore());
    }
    
    @Test
    public void testEndSession() {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(10);
        session.setStartTime(startTime);
        
        assertNull(session.getEndTime());
        assertNull(session.getTotalDurationSeconds());
        
        session.endSession();
        
        assertNotNull(session.getEndTime());
        assertNotNull(session.getTotalDurationSeconds());
        assertNotNull(session.getUpdatedAt());
        assertTrue(session.getTotalDurationSeconds() > 0);
    }
    
    @Test
    public void testIncrementMessageCount() {
        assertEquals(0, session.getMessageCount());
        
        session.incrementMessageCount();
        assertEquals(1, session.getMessageCount());
        assertNotNull(session.getUpdatedAt());
        
        session.incrementMessageCount();
        assertEquals(2, session.getMessageCount());
        
        session.incrementMessageCount();
        assertEquals(3, session.getMessageCount());
    }
    
    @Test
    public void testIncrementMessageCount_FromNull() {
        session.setMessageCount(null);
        
        session.incrementMessageCount();
        assertEquals(1, session.getMessageCount());
    }
    
    @Test
    public void testUpdateDominantEmotion() {
        assertNull(session.getDominantEmotion());
        assertNull(session.getEmotionIntensity());
        
        session.updateDominantEmotion("HAPPY", 0.8);
        
        assertEquals("HAPPY", session.getDominantEmotion());
        assertEquals(0.8, session.getEmotionIntensity());
        assertNotNull(session.getUpdatedAt());
    }
    
    @Test
    public void testIsActive_WhenEndTimeIsNull() {
        session.setEndTime(null);
        assertTrue(session.isActive());
    }
    
    @Test
    public void testIsActive_WhenEndTimeIsSet() {
        session.setEndTime(LocalDateTime.now());
        assertFalse(session.isActive());
    }
    
    @Test
    public void testGetDurationMinutes_WhenNull() {
        session.setTotalDurationSeconds(null);
        assertNull(session.getDurationMinutes());
    }
    
    @Test
    public void testGetDurationMinutes_WithSeconds() {
        session.setTotalDurationSeconds(300); // 5 minutes
        assertEquals(5, session.getDurationMinutes());
    }
    
    @Test
    public void testGetDurationMinutes_WithPartialMinute() {
        session.setTotalDurationSeconds(330); // 5.5 minutes
        assertEquals(5, session.getDurationMinutes()); // 应该向下取整
    }
    
    @Test
    public void testGetDurationMinutes_LessThanOneMinute() {
        session.setTotalDurationSeconds(30); // 0.5 minutes
        assertEquals(0, session.getDurationMinutes());
    }
}

package com.example.voicebox.app.device.emotional;

import com.example.voicebox.app.device.domain.UserEmotionalProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserEmotionalProfile 实体类单元测试
 */
public class UserEmotionalProfileTest {
    
    private UserEmotionalProfile profile;
    
    @BeforeEach
    public void setUp() {
        profile = new UserEmotionalProfile();
    }
    
    @Test
    public void testDefaultConstructor() {
        assertNotNull(profile.getCreatedAt());
        assertEquals(0, profile.getTotalSessions());
        assertEquals(0.5, profile.getEmotionalStability());
        assertEquals(0.5, profile.getExtroversionLevel());
        assertEquals(0.5, profile.getOptimismLevel());
        assertEquals(0.5, profile.getSensitivityLevel());
    }
    
    @Test
    public void testGettersAndSetters() {
        // 测试 userId
        profile.setUserId(123L);
        assertEquals(123L, profile.getUserId());
        
        // 测试 dominantPersonalityType
        profile.setDominantPersonalityType("EXTROVERT");
        assertEquals("EXTROVERT", profile.getDominantPersonalityType());
        
        // 测试 emotionalStability
        profile.setEmotionalStability(0.8);
        assertEquals(0.8, profile.getEmotionalStability());
        
        // 测试 extroversionLevel
        profile.setExtroversionLevel(0.9);
        assertEquals(0.9, profile.getExtroversionLevel());
        
        // 测试 optimismLevel
        profile.setOptimismLevel(0.7);
        assertEquals(0.7, profile.getOptimismLevel());
        
        // 测试 sensitivityLevel
        profile.setSensitivityLevel(0.6);
        assertEquals(0.6, profile.getSensitivityLevel());
        
        // 测试 preferredVoiceStyle
        profile.setPreferredVoiceStyle("WARM");
        assertEquals("WARM", profile.getPreferredVoiceStyle());
        
        // 测试 totalSessions
        profile.setTotalSessions(10);
        assertEquals(10, profile.getTotalSessions());
        
        // 测试 createdAt
        LocalDateTime now = LocalDateTime.now();
        profile.setCreatedAt(now);
        assertEquals(now, profile.getCreatedAt());
        
        // 测试 updatedAt
        profile.setUpdatedAt(now);
        assertEquals(now, profile.getUpdatedAt());
        
        // 测试 lastInteractionAt
        profile.setLastInteractionAt(now);
        assertEquals(now, profile.getLastInteractionAt());
        
        // 测试 preferredLanguage
        profile.setPreferredLanguage("zh-CN");
        assertEquals("zh-CN", profile.getPreferredLanguage());
        
        // 测试 mostFrequentEmotion
        profile.setMostFrequentEmotion("HAPPY");
        assertEquals("HAPPY", profile.getMostFrequentEmotion());
    }
    
    @Test
    public void testIsNewUser_WithNullSessions() {
        profile.setTotalSessions(null);
        assertTrue(profile.isNewUser());
    }
    
    @Test
    public void testIsNewUser_WithZeroSessions() {
        profile.setTotalSessions(0);
        assertTrue(profile.isNewUser());
    }
    
    @Test
    public void testIsNewUser_WithTwoSessions() {
        profile.setTotalSessions(2);
        assertTrue(profile.isNewUser());
    }
    
    @Test
    public void testIsNewUser_WithThreeSessions() {
        profile.setTotalSessions(3);
        assertFalse(profile.isNewUser());
    }
    
    @Test
    public void testIsNewUser_WithManySessions() {
        profile.setTotalSessions(10);
        assertFalse(profile.isNewUser());
    }
}

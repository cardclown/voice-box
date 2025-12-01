package com.example.voicebox.app.device.repository;

import com.example.voicebox.app.device.domain.EmotionalVoiceMessage;
import com.example.voicebox.app.device.domain.EmotionalVoiceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 情感语音消息数据访问层
 */
@Repository
public interface EmotionalVoiceMessageRepository extends JpaRepository<EmotionalVoiceMessage, Long> {
    
    /**
     * 根据会话ID查找消息
     */
    List<EmotionalVoiceMessage> findBySessionOrderByCreatedAtAsc(EmotionalVoiceSession session);
    
    /**
     * 根据会话ID查找消息（按时间倒序）
     */
    List<EmotionalVoiceMessage> findBySessionOrderByCreatedAtDesc(EmotionalVoiceSession session);
    
    /**
     * 根据用户ID查找消息
     */
    List<EmotionalVoiceMessage> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 根据消息类型查找消息
     */
    List<EmotionalVoiceMessage> findBySessionAndMessageTypeOrderByCreatedAtAsc(
            EmotionalVoiceSession session, EmotionalVoiceMessage.MessageType messageType);
    
    /**
     * 查找用户输入的消息
     */
    @Query("SELECT m FROM EmotionalVoiceMessage m WHERE m.userId = :userId " +
           "AND m.messageType = 'USER_INPUT' ORDER BY m.createdAt DESC")
    List<EmotionalVoiceMessage> findUserInputsByUserId(@Param("userId") Long userId);
    
    /**
     * 查找系统回复的消息
     */
    @Query("SELECT m FROM EmotionalVoiceMessage m WHERE m.userId = :userId " +
           "AND m.messageType = 'SYSTEM_RESPONSE' ORDER BY m.createdAt DESC")
    List<EmotionalVoiceMessage> findSystemResponsesByUserId(@Param("userId") Long userId);
    
    /**
     * 根据检测到的情绪查找消息
     */
    List<EmotionalVoiceMessage> findByUserIdAndDetectedEmotionOrderByCreatedAtDesc(
            Long userId, String detectedEmotion);
    
    /**
     * 查找需要触发系统响应的消息
     */
    @Query("SELECT m FROM EmotionalVoiceMessage m WHERE m.userId = :userId " +
           "AND m.messageType = 'USER_INPUT' AND m.isDirectedToSystem = true " +
           "AND m.triggerConfidence > :minConfidence ORDER BY m.createdAt DESC")
    List<EmotionalVoiceMessage> findTriggeredMessagesByUserId(
            @Param("userId") Long userId,
            @Param("minConfidence") Double minConfidence);
    
    /**
     * 查找指定时间范围内的消息
     */
    @Query("SELECT m FROM EmotionalVoiceMessage m WHERE m.userId = :userId " +
           "AND m.createdAt >= :startTime AND m.createdAt <= :endTime " +
           "ORDER BY m.createdAt DESC")
    List<EmotionalVoiceMessage> findByUserIdAndTimeRange(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计用户的消息数量
     */
    @Query("SELECT COUNT(m) FROM EmotionalVoiceMessage m WHERE m.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * 统计用户输入消息数量
     */
    @Query("SELECT COUNT(m) FROM EmotionalVoiceMessage m WHERE m.userId = :userId " +
           "AND m.messageType = 'USER_INPUT'")
    Long countUserInputsByUserId(@Param("userId") Long userId);
    
    /**
     * 计算用户的总语音时长（秒）
     */
    @Query("SELECT COALESCE(SUM(m.audioDurationSeconds), 0) FROM EmotionalVoiceMessage m " +
           "WHERE m.userId = :userId AND m.audioDurationSeconds IS NOT NULL")
    Long sumAudioDurationByUserId(@Param("userId") Long userId);
    
    /**
     * 获取用户的情绪分布统计
     */
    @Query("SELECT m.detectedEmotion, COUNT(m) FROM EmotionalVoiceMessage m " +
           "WHERE m.userId = :userId AND m.detectedEmotion IS NOT NULL " +
           "AND m.messageType = 'USER_INPUT' " +
           "GROUP BY m.detectedEmotion ORDER BY COUNT(m) DESC")
    List<Object[]> getEmotionDistributionByUserId(@Param("userId") Long userId);
    
    /**
     * 计算用户的平均情绪强度
     */
    @Query("SELECT AVG(m.emotionIntensity) FROM EmotionalVoiceMessage m " +
           "WHERE m.userId = :userId AND m.emotionIntensity IS NOT NULL " +
           "AND m.messageType = 'USER_INPUT'")
    Double getAverageEmotionIntensityByUserId(@Param("userId") Long userId);
    
    /**
     * 计算用户的平均情感分数
     */
    @Query("SELECT AVG(m.sentimentScore) FROM EmotionalVoiceMessage m " +
           "WHERE m.userId = :userId AND m.sentimentScore IS NOT NULL " +
           "AND m.messageType = 'USER_INPUT'")
    Double getAverageSentimentScoreByUserId(@Param("userId") Long userId);
    
    /**
     * 查找最近的用户输入消息
     */
    @Query("SELECT m FROM EmotionalVoiceMessage m WHERE m.userId = :userId " +
           "AND m.messageType = 'USER_INPUT' ORDER BY m.createdAt DESC")
    List<EmotionalVoiceMessage> findRecentUserInputsByUserId(@Param("userId") Long userId);
    
    /**
     * 查找未处理的消息
     */
    @Query("SELECT m FROM EmotionalVoiceMessage m WHERE m.processedAt IS NULL " +
           "ORDER BY m.createdAt ASC")
    List<EmotionalVoiceMessage> findUnprocessedMessages();
    
    /**
     * 查找正面情绪的消息
     */
    @Query("SELECT m FROM EmotionalVoiceMessage m WHERE m.userId = :userId " +
           "AND m.sentimentScore > 0.1 AND m.messageType = 'USER_INPUT' " +
           "ORDER BY m.createdAt DESC")
    List<EmotionalVoiceMessage> findPositiveMessagesByUserId(@Param("userId") Long userId);
    
    /**
     * 查找负面情绪的消息
     */
    @Query("SELECT m FROM EmotionalVoiceMessage m WHERE m.userId = :userId " +
           "AND m.sentimentScore < -0.1 AND m.messageType = 'USER_INPUT' " +
           "ORDER BY m.createdAt DESC")
    List<EmotionalVoiceMessage> findNegativeMessagesByUserId(@Param("userId") Long userId);
    
    /**
     * 根据语音特征查找消息
     */
    @Query("SELECT m FROM EmotionalVoiceMessage m WHERE m.userId = :userId " +
           "AND m.voiceTone = :tone ORDER BY m.createdAt DESC")
    List<EmotionalVoiceMessage> findByUserIdAndVoiceTone(
            @Param("userId") Long userId,
            @Param("tone") String tone);
    
    /**
     * 查找高置信度的触发消息
     */
    @Query("SELECT m FROM EmotionalVoiceMessage m WHERE m.userId = :userId " +
           "AND m.triggerConfidence > :minConfidence " +
           "AND m.isDirectedToSystem = true ORDER BY m.createdAt DESC")
    List<EmotionalVoiceMessage> findHighConfidenceTriggeredMessages(
            @Param("userId") Long userId,
            @Param("minConfidence") Double minConfidence);
    
    /**
     * 删除用户的所有消息数据
     */
    void deleteByUserId(Long userId);
    
    /**
     * 删除会话的所有消息
     */
    void deleteBySession(EmotionalVoiceSession session);
}

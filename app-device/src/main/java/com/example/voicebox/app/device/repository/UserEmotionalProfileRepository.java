package com.example.voicebox.app.device.repository;

import com.example.voicebox.app.device.domain.UserEmotionalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户情感画像数据访问层
 */
@Repository
public interface UserEmotionalProfileRepository extends JpaRepository<UserEmotionalProfile, Long> {
    
    /**
     * 根据用户ID查找情感画像
     */
    Optional<UserEmotionalProfile> findByUserId(Long userId);
    
    /**
     * 检查用户是否存在情感画像
     */
    boolean existsByUserId(Long userId);
    
    /**
     * 根据主导性格类型查找用户
     */
    List<UserEmotionalProfile> findByDominantPersonalityType(String personalityType);
    
    /**
     * 根据首选语音风格查找用户
     */
    List<UserEmotionalProfile> findByPreferredVoiceStyle(String voiceStyle);
    
    /**
     * 查找新用户（会话数少于指定数量）
     */
    @Query("SELECT p FROM UserEmotionalProfile p WHERE p.totalSessions < :maxSessions")
    List<UserEmotionalProfile> findNewUsers(@Param("maxSessions") Integer maxSessions);
    
    /**
     * 查找活跃用户（最近交互时间在指定时间之后）
     */
    @Query("SELECT p FROM UserEmotionalProfile p WHERE p.lastInteractionAt >= :sinceDate " +
           "ORDER BY p.lastInteractionAt DESC")
    List<UserEmotionalProfile> findActiveUsersSince(@Param("sinceDate") LocalDateTime sinceDate);
    
    /**
     * 查找不活跃用户（最近交互时间在指定时间之前）
     */
    @Query("SELECT p FROM UserEmotionalProfile p WHERE p.lastInteractionAt < :beforeDate " +
           "ORDER BY p.lastInteractionAt ASC")
    List<UserEmotionalProfile> findInactiveUsersBefore(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * 根据情绪稳定性范围查找用户
     */
    @Query("SELECT p FROM UserEmotionalProfile p WHERE p.emotionalStability >= :minStability " +
           "AND p.emotionalStability <= :maxStability")
    List<UserEmotionalProfile> findByEmotionalStabilityRange(
            @Param("minStability") Double minStability,
            @Param("maxStability") Double maxStability);
    
    /**
     * 根据外向程度范围查找用户
     */
    @Query("SELECT p FROM UserEmotionalProfile p WHERE p.extroversionLevel >= :minLevel " +
           "AND p.extroversionLevel <= :maxLevel")
    List<UserEmotionalProfile> findByExtroversionRange(
            @Param("minLevel") Double minLevel,
            @Param("maxLevel") Double maxLevel);
    
    /**
     * 根据敏感程度范围查找用户
     */
    @Query("SELECT p FROM UserEmotionalProfile p WHERE p.sensitivityLevel >= :minLevel " +
           "AND p.sensitivityLevel <= :maxLevel")
    List<UserEmotionalProfile> findBySensitivityRange(
            @Param("minLevel") Double minLevel,
            @Param("maxLevel") Double maxLevel);
    
    /**
     * 查找高满意度用户
     */
    @Query("SELECT p FROM UserEmotionalProfile p WHERE p.averageSessionSatisfaction >= :minSatisfaction " +
           "ORDER BY p.averageSessionSatisfaction DESC")
    List<UserEmotionalProfile> findHighSatisfactionUsers(@Param("minSatisfaction") Double minSatisfaction);
    
    /**
     * 查找低满意度用户
     */
    @Query("SELECT p FROM UserEmotionalProfile p WHERE p.averageSessionSatisfaction < :maxSatisfaction " +
           "AND p.averageSessionSatisfaction IS NOT NULL " +
           "ORDER BY p.averageSessionSatisfaction ASC")
    List<UserEmotionalProfile> findLowSatisfactionUsers(@Param("maxSatisfaction") Double maxSatisfaction);
    
    /**
     * 根据主要情感需求查找用户
     */
    List<UserEmotionalProfile> findByPrimaryEmotionalNeed(String emotionalNeed);
    
    /**
     * 根据最常见情绪查找用户
     */
    List<UserEmotionalProfile> findByMostFrequentEmotion(String emotion);
    
    /**
     * 统计不同性格类型的用户数量
     */
    @Query("SELECT p.dominantPersonalityType, COUNT(p) FROM UserEmotionalProfile p " +
           "WHERE p.dominantPersonalityType IS NOT NULL " +
           "GROUP BY p.dominantPersonalityType ORDER BY COUNT(p) DESC")
    List<Object[]> getPersonalityTypeDistribution();
    
    /**
     * 统计不同语音风格偏好的用户数量
     */
    @Query("SELECT p.preferredVoiceStyle, COUNT(p) FROM UserEmotionalProfile p " +
           "WHERE p.preferredVoiceStyle IS NOT NULL " +
           "GROUP BY p.preferredVoiceStyle ORDER BY COUNT(p) DESC")
    List<Object[]> getVoiceStyleDistribution();
    
    /**
     * 统计不同情感需求的用户数量
     */
    @Query("SELECT p.primaryEmotionalNeed, COUNT(p) FROM UserEmotionalProfile p " +
           "WHERE p.primaryEmotionalNeed IS NOT NULL " +
           "GROUP BY p.primaryEmotionalNeed ORDER BY COUNT(p) DESC")
    List<Object[]> getEmotionalNeedDistribution();
    
    /**
     * 计算所有用户的平均满意度
     */
    @Query("SELECT AVG(p.averageSessionSatisfaction) FROM UserEmotionalProfile p " +
           "WHERE p.averageSessionSatisfaction IS NOT NULL")
    Double getOverallAverageSatisfaction();
    
    /**
     * 计算所有用户的平均会话数
     */
    @Query("SELECT AVG(p.totalSessions) FROM UserEmotionalProfile p " +
           "WHERE p.totalSessions IS NOT NULL")
    Double getAverageTotalSessions();
    
    /**
     * 计算所有用户的平均交互时长
     */
    @Query("SELECT AVG(p.totalInteractionMinutes) FROM UserEmotionalProfile p " +
           "WHERE p.totalInteractionMinutes IS NOT NULL")
    Double getAverageInteractionMinutes();
    
    /**
     * 查找需要关注的用户（低满意度或不活跃）
     */
    @Query("SELECT p FROM UserEmotionalProfile p WHERE " +
           "(p.averageSessionSatisfaction < :minSatisfaction AND p.averageSessionSatisfaction IS NOT NULL) " +
           "OR p.lastInteractionAt < :inactiveDate " +
           "ORDER BY p.averageSessionSatisfaction ASC, p.lastInteractionAt ASC")
    List<UserEmotionalProfile> findUsersNeedingAttention(
            @Param("minSatisfaction") Double minSatisfaction,
            @Param("inactiveDate") LocalDateTime inactiveDate);
    
    /**
     * 查找高价值用户（高满意度且活跃）
     */
    @Query("SELECT p FROM UserEmotionalProfile p WHERE " +
           "p.averageSessionSatisfaction >= :minSatisfaction " +
           "AND p.totalSessions >= :minSessions " +
           "AND p.lastInteractionAt >= :recentDate " +
           "ORDER BY p.averageSessionSatisfaction DESC, p.totalSessions DESC")
    List<UserEmotionalProfile> findHighValueUsers(
            @Param("minSatisfaction") Double minSatisfaction,
            @Param("minSessions") Integer minSessions,
            @Param("recentDate") LocalDateTime recentDate);
    
    /**
     * 根据交互频率偏好查找用户
     */
    List<UserEmotionalProfile> findByInteractionFrequencyPreference(String frequencyPreference);
    
    /**
     * 删除用户的情感画像
     */
    void deleteByUserId(Long userId);
}

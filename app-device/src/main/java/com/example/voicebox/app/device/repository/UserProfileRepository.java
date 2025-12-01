package com.example.voicebox.app.device.repository;

import com.example.voicebox.app.device.domain.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * 用户画像数据访问层
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@Repository
public class UserProfileRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 根据用户ID查找用户画像
     */
    public UserProfile findByUserId(Long userId) {
        String sql = "SELECT * FROM user_profiles WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, 
                new BeanPropertyRowMapper<>(UserProfile.class), userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    /**
     * 创建用户画像
     */
    public UserProfile create(UserProfile profile) {
        String sql = "INSERT INTO user_profiles (user_id, openness, conscientiousness, " +
                    "extraversion, agreeableness, neuroticism, response_length_preference, " +
                    "language_style_preference, content_format_preference, interaction_style, " +
                    "total_messages, total_sessions, avg_session_duration, confidence_score) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, profile.getUserId());
            ps.setBigDecimal(2, profile.getOpenness());
            ps.setBigDecimal(3, profile.getConscientiousness());
            ps.setBigDecimal(4, profile.getExtraversion());
            ps.setBigDecimal(5, profile.getAgreeableness());
            ps.setBigDecimal(6, profile.getNeuroticism());
            ps.setString(7, profile.getResponseLengthPreference());
            ps.setString(8, profile.getLanguageStylePreference());
            ps.setString(9, profile.getContentFormatPreference());
            ps.setString(10, profile.getInteractionStyle());
            ps.setInt(11, profile.getTotalMessages());
            ps.setInt(12, profile.getTotalSessions());
            ps.setBigDecimal(13, profile.getAvgSessionDuration());
            ps.setBigDecimal(14, profile.getConfidenceScore());
            return ps;
        }, keyHolder);
        
        profile.setId(keyHolder.getKey().longValue());
        return profile;
    }
    
    /**
     * 更新用户画像
     */
    public UserProfile update(UserProfile profile) {
        String sql = "UPDATE user_profiles SET openness = ?, conscientiousness = ?, " +
                    "extraversion = ?, agreeableness = ?, neuroticism = ?, " +
                    "response_length_preference = ?, language_style_preference = ?, " +
                    "content_format_preference = ?, interaction_style = ?, " +
                    "total_messages = ?, total_sessions = ?, avg_session_duration = ?, " +
                    "confidence_score = ?, last_analyzed_at = NOW() " +
                    "WHERE id = ?";
        
        jdbcTemplate.update(sql,
            profile.getOpenness(),
            profile.getConscientiousness(),
            profile.getExtraversion(),
            profile.getAgreeableness(),
            profile.getNeuroticism(),
            profile.getResponseLengthPreference(),
            profile.getLanguageStylePreference(),
            profile.getContentFormatPreference(),
            profile.getInteractionStyle(),
            profile.getTotalMessages(),
            profile.getTotalSessions(),
            profile.getAvgSessionDuration(),
            profile.getConfidenceScore(),
            profile.getId()
        );
        
        return profile;
    }
    
    /**
     * 创建或更新用户画像
     */
    public UserProfile createOrUpdate(UserProfile profile) {
        UserProfile existing = findByUserId(profile.getUserId());
        if (existing != null) {
            profile.setId(existing.getId());
            return update(profile);
        } else {
            return create(profile);
        }
    }
    
    /**
     * 删除用户画像
     */
    public void delete(Long id) {
        String sql = "DELETE FROM user_profiles WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    /**
     * 根据用户ID删除
     */
    public void deleteByUserId(Long userId) {
        String sql = "DELETE FROM user_profiles WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }
    
    /**
     * 查找所有用户画像
     */
    public List<UserProfile> findAll() {
        String sql = "SELECT * FROM user_profiles ORDER BY updated_at DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserProfile.class));
    }
    
    /**
     * 分页查询用户画像
     */
    public List<UserProfile> findAll(int page, int size) {
        String sql = "SELECT * FROM user_profiles ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        int offset = page * size;
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserProfile.class), size, offset);
    }
    
    /**
     * 查找置信度大于指定值的用户画像
     */
    public List<UserProfile> findByConfidenceGreaterThan(double minConfidence) {
        String sql = "SELECT * FROM user_profiles WHERE confidence_score > ? " +
                    "ORDER BY confidence_score DESC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(UserProfile.class), minConfidence);
    }
    
    /**
     * 查找需要更新的用户画像
     */
    public List<UserProfile> findNeedingUpdate(int daysThreshold) {
        String sql = "SELECT * FROM user_profiles WHERE " +
                    "last_analyzed_at IS NULL OR " +
                    "last_analyzed_at < DATE_SUB(NOW(), INTERVAL ? DAY) " +
                    "ORDER BY last_analyzed_at ASC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(UserProfile.class), daysThreshold);
    }
    
    /**
     * 统计用户画像数量
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM user_profiles";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
    
    /**
     * 更新用户统计信息
     */
    public void updateStatistics(Long userId, int totalMessages, int totalSessions, 
                               BigDecimal avgSessionDuration) {
        String sql = "UPDATE user_profiles SET total_messages = ?, total_sessions = ?, " +
                    "avg_session_duration = ? WHERE user_id = ?";
        jdbcTemplate.update(sql, totalMessages, totalSessions, avgSessionDuration, userId);
    }
    
    /**
     * 增加消息计数
     */
    public void incrementMessageCount(Long userId) {
        String sql = "UPDATE user_profiles SET total_messages = total_messages + 1 " +
                    "WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }
    
    /**
     * 检查用户画像是否存在
     */
    public boolean existsByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM user_profiles WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }
    
    /**
     * 获取性格维度统计
     */
    public Map<String, Object> getPersonalityStatistics() {
        String sql = "SELECT " +
                    "AVG(openness) as avg_openness, " +
                    "AVG(conscientiousness) as avg_conscientiousness, " +
                    "AVG(extraversion) as avg_extraversion, " +
                    "AVG(agreeableness) as avg_agreeableness, " +
                    "AVG(neuroticism) as avg_neuroticism, " +
                    "AVG(confidence_score) as avg_confidence " +
                    "FROM user_profiles WHERE confidence_score > 0.3";
        
        return jdbcTemplate.queryForMap(sql);
    }
}

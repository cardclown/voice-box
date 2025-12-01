package com.example.voicebox.app.device.repository;

import com.example.voicebox.app.device.domain.UserFeedback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户反馈数据访问层
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@Repository
public class UserFeedbackRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 创建用户反馈记录
     */
    public UserFeedback create(UserFeedback feedback) {
        String sql = "INSERT INTO user_feedback (user_id, session_id, message_id, " +
                    "feedback_type, feedback_value, feedback_text, feedback_tags, " +
                    "ai_response_id, response_strategy) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, feedback.getUserId());
            ps.setLong(2, feedback.getSessionId());
            ps.setLong(3, feedback.getMessageId());
            ps.setString(4, feedback.getFeedbackType());
            ps.setObject(5, feedback.getFeedbackValue());
            ps.setString(6, feedback.getFeedbackText());
            ps.setString(7, feedback.getFeedbackTags());
            ps.setObject(8, feedback.getAiResponseId());
            ps.setString(9, feedback.getResponseStrategy());
            return ps;
        }, keyHolder);
        
        feedback.setId(keyHolder.getKey().longValue());
        return feedback;
    }
    
    /**
     * 根据ID查找反馈
     */
    public UserFeedback findById(Long id) {
        String sql = "SELECT * FROM user_feedback WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, 
                new BeanPropertyRowMapper<>(UserFeedback.class), id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    /**
     * 查找用户的所有反馈
     */
    public List<UserFeedback> findByUserId(Long userId) {
        String sql = "SELECT * FROM user_feedback WHERE user_id = ? " +
                    "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(UserFeedback.class), userId);
    }
    
    /**
     * 查找用户最近的反馈
     */
    public List<UserFeedback> findRecentByUserId(Long userId, int limit) {
        String sql = "SELECT * FROM user_feedback WHERE user_id = ? " +
                    "ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(UserFeedback.class), userId, limit);
    }
    
    /**
     * 查找会话的所有反馈
     */
    public List<UserFeedback> findBySessionId(Long sessionId) {
        String sql = "SELECT * FROM user_feedback WHERE session_id = ? " +
                    "ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(UserFeedback.class), sessionId);
    }
    
    /**
     * 根据消息ID查找反馈
     */
    public List<UserFeedback> findByMessageId(Long messageId) {
        String sql = "SELECT * FROM user_feedback WHERE message_id = ? " +
                    "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(UserFeedback.class), messageId);
    }
    
    /**
     * 根据反馈类型查找
     */
    public List<UserFeedback> findByFeedbackType(Long userId, String feedbackType) {
        String sql = "SELECT * FROM user_feedback WHERE user_id = ? AND feedback_type = ? " +
                    "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(UserFeedback.class), userId, feedbackType);
    }
    
    /**
     * 查找正面反馈
     */
    public List<UserFeedback> findPositiveFeedback(Long userId) {
        String sql = "SELECT * FROM user_feedback WHERE user_id = ? AND feedback_value > 0 " +
                    "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(UserFeedback.class), userId);
    }
    
    /**
     * 查找负面反馈
     */
    public List<UserFeedback> findNegativeFeedback(Long userId) {
        String sql = "SELECT * FROM user_feedback WHERE user_id = ? AND feedback_value < 0 " +
                    "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(UserFeedback.class), userId);
    }
    
    /**
     * 统计用户的反馈数量
     */
    public long countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM user_feedback WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, userId);
    }
    
    /**
     * 统计反馈类型分布
     */
    public List<Map<String, Object>> getFeedbackTypeDistribution(Long userId) {
        String sql = "SELECT feedback_type, COUNT(*) as count " +
                    "FROM user_feedback WHERE user_id = ? " +
                    "GROUP BY feedback_type ORDER BY count DESC";
        
        return jdbcTemplate.queryForList(sql, userId);
    }
    
    /**
     * 获取反馈统计
     */
    public Map<String, Object> getFeedbackStatistics(Long userId) {
        String sql = "SELECT " +
                    "SUM(CASE WHEN feedback_value > 0 THEN 1 ELSE 0 END) as positive_count, " +
                    "SUM(CASE WHEN feedback_value = 0 THEN 1 ELSE 0 END) as neutral_count, " +
                    "SUM(CASE WHEN feedback_value < 0 THEN 1 ELSE 0 END) as negative_count, " +
                    "COUNT(*) as total_count, " +
                    "AVG(feedback_value) as avg_feedback_value " +
                    "FROM user_feedback WHERE user_id = ? AND feedback_value IS NOT NULL";
        
        return jdbcTemplate.queryForMap(sql, userId);
    }
    
    /**
     * 查找指定时间范围内的反馈
     */
    public List<UserFeedback> findByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        String sql = "SELECT * FROM user_feedback WHERE user_id = ? " +
                    "AND created_at BETWEEN ? AND ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(UserFeedback.class), 
            userId, startDate, endDate);
    }
    
    /**
     * 删除反馈
     */
    public void delete(Long id) {
        String sql = "DELETE FROM user_feedback WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    /**
     * 删除用户的所有反馈
     */
    public void deleteByUserId(Long userId) {
        String sql = "DELETE FROM user_feedback WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }
    
    /**
     * 删除旧的反馈记录
     */
    public int deleteOlderThan(LocalDateTime cutoffDate) {
        String sql = "DELETE FROM user_feedback WHERE created_at < ?";
        return jdbcTemplate.update(sql, cutoffDate);
    }
    
    /**
     * 批量插入反馈
     */
    public void batchInsert(List<UserFeedback> feedbacks) {
        String sql = "INSERT INTO user_feedback (user_id, session_id, message_id, " +
                    "feedback_type, feedback_value, feedback_text, feedback_tags, " +
                    "ai_response_id, response_strategy) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, feedbacks, feedbacks.size(),
            (PreparedStatement ps, UserFeedback feedback) -> {
                ps.setLong(1, feedback.getUserId());
                ps.setLong(2, feedback.getSessionId());
                ps.setLong(3, feedback.getMessageId());
                ps.setString(4, feedback.getFeedbackType());
                ps.setObject(5, feedback.getFeedbackValue());
                ps.setString(6, feedback.getFeedbackText());
                ps.setString(7, feedback.getFeedbackTags());
                ps.setObject(8, feedback.getAiResponseId());
                ps.setString(9, feedback.getResponseStrategy());
            });
    }
    
    /**
     * 检查消息是否已有反馈
     */
    public boolean existsByMessageId(Long messageId) {
        String sql = "SELECT COUNT(*) FROM user_feedback WHERE message_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, messageId);
        return count != null && count > 0;
    }
}

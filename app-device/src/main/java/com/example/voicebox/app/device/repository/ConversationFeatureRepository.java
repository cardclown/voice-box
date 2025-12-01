package com.example.voicebox.app.device.repository;

import com.example.voicebox.app.device.domain.ConversationFeature;
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
 * 对话特征数据访问层
 * 
 * @author VoiceBox Team
 * @since 2.0
 */
@Repository
public class ConversationFeatureRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 创建对话特征记录
     */
    public ConversationFeature create(ConversationFeature feature) {
        String sql = "INSERT INTO conversation_features (user_id, session_id, message_id, " +
                    "message_length, word_count, sentence_count, avg_word_length, vocabulary_richness, " +
                    "topics, sentiment_score, intent, keywords, question_count, exclamation_count, " +
                    "emoji_count, code_block_count) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, feature.getUserId());
            ps.setLong(2, feature.getSessionId());
            ps.setLong(3, feature.getMessageId());
            ps.setObject(4, feature.getMessageLength());
            ps.setObject(5, feature.getWordCount());
            ps.setObject(6, feature.getSentenceCount());
            ps.setBigDecimal(7, feature.getAvgWordLength());
            ps.setBigDecimal(8, feature.getVocabularyRichness());
            ps.setString(9, feature.getTopics());
            ps.setBigDecimal(10, feature.getSentimentScore());
            ps.setString(11, feature.getIntent());
            ps.setString(12, feature.getKeywords());
            ps.setInt(13, feature.getQuestionCount());
            ps.setInt(14, feature.getExclamationCount());
            ps.setInt(15, feature.getEmojiCount());
            ps.setInt(16, feature.getCodeBlockCount());
            return ps;
        }, keyHolder);
        
        feature.setId(keyHolder.getKey().longValue());
        return feature;
    }
    
    /**
     * 根据ID查找对话特征
     */
    public ConversationFeature findById(Long id) {
        String sql = "SELECT * FROM conversation_features WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, 
                new BeanPropertyRowMapper<>(ConversationFeature.class), id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    /**
     * 根据消息ID查找对话特征
     */
    public ConversationFeature findByMessageId(Long messageId) {
        String sql = "SELECT * FROM conversation_features WHERE message_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, 
                new BeanPropertyRowMapper<>(ConversationFeature.class), messageId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    /**
     * 查找用户的所有对话特征
     */
    public List<ConversationFeature> findByUserId(Long userId) {
        String sql = "SELECT * FROM conversation_features WHERE user_id = ? " +
                    "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(ConversationFeature.class), userId);
    }
    
    /**
     * 查找用户最近的对话特征
     */
    public List<ConversationFeature> findRecentByUserId(Long userId, int limit) {
        String sql = "SELECT * FROM conversation_features WHERE user_id = ? " +
                    "ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(ConversationFeature.class), userId, limit);
    }
    
    /**
     * 查找会话的所有对话特征
     */
    public List<ConversationFeature> findBySessionId(Long sessionId) {
        String sql = "SELECT * FROM conversation_features WHERE session_id = ? " +
                    "ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(ConversationFeature.class), sessionId);
    }
    
    /**
     * 查找用户在指定时间范围内的对话特征
     */
    public List<ConversationFeature> findByUserIdAndDateRange(Long userId, 
                                                              LocalDateTime startDate, 
                                                              LocalDateTime endDate) {
        String sql = "SELECT * FROM conversation_features WHERE user_id = ? " +
                    "AND created_at BETWEEN ? AND ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(ConversationFeature.class), 
            userId, startDate, endDate);
    }
    
    /**
     * 统计用户的对话特征数量
     */
    public long countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM conversation_features WHERE user_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, userId);
    }
    
    /**
     * 删除对话特征
     */
    public void delete(Long id) {
        String sql = "DELETE FROM conversation_features WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    /**
     * 删除用户的所有对话特征
     */
    public void deleteByUserId(Long userId) {
        String sql = "DELETE FROM conversation_features WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }
    
    /**
     * 删除会话的所有对话特征
     */
    public void deleteBySessionId(Long sessionId) {
        String sql = "DELETE FROM conversation_features WHERE session_id = ?";
        jdbcTemplate.update(sql, sessionId);
    }
    
    /**
     * 获取用户的平均对话特征
     */
    public Map<String, Object> getAverageFeaturesByUserId(Long userId) {
        String sql = "SELECT " +
                    "AVG(message_length) as avg_message_length, " +
                    "AVG(word_count) as avg_word_count, " +
                    "AVG(sentence_count) as avg_sentence_count, " +
                    "AVG(avg_word_length) as avg_word_length, " +
                    "AVG(vocabulary_richness) as avg_vocabulary_richness, " +
                    "AVG(sentiment_score) as avg_sentiment_score, " +
                    "AVG(question_count) as avg_question_count, " +
                    "AVG(exclamation_count) as avg_exclamation_count, " +
                    "AVG(emoji_count) as avg_emoji_count, " +
                    "AVG(code_block_count) as avg_code_block_count " +
                    "FROM conversation_features WHERE user_id = ?";
        
        return jdbcTemplate.queryForMap(sql, userId);
    }
    
    /**
     * 获取用户最近N条消息的平均特征
     */
    public Map<String, Object> getRecentAverageFeatures(Long userId, int limit) {
        String sql = "SELECT " +
                    "AVG(message_length) as avg_message_length, " +
                    "AVG(word_count) as avg_word_count, " +
                    "AVG(sentiment_score) as avg_sentiment_score, " +
                    "AVG(question_count) as avg_question_count " +
                    "FROM (" +
                    "  SELECT * FROM conversation_features WHERE user_id = ? " +
                    "  ORDER BY created_at DESC LIMIT ?" +
                    ") recent_features";
        
        return jdbcTemplate.queryForMap(sql, userId, limit);
    }
    
    /**
     * 获取用户的情感分布
     */
    public Map<String, Object> getSentimentDistribution(Long userId) {
        String sql = "SELECT " +
                    "SUM(CASE WHEN sentiment_score > 0.2 THEN 1 ELSE 0 END) as positive_count, " +
                    "SUM(CASE WHEN sentiment_score BETWEEN -0.2 AND 0.2 THEN 1 ELSE 0 END) as neutral_count, " +
                    "SUM(CASE WHEN sentiment_score < -0.2 THEN 1 ELSE 0 END) as negative_count, " +
                    "COUNT(*) as total_count " +
                    "FROM conversation_features WHERE user_id = ? AND sentiment_score IS NOT NULL";
        
        return jdbcTemplate.queryForMap(sql, userId);
    }
    
    /**
     * 获取用户的意图分布
     */
    public List<Map<String, Object>> getIntentDistribution(Long userId) {
        String sql = "SELECT intent, COUNT(*) as count " +
                    "FROM conversation_features WHERE user_id = ? AND intent IS NOT NULL " +
                    "GROUP BY intent ORDER BY count DESC";
        
        return jdbcTemplate.queryForList(sql, userId);
    }
    
    /**
     * 查找包含代码的消息
     */
    public List<ConversationFeature> findMessagesWithCode(Long userId) {
        String sql = "SELECT * FROM conversation_features WHERE user_id = ? " +
                    "AND code_block_count > 0 ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(ConversationFeature.class), userId);
    }
    
    /**
     * 查找问题类型的消息
     */
    public List<ConversationFeature> findQuestions(Long userId) {
        String sql = "SELECT * FROM conversation_features WHERE user_id = ? " +
                    "AND (intent = 'question' OR question_count > 0) " +
                    "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(ConversationFeature.class), userId);
    }
    
    /**
     * 批量插入对话特征
     */
    public void batchInsert(List<ConversationFeature> features) {
        String sql = "INSERT INTO conversation_features (user_id, session_id, message_id, " +
                    "message_length, word_count, sentence_count, avg_word_length, vocabulary_richness, " +
                    "topics, sentiment_score, intent, keywords, question_count, exclamation_count, " +
                    "emoji_count, code_block_count) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.batchUpdate(sql, features, features.size(),
            (PreparedStatement ps, ConversationFeature feature) -> {
                ps.setLong(1, feature.getUserId());
                ps.setLong(2, feature.getSessionId());
                ps.setLong(3, feature.getMessageId());
                ps.setObject(4, feature.getMessageLength());
                ps.setObject(5, feature.getWordCount());
                ps.setObject(6, feature.getSentenceCount());
                ps.setBigDecimal(7, feature.getAvgWordLength());
                ps.setBigDecimal(8, feature.getVocabularyRichness());
                ps.setString(9, feature.getTopics());
                ps.setBigDecimal(10, feature.getSentimentScore());
                ps.setString(11, feature.getIntent());
                ps.setString(12, feature.getKeywords());
                ps.setInt(13, feature.getQuestionCount());
                ps.setInt(14, feature.getExclamationCount());
                ps.setInt(15, feature.getEmojiCount());
                ps.setInt(16, feature.getCodeBlockCount());
            });
    }
    
    /**
     * 删除旧的对话特征（数据清理）
     */
    public int deleteOlderThan(LocalDateTime cutoffDate) {
        String sql = "DELETE FROM conversation_features WHERE created_at < ?";
        return jdbcTemplate.update(sql, cutoffDate);
    }
    
    /**
     * 检查消息特征是否已存在
     */
    public boolean existsByMessageId(Long messageId) {
        String sql = "SELECT COUNT(*) FROM conversation_features WHERE message_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, messageId);
        return count != null && count > 0;
    }
}

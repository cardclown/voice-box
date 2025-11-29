-- ============================================
-- VoiceBox 索引和性能优化
-- 额外的索引和约束
-- ============================================

USE voicebox_db;

-- 复合索引优化查询性能

-- 1. 聊天消息：按会话和时间查询
CREATE INDEX IF NOT EXISTS idx_session_created 
ON chat_message(session_id, created_at);

-- 2. 对话特征：按用户和会话查询
CREATE INDEX IF NOT EXISTS idx_user_session 
ON conversation_features(user_id, session_id);

-- 3. 用户标签：按用户和分类查询
CREATE INDEX IF NOT EXISTS idx_user_category 
ON user_tags(user_id, category);

-- 4. 交互记录：按用户和时间查询
CREATE INDEX IF NOT EXISTS idx_user_created 
ON interactions(user_id, created_at);

-- 5. 用户反馈：按会话和类型查询
CREATE INDEX IF NOT EXISTS idx_session_type 
ON user_feedback(session_id, feedback_type);

-- 全文索引（如果需要搜索功能）
-- 注意：MySQL 5.7 的全文索引仅支持 InnoDB 引擎

-- ALTER TABLE chat_message ADD FULLTEXT INDEX idx_content_fulltext (content);
-- ALTER TABLE user_tags ADD FULLTEXT INDEX idx_tag_fulltext (tag_name);

-- 显示所有表的索引信息
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'voicebox_db'
GROUP BY TABLE_NAME, INDEX_NAME, INDEX_TYPE
ORDER BY TABLE_NAME, INDEX_NAME;

-- ============================================================================
-- VoiceBox v2.0 用户个性分析系统 - 数据库迁移脚本
-- 版本: 2.0
-- 兼容: MySQL 5.7
-- JDK: 1.8
-- 创建时间: 2024-01-15
-- ============================================================================

-- 1. 创建用户画像表
CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    
    -- 性格维度 (大五人格模型)
    openness DECIMAL(4,3) DEFAULT 0.500 COMMENT '开放性 0-1',
    conscientiousness DECIMAL(4,3) DEFAULT 0.500 COMMENT '尽责性 0-1',
    extraversion DECIMAL(4,3) DEFAULT 0.500 COMMENT '外向性 0-1',
    agreeableness DECIMAL(4,3) DEFAULT 0.500 COMMENT '宜人性 0-1',
    neuroticism DECIMAL(4,3) DEFAULT 0.500 COMMENT '神经质 0-1',
    
    -- 偏好维度
    response_length_preference VARCHAR(20) DEFAULT 'balanced' COMMENT 'concise/balanced/detailed',
    language_style_preference VARCHAR(20) DEFAULT 'balanced' COMMENT 'formal/balanced/casual',
    content_format_preference JSON COMMENT '内容格式偏好 ["lists", "code", "tables"]',
    interaction_style VARCHAR(20) DEFAULT 'balanced' COMMENT 'active/balanced/passive',
    
    -- 统计信息
    total_messages INT DEFAULT 0 COMMENT '总消息数',
    total_sessions INT DEFAULT 0 COMMENT '总会话数',
    avg_session_duration DECIMAL(10,2) COMMENT '平均会话时长(分钟)',
    confidence_score DECIMAL(4,3) DEFAULT 0.000 COMMENT '画像置信度 0-1',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_analyzed_at TIMESTAMP NULL COMMENT '最后分析时间',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_confidence (confidence_score),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';

-- 2. 优化现有user_tags表 (添加新字段)
ALTER TABLE user_tags 
ADD COLUMN IF NOT EXISTS weight DECIMAL(4,3) DEFAULT 1.000 COMMENT '标签权重',
ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP NULL COMMENT '过期时间',
ADD COLUMN IF NOT EXISTS metadata JSON COMMENT '标签元数据';

-- 添加索引
ALTER TABLE user_tags ADD INDEX IF NOT EXISTS idx_confidence (confidence);
ALTER TABLE user_tags ADD INDEX IF NOT EXISTS idx_expires_at (expires_at);

-- 3. 创建对话特征表
CREATE TABLE IF NOT EXISTS conversation_features (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    
    -- 语言学特征
    message_length INT COMMENT '消息长度',
    word_count INT COMMENT '词数',
    sentence_count INT COMMENT '句子数',
    avg_word_length DECIMAL(5,2) COMMENT '平均词长',
    vocabulary_richness DECIMAL(4,3) COMMENT '词汇丰富度',
    
    -- 语义特征
    topics JSON COMMENT '主题标签 ["tech", "life"]',
    sentiment_score DECIMAL(4,3) COMMENT '情感分数 -1到1',
    intent VARCHAR(50) COMMENT '意图类型',
    keywords JSON COMMENT '关键词列表',
    
    -- 对话模式
    question_count INT DEFAULT 0 COMMENT '问题数量',
    exclamation_count INT DEFAULT 0 COMMENT '感叹号数量',
    emoji_count INT DEFAULT 0 COMMENT '表情符号数量',
    code_block_count INT DEFAULT 0 COMMENT '代码块数量',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话特征表';

-- 4. 创建用户反馈表
CREATE TABLE IF NOT EXISTS user_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    
    -- 反馈类型
    feedback_type VARCHAR(20) NOT NULL COMMENT 'like/dislike/regenerate/edit/copy',
    feedback_value INT COMMENT '反馈值 1=正面 -1=负面',
    
    -- 反馈内容
    feedback_text TEXT COMMENT '文字反馈',
    feedback_tags JSON COMMENT '反馈标签',
    
    -- 上下文
    ai_response_id BIGINT COMMENT 'AI回复ID',
    response_strategy JSON COMMENT '使用的响应策略',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_feedback_type (feedback_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- 5. 创建学习记录表
CREATE TABLE IF NOT EXISTS learning_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    -- 学习类型
    learning_type VARCHAR(50) NOT NULL COMMENT 'profile_update/tag_adjustment/strategy_optimization',
    
    -- 学习前后对比
    before_state JSON COMMENT '学习前状态',
    after_state JSON COMMENT '学习后状态',
    
    -- 学习效果
    improvement_score DECIMAL(4,3) COMMENT '改进分数',
    confidence_change DECIMAL(4,3) COMMENT '置信度变化',
    
    -- 触发原因
    trigger_event VARCHAR(100) COMMENT '触发事件',
    trigger_data JSON COMMENT '触发数据',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_learning_type (learning_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录表';

-- ============================================================================
-- 数据初始化
-- ============================================================================

-- 为现有用户创建默认画像
INSERT INTO user_profiles (user_id)
SELECT id FROM users 
WHERE id NOT IN (SELECT user_id FROM user_profiles);

-- ============================================================================
-- 验证脚本
-- ============================================================================

-- 验证表是否创建成功
SELECT 
    TABLE_NAME, 
    TABLE_ROWS, 
    CREATE_TIME 
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME IN ('user_profiles', 'conversation_features', 'user_feedback', 'learning_records');

-- 验证索引是否创建成功
SELECT 
    TABLE_NAME, 
    INDEX_NAME, 
    COLUMN_NAME 
FROM information_schema.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME IN ('user_profiles', 'conversation_features', 'user_feedback', 'learning_records', 'user_tags')
ORDER BY TABLE_NAME, INDEX_NAME;

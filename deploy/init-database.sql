-- VoiceBox 数据库初始化脚本
-- 包含 V2.0 个性化分析功能所需的所有表

USE voicebox_db;

-- 1. users 表 - 用户基本信息
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP NULL DEFAULT NULL,
    preferences TEXT,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基本信息表';

-- 2. user_profiles 表 - 用户画像（大五人格 + 偏好）
CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    -- 大五人格维度 (0-1)
    openness DECIMAL(5,4) DEFAULT 0.5000 COMMENT '开放性',
    conscientiousness DECIMAL(5,4) DEFAULT 0.5000 COMMENT '尽责性',
    extraversion DECIMAL(5,4) DEFAULT 0.5000 COMMENT '外向性',
    agreeableness DECIMAL(5,4) DEFAULT 0.5000 COMMENT '宜人性',
    neuroticism DECIMAL(5,4) DEFAULT 0.5000 COMMENT '神经质',
    
    -- 偏好维度
    response_length_preference VARCHAR(20) DEFAULT 'balanced' COMMENT 'concise/balanced/detailed',
    language_style_preference VARCHAR(20) DEFAULT 'balanced' COMMENT 'formal/balanced/casual',
    content_format_preference TEXT COMMENT 'JSON数组: ["lists", "code", "tables"]',
    interaction_style VARCHAR(20) DEFAULT 'balanced' COMMENT 'active/balanced/passive',
    
    -- 统计信息
    total_messages INT DEFAULT 0,
    total_sessions INT DEFAULT 0,
    avg_session_duration DECIMAL(10,2),
    confidence_score DECIMAL(5,4) DEFAULT 0.0000 COMMENT '画像置信度',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_analyzed_at TIMESTAMP NULL COMMENT '最后分析时间',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_confidence (confidence_score),
    INDEX idx_last_analyzed (last_analyzed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';

-- 3. conversation_features 表 - 对话特征提取
CREATE TABLE IF NOT EXISTS conversation_features (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT,
    message_id BIGINT,
    
    -- 语言学特征
    message_length INT COMMENT '消息长度',
    word_count INT COMMENT '词数',
    sentence_count INT COMMENT '句子数',
    avg_word_length DECIMAL(5,2) COMMENT '平均词长',
    vocabulary_richness DECIMAL(5,4) COMMENT '词汇丰富度',
    
    -- 语义特征
    topics TEXT COMMENT '主题列表',
    sentiment_score DECIMAL(5,4) COMMENT '情感分数 -1到1',
    intent VARCHAR(50) COMMENT '意图',
    keywords TEXT COMMENT '关键词',
    
    -- 对话模式
    question_count INT DEFAULT 0 COMMENT '问号数量',
    exclamation_count INT DEFAULT 0 COMMENT '感叹号数量',
    emoji_count INT DEFAULT 0 COMMENT 'Emoji数量',
    code_block_count INT DEFAULT 0 COMMENT '代码块数量',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_message_id (message_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话特征表';

-- 4. user_tags 表 - 用户标签
CREATE TABLE IF NOT EXISTS user_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL COMMENT 'semantic/behavioral/preference',
    tag_name VARCHAR(100) NOT NULL,
    confidence DECIMAL(5,4) DEFAULT 0.5000 COMMENT '标签置信度',
    source VARCHAR(50) COMMENT 'auto/manual/admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_tag_name (tag_name),
    INDEX idx_confidence (confidence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户标签表';

-- 5. user_feedback 表 - 用户反馈
CREATE TABLE IF NOT EXISTS user_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT,
    message_id BIGINT,
    feedback_type VARCHAR(20) NOT NULL COMMENT 'like/dislike/report',
    feedback_content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_feedback_type (feedback_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- 6. interactions 表 - 用户交互记录
CREATE TABLE IF NOT EXISTS interactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT,
    interaction_type VARCHAR(50) COMMENT 'message/click/scroll/voice',
    interaction_data TEXT,
    device_info TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_interaction_type (interaction_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户交互记录表';

-- 7. devices 表 - 设备管理
CREATE TABLE IF NOT EXISTS devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_type VARCHAR(50) COMMENT 'web/mobile/raspberry_pi',
    device_name VARCHAR(100),
    api_key VARCHAR(255) UNIQUE,
    last_sync_at TIMESTAMP NULL DEFAULT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    device_metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_api_key (api_key),
    INDEX idx_device_type (device_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备管理表';

-- 插入默认用户（如果不存在）
INSERT IGNORE INTO users (id, username, email, created_at) 
VALUES (1, 'default_user', 'default@voicebox.local', NOW());

-- 为默认用户创建初始画像
INSERT IGNORE INTO user_profiles (user_id, created_at) 
VALUES (1, NOW());

-- 显示创建的表
SHOW TABLES;

-- 显示 user_profiles 表结构
DESCRIBE user_profiles;

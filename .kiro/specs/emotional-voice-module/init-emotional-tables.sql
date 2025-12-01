-- ============================================
-- 情感化语音交互模块 - 数据库表结构
-- ============================================

-- 1. 情感语音会话表
CREATE TABLE IF NOT EXISTS emotional_voice_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_name VARCHAR(100) COMMENT '会话名称',
    session_type VARCHAR(50) NOT NULL COMMENT '会话类型: CASUAL_CHAT, EMOTIONAL_SUPPORT, COMPANIONSHIP, VENTING, CELEBRATION, PROBLEM_SOLVING',
    mood_context VARCHAR(50) COMMENT '情绪背景: HAPPY, SAD, ANXIOUS, EXCITED, FRUSTRATED, CALM, STRESSED, LONELY, GRATEFUL, CONFUSED',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    total_duration_seconds INT COMMENT '总时长（秒）',
    message_count INT DEFAULT 0 COMMENT '消息数量',
    dominant_emotion VARCHAR(50) COMMENT '主导情绪',
    emotion_intensity DECIMAL(3,2) COMMENT '情绪强度 0-1',
    user_satisfaction_score DECIMAL(3,2) COMMENT '用户满意度 0-1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_session_type (session_type),
    INDEX idx_start_time (start_time),
    INDEX idx_dominant_emotion (dominant_emotion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='情感语音会话表';

-- 2. 情感语音消息表
CREATE TABLE IF NOT EXISTS emotional_voice_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    message_type VARCHAR(50) NOT NULL COMMENT '消息类型: USER_INPUT, SYSTEM_RESPONSE, SYSTEM_PROMPT',
    content TEXT COMMENT '消息内容',
    audio_file_path VARCHAR(500) COMMENT '音频文件路径',
    audio_duration_seconds INT COMMENT '音频时长（秒）',
    
    -- 情感分析结果
    detected_emotion VARCHAR(50) COMMENT '检测到的情绪',
    emotion_confidence DECIMAL(3,2) COMMENT '情绪置信度 0-1',
    emotion_intensity DECIMAL(3,2) COMMENT '情绪强度 0-1',
    sentiment_score DECIMAL(3,2) COMMENT '情感分数 -1到1',
    
    -- 语音特征
    voice_pitch_avg DECIMAL(5,2) COMMENT '平均音高',
    voice_speed_wpm INT COMMENT '语速（每分钟词数）',
    voice_volume_db DECIMAL(5,2) COMMENT '音量（分贝）',
    voice_tone VARCHAR(50) COMMENT '语调: questioning, assertive, gentle等',
    
    -- 智能触发相关
    is_directed_to_system BOOLEAN COMMENT '是否面向系统说话',
    trigger_confidence DECIMAL(3,2) COMMENT '触发置信度 0-1',
    speech_direction VARCHAR(50) COMMENT '说话方向: front, side, away',
    
    -- 个性化响应
    response_strategy VARCHAR(100) COMMENT '响应策略',
    emotional_response_type VARCHAR(100) COMMENT '情感响应类型',
    voice_style_used VARCHAR(50) COMMENT '使用的语音风格',
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    processed_at DATETIME COMMENT '处理时间',
    
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_message_type (message_type),
    INDEX idx_detected_emotion (detected_emotion),
    INDEX idx_created_at (created_at),
    INDEX idx_trigger (is_directed_to_system, trigger_confidence),
    FOREIGN KEY (session_id) REFERENCES emotional_voice_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='情感语音消息表';

-- 3. 用户情感画像表
CREATE TABLE IF NOT EXISTS user_emotional_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '画像ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    
    -- 基础情感特征
    dominant_personality_type VARCHAR(50) COMMENT '主导性格类型',
    emotional_stability DECIMAL(3,2) COMMENT '情绪稳定性 0-1',
    extroversion_level DECIMAL(3,2) COMMENT '外向程度 0-1',
    optimism_level DECIMAL(3,2) COMMENT '乐观程度 0-1',
    sensitivity_level DECIMAL(3,2) COMMENT '敏感程度 0-1',
    
    -- 语音偏好
    preferred_voice_style VARCHAR(50) COMMENT '偏好语音风格: gentle, energetic, calm, professional',
    preferred_response_length VARCHAR(50) COMMENT '偏好回复长度: short, medium, long',
    preferred_conversation_pace VARCHAR(50) COMMENT '偏好对话节奏: slow, normal, fast',
    preferred_emotional_tone VARCHAR(50) COMMENT '偏好情感基调: supportive, neutral, encouraging',
    
    -- 情感需求
    primary_emotional_need VARCHAR(100) COMMENT '主要情感需求: companionship, validation, advice, entertainment',
    comfort_strategies TEXT COMMENT '安慰策略（JSON格式）',
    trigger_words TEXT COMMENT '敏感词汇（JSON格式）',
    positive_reinforcement_style VARCHAR(50) COMMENT '正向强化风格: praise, encouragement, celebration',
    
    -- 交互模式
    interaction_frequency_preference VARCHAR(50) COMMENT '交互频率偏好: high, medium, low',
    privacy_comfort_level DECIMAL(3,2) COMMENT '隐私舒适度 0-1',
    emotional_sharing_willingness DECIMAL(3,2) COMMENT '情感分享意愿 0-1',
    
    -- 学习和适应
    learning_rate DECIMAL(3,2) COMMENT '学习速率 0-1',
    adaptation_sensitivity DECIMAL(3,2) COMMENT '适应敏感度 0-1',
    feedback_responsiveness DECIMAL(3,2) COMMENT '反馈响应度 0-1',
    
    -- 统计信息
    total_sessions INT DEFAULT 0 COMMENT '总会话数',
    total_interaction_minutes INT DEFAULT 0 COMMENT '总交互时长（分钟）',
    average_session_satisfaction DECIMAL(3,2) COMMENT '平均会话满意度',
    most_frequent_emotion VARCHAR(50) COMMENT '最常见情绪',
    emotional_range_score DECIMAL(3,2) COMMENT '情感丰富度',
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    last_interaction_at DATETIME COMMENT '最后交互时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_personality_type (dominant_personality_type),
    INDEX idx_voice_style (preferred_voice_style),
    INDEX idx_emotional_need (primary_emotional_need),
    INDEX idx_last_interaction (last_interaction_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户情感画像表';

-- 4. 情感标签表（用于标签管理）
CREATE TABLE IF NOT EXISTS emotional_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    tag_category VARCHAR(50) NOT NULL COMMENT '标签类别: EMOTION, PERSONALITY, VOICE_STYLE, PREFERENCE',
    tag_name VARCHAR(100) NOT NULL COMMENT '标签名称',
    tag_value VARCHAR(200) COMMENT '标签值',
    confidence DECIMAL(3,2) COMMENT '置信度 0-1',
    source VARCHAR(100) COMMENT '标签来源: AUTO_GENERATED, USER_SET, SYSTEM_INFERRED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    expires_at DATETIME COMMENT '过期时间',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    
    INDEX idx_user_id (user_id),
    INDEX idx_tag_category (tag_category),
    INDEX idx_tag_name (tag_name),
    INDEX idx_confidence (confidence),
    INDEX idx_expires_at (expires_at),
    INDEX idx_is_active (is_active),
    UNIQUE KEY uk_user_tag (user_id, tag_category, tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='情感标签表';

-- 5. 情绪历史记录表
CREATE TABLE IF NOT EXISTS emotion_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id BIGINT COMMENT '会话ID',
    message_id BIGINT COMMENT '消息ID',
    emotion_type VARCHAR(50) NOT NULL COMMENT '情绪类型',
    emotion_intensity DECIMAL(3,2) COMMENT '情绪强度 0-1',
    sentiment_score DECIMAL(3,2) COMMENT '情感分数 -1到1',
    trigger_context TEXT COMMENT '触发上下文',
    detected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_emotion_type (emotion_type),
    INDEX idx_detected_at (detected_at),
    FOREIGN KEY (session_id) REFERENCES emotional_voice_sessions(id) ON DELETE SET NULL,
    FOREIGN KEY (message_id) REFERENCES emotional_voice_messages(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='情绪历史记录表';

-- 6. 语音特征数据表
CREATE TABLE IF NOT EXISTS voice_features (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '特征ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    message_id BIGINT COMMENT '消息ID',
    
    -- 音频基础特征
    pitch_mean DECIMAL(5,2) COMMENT '平均音高',
    pitch_std DECIMAL(5,2) COMMENT '音高标准差',
    pitch_min DECIMAL(5,2) COMMENT '最低音高',
    pitch_max DECIMAL(5,2) COMMENT '最高音高',
    
    volume_mean DECIMAL(5,2) COMMENT '平均音量',
    volume_std DECIMAL(5,2) COMMENT '音量标准差',
    
    speed_wpm INT COMMENT '语速（每分钟词数）',
    pause_count INT COMMENT '停顿次数',
    pause_duration_total DECIMAL(5,2) COMMENT '总停顿时长（秒）',
    
    -- 音色特征
    timbre_brightness DECIMAL(3,2) COMMENT '音色明亮度 0-1',
    timbre_warmth DECIMAL(3,2) COMMENT '音色温暖度 0-1',
    
    -- 情感特征
    energy_level DECIMAL(3,2) COMMENT '能量水平 0-1',
    arousal_level DECIMAL(3,2) COMMENT '唤醒度 0-1',
    valence_level DECIMAL(3,2) COMMENT '效价（正负情绪）-1到1',
    
    -- 质量指标
    audio_quality_score DECIMAL(3,2) COMMENT '音频质量分数 0-1',
    noise_level DECIMAL(3,2) COMMENT '噪音水平 0-1',
    
    extracted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提取时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_message_id (message_id),
    INDEX idx_extracted_at (extracted_at),
    FOREIGN KEY (message_id) REFERENCES emotional_voice_messages(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音特征数据表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入一些默认的情感标签类别（可选）
-- 这些可以作为系统预定义的标签类别

-- ============================================
-- 视图定义（可选，用于方便查询）
-- ============================================

-- 用户情感概览视图
CREATE OR REPLACE VIEW v_user_emotional_overview AS
SELECT 
    uep.user_id,
    uep.dominant_personality_type,
    uep.preferred_voice_style,
    uep.total_sessions,
    uep.total_interaction_minutes,
    uep.average_session_satisfaction,
    uep.most_frequent_emotion,
    uep.last_interaction_at,
    COUNT(DISTINCT evs.id) as active_sessions,
    COUNT(DISTINCT evm.id) as total_messages
FROM user_emotional_profiles uep
LEFT JOIN emotional_voice_sessions evs ON uep.user_id = evs.user_id AND evs.end_time IS NULL
LEFT JOIN emotional_voice_messages evm ON uep.user_id = evm.user_id
GROUP BY uep.user_id;

-- 会话情感统计视图
CREATE OR REPLACE VIEW v_session_emotion_stats AS
SELECT 
    evs.id as session_id,
    evs.user_id,
    evs.session_type,
    evs.dominant_emotion,
    COUNT(evm.id) as message_count,
    AVG(evm.emotion_intensity) as avg_emotion_intensity,
    AVG(evm.sentiment_score) as avg_sentiment_score,
    SUM(evm.audio_duration_seconds) as total_audio_duration
FROM emotional_voice_sessions evs
LEFT JOIN emotional_voice_messages evm ON evs.id = evm.session_id
GROUP BY evs.id;

-- ============================================
-- 完成
-- ============================================

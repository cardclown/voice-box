-- 语音交互功能数据库表初始化脚本
-- 版本: 1.0
-- 创建日期: 2024-11-29

USE voicebox_db;

-- 1. 语音消息表
CREATE TABLE IF NOT EXISTS voice_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    message_id BIGINT COMMENT '关联的聊天消息ID',
    
    -- 文件信息
    file_id VARCHAR(255) NOT NULL COMMENT '音频文件ID（UUID）',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    duration INT NOT NULL COMMENT '音频时长（秒）',
    format VARCHAR(20) NOT NULL DEFAULT 'mp3' COMMENT '音频格式（mp3/wav/ogg）',
    sample_rate INT DEFAULT 16000 COMMENT '采样率（Hz）',
    
    -- 识别信息
    recognized_text TEXT COMMENT '识别的文本内容',
    confidence DECIMAL(5,4) COMMENT '识别置信度（0.0000-1.0000）',
    language VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言代码',
    
    -- 类型标识
    is_input BOOLEAN DEFAULT TRUE COMMENT 'true=用户输入, false=AI输出',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_message_id (message_id),
    INDEX idx_file_id (file_id),
    INDEX idx_created_at (created_at),
    INDEX idx_is_input (is_input),
    
    -- 外键
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音消息表';

-- 2. 语音服务调用日志表
CREATE TABLE IF NOT EXISTS voice_service_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    
    -- 服务信息
    service_type VARCHAR(20) NOT NULL COMMENT '服务类型（STT/TTS）',
    provider VARCHAR(50) NOT NULL COMMENT '服务提供商（aliyun/tencent/azure）',
    request_id VARCHAR(255) COMMENT '请求ID',
    
    -- 请求响应信息
    input_size BIGINT COMMENT '输入大小（字节）',
    output_size BIGINT COMMENT '输出大小（字节）',
    duration_ms INT COMMENT '处理时长（毫秒）',
    
    -- 状态信息
    status VARCHAR(20) NOT NULL COMMENT '状态（success/failed/timeout）',
    error_code VARCHAR(50) COMMENT '错误代码',
    error_message TEXT COMMENT '错误信息',
    
    -- 成本信息
    cost DECIMAL(10,6) COMMENT '成本（元）',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_service_type (service_type),
    INDEX idx_provider (provider),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音服务调用日志表';

-- 3. 用户语音偏好表
CREATE TABLE IF NOT EXISTS user_voice_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    
    -- 语音偏好
    preferred_language VARCHAR(10) DEFAULT 'zh-CN' COMMENT '首选语言',
    voice_name VARCHAR(50) COMMENT '首选音色名称',
    speech_rate DECIMAL(3,2) DEFAULT 1.00 COMMENT '语速（0.50-2.00）',
    pitch DECIMAL(3,2) DEFAULT 1.00 COMMENT '音调（0.50-2.00）',
    volume DECIMAL(3,2) DEFAULT 0.80 COMMENT '音量（0.00-1.00）',
    
    -- 辅助功能
    subtitle_enabled BOOLEAN DEFAULT FALSE COMMENT '是否启用字幕',
    noise_reduction_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用降噪',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 外键
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户语音偏好表';

-- 4. 语音缓存表（用于TTS结果缓存）
CREATE TABLE IF NOT EXISTS voice_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 缓存键
    cache_key VARCHAR(255) NOT NULL UNIQUE COMMENT '缓存键（文本+语言+音色的hash）',
    
    -- 音频信息
    file_id VARCHAR(255) NOT NULL COMMENT '音频文件ID',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    duration INT NOT NULL COMMENT '音频时长（秒）',
    
    -- 元数据
    text_hash VARCHAR(64) NOT NULL COMMENT '文本内容的hash',
    language VARCHAR(10) NOT NULL COMMENT '语言代码',
    voice_name VARCHAR(50) NOT NULL COMMENT '音色名称',
    
    -- 统计信息
    hit_count INT DEFAULT 0 COMMENT '命中次数',
    last_hit_at TIMESTAMP NULL COMMENT '最后命中时间',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expires_at TIMESTAMP NULL COMMENT '过期时间',
    
    -- 索引
    INDEX idx_cache_key (cache_key),
    INDEX idx_text_hash (text_hash),
    INDEX idx_expires_at (expires_at),
    INDEX idx_hit_count (hit_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音缓存表';

-- 5. 语音服务配置表
CREATE TABLE IF NOT EXISTS voice_service_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    
    -- 服务信息
    provider VARCHAR(50) NOT NULL COMMENT '服务提供商',
    service_type VARCHAR(20) NOT NULL COMMENT '服务类型（STT/TTS）',
    
    -- 配置信息
    api_endpoint VARCHAR(500) NOT NULL COMMENT 'API端点',
    app_key VARCHAR(255) COMMENT '应用密钥（加密存储）',
    access_key VARCHAR(255) COMMENT '访问密钥（加密存储）',
    
    -- 配额信息
    daily_quota INT DEFAULT 10000 COMMENT '每日配额',
    used_today INT DEFAULT 0 COMMENT '今日已用',
    quota_reset_at TIMESTAMP NULL COMMENT '配额重置时间',
    
    -- 状态信息
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    is_primary BOOLEAN DEFAULT FALSE COMMENT '是否为主服务',
    priority INT DEFAULT 0 COMMENT '优先级（数字越小优先级越高）',
    
    -- 健康检查
    last_health_check TIMESTAMP NULL COMMENT '最后健康检查时间',
    health_status VARCHAR(20) DEFAULT 'unknown' COMMENT '健康状态（healthy/unhealthy/unknown）',
    consecutive_failures INT DEFAULT 0 COMMENT '连续失败次数',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_provider (provider),
    INDEX idx_service_type (service_type),
    INDEX idx_is_active (is_active),
    INDEX idx_priority (priority),
    UNIQUE INDEX idx_provider_type (provider, service_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音服务配置表';

-- 插入默认配置（需要替换为实际的密钥）
INSERT INTO voice_service_config (provider, service_type, api_endpoint, is_primary, priority) VALUES
('aliyun', 'STT', 'https://nls-gateway.cn-shanghai.aliyuncs.com/stream/v1/asr', TRUE, 1),
('aliyun', 'TTS', 'https://nls-gateway.cn-shanghai.aliyuncs.com/stream/v1/tts', TRUE, 1),
('tencent', 'STT', 'https://asr.cloud.tencent.com/asr/v2/', FALSE, 2),
('tencent', 'TTS', 'https://tts.cloud.tencent.com/stream', FALSE, 2);

-- 创建清理过期缓存的存储过程
DROP PROCEDURE IF EXISTS cleanup_voice_cache;

DELIMITER //

CREATE PROCEDURE cleanup_voice_cache()
BEGIN
    -- 删除过期的缓存记录
    DELETE FROM voice_cache WHERE expires_at < NOW();
    
    -- 删除超过90天的语音消息
    DELETE FROM voice_messages WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);
    
    -- 删除超过30天的服务日志
    DELETE FROM voice_service_logs WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
END //

DELIMITER ;

-- 创建定时任务（每天凌晨3点执行清理）
-- 注意：需要MySQL事件调度器开启（SET GLOBAL event_scheduler = ON;）
DROP EVENT IF EXISTS cleanup_voice_data_daily;

CREATE EVENT cleanup_voice_data_daily
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURRENT_DATE, '03:00:00')
DO CALL cleanup_voice_cache();

-- 创建视图：语音使用统计
CREATE OR REPLACE VIEW voice_usage_stats AS
SELECT 
    DATE(created_at) as date,
    service_type,
    provider,
    COUNT(*) as total_calls,
    SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as successful_calls,
    SUM(CASE WHEN status = 'failed' THEN 1 ELSE 0 END) as failed_calls,
    AVG(duration_ms) as avg_duration_ms,
    SUM(cost) as total_cost
FROM voice_service_logs
GROUP BY DATE(created_at), service_type, provider;

-- 创建视图：用户语音活跃度
CREATE OR REPLACE VIEW user_voice_activity AS
SELECT 
    user_id,
    COUNT(*) as total_messages,
    SUM(CASE WHEN is_input = TRUE THEN 1 ELSE 0 END) as input_messages,
    SUM(CASE WHEN is_input = FALSE THEN 1 ELSE 0 END) as output_messages,
    SUM(duration) as total_duration_seconds,
    MAX(created_at) as last_activity_at
FROM voice_messages
GROUP BY user_id;

-- 授权（根据实际用户调整）
-- GRANT SELECT, INSERT, UPDATE, DELETE ON voicebox_db.voice_messages TO 'voicebox'@'%';
-- GRANT SELECT, INSERT ON voicebox_db.voice_service_logs TO 'voicebox'@'%';
-- GRANT SELECT, INSERT, UPDATE ON voicebox_db.user_voice_preferences TO 'voicebox'@'%';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON voicebox_db.voice_cache TO 'voicebox'@'%';
-- GRANT SELECT, UPDATE ON voicebox_db.voice_service_config TO 'voicebox'@'%';

-- 完成
SELECT 'Voice interaction tables created successfully!' as status;

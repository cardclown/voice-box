-- ==========================================
-- 语音交互功能数据表
-- ==========================================

-- 语音消息表
CREATE TABLE IF NOT EXISTS voice_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id BIGINT COMMENT '会话ID',
    message_id BIGINT COMMENT '关联的聊天消息ID',
    file_id VARCHAR(255) NOT NULL COMMENT '音频文件ID',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小（字节）',
    duration INT NOT NULL COMMENT '音频时长（秒）',
    format VARCHAR(20) NOT NULL COMMENT '音频格式（mp3/wav/ogg）',
    sample_rate INT DEFAULT 16000 COMMENT '采样率',
    recognized_text TEXT COMMENT '识别的文本',
    confidence DECIMAL(5,4) COMMENT '识别置信度',
    language VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言',
    is_input BOOLEAN DEFAULT TRUE COMMENT 'true=用户输入, false=AI输出',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_message_id (message_id),
    INDEX idx_file_id (file_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音消息表';

-- 语音服务调用日志表
CREATE TABLE IF NOT EXISTS voice_service_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT COMMENT '用户ID',
    service_type VARCHAR(20) NOT NULL COMMENT '服务类型（STT/TTS）',
    provider VARCHAR(50) NOT NULL COMMENT '服务提供商（doubao/aliyun/tencent）',
    request_id VARCHAR(255) COMMENT '请求ID',
    input_size BIGINT COMMENT '输入大小（字节）',
    output_size BIGINT COMMENT '输出大小（字节）',
    duration_ms INT COMMENT '处理时长（毫秒）',
    status VARCHAR(20) NOT NULL COMMENT '状态（success/failed）',
    error_message TEXT COMMENT '错误信息',
    cost DECIMAL(10,6) COMMENT '成本（元）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_service_type (service_type),
    INDEX idx_provider (provider),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音服务调用日志表';

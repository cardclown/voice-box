-- ============================================
-- VoiceBox 用户管理表
-- 包含用户、设备和交互记录
-- ============================================

USE voicebox_db;

-- 1. 用户基本信息表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP NULL DEFAULT NULL,
    preferences TEXT COMMENT '用户偏好设置（JSON格式）',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_last_active (last_active_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基本信息表';

-- 2. 设备管理表
CREATE TABLE IF NOT EXISTS devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_type VARCHAR(50) COMMENT 'web/mobile/raspberry_pi',
    device_name VARCHAR(100),
    api_key VARCHAR(255) UNIQUE,
    last_sync_at TIMESTAMP NULL DEFAULT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    device_metadata TEXT COMMENT '设备元数据（JSON格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_api_key (api_key),
    INDEX idx_device_type (device_type),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备管理表';

-- 3. 用户交互记录表
CREATE TABLE IF NOT EXISTS interactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT,
    interaction_type VARCHAR(50) COMMENT 'message/click/scroll/voice',
    interaction_data TEXT COMMENT '交互数据（JSON格式）',
    device_info TEXT COMMENT '设备信息（JSON格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_interaction_type (interaction_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户交互记录表';

-- 插入默认用户
INSERT IGNORE INTO users (id, username, email, created_at) 
VALUES (1, 'default_user', 'default@voicebox.local', NOW());

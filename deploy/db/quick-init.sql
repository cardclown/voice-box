-- ============================================
-- VoiceBox 快速初始化脚本
-- 用于快速创建数据库和用户
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS voicebox_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER IF NOT EXISTS 'voicebox'@'localhost' IDENTIFIED BY 'voicebox123';
CREATE USER IF NOT EXISTS 'voicebox'@'%' IDENTIFIED BY 'voicebox123';

-- 授权
GRANT ALL PRIVILEGES ON voicebox_db.* TO 'voicebox'@'localhost';
GRANT ALL PRIVILEGES ON voicebox_db.* TO 'voicebox'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 显示结果
SELECT 'Database created successfully!' AS status;
SHOW DATABASES LIKE 'voicebox_db';

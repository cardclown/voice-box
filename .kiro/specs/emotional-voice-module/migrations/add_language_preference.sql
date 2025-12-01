-- 添加用户语言偏好字段
-- 支持多语言功能（需求19）

ALTER TABLE user_emotional_profiles 
ADD COLUMN preferred_language VARCHAR(10) DEFAULT 'zh-CN' COMMENT '用户偏好语言';

-- 为现有用户设置默认语言
UPDATE user_emotional_profiles 
SET preferred_language = 'zh-CN' 
WHERE preferred_language IS NULL;

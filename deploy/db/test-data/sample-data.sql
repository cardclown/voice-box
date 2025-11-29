-- ============================================
-- VoiceBox 测试数据
-- 仅用于开发和测试环境
-- 警告：不要在生产环境执行此脚本
-- ============================================

USE voicebox_db;

-- 清空现有测试数据（可选）
-- TRUNCATE TABLE user_feedback;
-- TRUNCATE TABLE user_tags;
-- TRUNCATE TABLE conversation_features;
-- TRUNCATE TABLE user_profiles;
-- TRUNCATE TABLE interactions;
-- TRUNCATE TABLE devices;
-- TRUNCATE TABLE chat_message;
-- TRUNCATE TABLE chat_session;
-- TRUNCATE TABLE users;

-- ============================================
-- 1. 用户数据
-- ============================================

INSERT INTO users (id, username, email, avatar_url, created_at, last_active_at) VALUES
(1, 'default_user', 'default@voicebox.local', NULL, NOW(), NOW()),
(2, 'test_user', 'test@example.com', 'https://avatar.example.com/test.jpg', NOW(), NOW()),
(3, 'demo_user', 'demo@example.com', NULL, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 1 DAY);

-- ============================================
-- 2. 用户画像数据
-- ============================================

INSERT INTO user_profiles (user_id, openness, conscientiousness, extraversion, agreeableness, neuroticism, 
    response_length_preference, language_style_preference, interaction_style, 
    total_messages, total_sessions, confidence_score, created_at, last_analyzed_at) VALUES
(1, 0.5000, 0.5000, 0.5000, 0.5000, 0.5000, 'balanced', 'balanced', 'balanced', 0, 0, 0.0000, NOW(), NULL),
(2, 0.7500, 0.6000, 0.8000, 0.7000, 0.3000, 'detailed', 'casual', 'active', 150, 25, 0.8500, NOW(), NOW()),
(3, 0.4000, 0.7000, 0.3000, 0.6000, 0.5000, 'concise', 'formal', 'passive', 50, 10, 0.6000, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 1 DAY);

-- ============================================
-- 3. 聊天会话数据
-- ============================================

INSERT INTO chat_session (id, title, model, device_info, created_at) VALUES
(1, '技术讨论', 'doubao', '{"device": "web", "browser": "Chrome"}', NOW() - INTERVAL 2 DAY),
(2, '日常聊天', 'doubao', '{"device": "mobile", "os": "iOS"}', NOW() - INTERVAL 1 DAY),
(3, '代码帮助', 'doubao', '{"device": "web", "browser": "Firefox"}', NOW());

-- ============================================
-- 4. 聊天消息数据
-- ============================================

INSERT INTO chat_message (session_id, role, content, created_at) VALUES
-- 会话 1
(1, 'user', '你好，我想了解一下 Vue 3 的组合式 API', NOW() - INTERVAL 2 DAY),
(1, 'assistant', '你好！Vue 3 的组合式 API（Composition API）是一种新的代码组织方式...', NOW() - INTERVAL 2 DAY + INTERVAL 1 MINUTE),
(1, 'user', '能给我一个简单的例子吗？', NOW() - INTERVAL 2 DAY + INTERVAL 2 MINUTE),
(1, 'assistant', '当然可以！这里是一个简单的计数器例子：\n```javascript\nimport { ref } from "vue";\n\nexport default {\n  setup() {\n    const count = ref(0);\n    const increment = () => count.value++;\n    return { count, increment };\n  }\n};\n```', NOW() - INTERVAL 2 DAY + INTERVAL 3 MINUTE),

-- 会话 2
(2, 'user', '今天天气怎么样？', NOW() - INTERVAL 1 DAY),
(2, 'assistant', '抱歉，我无法获取实时天气信息。建议你查看天气预报应用或网站。', NOW() - INTERVAL 1 DAY + INTERVAL 1 MINUTE),

-- 会话 3
(3, 'user', '如何在 JavaScript 中实现防抖函数？', NOW()),
(3, 'assistant', '防抖函数的实现如下：\n```javascript\nfunction debounce(func, delay) {\n  let timeoutId;\n  return function(...args) {\n    clearTimeout(timeoutId);\n    timeoutId = setTimeout(() => func.apply(this, args), delay);\n  };\n}\n```', NOW() + INTERVAL 1 MINUTE);

-- ============================================
-- 5. 对话特征数据
-- ============================================

INSERT INTO conversation_features (user_id, session_id, message_id, message_length, word_count, 
    sentence_count, sentiment_score, question_count, code_block_count, created_at) VALUES
(2, 1, 1, 25, 12, 1, 0.2000, 0, 0, NOW() - INTERVAL 2 DAY),
(2, 1, 3, 15, 8, 1, 0.1000, 1, 0, NOW() - INTERVAL 2 DAY + INTERVAL 2 MINUTE),
(2, 3, 7, 30, 15, 1, 0.0000, 1, 0, NOW());

-- ============================================
-- 6. 用户标签数据
-- ============================================

INSERT INTO user_tags (user_id, category, tag_name, confidence, source, created_at) VALUES
(2, 'semantic', '技术爱好者', 0.8500, 'auto', NOW()),
(2, 'behavioral', '活跃用户', 0.9000, 'auto', NOW()),
(2, 'preference', '喜欢详细解释', 0.7500, 'auto', NOW()),
(3, 'semantic', '专业人士', 0.7000, 'auto', NOW()),
(3, 'behavioral', '偶尔使用', 0.6000, 'auto', NOW());

-- ============================================
-- 7. 设备数据
-- ============================================

INSERT INTO devices (user_id, device_type, device_name, api_key, last_sync_at, is_active, created_at) VALUES
(2, 'web', 'Chrome on MacBook', 'test_api_key_001', NOW(), TRUE, NOW()),
(2, 'mobile', 'iPhone 13', 'test_api_key_002', NOW() - INTERVAL 1 DAY, TRUE, NOW() - INTERVAL 7 DAY),
(3, 'web', 'Firefox on Windows', 'test_api_key_003', NOW() - INTERVAL 2 DAY, TRUE, NOW() - INTERVAL 10 DAY);

-- ============================================
-- 8. 用户反馈数据
-- ============================================

INSERT INTO user_feedback (user_id, session_id, message_id, feedback_type, feedback_content, created_at) VALUES
(2, 1, 2, 'like', '解释得很清楚', NOW() - INTERVAL 2 DAY),
(2, 1, 4, 'like', '例子很有帮助', NOW() - INTERVAL 2 DAY),
(3, 3, 8, 'like', NULL, NOW());

-- ============================================
-- 9. 交互记录数据
-- ============================================

INSERT INTO interactions (user_id, session_id, interaction_type, interaction_data, created_at) VALUES
(2, 1, 'message', '{"action": "send", "length": 25}', NOW() - INTERVAL 2 DAY),
(2, 1, 'message', '{"action": "send", "length": 15}', NOW() - INTERVAL 2 DAY + INTERVAL 2 MINUTE),
(3, 3, 'message', '{"action": "send", "length": 30}', NOW());

-- ============================================
-- 验证数据
-- ============================================

-- 检查插入的数据
SELECT '用户数量:' AS info, COUNT(*) AS count FROM users
UNION ALL
SELECT '画像数量:', COUNT(*) FROM user_profiles
UNION ALL
SELECT '会话数量:', COUNT(*) FROM chat_session
UNION ALL
SELECT '消息数量:', COUNT(*) FROM chat_message
UNION ALL
SELECT '标签数量:', COUNT(*) FROM user_tags
UNION ALL
SELECT '设备数量:', COUNT(*) FROM devices
UNION ALL
SELECT '反馈数量:', COUNT(*) FROM user_feedback;

-- 显示测试用户信息
SELECT 
    u.id,
    u.username,
    u.email,
    up.total_messages,
    up.total_sessions,
    up.confidence_score
FROM users u
LEFT JOIN user_profiles up ON u.id = up.user_id
ORDER BY u.id;

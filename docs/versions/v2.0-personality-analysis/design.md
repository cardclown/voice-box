# v2.0 用户个性分析系统 - 设计文档

**版本**: v2.0  
**创建日期**: 2024-01-15  
**状态**: 📝 规划中  
**设计负责人**: VoiceBox架构团队

---

## 📐 系统架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                          前端层                                   │
├──────────────────────┬──────────────────────────────────────────┤
│   用户画像展示        │    个性化设置                              │
│   - 性格雷达图        │    - 偏好配置                              │
│   - 兴趣云图          │    - 隐私控制                              │
│   - 活跃度曲线        │    - 数据导出                              │
└──────────────────────┴──────────────────────────────────────────┘
                              ↓ HTTPS
┌─────────────────────────────────────────────────────────────────┐
│                        API网关层                                  │
│  - 请求路由  - 身份验证  - 限流控制  - 日志记录                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        应用服务层                                 │
├──────────────────────┬──────────────────────────────────────────┤
│  个性分析服务         │   标签管理服务                            │
│  - 特征提取          │   - 标签生成                              │
│  - 画像构建          │   - 标签更新                              │
│  - 策略计算          │   - 标签清理                              │
├──────────────────────┼──────────────────────────────────────────┤
│  学习适应服务         │   上下文记忆服务                          │
│  - 反馈收集          │   - 短期记忆                              │
│  - 模型更新          │   - 长期记忆                              │
│  - 效果评估          │   - 记忆检索                              │
└──────────────────────┴──────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        数据层                                     │
├──────────────────────┬──────────────────────────────────────────┤
│   MySQL数据库         │    Redis缓存                              │
│  - 用户画像          │   - 热点数据                              │
│  - 标签数据          │   - 会话缓存                              │
│  - 对话历史          │   - 计算结果                              │
├──────────────────────┼──────────────────────────────────────────┤
│   Elasticsearch       │    消息队列                               │
│  - 全文检索          │   - 异步任务                              │
│  - 日志分析          │   - 事件通知                              │
└──────────────────────┴──────────────────────────────────────────┘
```

### 技术栈

**后端**:
- 框架: Spring Boot 2.3.x (兼容JDK 1.8)
- 数据库: MySQL 5.7
- 缓存: Redis 5.0+ (推荐6.0+)
- 搜索: Elasticsearch 6.x (可选)
- 消息队列: RabbitMQ 3.x (可选)
- NLP: HanLP portable-1.8.4

**前端**:
- 框架: Vue 3 + Vite
- 图表: ECharts 5.x
- 状态管理: Pinia
- HTTP客户端: Axios

**机器学习** (可选):
- Python 3.7+
- scikit-learn
- 轻量级模型


## 🗄️ 数据模型设计

### 用户画像表 (user_profiles)

```sql
CREATE TABLE user_profiles (
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
    last_analyzed_at TIMESTAMP COMMENT '最后分析时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_confidence (confidence_score),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';
```

### 用户标签表 (user_tags) - 已存在，需优化

```sql
ALTER TABLE user_tags ADD COLUMN weight DECIMAL(4,3) DEFAULT 1.000 COMMENT '标签权重';
ALTER TABLE user_tags ADD COLUMN expires_at TIMESTAMP NULL COMMENT '过期时间';
ALTER TABLE user_tags ADD COLUMN metadata JSON COMMENT '标签元数据';
ALTER TABLE user_tags ADD INDEX idx_confidence (confidence);
ALTER TABLE user_tags ADD INDEX idx_expires_at (expires_at);
```

### 对话特征表 (conversation_features)

```sql
CREATE TABLE conversation_features (
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
    
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话特征表';
```

### 用户反馈表 (user_feedback)

```sql
CREATE TABLE user_feedback (
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
    
    INDEX idx_user_id (user_id),
    INDEX idx_feedback_type (feedback_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';
```

### 学习记录表 (learning_records)

```sql
CREATE TABLE learning_records (
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
    
    INDEX idx_user_id (user_id),
    INDEX idx_learning_type (learning_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录表';
```


## 🔧 核心组件设计

### 1. 个性分析引擎 (PersonalityAnalysisEngine)

**职责**: 分析用户对话和行为，构建用户画像

**核心算法**:

```java
@Service
public class PersonalityAnalysisEngine {
    
    /**
     * 分析用户性格维度
     * 基于大五人格模型 + 对话特征
     */
    public PersonalityScores analyzePersonality(Long userId) {
        // 1. 获取用户最近100条对话
        List<ConversationFeature> features = getRecentFeatures(userId, 100);
        
        if (features.size() < 10) {
            return getDefaultScores(); // 数据不足，返回默认值
        }
        
        PersonalityScores scores = new PersonalityScores();
        
        // 2. 计算开放性 (Openness)
        scores.setOpenness(calculateOpenness(features));
        
        // 3. 计算尽责性 (Conscientiousness)
        scores.setConscientiousness(calculateConscientiousness(features));
        
        // 4. 计算外向性 (Extraversion)
        scores.setExtraversion(calculateExtraversion(features));
        
        // 5. 计算宜人性 (Agreeableness)
        scores.setAgreeableness(calculateAgreeableness(features));
        
        // 6. 计算神经质 (Neuroticism)
        scores.setNeuroticism(calculateNeuroticism(features));
        
        // 7. 计算置信度
        scores.setConfidence(calculateConfidence(features.size()));
        
        return scores;
    }
    
    /**
     * 计算开放性
     * 指标: 话题多样性、词汇丰富度、问题数量
     */
    private double calculateOpenness(List<ConversationFeature> features) {
        double score = 0.5; // 基准分
        
        // 话题多样性 (0-0.3分)
        Set<String> uniqueTopics = extractUniqueTopics(features);
        score += Math.min(0.3, uniqueTopics.size() * 0.03);
        
        // 词汇丰富度 (0-0.3分)
        double avgVocabularyRichness = features.stream()
            .mapToDouble(ConversationFeature::getVocabularyRichness)
            .average().orElse(0.5);
        score += (avgVocabularyRichness - 0.5) * 0.6;
        
        // 问题数量 (0-0.2分)
        double avgQuestions = features.stream()
            .mapToDouble(ConversationFeature::getQuestionCount)
            .average().orElse(0);
        score += Math.min(0.2, avgQuestions * 0.05);
        
        return clamp(score, 0, 1);
    }
    
    /**
     * 计算外向性
     * 指标: 消息长度、感叹号使用、表情符号、积极情感
     */
    private double calculateExtraversion(List<ConversationFeature> features) {
        double score = 0.5;
        
        // 消息长度 (0-0.25分)
        double avgLength = features.stream()
            .mapToDouble(ConversationFeature::getMessageLength)
            .average().orElse(50);
        score += Math.min(0.25, (avgLength - 50) / 400);
        
        // 感叹号使用 (0-0.25分)
        double avgExclamations = features.stream()
            .mapToDouble(ConversationFeature::getExclamationCount)
            .average().orElse(0);
        score += Math.min(0.25, avgExclamations * 0.1);
        
        // 表情符号 (0-0.25分)
        double avgEmojis = features.stream()
            .mapToDouble(ConversationFeature::getEmojiCount)
            .average().orElse(0);
        score += Math.min(0.25, avgEmojis * 0.08);
        
        // 积极情感 (0-0.25分)
        double avgSentiment = features.stream()
            .mapToDouble(f -> Math.max(0, f.getSentimentScore()))
            .average().orElse(0);
        score += avgSentiment * 0.25;
        
        return clamp(score, 0, 1);
    }
    
    /**
     * 计算神经质 (情绪稳定性)
     * 指标: 情绪波动、负面情感、焦虑词汇
     */
    private double calculateNeuroticism(List<ConversationFeature> features) {
        double score = 0.5;
        
        // 情绪波动 (0-0.4分)
        double sentimentVariance = calculateSentimentVariance(features);
        score += sentimentVariance * 0.4;
        
        // 负面情感频率 (0-0.3分)
        long negativeCount = features.stream()
            .filter(f -> f.getSentimentScore() < -0.2)
            .count();
        score += (double) negativeCount / features.size() * 0.3;
        
        // 焦虑词汇 (0-0.3分)
        double anxietyScore = calculateAnxietyScore(features);
        score += anxietyScore * 0.3;
        
        return clamp(score, 0, 1);
    }
    
    /**
     * 计算置信度
     * 基于样本数量和数据质量
     */
    private double calculateConfidence(int sampleSize) {
        if (sampleSize < 10) return 0.0;
        if (sampleSize < 30) return 0.3;
        if (sampleSize < 50) return 0.5;
        if (sampleSize < 100) return 0.7;
        if (sampleSize < 200) return 0.85;
        return 0.95;
    }
    
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
```

### 2. 智能标签生成器 (SmartTagGenerator)

**职责**: 自动生成和管理用户标签

**实现方案**:

```java
@Service
public class SmartTagGenerator {
    
    @Autowired
    private NLPService nlpService;
    
    @Autowired
    private UserTagRepository tagRepository;
    
    /**
     * 从对话中生成标签
     * 使用NLP技术提取关键信息
     */
    public List<UserTag> generateTagsFromConversation(
        Long userId, 
        String conversationText
    ) {
        List<UserTag> tags = new ArrayList<>();
        
        // 1. 主题提取
        List<String> topics = nlpService.extractTopics(conversationText);
        for (String topic : topics) {
            tags.add(createTag(userId, "semantic", "topic_" + topic, 0.7));
        }
        
        // 2. 关键词提取
        List<String> keywords = nlpService.extractKeywords(conversationText, 10);
        for (String keyword : keywords) {
            tags.add(createTag(userId, "semantic", "keyword_" + keyword, 0.6));
        }
        
        // 3. 实体识别
        Map<String, List<String>> entities = nlpService.extractEntities(conversationText);
        for (Map.Entry<String, List<String>> entry : entities.entrySet()) {
            String entityType = entry.getKey();
            for (String entity : entry.getValue()) {
                tags.add(createTag(userId, "semantic", entityType + "_" + entity, 0.8));
            }
        }
        
        // 4. 情感分析
        double sentiment = nlpService.analyzeSentiment(conversationText);
        if (sentiment > 0.3) {
            tags.add(createTag(userId, "emotional", "positive_mood", 0.7));
        } else if (sentiment < -0.3) {
            tags.add(createTag(userId, "emotional", "negative_mood", 0.7));
        }
        
        return tags;
    }
    
    /**
     * 基于行为模式生成标签
     */
    public List<UserTag> generateBehavioralTags(
        Long userId,
        BehaviorMetrics metrics
    ) {
        List<UserTag> tags = new ArrayList<>();
        
        // 活跃度标签
        if (metrics.getMessageCount() > 100) {
            tags.add(createTag(userId, "behavioral", "frequent_user", 0.9));
        }
        
        // 时间偏好标签
        Map<String, Integer> timeDistribution = metrics.getTimeDistribution();
        String peakTime = findPeakTime(timeDistribution);
        tags.add(createTag(userId, "behavioral", "peak_time_" + peakTime, 0.8));
        
        // 对话深度标签
        if (metrics.getAvgSessionDuration() > 30) {
            tags.add(createTag(userId, "behavioral", "deep_thinker", 0.7));
        }
        
        // 响应速度标签
        if (metrics.getAvgResponseTime() < 5) {
            tags.add(createTag(userId, "behavioral", "quick_responder", 0.6));
        }
        
        return tags;
    }
    
    /**
     * 智能标签合并
     * 合并相似标签，提高置信度
     */
    public void mergeSimil arTags(Long userId) {
        List<UserTag> tags = tagRepository.findByUserId(userId);
        
        // 按类别分组
        Map<String, List<UserTag>> tagsByCategory = tags.stream()
            .collect(Collectors.groupingBy(UserTag::getCategory));
        
        for (List<UserTag> categoryTags : tagsByCategory.values()) {
            // 查找相似标签
            for (int i = 0; i < categoryTags.size(); i++) {
                for (int j = i + 1; j < categoryTags.size(); j++) {
                    UserTag tag1 = categoryTags.get(i);
                    UserTag tag2 = categoryTags.get(j);
                    
                    if (isSimilar(tag1.getTagName(), tag2.getTagName())) {
                        // 合并标签
                        double newConfidence = Math.max(
                            tag1.getConfidence().doubleValue(),
                            tag2.getConfidence().doubleValue()
                        ) + 0.1;
                        tag1.setConfidence(BigDecimal.valueOf(Math.min(1.0, newConfidence)));
                        tagRepository.update(tag1);
                        tagRepository.delete(tag2.getId());
                    }
                }
            }
        }
    }
    
    /**
     * 标签衰减
     * 降低长时间未更新标签的置信度
     */
    public void decayOldTags(Long userId, int daysThreshold) {
        List<UserTag> tags = tagRepository.findByUserId(userId);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysThreshold);
        
        for (UserTag tag : tags) {
            if (tag.getUpdatedAt().isBefore(cutoff)) {
                double decay = 0.05 * ((daysThreshold - 
                    ChronoUnit.DAYS.between(tag.getUpdatedAt(), LocalDateTime.now())) / daysThreshold);
                double newConfidence = Math.max(0, tag.getConfidence().doubleValue() - decay);
                
                if (newConfidence < 0.3) {
                    tagRepository.delete(tag.getId());
                } else {
                    tag.setConfidence(BigDecimal.valueOf(newConfidence));
                    tagRepository.update(tag);
                }
            }
        }
    }
}
```


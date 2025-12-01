# v2.0 用户个性分析系统 - 执行指南

**版本**: v2.0  
**创建日期**: 2024-01-15  
**适用人员**: 开发团队、测试团队

---

## 🚀 快速开始

### 前置条件

1. **环境准备**
   - JDK 1.8
   - Maven 3.6.3
   - MySQL 5.7
   - Redis 5.0+ (推荐6.0+)
   - Node.js 16+

2. **依赖安装**
   ```bash
   # 后端依赖
   cd app-device
   mvn clean install
   
   # 前端依赖
   cd app-web
   npm install
   ```

3. **数据库初始化**
   ```bash
   # 执行迁移脚本
   mysql -u root -p voicebox < migrations/v2.0-personality-analysis.sql
   ```

---

## 📋 详细执行步骤

### 阶段1: 数据库设计 (第1天)

#### 步骤1.1: 创建新表

```sql
-- 1. 创建用户画像表
CREATE TABLE user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    openness DECIMAL(4,3) DEFAULT 0.500,
    conscientiousness DECIMAL(4,3) DEFAULT 0.500,
    extraversion DECIMAL(4,3) DEFAULT 0.500,
    agreeableness DECIMAL(4,3) DEFAULT 0.500,
    neuroticism DECIMAL(4,3) DEFAULT 0.500,
    response_length_preference VARCHAR(20) DEFAULT 'balanced',
    language_style_preference VARCHAR(20) DEFAULT 'balanced',
    content_format_preference JSON,
    interaction_style VARCHAR(20) DEFAULT 'balanced',
    total_messages INT DEFAULT 0,
    total_sessions INT DEFAULT 0,
    avg_session_duration DECIMAL(10,2),
    confidence_score DECIMAL(4,3) DEFAULT 0.000,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_analyzed_at TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_confidence (confidence_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 创建对话特征表
CREATE TABLE conversation_features (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    message_length INT,
    word_count INT,
    sentence_count INT,
    avg_word_length DECIMAL(5,2),
    vocabulary_richness DECIMAL(4,3),
    topics JSON,
    sentiment_score DECIMAL(4,3),
    intent VARCHAR(50),
    keywords JSON,
    question_count INT DEFAULT 0,
    exclamation_count INT DEFAULT 0,
    emoji_count INT DEFAULT 0,
    code_block_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 创建用户反馈表
CREATE TABLE user_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    feedback_type VARCHAR(20) NOT NULL,
    feedback_value INT,
    feedback_text TEXT,
    feedback_tags JSON,
    ai_response_id BIGINT,
    response_strategy JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_feedback_type (feedback_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 创建学习记录表
CREATE TABLE learning_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    learning_type VARCHAR(50) NOT NULL,
    before_state JSON,
    after_state JSON,
    improvement_score DECIMAL(4,3),
    confidence_change DECIMAL(4,3),
    trigger_event VARCHAR(100),
    trigger_data JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_learning_type (learning_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 步骤1.2: 优化现有表

```sql
-- 优化user_tags表
ALTER TABLE user_tags 
ADD COLUMN weight DECIMAL(4,3) DEFAULT 1.000 COMMENT '标签权重',
ADD COLUMN expires_at TIMESTAMP NULL COMMENT '过期时间',
ADD COLUMN metadata JSON COMMENT '标签元数据',
ADD INDEX idx_confidence (confidence),
ADD INDEX idx_expires_at (expires_at);
```

#### 验收检查
```bash
# 检查表是否创建成功
mysql -u root -p -e "SHOW TABLES LIKE '%profile%'" voicebox
mysql -u root -p -e "SHOW TABLES LIKE '%feature%'" voicebox
mysql -u root -p -e "SHOW TABLES LIKE '%feedback%'" voicebox
mysql -u root -p -e "SHOW TABLES LIKE '%learning%'" voicebox

# 检查索引
mysql -u root -p -e "SHOW INDEX FROM user_profiles" voicebox
```

---

### 阶段2: Repository层开发 (第2天)

#### 步骤2.1: 创建UserProfileRepository

```java
// app-device/src/main/java/com/example/voicebox/app/device/repository/UserProfileRepository.java

@Repository
public class UserProfileRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public UserProfile findByUserId(Long userId) {
        String sql = "SELECT * FROM user_profiles WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, 
                new BeanPropertyRowMapper<>(UserProfile.class), userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    public UserProfile create(UserProfile profile) {
        String sql = "INSERT INTO user_profiles (user_id, openness, conscientiousness, " +
                    "extraversion, agreeableness, neuroticism, response_length_preference, " +
                    "language_style_preference, content_format_preference, interaction_style, " +
                    "total_messages, total_sessions, avg_session_duration, confidence_score) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, profile.getUserId());
            ps.setBigDecimal(2, profile.getOpenness());
            ps.setBigDecimal(3, profile.getConscientiousness());
            ps.setBigDecimal(4, profile.getExtraversion());
            ps.setBigDecimal(5, profile.getAgreeableness());
            ps.setBigDecimal(6, profile.getNeuroticism());
            ps.setString(7, profile.getResponseLengthPreference());
            ps.setString(8, profile.getLanguageStylePreference());
            ps.setString(9, profile.getContentFormatPreference());
            ps.setString(10, profile.getInteractionStyle());
            ps.setInt(11, profile.getTotalMessages());
            ps.setInt(12, profile.getTotalSessions());
            ps.setBigDecimal(13, profile.getAvgSessionDuration());
            ps.setBigDecimal(14, profile.getConfidenceScore());
            return ps;
        }, keyHolder);
        
        profile.setId(keyHolder.getKey().longValue());
        return profile;
    }
    
    public UserProfile update(UserProfile profile) {
        String sql = "UPDATE user_profiles SET openness = ?, conscientiousness = ?, " +
                    "extraversion = ?, agreeableness = ?, neuroticism = ?, " +
                    "response_length_preference = ?, language_style_preference = ?, " +
                    "content_format_preference = ?, interaction_style = ?, " +
                    "total_messages = ?, total_sessions = ?, avg_session_duration = ?, " +
                    "confidence_score = ?, last_analyzed_at = NOW() " +
                    "WHERE id = ?";
        
        jdbcTemplate.update(sql,
            profile.getOpenness(),
            profile.getConscientiousness(),
            profile.getExtraversion(),
            profile.getAgreeableness(),
            profile.getNeuroticism(),
            profile.getResponseLengthPreference(),
            profile.getLanguageStylePreference(),
            profile.getContentFormatPreference(),
            profile.getInteractionStyle(),
            profile.getTotalMessages(),
            profile.getTotalSessions(),
            profile.getAvgSessionDuration(),
            profile.getConfidenceScore(),
            profile.getId()
        );
        
        return profile;
    }
    
    public void delete(Long id) {
        String sql = "DELETE FROM user_profiles WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    public List<UserProfile> findAll() {
        String sql = "SELECT * FROM user_profiles ORDER BY updated_at DESC";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserProfile.class));
    }
    
    public List<UserProfile> findByConfidenceGreaterThan(double minConfidence) {
        String sql = "SELECT * FROM user_profiles WHERE confidence_score > ? " +
                    "ORDER BY confidence_score DESC";
        return jdbcTemplate.query(sql, 
            new BeanPropertyRowMapper<>(UserProfile.class), minConfidence);
    }
}
```

#### 步骤2.2: 创建单元测试

```java
// app-device/src/test/java/com/example/voicebox/app/device/repository/UserProfileRepositoryTest.java

@SpringBootTest
@Transactional
public class UserProfileRepositoryTest {
    
    @Autowired
    private UserProfileRepository repository;
    
    @Test
    public void testCreateAndFind() {
        // 创建测试数据
        UserProfile profile = new UserProfile();
        profile.setUserId(1L);
        profile.setOpenness(new BigDecimal("0.750"));
        profile.setConscientiousness(new BigDecimal("0.650"));
        profile.setExtraversion(new BigDecimal("0.800"));
        profile.setAgreeableness(new BigDecimal("0.700"));
        profile.setNeuroticism(new BigDecimal("0.400"));
        profile.setConfidenceScore(new BigDecimal("0.500"));
        
        // 保存
        UserProfile saved = repository.create(profile);
        assertNotNull(saved.getId());
        
        // 查询
        UserProfile found = repository.findByUserId(1L);
        assertNotNull(found);
        assertEquals(new BigDecimal("0.750"), found.getOpenness());
    }
    
    @Test
    public void testUpdate() {
        // 创建
        UserProfile profile = createTestProfile();
        UserProfile saved = repository.create(profile);
        
        // 更新
        saved.setOpenness(new BigDecimal("0.850"));
        repository.update(saved);
        
        // 验证
        UserProfile updated = repository.findByUserId(profile.getUserId());
        assertEquals(new BigDecimal("0.850"), updated.getOpenness());
    }
    
    private UserProfile createTestProfile() {
        UserProfile profile = new UserProfile();
        profile.setUserId(System.currentTimeMillis());
        profile.setOpenness(new BigDecimal("0.500"));
        profile.setConscientiousness(new BigDecimal("0.500"));
        profile.setExtraversion(new BigDecimal("0.500"));
        profile.setAgreeableness(new BigDecimal("0.500"));
        profile.setNeuroticism(new BigDecimal("0.500"));
        profile.setConfidenceScore(new BigDecimal("0.300"));
        return profile;
    }
}
```

#### 验收检查
```bash
# 运行单元测试
cd app-device
mvn test -Dtest=UserProfileRepositoryTest

# 检查测试覆盖率
mvn jacoco:report
open target/site/jacoco/index.html
```

---

### 阶段3: NLP服务集成 (第3-4天)

#### 步骤3.1: 添加依赖

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.hankcs</groupId>
    <artifactId>hanlp</artifactId>
    <version>portable-1.8.4</version>
</dependency>
```

#### 步骤3.2: 实现NLP服务

```java
// app-device/src/main/java/com/example/voicebox/app/device/service/NLPService.java

@Service
public class NLPService {
    
    /**
     * 提取主题
     */
    public List<String> extractTopics(String text) {
        List<String> topics = new ArrayList<>();
        
        // 使用HanLP提取关键词
        List<String> keywords = HanLP.extractKeyword(text, 5);
        
        // 映射到主题
        for (String keyword : keywords) {
            String topic = mapKeywordToTopic(keyword);
            if (topic != null && !topics.contains(topic)) {
                topics.add(topic);
            }
        }
        
        return topics;
    }
    
    /**
     * 提取关键词
     */
    public List<String> extractKeywords(String text, int topN) {
        return HanLP.extractKeyword(text, topN);
    }
    
    /**
     * 实体识别
     */
    public Map<String, List<String>> extractEntities(String text) {
        Map<String, List<String>> entities = new HashMap<>();
        
        // 使用HanLP进行命名实体识别
        List<Term> terms = HanLP.segment(text);
        
        for (Term term : terms) {
            String nature = term.nature.toString();
            if (nature.startsWith("nr")) { // 人名
                entities.computeIfAbsent("person", k -> new ArrayList<>())
                       .add(term.word);
            } else if (nature.startsWith("ns")) { // 地名
                entities.computeIfAbsent("location", k -> new ArrayList<>())
                       .add(term.word);
            } else if (nature.startsWith("nt")) { // 机构名
                entities.computeIfAbsent("organization", k -> new ArrayList<>())
                       .add(term.word);
            }
        }
        
        return entities;
    }
    
    /**
     * 情感分析
     */
    public double analyzeSentiment(String text) {
        // 简单的情感词典方法
        int positiveCount = 0;
        int negativeCount = 0;
        
        List<Term> terms = HanLP.segment(text);
        
        for (Term term : terms) {
            if (isPositiveWord(term.word)) {
                positiveCount++;
            } else if (isNegativeWord(term.word)) {
                negativeCount++;
            }
        }
        
        int total = positiveCount + negativeCount;
        if (total == 0) return 0.0;
        
        return (double) (positiveCount - negativeCount) / total;
    }
    
    private String mapKeywordToTopic(String keyword) {
        // 技术相关
        if (keyword.matches(".*(?:编程|代码|开发|算法|数据库).*")) {
            return "技术";
        }
        // 娱乐相关
        if (keyword.matches(".*(?:电影|音乐|游戏|小说).*")) {
            return "娱乐";
        }
        // 学习相关
        if (keyword.matches(".*(?:学习|课程|教程|考试).*")) {
            return "学习";
        }
        // 生活相关
        if (keyword.matches(".*(?:美食|旅游|健康|运动).*")) {
            return "生活";
        }
        return null;
    }
    
    private boolean isPositiveWord(String word) {
        Set<String> positiveWords = Set.of(
            "好", "棒", "喜欢", "开心", "满意", "优秀", "完美", "赞"
        );
        return positiveWords.contains(word);
    }
    
    private boolean isNegativeWord(String word) {
        Set<String> negativeWords = Set.of(
            "不好", "差", "讨厌", "难过", "失望", "糟糕", "烂", "垃圾"
        );
        return negativeWords.contains(word);
    }
}
```

#### 验收检查
```bash
# 运行NLP测试
mvn test -Dtest=NLPServiceTest

# 手动测试
curl -X POST http://localhost:10088/api/nlp/test \
  -H "Content-Type: application/json" \
  -d '{"text": "我很喜欢学习编程，Python和Java都很有趣"}'
```

---

## 🎯 关键检查点

### 每日检查
- [ ] 代码提交到Git
- [ ] 单元测试通过
- [ ] 代码审查完成
- [ ] 文档更新

### 每周检查
- [ ] 里程碑达成
- [ ] 性能测试通过
- [ ] 集成测试通过
- [ ] 风险评估更新

---

## 📝 常见问题

### Q1: NLP准确率不够怎么办？
**A**: 
1. 扩充情感词典
2. 调整关键词提取参数
3. 考虑使用深度学习模型
4. 收集用户反馈优化

### Q2: 性能不达标怎么办？
**A**:
1. 添加Redis缓存
2. 优化数据库查询
3. 使用异步处理
4. 分批处理大量数据

### Q3: 算法准确率如何验证？
**A**:
1. 准备标注数据集
2. 计算准确率、召回率
3. 进行A/B测试
4. 收集用户反馈

---

**文档维护**: VoiceBox开发团队  
**最后更新**: 2024-01-15

# 国外大模型 API 调用 - 最佳实践

## 1. 架构设计最佳实践

### 1.1 高可用设计

**多节点部署**
```yaml
# 推荐配置：至少 2 个代理节点
- 主节点：处理主要流量
- 备用节点：故障转移
- 负载均衡：自动分配流量
```

**健康检查**
```javascript
// 实现完善的健康检查
app.get('/health', async (req, res) => {
  const checks = {
    service: 'ok',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    memory: process.memoryUsage(),
    // 检查外部依赖
    openai: await checkOpenAI(),
    anthropic: await checkAnthropic()
  };
  
  const isHealthy = checks.openai && checks.anthropic;
  res.status(isHealthy ? 200 : 503).json(checks);
});
```

**故障转移**
```java
@Service
public class FailoverProxyService {
    
    private List<String> proxyNodes = Arrays.asList(
        "https://proxy1.yourdomain.com",
        "https://proxy2.yourdomain.com"
    );
    
    public ChatResponse callWithFailover(ChatRequest request) {
        for (String node : proxyNodes) {
            try {
                return callNode(node, request);
            } catch (Exception e) {
                log.warn("Node {} failed, trying next", node);
                continue;
            }
        }
        throw new ServiceUnavailableException("All nodes failed");
    }
}
```

### 1.2 性能优化

**连接池配置**
```java
@Bean
public WebClient llmWebClient() {
    ConnectionProvider provider = ConnectionProvider.builder("llm-pool")
        .maxConnections(100)
        .maxIdleTime(Duration.ofSeconds(20))
        .maxLifeTime(Duration.ofSeconds(60))
        .pendingAcquireTimeout(Duration.ofSeconds(45))
        .evictInBackground(Duration.ofSeconds(120))
        .build();
    
    HttpClient httpClient = HttpClient.create(provider)
        .responseTimeout(Duration.ofSeconds(60));
    
    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

**缓存策略**
```java
@Service
public class SmartCacheService {
    
    // 缓存相同的请求
    @Cacheable(value = "llm-responses", 
               key = "#request.cacheKey()",
               unless = "#result == null")
    public ChatResponse getCachedResponse(ChatRequest request) {
        return callLLMApi(request);
    }
    
    // 缓存键生成
    public String generateCacheKey(ChatRequest request) {
        return DigestUtils.md5Hex(
            request.getModel() +
            request.getMessages().toString() +
            request.getTemperature() +
            request.getMaxTokens()
        );
    }
}
```

**请求合并**
```java
// 对于批量请求，使用批处理 API
public List<ChatResponse> batchChat(List<ChatRequest> requests) {
    return requests.stream()
        .collect(Collectors.groupingBy(ChatRequest::getModel))
        .entrySet().stream()
        .flatMap(entry -> callBatchAPI(entry.getKey(), entry.getValue()).stream())
        .collect(Collectors.toList());
}
```

### 1.3 安全最佳实践

**API 密钥管理**
```java
@Component
public class SecureApiKeyManager {
    
    @Autowired
    private AESEncryption encryption;
    
    // 加密存储
    public void storeApiKey(String provider, String key) {
        String encrypted = encryption.encrypt(key);
        repository.save(new ApiKey(provider, encrypted));
    }
    
    // 定期轮换
    @Scheduled(cron = "0 0 0 1 * ?")  // 每月 1 号
    public void rotateApiKeys() {
        log.info("Starting API key rotation");
        // 实现密钥轮换逻辑
    }
    
    // 使用环境变量
    @Value("${llm.api.key:#{environment.OPENAI_API_KEY}}")
    private String apiKey;
}
```

**请求签名**
```java
public class RequestSigner {
    
    public String signRequest(String payload, String secret) {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] signature = mac.doFinal(payload.getBytes());
        return Base64.getEncoder().encodeToString(signature);
    }
    
    public boolean verifySignature(String payload, String signature, String secret) {
        String expected = signRequest(payload, secret);
        return MessageDigest.isEqual(
            expected.getBytes(),
            signature.getBytes()
        );
    }
}
```

**数据脱敏**
```java
@Aspect
@Component
public class DataMaskingAspect {
    
    @Around("@annotation(MaskSensitiveData)")
    public Object maskData(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String) {
                args[i] = maskSensitiveInfo((String) args[i]);
            }
        }
        
        return joinPoint.proceed(args);
    }
    
    private String maskSensitiveInfo(String content) {
        // 手机号
        content = content.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        // 身份证
        content = content.replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2");
        // 邮箱
        content = content.replaceAll("(\\w{2})\\w+(\\w@)", "$1***$2");
        return content;
    }
}
```

## 2. 成本优化最佳实践

### 2.1 智能路由

**根据任务选择模型**
```java
@Service
public class ModelSelector {
    
    public String selectModel(ChatRequest request) {
        int complexity = analyzeComplexity(request);
        
        if (complexity < 3) {
            return "gpt-3.5-turbo";  // 简单任务，低成本
        } else if (complexity < 7) {
            return "gpt-4";  // 中等任务
        } else {
            return "gpt-4-turbo";  // 复杂任务
        }
    }
    
    private int analyzeComplexity(ChatRequest request) {
        // 基于消息长度、历史对话等因素评估复杂度
        int score = 0;
        score += request.getMessages().size();
        score += request.getMessages().stream()
            .mapToInt(m -> m.getContent().length() / 100)
            .sum();
        return Math.min(score, 10);
    }
}
```

**配额管理**
```java
@Service
public class QuotaManager {
    
    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;
    
    public boolean checkAndDeductQuota(String userId, int tokens) {
        String key = "quota:" + userId + ":" + LocalDate.now();
        Integer remaining = redisTemplate.opsForValue().get(key);
        
        if (remaining == null) {
            remaining = 10000;  // 每日配额
            redisTemplate.opsForValue().set(key, remaining, 1, TimeUnit.DAYS);
        }
        
        if (remaining < tokens) {
            return false;  // 配额不足
        }
        
        redisTemplate.opsForValue().decrement(key, tokens);
        return true;
    }
}
```

### 2.2 缓存优化

**多级缓存**
```java
@Service
public class MultiLevelCacheService {
    
    // L1: 本地缓存（Caffeine）
    private LoadingCache<String, ChatResponse> localCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build(key -> null);
    
    // L2: Redis 缓存
    @Autowired
    private RedisTemplate<String, ChatResponse> redisTemplate;
    
    public ChatResponse getCached(ChatRequest request) {
        String key = generateKey(request);
        
        // 先查本地缓存
        ChatResponse response = localCache.getIfPresent(key);
        if (response != null) {
            return response;
        }
        
        // 再查 Redis
        response = redisTemplate.opsForValue().get(key);
        if (response != null) {
            localCache.put(key, response);
            return response;
        }
        
        // 调用 API
        response = callLLMApi(request);
        
        // 写入缓存
        localCache.put(key, response);
        redisTemplate.opsForValue().set(key, response, 1, TimeUnit.HOURS);
        
        return response;
    }
}
```

**智能缓存失效**
```java
// 根据内容相似度判断是否可以使用缓存
public boolean isCacheable(ChatRequest request) {
    // 不缓存包含时间敏感信息的请求
    String content = request.getMessages().toString().toLowerCase();
    if (content.contains("今天") || content.contains("现在") || content.contains("最新")) {
        return false;
    }
    
    // 不缓存个性化请求
    if (content.contains("我的") || content.contains("帮我")) {
        return false;
    }
    
    return true;
}
```

### 2.3 成本监控

**实时成本统计**
```java
@Component
public class CostTracker {
    
    private static final Map<String, Double> MODEL_COSTS = Map.of(
        "gpt-3.5-turbo", 0.002,  // 每 1K tokens
        "gpt-4", 0.03,
        "gpt-4-turbo", 0.01,
        "claude-3-sonnet", 0.003
    );
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public void trackCost(String model, int inputTokens, int outputTokens) {
        double costPer1K = MODEL_COSTS.getOrDefault(model, 0.0);
        double cost = (inputTokens + outputTokens) / 1000.0 * costPer1K;
        
        meterRegistry.counter("llm.cost.total",
            "model", model
        ).increment(cost);
        
        // 记录到数据库
        saveCostRecord(model, inputTokens, outputTokens, cost);
    }
}
```

**成本告警**
```java
@Scheduled(cron = "0 0 * * * ?")  // 每小时
public void checkCostThreshold() {
    double hourlyCost = calculateHourlyCost();
    double dailyBudget = 100.0;  // $100/天
    double hourlyBudget = dailyBudget / 24;
    
    if (hourlyCost > hourlyBudget * 1.5) {
        sendAlert("成本超出预算 50%: $" + hourlyCost);
    }
}
```

## 3. 可靠性最佳实践

### 3.1 重试策略

**指数退避重试**
```java
@Service
public class RetryableService {
    
    @Retryable(
        value = {RestClientException.class, TimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(
            delay = 1000,
            multiplier = 2,
            maxDelay = 10000
        )
    )
    public ChatResponse callWithRetry(ChatRequest request) {
        return llmClient.chat(request);
    }
    
    @Recover
    public ChatResponse recover(Exception e, ChatRequest request) {
        log.error("All retry attempts failed", e);
        // 返回降级响应
        return getFallbackResponse(request);
    }
}
```

**熔断器**
```java
@Service
public class CircuitBreakerService {
    
    private final CircuitBreaker circuitBreaker = CircuitBreaker.of(
        "llm-api",
        CircuitBreakerConfig.custom()
            .failureRateThreshold(50)  // 失败率 50%
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build()
    );
    
    public ChatResponse callWithCircuitBreaker(ChatRequest request) {
        return circuitBreaker.executeSupplier(() -> llmClient.chat(request));
    }
}
```

### 3.2 超时控制

**分级超时**
```java
public class TimeoutConfig {
    
    // 连接超时
    private static final int CONNECT_TIMEOUT = 5000;  // 5 秒
    
    // 读取超时（根据任务类型）
    public int getReadTimeout(ChatRequest request) {
        if (request.isStream()) {
            return 300000;  // 流式：5 分钟
        } else if (request.getMaxTokens() > 2000) {
            return 120000;  // 长文本：2 分钟
        } else {
            return 60000;   // 普通：1 分钟
        }
    }
}
```

### 3.3 限流保护

**多维度限流**
```java
@Component
public class MultiDimensionRateLimiter {
    
    // 用户级别限流
    public boolean checkUserLimit(String userId) {
        String key = "rate:user:" + userId;
        return checkLimit(key, 60, 1);  // 每分钟 60 次
    }
    
    // IP 级别限流
    public boolean checkIpLimit(String ip) {
        String key = "rate:ip:" + ip;
        return checkLimit(key, 100, 1);  // 每分钟 100 次
    }
    
    // 全局限流
    public boolean checkGlobalLimit() {
        String key = "rate:global";
        return checkLimit(key, 1000, 1);  // 每分钟 1000 次
    }
    
    private boolean checkLimit(String key, int limit, int minutes) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, minutes, TimeUnit.MINUTES);
        }
        return count <= limit;
    }
}
```

## 4. 监控和运维最佳实践

### 4.1 全面监控

**关键指标**
```java
@Component
public class LLMMetrics {
    
    @Autowired
    private MeterRegistry registry;
    
    // 请求指标
    public void recordRequest(String provider, String model, String status) {
        registry.counter("llm.requests.total",
            "provider", provider,
            "model", model,
            "status", status
        ).increment();
    }
    
    // 延迟指标
    public void recordLatency(String provider, long milliseconds) {
        registry.timer("llm.latency",
            "provider", provider
        ).record(milliseconds, TimeUnit.MILLISECONDS);
    }
    
    // Token 使用
    public void recordTokens(String model, int input, int output) {
        registry.counter("llm.tokens.input", "model", model).increment(input);
        registry.counter("llm.tokens.output", "model", model).increment(output);
    }
    
    // 缓存命中率
    public void recordCacheHit(boolean hit) {
        registry.counter("llm.cache",
            "result", hit ? "hit" : "miss"
        ).increment();
    }
}
```

**业务指标**
```java
// 用户满意度
public void recordUserFeedback(String sessionId, int rating) {
    registry.gauge("llm.user.satisfaction",
        Tags.of("rating", String.valueOf(rating)),
        rating
    );
}

// 响应质量
public void recordResponseQuality(ChatResponse response) {
    int quality = analyzeQuality(response);
    registry.gauge("llm.response.quality", quality);
}
```

### 4.2 日志最佳实践

**结构化日志**
```java
@Slf4j
@Component
public class StructuredLogger {
    
    public void logRequest(ChatRequest request, ChatResponse response, long duration) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", Instant.now());
        logData.put("request_id", request.getRequestId());
        logData.put("user_id", request.getUserId());
        logData.put("model", request.getModel());
        logData.put("input_tokens", response.getUsage().getInputTokens());
        logData.put("output_tokens", response.getUsage().getOutputTokens());
        logData.put("duration_ms", duration);
        logData.put("cost", calculateCost(response));
        
        log.info("LLM_REQUEST: {}", new ObjectMapper().writeValueAsString(logData));
    }
}
```

**敏感信息过滤**
```java
public String sanitizeLog(String content) {
    // 移除 API 密钥
    content = content.replaceAll("sk-[a-zA-Z0-9]{48}", "sk-***");
    
    // 移除个人信息
    content = maskSensitiveData(content);
    
    return content;
}
```

### 4.3 告警策略

**分级告警**
```yaml
# 告警规则
alerts:
  # P0: 紧急（短信 + 电话）
  - name: ServiceDown
    condition: up == 0
    duration: 1m
    severity: critical
    
  # P1: 重要（企业微信 + 邮件）
  - name: HighErrorRate
    condition: error_rate > 0.05
    duration: 5m
    severity: warning
    
  # P2: 一般（邮件）
  - name: HighLatency
    condition: p95_latency > 5s
    duration: 10m
    severity: info
```

## 5. 开发最佳实践

### 5.1 错误处理

**统一错误处理**
```java
@ControllerAdvice
public class LLMExceptionHandler {
    
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<?> handleRateLimit(RateLimitException e) {
        return ResponseEntity.status(429)
            .body(ErrorResponse.builder()
                .code("RATE_LIMIT_EXCEEDED")
                .message("请求过于频繁，请稍后再试")
                .retryAfter(60)
                .build());
    }
    
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<?> handleQuotaExceeded(QuotaExceededException e) {
        return ResponseEntity.status(402)
            .body(ErrorResponse.builder()
                .code("QUOTA_EXCEEDED")
                .message("配额已用完")
                .build());
    }
    
    @ExceptionHandler(LLMServiceException.class)
    public ResponseEntity<?> handleServiceError(LLMServiceException e) {
        return ResponseEntity.status(503)
            .body(ErrorResponse.builder()
                .code("SERVICE_UNAVAILABLE")
                .message("服务暂时不可用，请稍后重试")
                .build());
    }
}
```

### 5.2 测试策略

**单元测试**
```java
@Test
public void testRateLimiter() {
    String userId = "test-user";
    
    // 前 60 次应该成功
    for (int i = 0; i < 60; i++) {
        assertTrue(rateLimiter.allowRequest(userId));
    }
    
    // 第 61 次应该失败
    assertFalse(rateLimiter.allowRequest(userId));
}
```

**集成测试**
```java
@SpringBootTest
@AutoConfigureMockMvc
public class LLMProxyIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testChatCompletion() throws Exception {
        mockMvc.perform(post("/api/v1/llm/chat/completions")
            .header("Authorization", "Bearer test-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "model": "gpt-3.5-turbo",
                    "messages": [{"role": "user", "content": "Hello"}]
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.choices[0].message.content").exists());
    }
}
```

**性能测试**
```java
@Test
public void testConcurrentRequests() throws Exception {
    int concurrency = 100;
    ExecutorService executor = Executors.newFixedThreadPool(concurrency);
    CountDownLatch latch = new CountDownLatch(concurrency);
    
    long startTime = System.currentTimeMillis();
    
    for (int i = 0; i < concurrency; i++) {
        executor.submit(() -> {
            try {
                llmService.chat(createTestRequest());
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await();
    long duration = System.currentTimeMillis() - startTime;
    
    assertTrue(duration < 10000);  // 应在 10 秒内完成
}
```

## 6. 合规性最佳实践

### 6.1 数据安全

**数据加密**
```java
// 传输加密：使用 HTTPS/TLS
// 存储加密：敏感数据 AES-256 加密
@Component
public class DataEncryption {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    
    public String encrypt(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
```

**访问控制**
```java
@PreAuthorize("hasRole('LLM_USER')")
public ChatResponse chat(ChatRequest request) {
    // 只有授权用户可以调用
    return llmService.chat(request);
}
```

### 6.2 审计日志

**完整的审计追踪**
```java
@Aspect
@Component
public class AuditAspect {
    
    @Around("@annotation(Audited)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        AuditLog log = new AuditLog();
        log.setTimestamp(Instant.now());
        log.setUserId(getCurrentUserId());
        log.setAction(joinPoint.getSignature().getName());
        log.setIpAddress(getClientIp());
        
        try {
            Object result = joinPoint.proceed();
            log.setStatus("SUCCESS");
            return result;
        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setError(e.getMessage());
            throw e;
        } finally {
            auditRepository.save(log);
        }
    }
}
```

## 7. 总结

### 7.1 核心原则

1. **可靠性优先**：多节点部署、故障转移、熔断保护
2. **成本可控**：智能路由、缓存优化、配额管理
3. **安全第一**：加密存储、访问控制、数据脱敏
4. **可观测性**：全面监控、结构化日志、分级告警
5. **持续优化**：性能测试、成本分析、用户反馈

### 7.2 检查清单

部署前检查：
- [ ] 服务器配置正确
- [ ] SSL 证书有效
- [ ] API 密钥已加密存储
- [ ] 限流规则已配置
- [ ] 监控告警已设置
- [ ] 备份策略已实施
- [ ] 文档已完善

上线后检查：
- [ ] 服务运行正常
- [ ] 监控指标正常
- [ ] 日志输出正常
- [ ] 成本在预算内
- [ ] 用户反馈良好
- [ ] 无安全告警
- [ ] 性能达标

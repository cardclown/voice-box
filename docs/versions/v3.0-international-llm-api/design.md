# 国外大模型 API 调用 - 技术方案设计

## 1. 整体架构

### 1.1 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        用户应用层                              │
│  (VoiceBox App / Web UI / Mobile App)                       │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│                    国内应用服务器                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ API Gateway  │  │  Auth Service │  │ Rate Limiter │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│                    LLM 代理服务层                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              负载均衡器 (Load Balancer)                │   │
│  └────────┬─────────────────────────────────────────────┘   │
│           │                                                  │
│  ┌────────┴────────┬──────────────┬──────────────┐         │
│  │  Proxy Node 1   │ Proxy Node 2 │ Proxy Node 3 │         │
│  │  (海外服务器)    │ (海外服务器)  │ (海外服务器)  │         │
│  └─────────────────┴──────────────┴──────────────┘         │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ↓
┌─────────────────────────────────────────────────────────────┐
│                    大模型 API 服务                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ OpenAI   │  │ Anthropic│  │  Gemini  │  │  Others  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 数据流

```
请求流程：
1. 用户应用 → 国内服务器（鉴权、限流）
2. 国内服务器 → 海外代理节点（负载均衡）
3. 海外代理节点 → 大模型 API（实际调用）
4. 响应原路返回（支持流式传输）
```

## 2. 方案详细设计

### 方案一：自建代理服务器（推荐）

#### 2.1.1 架构组件

**国内服务器组件**
```java
// API 网关服务
@RestController
@RequestMapping("/api/llm")
public class LLMProxyController {
    
    @Autowired
    private ProxyService proxyService;
    
    @Autowired
    private RateLimiter rateLimiter;
    
    @PostMapping("/chat/completions")
    public ResponseEntity<?> chatCompletions(
        @RequestHeader("Authorization") String apiKey,
        @RequestBody ChatRequest request) {
        
        // 1. 验证 API 密钥
        if (!validateApiKey(apiKey)) {
            return ResponseEntity.status(401).build();
        }
        
        // 2. 限流检查
        if (!rateLimiter.allowRequest(apiKey)) {
            return ResponseEntity.status(429).build();
        }
        
        // 3. 转发到海外代理
        return proxyService.forwardRequest(request);
    }
}
```

**海外代理服务器（Node.js）**
```javascript
// proxy-server.js
const express = require('express');
const axios = require('axios');
const app = express();

// 配置多个大模型 API
const API_CONFIGS = {
  openai: {
    baseURL: 'https://api.openai.com/v1',
    key: process.env.OPENAI_API_KEY
  },
  anthropic: {
    baseURL: 'https://api.anthropic.com/v1',
    key: process.env.ANTHROPIC_API_KEY
  },
  gemini: {
    baseURL: 'https://generativelanguage.googleapis.com/v1',
    key: process.env.GEMINI_API_KEY
  }
};

// 代理转发
app.post('/proxy/:provider/*', async (req, res) => {
  const provider = req.params.provider;
  const config = API_CONFIGS[provider];
  
  if (!config) {
    return res.status(400).json({ error: 'Unknown provider' });
  }
  
  try {
    const response = await axios({
      method: req.method,
      url: `${config.baseURL}${req.params[0]}`,
      headers: {
        'Authorization': `Bearer ${config.key}`,
        'Content-Type': 'application/json'
      },
      data: req.body,
      responseType: req.body.stream ? 'stream' : 'json'
    });
    
    // 流式响应处理
    if (req.body.stream) {
      res.setHeader('Content-Type', 'text/event-stream');
      response.data.pipe(res);
    } else {
      res.json(response.data);
    }
  } catch (error) {
    console.error('Proxy error:', error);
    res.status(error.response?.status || 500)
       .json({ error: error.message });
  }
});

app.listen(3000, () => {
  console.log('Proxy server running on port 3000');
});
```

#### 2.1.2 部署架构

**服务器配置**
```yaml
# docker-compose.yml
version: '3.8'

services:
  # 海外代理服务
  proxy-node-1:
    image: llm-proxy:latest
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - NODE_ENV=production
    ports:
      - "3001:3000"
    restart: always
    
  proxy-node-2:
    image: llm-proxy:latest
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - NODE_ENV=production
    ports:
      - "3002:3000"
    restart: always
    
  # Nginx 负载均衡
  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - proxy-node-1
      - proxy-node-2
    restart: always
```

**Nginx 配置**
```nginx
# nginx.conf
upstream llm_proxy {
    least_conn;  # 最少连接负载均衡
    server proxy-node-1:3000 max_fails=3 fail_timeout=30s;
    server proxy-node-2:3000 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    server_name api.yourdomain.com;
    
    # 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;
    
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    
    # 安全配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    location / {
        proxy_pass http://llm_proxy;
        proxy_http_version 1.1;
        
        # 流式响应支持
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_cache off;
        
        # 超时配置
        proxy_connect_timeout 60s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
        
        # 请求头转发
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

#### 2.1.3 成本估算

| 项目 | 配置 | 月费用（USD） | 说明 |
|------|------|--------------|------|
| 海外服务器 × 2 | 2C4G | $20 × 2 = $40 | AWS Lightsail / DigitalOcean |
| 流量费用 | 1TB | $10 | 按实际使用计费 |
| 域名 + SSL | - | $2 | Let's Encrypt 免费 |
| **总计** | - | **$52/月** | 约 ¥370/月 |

### 方案二：第三方中转服务

#### 2.2.1 服务商对比

| 服务商 | 支持模型 | 价格 | 稳定性 | 推荐度 |
|--------|---------|------|--------|--------|
| OpenAI 中转 API | GPT 系列 | 官方价格 + 10% | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| API2D | 多模型 | 官方价格 + 15% | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| CloseAI | OpenAI | 官方价格 + 20% | ⭐⭐⭐ | ⭐⭐⭐ |
| 自建中转 | 全部 | 服务器成本 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

#### 2.2.2 接入示例

```java
// 使用第三方中转服务
@Service
public class ThirdPartyLLMService {
    
    private static final String API_BASE_URL = "https://api.api2d.com/v1";
    
    @Value("${llm.api.key}")
    private String apiKey;
    
    public ChatResponse chat(ChatRequest request) {
        RestTemplate restTemplate = new RestTemplate();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);
        
        ResponseEntity<ChatResponse> response = restTemplate.exchange(
            API_BASE_URL + "/chat/completions",
            HttpMethod.POST,
            entity,
            ChatResponse.class
        );
        
        return response.getBody();
    }
}
```

### 方案三：API 网关 + 云服务

#### 2.3.1 架构设计

使用云服务商的 API 网关 + 海外函数计算

```yaml
# AWS 架构示例
Components:
  - API Gateway (国内区域)
  - Lambda Function (海外区域)
  - CloudFront (CDN 加速)
  - DynamoDB (缓存)
  - CloudWatch (监控)
```

#### 2.3.2 Lambda 函数示例

```javascript
// lambda-proxy.js
const axios = require('axios');

exports.handler = async (event) => {
    const { provider, endpoint, body } = JSON.parse(event.body);
    
    const apiConfigs = {
        openai: {
            url: `https://api.openai.com/v1${endpoint}`,
            key: process.env.OPENAI_KEY
        },
        anthropic: {
            url: `https://api.anthropic.com/v1${endpoint}`,
            key: process.env.ANTHROPIC_KEY
        }
    };
    
    const config = apiConfigs[provider];
    
    try {
        const response = await axios.post(config.url, body, {
            headers: {
                'Authorization': `Bearer ${config.key}`,
                'Content-Type': 'application/json'
            },
            timeout: 60000
        });
        
        return {
            statusCode: 200,
            body: JSON.stringify(response.data)
        };
    } catch (error) {
        return {
            statusCode: error.response?.status || 500,
            body: JSON.stringify({ error: error.message })
        };
    }
};
```

## 3. 核心功能设计

### 3.1 请求管理

#### 3.1.1 API 密钥管理

```java
@Entity
@Table(name = "llm_api_keys")
public class LLMApiKey {
    @Id
    private String id;
    
    private String provider;  // openai, anthropic, gemini
    
    @Column(columnDefinition = "TEXT")
    private String encryptedKey;  // AES 加密存储
    
    private Integer dailyQuota;  // 每日配额
    private Integer usedToday;   // 今日已用
    
    private LocalDateTime lastRotated;  // 上次轮换时间
    private Boolean isActive;
}

@Service
public class ApiKeyService {
    
    @Autowired
    private AESEncryption encryption;
    
    public String getDecryptedKey(String provider) {
        LLMApiKey apiKey = repository.findByProviderAndIsActive(provider, true);
        return encryption.decrypt(apiKey.getEncryptedKey());
    }
    
    public void rotateKey(String provider, String newKey) {
        LLMApiKey apiKey = repository.findByProviderAndIsActive(provider, true);
        apiKey.setEncryptedKey(encryption.encrypt(newKey));
        apiKey.setLastRotated(LocalDateTime.now());
        repository.save(apiKey);
    }
}
```

#### 3.1.2 限流策略

```java
@Component
public class RateLimiter {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // 令牌桶算法
    public boolean allowRequest(String userId, String provider) {
        String key = String.format("rate_limit:%s:%s", userId, provider);
        
        // 每分钟 60 次请求
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        
        return count <= 60;
    }
    
    // 配额管理
    public boolean checkQuota(String userId, String provider) {
        String key = String.format("quota:%s:%s:%s", 
            userId, provider, LocalDate.now());
        
        Long used = redisTemplate.opsForValue().increment(key);
        
        if (used == 1) {
            redisTemplate.expire(key, 1, TimeUnit.DAYS);
        }
        
        // 每日 1000 次请求配额
        return used <= 1000;
    }
}
```

### 3.2 重试机制

```java
@Service
public class RetryableProxyService {
    
    @Retryable(
        value = {RestClientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public ChatResponse callLLMApi(ChatRequest request) {
        // 尝试主节点
        try {
            return callNode(primaryNode, request);
        } catch (Exception e) {
            log.warn("Primary node failed, trying backup", e);
            // 自动切换到备用节点
            return callNode(backupNode, request);
        }
    }
    
    @Recover
    public ChatResponse recover(RestClientException e, ChatRequest request) {
        log.error("All retry attempts failed", e);
        throw new LLMServiceException("Service temporarily unavailable");
    }
}
```

### 3.3 缓存策略

```java
@Service
public class LLMCacheService {
    
    @Autowired
    private RedisTemplate<String, ChatResponse> redisTemplate;
    
    public ChatResponse getCachedResponse(ChatRequest request) {
        // 对于相同的请求，返回缓存结果
        String cacheKey = generateCacheKey(request);
        return redisTemplate.opsForValue().get(cacheKey);
    }
    
    public void cacheResponse(ChatRequest request, ChatResponse response) {
        String cacheKey = generateCacheKey(request);
        // 缓存 1 小时
        redisTemplate.opsForValue().set(cacheKey, response, 1, TimeUnit.HOURS);
    }
    
    private String cacheKey(ChatRequest request) {
        // 基于请求内容生成 hash
        return DigestUtils.md5Hex(
            request.getModel() + 
            request.getMessages().toString() +
            request.getTemperature()
        );
    }
}
```

### 3.4 流式响应处理

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatRequest request) {
    
    return webClient
        .post()
        .uri(proxyUrl + "/chat/completions")
        .bodyValue(request)
        .retrieve()
        .bodyToFlux(String.class)
        .map(chunk -> ServerSentEvent.<String>builder()
            .data(chunk)
            .build())
        .doOnError(error -> log.error("Stream error", error))
        .doOnComplete(() -> log.info("Stream completed"));
}
```

## 4. 监控和运维

### 4.1 监控指标

```java
@Component
public class LLMMetrics {
    
    private final MeterRegistry registry;
    
    // 请求计数
    public void recordRequest(String provider, String model) {
        registry.counter("llm.requests", 
            "provider", provider,
            "model", model
        ).increment();
    }
    
    // 响应时间
    public void recordLatency(String provider, long milliseconds) {
        registry.timer("llm.latency",
            "provider", provider
        ).record(milliseconds, TimeUnit.MILLISECONDS);
    }
    
    // 错误率
    public void recordError(String provider, String errorType) {
        registry.counter("llm.errors",
            "provider", provider,
            "type", errorType
        ).increment();
    }
    
    // 成本统计
    public void recordCost(String provider, double cost) {
        registry.counter("llm.cost",
            "provider", provider
        ).increment(cost);
    }
}
```

### 4.2 健康检查

```java
@Component
public class LLMHealthIndicator implements HealthIndicator {
    
    @Autowired
    private ProxyService proxyService;
    
    @Override
    public Health health() {
        try {
            // 检查代理节点状态
            boolean isHealthy = proxyService.checkHealth();
            
            if (isHealthy) {
                return Health.up()
                    .withDetail("proxy", "available")
                    .build();
            } else {
                return Health.down()
                    .withDetail("proxy", "unavailable")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### 4.3 日志记录

```java
@Aspect
@Component
public class LLMLoggingAspect {
    
    @Around("@annotation(LogLLMCall)")
    public Object logLLMCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("LLM call succeeded: method={}, duration={}ms",
                joinPoint.getSignature().getName(), duration);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            log.error("LLM call failed: method={}, duration={}ms, error={}",
                joinPoint.getSignature().getName(), duration, e.getMessage());
            
            throw e;
        }
    }
}
```

## 5. 安全设计

### 5.1 传输安全

- 全程 HTTPS/TLS 加密
- API 密钥加密存储（AES-256）
- 请求签名验证
- IP 白名单限制

### 5.2 访问控制

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/api/llm/**").authenticated()
                .anyRequest().permitAll()
            .and()
            .addFilterBefore(new ApiKeyAuthFilter(), 
                UsernamePasswordAuthenticationFilter.class);
    }
}
```

### 5.3 数据脱敏

```java
@Component
public class DataMasking {
    
    public String maskSensitiveData(String content) {
        // 手机号脱敏
        content = content.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        
        // 身份证号脱敏
        content = content.replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2");
        
        // 邮箱脱敏
        content = content.replaceAll("(\\w{2})\\w+(\\w@)", "$1***$2");
        
        return content;
    }
}
```

## 6. 方案对比总结

| 维度 | 自建代理 | 第三方服务 | API 网关 |
|------|---------|-----------|----------|
| 成本 | 低（$50/月） | 中（+15%） | 高（$200+/月） |
| 稳定性 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 可控性 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| 技术难度 | 中 | 低 | 高 |
| 部署时间 | 1-2 天 | 1 小时 | 3-5 天 |
| 推荐场景 | 中小企业 | 快速验证 | 大型企业 |

## 7. 推荐方案

**阶段一（MVP）**：使用第三方中转服务快速验证
**阶段二（成长期）**：自建代理服务器，降低成本
**阶段三（成熟期）**：构建完整的 API 网关体系

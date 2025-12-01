# 国外大模型 API 调用 - 部署指南

## 1. 环境准备

### 1.1 服务器要求

**海外服务器（代理节点）**
- 操作系统：Ubuntu 22.04 LTS
- CPU：2 核心
- 内存：4GB
- 硬盘：50GB SSD
- 网络：至少 100Mbps
- 数量：建议 2 台（主备）

**国内服务器（应用服务器）**
- 根据现有 VoiceBox 项目配置
- 需要安装 Redis（用于限流和缓存）

### 1.2 软件依赖

**海外服务器**
```bash
# 更新系统
sudo apt-get update && sudo apt-get upgrade -y

# 安装必要软件
sudo apt-get install -y \
  curl \
  git \
  vim \
  htop \
  net-tools

# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

**国内服务器**
```bash
# 安装 Redis
sudo apt-get install redis-server -y
sudo systemctl enable redis-server
sudo systemctl start redis-server

# 验证 Redis
redis-cli ping  # 应返回 PONG
```

### 1.3 域名和 SSL 证书

**步骤 1：注册域名**

- 推荐使用：阿里云、腾讯云、Cloudflare
- 域名示例：`llm-proxy.yourdomain.com`

**步骤 2：配置 DNS 解析**
```
类型: A
主机记录: llm-proxy
记录值: [海外服务器IP]
TTL: 600
```

**步骤 3：申请 SSL 证书**
```bash
# 安装 Certbot
sudo apt-get install certbot -y

# 申请证书（使用 standalone 模式）
sudo certbot certonly --standalone \
  -d llm-proxy.yourdomain.com \
  --email your-email@example.com \
  --agree-tos

# 证书位置
# /etc/letsencrypt/live/llm-proxy.yourdomain.com/fullchain.pem
# /etc/letsencrypt/live/llm-proxy.yourdomain.com/privkey.pem

# 设置自动续期
echo "0 0 1 * * root certbot renew --quiet" | sudo tee -a /etc/crontab
```

## 2. 海外代理服务部署

### 2.1 创建项目目录

```bash
# 创建项目目录
sudo mkdir -p /opt/llm-proxy
cd /opt/llm-proxy

# 设置权限
sudo chown -R $USER:$USER /opt/llm-proxy
```

### 2.2 创建代理服务代码

**package.json**
```json
{
  "name": "llm-proxy-service",
  "version": "1.0.0",
  "description": "LLM API Proxy Service",
  "main": "src/index.js",
  "scripts": {
    "start": "node src/index.js",
    "dev": "nodemon src/index.js"
  },
  "dependencies": {
    "express": "^4.18.2",
    "axios": "^1.6.0",
    "dotenv": "^16.3.1",
    "express-rate-limit": "^7.1.5",
    "helmet": "^7.1.0",
    "morgan": "^1.10.0",
    "cors": "^2.8.5"
  },
  "devDependencies": {
    "nodemon": "^3.0.2"
  }
}
```

**src/index.js**
```javascript
require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const morgan = require('morgan');
const cors = require('cors');
const proxyRoutes = require('./routes/proxy');

const app = express();
const PORT = process.env.PORT || 3000;

// 中间件
app.use(helmet());
app.use(cors());
app.use(morgan('combined'));
app.use(express.json({ limit: '10mb' }));

// 健康检查
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// 代理路由
app.use('/proxy', proxyRoutes);

// 错误处理
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(err.status || 500).json({
    error: err.message || 'Internal Server Error'
  });
});

app.listen(PORT, () => {
  console.log(`LLM Proxy Service running on port ${PORT}`);
});
```

**src/routes/proxy.js**
```javascript
const express = require('express');
const axios = require('axios');
const router = express.Router();

// API 配置
const API_CONFIGS = {
  openai: {
    baseURL: 'https://api.openai.com/v1',
    key: process.env.OPENAI_API_KEY,
    headers: (key) => ({
      'Authorization': `Bearer ${key}`,
      'Content-Type': 'application/json'
    })
  },
  anthropic: {
    baseURL: 'https://api.anthropic.com/v1',
    key: process.env.ANTHROPIC_API_KEY,
    headers: (key) => ({
      'x-api-key': key,
      'anthropic-version': '2023-06-01',
      'Content-Type': 'application/json'
    })
  },
  gemini: {
    baseURL: 'https://generativelanguage.googleapis.com/v1',
    key: process.env.GEMINI_API_KEY,
    headers: (key) => ({
      'Content-Type': 'application/json'
    })
  }
};

// 通用代理处理
router.all('/:provider/*', async (req, res) => {
  const { provider } = req.params;
  const path = req.params[0];
  
  const config = API_CONFIGS[provider];
  if (!config) {
    return res.status(400).json({ error: 'Unknown provider' });
  }
  
  try {
    const url = provider === 'gemini' 
      ? `${config.baseURL}/${path}?key=${config.key}`
      : `${config.baseURL}/${path}`;
    
    const response = await axios({
      method: req.method,
      url: url,
      headers: config.headers(config.key),
      data: req.body,
      params: req.query,
      responseType: req.body?.stream ? 'stream' : 'json',
      timeout: 60000
    });
    
    // 流式响应
    if (req.body?.stream) {
      res.setHeader('Content-Type', 'text/event-stream');
      res.setHeader('Cache-Control', 'no-cache');
      res.setHeader('Connection', 'keep-alive');
      response.data.pipe(res);
    } else {
      res.json(response.data);
    }
  } catch (error) {
    console.error(`Proxy error for ${provider}:`, error.message);
    
    const status = error.response?.status || 500;
    const message = error.response?.data || { error: error.message };
    
    res.status(status).json(message);
  }
});

module.exports = router;
```

**src/config.js**
```javascript
module.exports = {
  port: process.env.PORT || 3000,
  nodeEnv: process.env.NODE_ENV || 'development',
  
  // API 密钥
  apiKeys: {
    openai: process.env.OPENAI_API_KEY,
    anthropic: process.env.ANTHROPIC_API_KEY,
    gemini: process.env.GEMINI_API_KEY
  },
  
  // 超时配置
  timeout: {
    request: 60000,  // 60 秒
    stream: 300000   // 5 分钟
  },
  
  // 重试配置
  retry: {
    maxAttempts: 3,
    delay: 1000,
    backoff: 2
  }
};
```

**.env.example**
```bash
# 服务配置
PORT=3000
NODE_ENV=production

# API 密钥
OPENAI_API_KEY=sk-xxx
ANTHROPIC_API_KEY=sk-ant-xxx
GEMINI_API_KEY=xxx

# 日志级别
LOG_LEVEL=info
```

### 2.3 创建 Docker 配置

**Dockerfile**
```dockerfile
FROM node:18-alpine

WORKDIR /app

# 安装依赖
COPY package*.json ./
RUN npm ci --only=production

# 复制代码
COPY src ./src

# 暴露端口
EXPOSE 3000

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD node -e "require('http').get('http://localhost:3000/health', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})"

# 启动服务
CMD ["npm", "start"]
```

**docker-compose.yml**
```yaml
version: '3.8'

services:
  proxy-node-1:
    build: .
    container_name: llm-proxy-1
    restart: always
    environment:
      - PORT=3000
      - NODE_ENV=production
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - GEMINI_API_KEY=${GEMINI_API_KEY}
    ports:
      - "3001:3000"
    networks:
      - llm-network
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
  
  proxy-node-2:
    build: .
    container_name: llm-proxy-2
    restart: always
    environment:
      - PORT=3000
      - NODE_ENV=production
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - ANTHROPIC_API_KEY=${ANTHROPIC_API_KEY}
      - GEMINI_API_KEY=${GEMINI_API_KEY}
    ports:
      - "3002:3000"
    networks:
      - llm-network
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
  
  nginx:
    image: nginx:alpine
    container_name: llm-nginx
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
    depends_on:
      - proxy-node-1
      - proxy-node-2
    networks:
      - llm-network
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

networks:
  llm-network:
    driver: bridge
```

**nginx/nginx.conf**
```nginx
events {
    worker_connections 1024;
}

http {
    upstream llm_proxy {
        least_conn;
        server proxy-node-1:3000 max_fails=3 fail_timeout=30s;
        server proxy-node-2:3000 max_fails=3 fail_timeout=30s;
    }
    
    # HTTP 重定向到 HTTPS
    server {
        listen 80;
        server_name llm-proxy.yourdomain.com;
        return 301 https://$server_name$request_uri;
    }
    
    # HTTPS 服务
    server {
        listen 443 ssl http2;
        server_name llm-proxy.yourdomain.com;
        
        # SSL 证书
        ssl_certificate /etc/letsencrypt/live/llm-proxy.yourdomain.com/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/llm-proxy.yourdomain.com/privkey.pem;
        
        # SSL 配置
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;
        ssl_prefer_server_ciphers on;
        ssl_session_cache shared:SSL:10m;
        ssl_session_timeout 10m;
        
        # 安全头
        add_header Strict-Transport-Security "max-age=31536000" always;
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        
        # 代理配置
        location / {
            proxy_pass http://llm_proxy;
            proxy_http_version 1.1;
            
            # 流式响应支持
            proxy_set_header Connection "";
            proxy_buffering off;
            proxy_cache off;
            proxy_read_timeout 300s;
            
            # 请求头
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        
        # 健康检查
        location /health {
            proxy_pass http://llm_proxy/health;
            access_log off;
        }
    }
}
```

### 2.4 部署步骤

```bash
# 1. 进入项目目录
cd /opt/llm-proxy

# 2. 配置环境变量
cp .env.example .env
vim .env  # 填入实际的 API 密钥

# 3. 修改 nginx 配置中的域名
vim nginx/nginx.conf  # 替换 yourdomain.com

# 4. 构建镜像
docker-compose build

# 5. 启动服务
docker-compose up -d

# 6. 查看日志
docker-compose logs -f

# 7. 检查服务状态
docker-compose ps
curl http://localhost/health
```

### 2.5 验证部署

```bash
# 测试健康检查
curl https://llm-proxy.yourdomain.com/health

# 测试 OpenAI 代理
curl -X POST https://llm-proxy.yourdomain.com/proxy/openai/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

## 3. 国内应用服务配置

### 3.1 添加 Maven 依赖

在 `pom.xml` 中添加：

```xml
<!-- HTTP 客户端 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- 限流 -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
```

### 3.2 配置文件

在 `config.properties` 中添加：

```properties
# LLM 代理配置
llm.proxy.base-url=https://llm-proxy.yourdomain.com
llm.proxy.timeout=60000
llm.proxy.max-retries=3

# Redis 配置
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
spring.redis.database=0

# 限流配置
llm.rate-limit.requests-per-minute=60
llm.rate-limit.daily-quota=1000
```

### 3.3 创建 Java 代码

在 `app-device/src/main/java/com/example/voicebox/app/device/llm/` 目录下创建以下文件：

**LLMProxyConfig.java**
```java
package com.example.voicebox.app.device.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
public class LLMProxyConfig {
    
    @Value("${llm.proxy.base-url}")
    private String baseUrl;
    
    @Value("${llm.proxy.timeout:60000}")
    private int timeout;
    
    @Bean
    public WebClient llmWebClient() {
        return WebClient.builder()
            .baseUrl(baseUrl)
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024))  // 10MB
            .build();
    }
}
```

**LLMProxyService.java**
```java
package com.example.voicebox.app.device.llm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class LLMProxyService {
    
    @Autowired
    private WebClient llmWebClient;
    
    @Autowired
    private RateLimiterService rateLimiter;
    
    public Mono<ChatResponse> chat(String provider, ChatRequest request) {
        // 检查限流
        if (!rateLimiter.allowRequest(request.getUserId(), provider)) {
            return Mono.error(new RateLimitException("请求过于频繁"));
        }
        
        return llmWebClient
            .post()
            .uri("/proxy/" + provider + "/chat/completions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse.class)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(10)))
            .timeout(Duration.ofSeconds(60));
    }
}
```

### 3.4 部署到现有项目

```bash
# 1. 编译项目
cd /path/to/voicebox
mvn clean package -DskipTests

# 2. 重启服务
./stop-all.sh
./start-all.sh

# 3. 查看日志
tail -f backend.log
```

## 4. 监控部署

### 4.1 部署 Prometheus

```bash
# 创建配置目录
sudo mkdir -p /opt/monitoring/prometheus
cd /opt/monitoring/prometheus

# 创建配置文件
cat > prometheus.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'llm-proxy'
    static_configs:
      - targets: ['llm-proxy.yourdomain.com:9090']
    
  - job_name: 'voicebox-app'
    static_configs:
      - targets: ['localhost:8080']
EOF

# 启动 Prometheus
docker run -d \
  --name prometheus \
  --restart always \
  -p 9090:9090 \
  -v $(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```

### 4.2 部署 Grafana

```bash
# 启动 Grafana
docker run -d \
  --name grafana \
  --restart always \
  -p 3000:3000 \
  -e "GF_SECURITY_ADMIN_PASSWORD=admin" \
  grafana/grafana

# 访问 Grafana
# http://your-server:3000
# 默认账号：admin / admin
```

### 4.3 配置告警

在 Prometheus 中添加告警规则：

```yaml
# /opt/monitoring/prometheus/alerts.yml
groups:
  - name: llm_proxy_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "LLM 代理错误率过高"
          description: "错误率超过 5%"
      
      - alert: ServiceDown
        expr: up{job="llm-proxy"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "LLM 代理服务不可用"
```

## 5. 安全加固

### 5.1 防火墙配置

```bash
# 安装 UFW
sudo apt-get install ufw -y

# 配置规则
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS

# 启用防火墙
sudo ufw enable
sudo ufw status
```

### 5.2 SSH 安全

```bash
# 编辑 SSH 配置
sudo vim /etc/ssh/sshd_config

# 修改以下配置
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
Port 22

# 重启 SSH
sudo systemctl restart sshd
```

### 5.3 自动更新

```bash
# 安装自动更新
sudo apt-get install unattended-upgrades -y

# 配置自动更新
sudo dpkg-reconfigure -plow unattended-upgrades
```

## 6. 备份和恢复

### 6.1 配置备份

```bash
# 创建备份脚本
cat > /opt/llm-proxy/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/backups/llm-proxy"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# 备份配置文件
tar -czf $BACKUP_DIR/config_$DATE.tar.gz \
  /opt/llm-proxy/.env \
  /opt/llm-proxy/nginx/nginx.conf \
  /opt/llm-proxy/docker-compose.yml

# 保留最近 7 天的备份
find $BACKUP_DIR -name "config_*.tar.gz" -mtime +7 -delete

echo "Backup completed: $BACKUP_DIR/config_$DATE.tar.gz"
EOF

chmod +x /opt/llm-proxy/backup.sh

# 添加定时任务
echo "0 2 * * * /opt/llm-proxy/backup.sh" | sudo tee -a /etc/crontab
```

### 6.2 恢复步骤

```bash
# 停止服务
cd /opt/llm-proxy
docker-compose down

# 恢复配置
tar -xzf /opt/backups/llm-proxy/config_YYYYMMDD_HHMMSS.tar.gz -C /

# 重启服务
docker-compose up -d
```

## 7. 故障排查

### 7.1 常见问题

**问题 1：服务无法启动**
```bash
# 查看日志
docker-compose logs

# 检查端口占用
sudo netstat -tlnp | grep :3000

# 检查配置文件
docker-compose config
```

**问题 2：SSL 证书错误**
```bash
# 检查证书有效期
sudo certbot certificates

# 手动续期
sudo certbot renew

# 重启 Nginx
docker-compose restart nginx
```

**问题 3：代理请求失败**
```bash
# 测试网络连通性
curl -I https://api.openai.com

# 检查 API 密钥
echo $OPENAI_API_KEY

# 查看详细日志
docker-compose logs -f proxy-node-1
```

### 7.2 性能优化

```bash
# 查看系统资源
htop
docker stats

# 优化 Docker
# 在 docker-compose.yml 中添加资源限制
services:
  proxy-node-1:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 2G
        reservations:
          cpus: '0.5'
          memory: 1G
```

## 8. 维护清单

### 8.1 日常维护

- [ ] 每日检查服务状态
- [ ] 每日查看错误日志
- [ ] 每周检查磁盘空间
- [ ] 每周查看监控指标
- [ ] 每月更新系统补丁
- [ ] 每月检查 SSL 证书有效期
- [ ] 每季度进行性能测试
- [ ] 每季度审查安全配置

### 8.2 应急联系

- 运维负责人：[姓名] [电话]
- 开发负责人：[姓名] [电话]
- 云服务商支持：[电话]

## 9. 附录

### 9.1 有用的命令

```bash
# 查看服务状态
docker-compose ps

# 重启服务
docker-compose restart

# 查看实时日志
docker-compose logs -f --tail=100

# 进入容器
docker exec -it llm-proxy-1 sh

# 清理日志
docker-compose logs --no-log-prefix > /dev/null

# 更新镜像
docker-compose pull
docker-compose up -d
```

### 9.2 监控指标说明

| 指标 | 说明 | 正常范围 |
|------|------|----------|
| CPU 使用率 | 服务器 CPU 占用 | < 70% |
| 内存使用率 | 服务器内存占用 | < 80% |
| 磁盘使用率 | 磁盘空间占用 | < 80% |
| 请求成功率 | API 调用成功率 | > 99% |
| 响应时间 P95 | 95% 请求响应时间 | < 2s |
| 错误率 | 5xx 错误占比 | < 1% |

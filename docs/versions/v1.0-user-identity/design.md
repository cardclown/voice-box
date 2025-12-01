# v1.0 无注册用户身份识别系统 - 设计文档

**版本**: v1.0  
**创建日期**: 2024-01-15  
**状态**: 📝 规划中  
**设计负责人**: VoiceBox架构团队

---

## 📐 架构设计

### 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         客户端层                              │
├──────────────────────┬──────────────────────────────────────┤
│   Web前端 (Vue 3)    │    小程序前端 (微信原生)              │
│  - 设备指纹生成       │   - 微信登录                          │
│  - 本地存储管理       │   - 设备信息收集                      │
│  - 用户身份管理       │   - 用户授权                          │
└──────────────────────┴──────────────────────────────────────┘
                              ↓ HTTPS
┌─────────────────────────────────────────────────────────────┐
│                         API网关层                             │
│  - 请求路由                                                   │
│  - 身份验证                                                   │
│  - 限流控制                                                   │
│  - 日志记录                                                   │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                        应用服务层                             │
├──────────────────────┬──────────────────────────────────────┤
│  用户身份服务         │   设备管理服务                        │
│  - 用户识别          │   - 设备指纹验证                      │
│  - ID生成            │   - 设备绑定                          │
│  - 身份绑定          │   - 设备列表管理                      │
└──────────────────────┴──────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                        数据访问层                             │
├──────────────────────┬──────────────────────────────────────┤
│   Redis缓存          │    MySQL数据库                        │
│  - 用户信息缓存       │   - 用户表                            │
│  - 会话缓存          │   - 设备表                            │
│  - 热点数据          │   - 会话表                            │
└──────────────────────┴──────────────────────────────────────┘
```

### 技术栈选型

**后端**:
- 框架: Spring Boot 2.7+
- 数据库: MySQL 8.0+
- 缓存: Redis 6.0+
- 构建工具: Maven 3.8+

**前端**:
- 框架: Vue 3 + Vite
- 状态管理: Pinia
- HTTP客户端: Axios
- 加密库: CryptoJS

**小程序**:
- 框架: 微信小程序原生
- 网络请求: wx.request
- 存储: wx.storage


## 🗄️ 数据模型设计

### 用户表 (users)

```sql
CREATE TABLE users (
    id VARCHAR(20) PRIMARY KEY COMMENT '用户ID，格式：u_xxxxxxxxxxxxxx',
    device_fingerprint VARCHAR(64) UNIQUE COMMENT '设备指纹SHA-256哈希值',
    phone_number VARCHAR(20) COMMENT '手机号（加密存储）',
    phone_number_hash VARCHAR(64) COMMENT '手机号哈希值（用于查询）',
    nickname VARCHAR(100) COMMENT '用户昵称',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    
    -- 设备信息
    ip_address VARCHAR(45) COMMENT '注册IP地址',
    user_agent TEXT COMMENT '浏览器UserAgent',
    platform VARCHAR(50) COMMENT '平台：web/wechat-miniprogram/mobile-web',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    last_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
    
    -- 状态
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    
    -- 索引
    INDEX idx_device_fingerprint (device_fingerprint),
    INDEX idx_phone_number_hash (phone_number_hash),
    INDEX idx_created_at (created_at),
    INDEX idx_last_seen_at (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 用户设备表 (user_devices)

```sql
CREATE TABLE user_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(20) NOT NULL COMMENT '用户ID',
    device_fingerprint VARCHAR(64) NOT NULL COMMENT '设备指纹',
    device_name VARCHAR(100) COMMENT '设备名称',
    device_type VARCHAR(50) COMMENT '设备类型：desktop/mobile/tablet',
    
    -- 设备详细信息
    platform VARCHAR(50) COMMENT '平台',
    browser VARCHAR(50) COMMENT '浏览器',
    os VARCHAR(50) COMMENT '操作系统',
    screen_resolution VARCHAR(20) COMMENT '屏幕分辨率',
    
    -- 状态
    is_primary BOOLEAN DEFAULT FALSE COMMENT '是否主设备',
    is_trusted BOOLEAN DEFAULT TRUE COMMENT '是否可信设备',
    last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后使用时间',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_device (user_id, device_fingerprint),
    INDEX idx_user_id (user_id),
    INDEX idx_device_fingerprint (device_fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设备表';
```

### 用户会话表 (user_sessions)

```sql
CREATE TABLE user_sessions (
    id VARCHAR(32) PRIMARY KEY COMMENT '会话ID',
    user_id VARCHAR(20) NOT NULL COMMENT '用户ID',
    device_fingerprint VARCHAR(64) COMMENT '设备指纹',
    
    -- 会话信息
    ip_address VARCHAR(45) COMMENT 'IP地址',
    user_agent TEXT COMMENT 'UserAgent',
    
    -- 时间
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    expires_at TIMESTAMP COMMENT '过期时间',
    last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活动时间',
    
    -- 状态
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_device_fingerprint (device_fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表';
```


## 🔧 核心组件设计

### 1. 设备指纹生成器 (DeviceFingerprintGenerator)

**职责**: 生成稳定、唯一的设备指纹

**实现方案**:

```javascript
// 前端实现
class DeviceFingerprintGenerator {
  async generate() {
    const components = await this.collectComponents()
    const fingerprint = this.hashComponents(components)
    return fingerprint
  }
  
  async collectComponents() {
    return {
      canvas: this.getCanvasFingerprint(),
      webgl: this.getWebGLFingerprint(),
      audio: await this.getAudioFingerprint(),
      screen: this.getScreenInfo(),
      timezone: this.getTimezoneInfo(),
      language: this.getLanguageInfo(),
      platform: this.getPlatformInfo(),
      plugins: this.getPluginsInfo(),
      fonts: this.getFontsInfo()
    }
  }
  
  hashComponents(components) {
    const json = JSON.stringify(components)
    return CryptoJS.SHA256(json).toString()
  }
}
```

**关键技术点**:
- Canvas指纹: 绘制特定文本和图形，获取像素数据
- WebGL指纹: 获取GPU厂商、渲染器信息
- 音频指纹: 生成音频信号，分析频谱特征
- 屏幕指纹: 分辨率、色深、像素比
- 系统指纹: 时区、语言、平台、插件

### 2. 用户身份服务 (UserIdentityService)

**职责**: 管理用户身份识别和生命周期

**核心方法**:

```java
@Service
public class UserIdentityService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 获取或创建用户ID
     * 识别优先级: 本地存储 > 设备指纹 > 手机号 > 创建新用户
     */
    public UserIdentityResponse getOrCreateUserId(UserIdentityRequest request) {
        // 1. 尝试从缓存获取
        String cachedUserId = getCachedUserId(request.getDeviceFingerprint());
        if (cachedUserId != null) {
            return buildResponse(cachedUserId, false);
        }
        
        // 2. 尝试通过设备指纹查找
        Optional<User> userByFingerprint = userRepository
            .findByDeviceFingerprint(request.getDeviceFingerprint());
        if (userByFingerprint.isPresent()) {
            User user = userByFingerprint.get();
            cacheUserId(request.getDeviceFingerprint(), user.getId());
            updateLastSeen(user.getId(), request);
            return buildResponse(user.getId(), false);
        }
        
        // 3. 尝试通过手机号查找
        if (request.getPhoneNumber() != null) {
            String phoneHash = hashPhoneNumber(request.getPhoneNumber());
            Optional<User> userByPhone = userRepository
                .findByPhoneNumberHash(phoneHash);
            if (userByPhone.isPresent()) {
                User user = userByPhone.get();
                // 绑定新设备
                bindDevice(user.getId(), request);
                cacheUserId(request.getDeviceFingerprint(), user.getId());
                return buildResponse(user.getId(), false);
            }
        }
        
        // 4. 创建新用户
        String newUserId = createNewUser(request);
        cacheUserId(request.getDeviceFingerprint(), newUserId);
        return buildResponse(newUserId, true);
    }
    
    /**
     * 生成用户ID
     */
    private String generateUserId() {
        String id;
        do {
            id = "u_" + RandomStringUtils.randomAlphanumeric(14);
        } while (userRepository.existsById(id));
        return id;
    }
    
    /**
     * 创建新用户
     */
    private String createNewUser(UserIdentityRequest request) {
        String userId = generateUserId();
        
        User user = User.builder()
            .id(userId)
            .deviceFingerprint(request.getDeviceFingerprint())
            .ipAddress(request.getIpAddress())
            .userAgent(request.getUserAgent())
            .platform(request.getPlatform())
            .createdAt(LocalDateTime.now())
            .lastSeenAt(LocalDateTime.now())
            .isActive(true)
            .isDeleted(false)
            .build();
            
        userRepository.save(user);
        
        // 创建设备记录
        createDeviceRecord(userId, request, true);
        
        return userId;
    }
    
    /**
     * 绑定设备
     */
    private void bindDevice(String userId, UserIdentityRequest request) {
        // 检查设备是否已绑定
        boolean exists = deviceRepository.existsByUserIdAndDeviceFingerprint(
            userId, request.getDeviceFingerprint()
        );
        
        if (!exists) {
            createDeviceRecord(userId, request, false);
        }
    }
}
```

### 3. 设备管理服务 (DeviceManagementService)

**职责**: 管理用户设备信息

```java
@Service
public class DeviceManagementService {
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    /**
     * 获取用户所有设备
     */
    public List<UserDevice> getUserDevices(String userId) {
        return deviceRepository.findByUserIdOrderByLastUsedAtDesc(userId);
    }
    
    /**
     * 设置主设备
     */
    public void setPrimaryDevice(String userId, String deviceFingerprint) {
        // 取消其他设备的主设备状态
        deviceRepository.updatePrimaryStatusByUserId(userId, false);
        
        // 设置新的主设备
        deviceRepository.updatePrimaryStatusByUserIdAndFingerprint(
            userId, deviceFingerprint, true
        );
    }
    
    /**
     * 解绑设备
     */
    public void unbindDevice(String userId, String deviceFingerprint) {
        deviceRepository.deleteByUserIdAndDeviceFingerprint(
            userId, deviceFingerprint
        );
    }
}
```


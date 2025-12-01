# 豆包服务问题解决方案

**问题**：豆包WebSocket API返回404  
**日期**：2024-11-30  
**状态**：待解决

---

## 🔍 问题分析

### 根本原因
豆包语音服务的WebSocket API端点 `wss://openspeech.bytedance.com/api/v1/tts` 返回HTTP 404错误。

### 诊断结果
```
域名解析: ✅ 正常 (198.18.5.210)
端口连接: ✅ 正常 (443端口可达)
HTTP连接: ❌ 404 Not Found
```

### 可能原因
1. **API地址已变更** - 豆包可能已升级API版本
2. **认证方式改变** - 可能需要不同的认证机制
3. **服务已下线** - 该API端点可能已停止服务
4. **需要新的接入方式** - 可能需要通过火山引擎平台接入

---

## 💡 解决方案

### 方案1：使用Mock服务（临时方案）✅ 推荐

**优点**：
- 立即可用，不阻塞开发
- 可以完成前端和业务逻辑开发
- 便于测试和演示

**实施步骤**：

1. **已创建MockVoiceService**
   ```java
   // app-device/src/main/java/.../MockVoiceService.java
   // 提供模拟的语音识别和合成功能
   ```

2. **修改配置启用Mock模式**
   ```properties
   # 在config.properties中添加
   voicebox.voice.use.mock=true
   ```

3. **修改VoiceServiceProxy支持Mock**
   ```java
   // 检测豆包服务失败时自动降级到Mock服务
   ```

### 方案2：更新豆包API配置

**需要做的事情**：

1. **查阅最新文档**
   - 访问：https://www.volcengine.com/docs/6561/79817
   - 查找最新的API端点和认证方式

2. **联系技术支持**
   - 确认当前AppID和Token是否有效
   - 获取正确的API地址
   - 了解新的接入方式

3. **可能需要的变更**
   - 使用HTTP API而非WebSocket
   - 更新认证机制
   - 使用新的SDK

### 方案3：更换语音服务提供商

**备选方案**：

#### A. 阿里云语音服务
- **优点**：稳定可靠，文档完善
- **价格**：按调用次数计费
- **接入**：提供Java SDK
- **文档**：https://help.aliyun.com/product/30413.html

#### B. 腾讯云语音服务
- **优点**：性价比高，支持多种语言
- **价格**：有免费额度
- **接入**：提供Java SDK
- **文档**：https://cloud.tencent.com/product/asr

#### C. 讯飞语音服务
- **优点**：国内领先，识别准确
- **价格**：有免费额度
- **接入**：提供Java SDK
- **文档**：https://www.xfyun.cn/

---

## 🚀 立即执行的方案

### 步骤1：启用Mock服务（5分钟）

修改服务器配置：

```bash
# 1. 添加mock配置
ssh root@129.211.180.183 "echo 'voicebox.voice.use.mock=true' >> /opt/voicebox/config.properties"

# 2. 重新编译包含MockVoiceService的代码
cd app-device
mvn jar:jar spring-boot:repackage -Dmaven.test.skip=true

# 3. 上传并重启
scp target/app-device-0.0.1-SNAPSHOT.jar root@129.211.180.183:/tmp/
ssh root@129.211.180.183 "
  pkill -f 'app-device.*jar'
  mv /tmp/app-device-0.0.1-SNAPSHOT.jar /opt/voicebox/app-device/target/
  cd /opt/voicebox
  nohup java -Xmx1024m -Xms512m -jar app-device/target/app-device-0.0.1-SNAPSHOT.jar --spring.config.location=/opt/voicebox/config.properties > logs/app.log 2>&1 &
"
```

### 步骤2：修改VoiceServiceProxy支持Mock（15分钟）

```java
@Service
public class VoiceServiceProxy {
    
    @Autowired
    private DoubaoVoiceService doubaoVoiceService;
    
    @Autowired(required = false)
    private MockVoiceService mockVoiceService;
    
    @Value("${voicebox.voice.use.mock:false}")
    private boolean useMock;
    
    public String speechToText(InputStream audioStream, String language) throws Exception {
        if (useMock && mockVoiceService != null) {
            log.info("使用Mock语音识别服务");
            return mockVoiceService.speechToText(audioStream, language).get();
        }
        
        // 尝试使用豆包服务，失败时降级到Mock
        try {
            return doubaoVoiceService.speechToText(audioStream, language).get();
        } catch (Exception e) {
            if (mockVoiceService != null) {
                log.warn("豆包服务失败，降级到Mock服务", e);
                return mockVoiceService.speechToText(audioStream, language).get();
            }
            throw e;
        }
    }
    
    // textToSpeech方法类似
}
```

### 步骤3：测试Mock服务（5分钟）

```bash
# 测试语音合成
curl -X POST http://129.211.180.183:10088/api/voice/synthesize \
  -H "Content-Type: application/json" \
  -d '{"text":"测试Mock服务","userId":1,"sessionId":1,"language":"zh-CN"}'

# 应该返回成功响应
```

---

## 📋 后续工作

### 短期（1-2天）
1. ✅ 实施Mock服务方案
2. 🔄 完成前端开发和测试
3. 🔄 编写单元测试

### 中期（1周）
1. 🔄 联系豆包技术支持
2. 🔄 评估备选语音服务
3. 🔄 准备迁移方案

### 长期（2周）
1. 🔄 完成语音服务迁移
2. 🔄 性能优化
3. 🔄 生产环境测试

---

## 🎯 成功标准

### Mock服务方案
- ✅ 语音合成API返回成功
- ✅ 语音识别API返回成功
- ✅ 前端可以正常调用
- ✅ 不阻塞开发进度

### 最终方案
- ✅ 真实语音识别功能正常
- ✅ 真实语音合成功能正常
- ✅ 响应时间符合要求（STT<5s, TTS<2s）
- ✅ 音质满足要求

---

## 📞 联系方式

### 豆包/火山引擎技术支持
- 官网：https://www.volcengine.com/
- 文档：https://www.volcengine.com/docs/6561/79817
- 工单系统：https://console.volcengine.com/workorder

### 备选服务商
- 阿里云：https://help.aliyun.com/
- 腾讯云：https://cloud.tencent.com/
- 讯飞：https://www.xfyun.cn/

---

## 📝 决策记录

### 2024-11-30
- **决策**：先使用Mock服务，不阻塞开发
- **理由**：豆包API问题需要时间解决，不应影响整体进度
- **影响**：前端和业务逻辑可以继续开发，后续替换真实服务

---

**创建时间**：2024-11-30 14:45  
**更新时间**：2024-11-30 14:45  
**状态**：进行中

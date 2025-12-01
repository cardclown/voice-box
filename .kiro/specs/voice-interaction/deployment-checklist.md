# 语音交互功能部署检查清单

## 部署前准备

### 1. 数据库准备

- [ ] 执行数据库迁移脚本
  ```bash
  mysql -u root -p voicebox_db < deploy/db/schema/04-voice-tables.sql
  ```

- [ ] 验证表创建成功
  ```bash
  mysql -u root -p voicebox_db -e "SHOW TABLES LIKE 'voice%';"
  ```

- [ ] 检查表结构
  ```bash
  mysql -u root -p voicebox_db -e "DESCRIBE voice_messages;"
  mysql -u root -p voicebox_db -e "DESCRIBE voice_service_logs;"
  ```

### 2. 文件系统准备

- [ ] 创建音频存储目录
  ```bash
  sudo mkdir -p /data/voicebox/audio
  ```

- [ ] 设置目录权限
  ```bash
  sudo chown -R voicebox:voicebox /data/voicebox/audio
  sudo chmod 755 /data/voicebox/audio
  ```

- [ ] 验证目录可写
  ```bash
  touch /data/voicebox/audio/test.txt && rm /data/voicebox/audio/test.txt
  ```

### 3. 配置文件准备

- [ ] 更新 `config.properties`
  ```properties
  # 豆包语音配置
  voicebox.doubao.voice.appid=7112763635
  voicebox.doubao.voice.token=xfjd9wi3AgzAmFVBckiWad9437lcx2HB
  voicebox.doubao.voice.secret=NiiqP5oNG8uaUNsbaoC1PdQDL_ORqn46
  
  # 存储配置
  voice.storage.base-path=/data/voicebox/audio
  voice.storage.max-file-size=10485760
  
  # 服务配置
  voicebox.voice.primary.provider=doubao
  voicebox.voice.max.retries=3
  ```

- [ ] 验证配置文件语法
  ```bash
  cat config.properties | grep "voicebox.doubao"
  ```

### 4. 依赖检查

- [ ] 检查Java版本（需要Java 11+）
  ```bash
  java -version
  ```

- [ ] 检查Maven版本
  ```bash
  mvn -version
  ```

- [ ] 检查网络连接
  ```bash
  ping -c 3 openspeech.bytedance.com
  ```

## 本地开发环境部署

### 1. 编译项目

```bash
cd /path/to/voicebox
mvn clean install -DskipTests
```

- [ ] 编译成功，无错误

### 2. 启动服务

```bash
./start-all.sh dev
```

- [ ] 后端服务启动成功
- [ ] 前端服务启动成功

### 3. 验证服务

```bash
# 检查服务状态
./status.sh

# 检查端口
netstat -an | grep 10088
```

- [ ] 服务运行正常
- [ ] 端口监听正常

### 4. 测试API

```bash
# 运行测试脚本
./scripts/test-voice-api.sh
```

- [ ] 语音合成测试通过
- [ ] 音频下载测试通过
- [ ] 语音上传测试通过

## 生产环境部署

### 1. 备份

- [ ] 备份数据库
  ```bash
  mysqldump -u root -p voicebox_db > backup_$(date +%Y%m%d_%H%M%S).sql
  ```

- [ ] 备份配置文件
  ```bash
  cp config.properties config.properties.backup
  ```

- [ ] 备份现有代码
  ```bash
  tar -czf voicebox_backup_$(date +%Y%m%d_%H%M%S).tar.gz /opt/voicebox
  ```

### 2. 上传代码

```bash
# 本地打包
tar -czf voicebox-voice.tar.gz \
  --exclude=node_modules \
  --exclude=target \
  --exclude=.git \
  app-device/src/main/java/com/example/voicebox/app/device/service/voice/ \
  app-device/src/main/java/com/example/voicebox/app/device/controller/VoiceController.java \
  app-device/src/main/java/com/example/voicebox/app/device/domain/VoiceMessage.java \
  app-device/src/main/java/com/example/voicebox/app/device/repository/VoiceMessageRepository.java \
  deploy/db/schema/04-voice-tables.sql \
  config.properties

# 上传到服务器
scp voicebox-voice.tar.gz root@129.211.180.183:/tmp/
```

- [ ] 文件上传成功

### 3. 服务器部署

```bash
# 连接服务器
ssh root@129.211.180.183

# 停止服务
cd /opt/voicebox
./stop-all.sh

# 解压新代码
cd /opt/voicebox
tar -xzf /tmp/voicebox-voice.tar.gz

# 执行数据库迁移
mysql -u voicebox -pvoicebox123 voicebox_db < deploy/db/schema/04-voice-tables.sql

# 创建存储目录
mkdir -p /data/voicebox/audio
chown -R voicebox:voicebox /data/voicebox/audio

# 编译
mvn clean install -DskipTests

# 启动服务
./start-all.sh prod
```

- [ ] 数据库迁移成功
- [ ] 编译成功
- [ ] 服务启动成功

### 4. 验证部署

```bash
# 检查服务状态
./status.sh

# 检查日志
tail -f logs/app-device.log

# 测试API
curl http://localhost:10088/api/voice/synthesize \
  -H "Content-Type: application/json" \
  -d '{"text":"测试","userId":1,"language":"zh-CN"}'
```

- [ ] 服务运行正常
- [ ] 日志无错误
- [ ] API响应正常

### 5. 监控配置

- [ ] 配置日志监控
  ```bash
  # 添加到crontab
  */5 * * * * tail -100 /opt/voicebox/logs/app-device.log | grep -i "error\|exception" | mail -s "VoiceBox Error" admin@example.com
  ```

- [ ] 配置磁盘空间监控
  ```bash
  # 检查存储空间
  df -h /data/voicebox/audio
  ```

- [ ] 配置定期清理
  ```bash
  # 添加到crontab（每天凌晨2点清理90天前的文件）
  0 2 * * * find /data/voicebox/audio -type f -mtime +90 -delete
  ```

## 回滚计划

如果部署出现问题，执行以下步骤回滚：

### 1. 停止服务

```bash
cd /opt/voicebox
./stop-all.sh
```

### 2. 恢复代码

```bash
# 删除新代码
rm -rf /opt/voicebox/*

# 恢复备份
tar -xzf /path/to/backup/voicebox_backup_YYYYMMDD_HHMMSS.tar.gz -C /
```

### 3. 恢复数据库

```bash
# 删除新表
mysql -u voicebox -pvoicebox123 voicebox_db -e "DROP TABLE IF EXISTS voice_messages, voice_service_logs;"

# 恢复备份
mysql -u voicebox -pvoicebox123 voicebox_db < backup_YYYYMMDD_HHMMSS.sql
```

### 4. 重启服务

```bash
cd /opt/voicebox
./start-all.sh prod
```

## 部署后验证

### 1. 功能测试

- [ ] 语音合成功能正常
- [ ] 语音识别功能正常
- [ ] 音频文件下载正常
- [ ] 个性化音色选择正常

### 2. 性能测试

- [ ] API响应时间 < 5秒
- [ ] 并发请求处理正常
- [ ] 内存使用正常
- [ ] CPU使用正常

### 3. 数据验证

- [ ] 语音消息正确保存到数据库
- [ ] 音频文件正确保存到文件系统
- [ ] 文件路径正确记录

### 4. 日志检查

- [ ] 无ERROR级别日志
- [ ] 无WARN级别异常日志
- [ ] 请求日志正常记录

## 常见问题

### 问题1：WebSocket连接失败

**检查项**：
- [ ] 网络连接正常
- [ ] AppID、Token、Secret配置正确
- [ ] 防火墙未阻止WebSocket连接

**解决方案**：
```bash
# 测试网络连接
curl -I https://openspeech.bytedance.com

# 检查配置
grep "voicebox.doubao" config.properties
```

### 问题2：音频文件保存失败

**检查项**：
- [ ] 存储目录存在
- [ ] 目录权限正确
- [ ] 磁盘空间充足

**解决方案**：
```bash
# 检查目录
ls -la /data/voicebox/audio

# 检查权限
sudo chown -R voicebox:voicebox /data/voicebox/audio

# 检查空间
df -h /data/voicebox
```

### 问题3：数据库连接失败

**检查项**：
- [ ] 数据库服务运行正常
- [ ] 数据库配置正确
- [ ] 表已创建

**解决方案**：
```bash
# 检查数据库
systemctl status mysqld

# 测试连接
mysql -u voicebox -pvoicebox123 voicebox_db -e "SELECT 1;"

# 检查表
mysql -u voicebox -pvoicebox123 voicebox_db -e "SHOW TABLES;"
```

## 部署完成确认

- [ ] 所有检查项已完成
- [ ] 所有测试已通过
- [ ] 监控已配置
- [ ] 文档已更新
- [ ] 团队已通知

**部署人员签名**：_______________

**部署时间**：_______________

**验证人员签名**：_______________

**验证时间**：_______________

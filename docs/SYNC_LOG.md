# 环境同步日志

本文档记录本地开发环境和云服务器环境之间的同步操作。

---

## 2024-11-29 - Maven 版本升级计划

### 变更类型
环境配置变更

### 方向
本地 → 云服务器

### 变更内容

#### 问题描述
- 云服务器当前使用 Maven 3.0.5 (Red Hat 版本)
- 版本过旧，不支持新特性
- 与本地开发环境版本不一致

#### 解决方案
- 升级服务器 Maven 到 3.9.9
- 手动安装到 /opt/maven
- 配置环境变量
- 移除旧的 yum 版本

#### 创建的文件
1. `scripts/server/upgrade-maven.sh` - Maven 升级脚本
2. `docs/MAVEN_UPGRADE_GUIDE.md` - 详细升级指南

#### 更新的文档
1. `docs/SERVER_ENVIRONMENT.md` - 更新 Maven 版本信息
2. `QUICK_START.md` - 添加环境要求说明
3. `deploy/README.md` - 添加 Maven 升级步骤
4. `docs/SYNC_LOG.md` - 本文档

### 执行步骤

#### 准备阶段（已完成）
- [x] 创建升级脚本
- [x] 编写升级指南
- [x] 更新相关文档
- [x] 提交到版本控制

#### 执行阶段（待执行）
- [ ] 上传升级脚本到服务器
- [ ] 执行升级脚本
- [ ] 验证 Maven 版本
- [ ] 重新编译项目
- [ ] 重启后端服务
- [ ] 测试应用功能

#### 验证阶段（待执行）
- [ ] 确认 Maven 版本为 3.9.9
- [ ] 确认编译成功
- [ ] 确认服务正常运行
- [ ] 确认 API 正常响应

### 执行命令

```bash
# 1. 上传升级脚本
scp scripts/server/upgrade-maven.sh root@129.211.180.183:/tmp/

# 2. 连接服务器
ssh root@129.211.180.183

# 3. 执行升级
cd /tmp
chmod +x upgrade-maven.sh
./upgrade-maven.sh

# 4. 验证版本
source /etc/profile.d/maven.sh
mvn -version

# 5. 重新编译项目
cd /opt/voicebox
mvn clean install -DskipTests

# 6. 重启服务
systemctl restart voicebox-backend

# 7. 测试 API
curl -X POST http://localhost:10088/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"测试","userId":1}'
```

### 回滚方案

如果升级失败，可以回滚：

```bash
# 恢复旧版本
rm -rf /opt/maven
mv /opt/maven.backup.YYYYMMDD_HHMMSS /opt/maven

# 或重新安装 yum 版本
yum install -y maven

# 重启服务
systemctl restart voicebox-backend
```

### 影响范围
- 云服务器环境
- 后端编译和部署流程
- 不影响运行中的服务（升级后需重启）

### 风险评估
- **风险等级**: 低
- **影响范围**: 仅服务器环境
- **可回滚**: 是
- **停机时间**: 约 5-10 分钟

### 相关文档
- [Maven 升级指南](./MAVEN_UPGRADE_GUIDE.md)
- [服务器环境文档](./SERVER_ENVIRONMENT.md)
- [环境同步规范](../.kiro/steering/environment-sync.md)

### 执行结果

#### 升级过程
1. ✅ 在本地下载 Maven 3.9.9 安装包
2. ✅ 上传到服务器 /tmp/ 目录
3. ✅ 解压到 /opt/maven
4. ✅ 配置环境变量 /etc/profile.d/maven.sh
5. ✅ 移除旧的 yum 版本
6. ✅ 验证 Maven 版本: 3.9.9
7. ✅ 同步 pom.xml (添加 JAXB 依赖)
8. ✅ 重新编译项目
9. ✅ 重启后端服务
10. ✅ 测试 API 正常

#### 遇到的问题
1. **问题**: 下载速度慢
   - **解决**: 在本地下载后上传到服务器

2. **问题**: JAR 文件缺少 main manifest
   - **解决**: 使用 `spring-boot:repackage` 重新打包

3. **问题**: 缺少 JAXB 依赖 (Java 11)
   - **解决**: 同步本地 pom.xml，包含 JAXB 依赖

#### 验证结果
```bash
# Maven 版本
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: /opt/maven
Java version: 11.0.23

# 编译成功
BUILD SUCCESS
Total time: 8.240 s

# 服务运行
Active: active (running)
Tomcat started on port(s): 10088

# API 测试
curl -X POST http://localhost:10088/api/chat
返回正常响应
```

### 状态
✅ 已完成 (2024-11-29 11:09)

---

## 2024-11-28 - 初始部署

### 变更类型
初始部署

### 方向
本地 → 云服务器

### 变更内容
- 部署 VoiceBox 应用到云服务器
- 配置 MySQL 数据库
- 配置 Redis 缓存
- 配置 Nginx 反向代理
- 创建 systemd 服务

### 执行步骤
- [x] 安装基础环境（Java, Node.js, MySQL, Redis, Nginx）
- [x] 创建数据库和用户
- [x] 上传应用代码
- [x] 编译后端项目
- [x] 构建前端项目
- [x] 配置 Nginx
- [x] 创建 systemd 服务
- [x] 启动所有服务

### 验证结果
- ✅ 前端可访问: http://129.211.180.183
- ✅ 后端 API 正常: http://129.211.180.183/api
- ✅ 数据库连接正常
- ✅ 聊天功能正常

### 状态
✅ 已完成

---

## 同步规范

### 代码变更同步
1. 在本地开发和测试
2. 提交到版本控制
3. 部署到服务器
4. 在服务器上测试
5. 记录到本日志

### 配置变更同步
1. 识别配置差异（本地 vs 服务器）
2. 创建服务器版本的配置
3. 上传并应用配置
4. 重启相关服务
5. 验证功能
6. 记录到本日志

### 数据库变更同步
1. 创建迁移脚本
2. 在本地测试
3. 上传到服务器
4. 在服务器上应用
5. 验证数据结构
6. 更新 init-database.sql
7. 记录到本日志

### 环境变更同步
1. 识别环境差异
2. 创建升级/安装脚本
3. 编写详细文档
4. 在服务器上执行
5. 验证环境
6. 更新相关文档
7. 记录到本日志

---

## 日志模板

```markdown
## YYYY-MM-DD - 变更标题

### 变更类型
[代码变更 / 配置变更 / 数据库变更 / 环境变更]

### 方向
[本地 → 服务器 / 服务器 → 本地 / 双向同步]

### 变更内容
[详细描述变更内容]

### 变更文件
- file1.java
- file2.vue
- config.properties

### 执行步骤
- [ ] 步骤1
- [ ] 步骤2
- [ ] 步骤3

### 验证结果
- [ ] 验证项1
- [ ] 验证项2

### 影响范围
[描述影响的模块和功能]

### 相关文档
- [文档链接1](./doc1.md)
- [文档链接2](./doc2.md)

### 状态
[⏳ 待执行 / 🔄 进行中 / ✅ 已完成 / ❌ 失败]
```

---

## 注意事项

1. **每次同步都要记录** - 便于追踪和问题排查
2. **详细记录步骤** - 便于复现和回滚
3. **记录验证结果** - 确保同步成功
4. **更新相关文档** - 保持文档与实际环境一致
5. **提交到版本控制** - 团队成员可见

---

## 快速参考

### 查看最近的同步
```bash
# 查看本文档的最近更新
git log -p docs/SYNC_LOG.md
```

### 查看服务器当前环境
```bash
ssh root@129.211.180.183 "
  echo '=== Java 版本 ==='
  java -version
  echo ''
  echo '=== Maven 版本 ==='
  mvn -version
  echo ''
  echo '=== Node.js 版本 ==='
  node -v
  echo ''
  echo '=== 服务状态 ==='
  systemctl status voicebox-backend --no-pager
"
```

### 同步检查清单
- [ ] 代码已在本地测试
- [ ] 代码已提交到 Git
- [ ] 配置文件已更新
- [ ] 数据库迁移已准备
- [ ] 部署脚本已测试
- [ ] 文档已更新
- [ ] 同步日志已记录

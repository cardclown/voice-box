---
inclusion: always
---

# 环境同步规范

## 核心原则

**云服务器（生产环境）和本地开发环境必须保持同步**，确保：
1. 代码变更在两个环境中一致
2. 配置变更在两个环境中同步
3. 数据库结构在两个环境中匹配
4. 避免环境差异导致的问题

---

## 强制同步规则

### 1. 代码变更同步

#### 云服务器 → 本地开发环境

**场景**：在云服务器上直接修改了代码（紧急修复、调试等）

**必须执行**：
```bash
# 1. 从服务器下载变更的文件
scp root@129.211.180.183:/opt/voicebox/[变更的文件路径] [本地对应路径]

# 或者下载整个目录
scp -r root@129.211.180.183:/opt/voicebox/[目录] [本地目录]

# 2. 在本地验证变更
./start-all.sh
./status.sh

# 3. 提交到版本控制
git add [变更的文件]
git commit -m "同步服务器变更: [描述]"
git push
```

**检查清单**：
- [ ] 已下载所有变更的文件
- [ ] 本地测试通过
- [ ] 已提交到 Git
- [ ] 文档已更新（如需要）

#### 本地开发环境 → 云服务器

**场景**：在本地开发了新功能或修复了问题

**必须执行**：
```bash
# 1. 在本地测试通过
./start-all.sh
./status.sh
# 进行完整测试

# 2. 提交到版本控制
git add .
git commit -m "[描述变更]"
git push

# 3. 部署到服务器
# 方式 A: 使用部署脚本
cd deploy
./deploy-to-server.sh

# 方式 B: 手动部署
# 压缩代码
tar -czf voicebox.tar.gz \
  --exclude=node_modules \
  --exclude=target \
  --exclude=.git \
  --exclude=*.log \
  --exclude=*.pid \
  .

# 上传到服务器
scp voicebox.tar.gz root@129.211.180.183:/tmp/

# 连接服务器并部署
ssh root@129.211.180.183
cd /opt/voicebox
tar -xzf /tmp/voicebox.tar.gz
./stop-all.sh
mvn clean install -DskipTests
cd app-web && npm install && npm run build && cd ..
./start-all.sh
```

**检查清单**：
- [ ] 本地测试通过
- [ ] 已提交到 Git
- [ ] 已部署到服务器
- [ ] 服务器测试通过
- [ ] 服务正常运行

---

### 2. 配置文件同步

#### 需要同步的配置文件

| 文件 | 位置 | 说明 |
|------|------|------|
| `config.properties` | 根目录 | 应用配置 |
| `application.properties` | `app-device/src/main/resources/` | Spring Boot 配置 |
| `.env` | `app-web/` | 前端环境变量 |
| `nginx.conf` | `/etc/nginx/conf.d/` | Nginx 配置（仅服务器） |

#### 配置变更流程

**在云服务器修改配置后**：
```bash
# 1. 下载服务器配置
scp root@129.211.180.183:/opt/voicebox/config.properties ./config.properties.server

# 2. 对比差异
diff config.properties config.properties.server

# 3. 合并到本地配置
# 手动编辑 config.properties，保留本地开发环境的特定配置

# 4. 记录变更
echo "配置变更: [描述]" >> docs/CHANGELOG.md

# 5. 提交到版本控制
git add config.properties docs/CHANGELOG.md
git commit -m "同步服务器配置变更: [描述]"
```

**在本地修改配置后**：
```bash
# 1. 测试本地配置
./restart-all.sh

# 2. 创建服务器版本的配置
cp config.properties config.properties.server
# 编辑 config.properties.server，修改为服务器环境的值

# 3. 上传到服务器
scp config.properties.server root@129.211.180.183:/opt/voicebox/config.properties

# 4. 重启服务器服务
ssh root@129.211.180.183 "cd /opt/voicebox && ./restart-all.sh"

# 5. 提交到版本控制
git add config.properties
git commit -m "更新配置: [描述]"
```

---

### 3. 数据库变更同步

#### 数据库结构变更

**在云服务器修改数据库后**：
```bash
# 1. 导出服务器数据库结构
ssh root@129.211.180.183 "mysqldump -u voicebox -pvoicebox123 --no-data voicebox_db > /tmp/schema.sql"

# 2. 下载到本地
scp root@129.211.180.183:/tmp/schema.sql ./deploy/schema-from-server.sql

# 3. 对比差异
diff deploy/init-database.sql deploy/schema-from-server.sql

# 4. 应用到本地数据库
mysql -u root -p voicebox_db < deploy/schema-from-server.sql

# 5. 更新初始化脚本
cp deploy/schema-from-server.sql deploy/init-database.sql

# 6. 提交到版本控制
git add deploy/init-database.sql
git commit -m "同步服务器数据库结构: [描述]"
```

**在本地修改数据库后**：
```bash
# 1. 导出本地数据库结构
mysqldump -u root -p --no-data voicebox_db > deploy/init-database.sql

# 2. 测试初始化脚本
mysql -u root -p -e "DROP DATABASE IF EXISTS voicebox_db_test; CREATE DATABASE voicebox_db_test;"
mysql -u root -p voicebox_db_test < deploy/init-database.sql

# 3. 上传到服务器
scp deploy/init-database.sql root@129.211.180.183:/tmp/

# 4. 在服务器上应用变更
ssh root@129.211.180.183 "mysql -u voicebox -pvoicebox123 voicebox_db < /tmp/init-database.sql"

# 5. 提交到版本控制
git add deploy/init-database.sql
git commit -m "数据库结构变更: [描述]"
```

#### 数据库迁移脚本

**创建迁移脚本**：
```bash
# 在 deploy/migrations/ 目录创建迁移脚本
# 命名格式: YYYYMMDD_HHMMSS_description.sql

# 例如: deploy/migrations/20241128_153000_add_user_preferences.sql
```

**应用迁移脚本**：
```bash
# 本地
mysql -u root -p voicebox_db < deploy/migrations/[脚本名].sql

# 服务器
scp deploy/migrations/[脚本名].sql root@129.211.180.183:/tmp/
ssh root@129.211.180.183 "mysql -u voicebox -pvoicebox123 voicebox_db < /tmp/[脚本名].sql"
```

---

### 4. 依赖变更同步

#### 后端依赖（Maven）

**在任一环境修改 pom.xml 后**：
```bash
# 1. 提交到版本控制
git add pom.xml
git commit -m "更新 Maven 依赖: [描述]"
git push

# 2. 在另一环境拉取并更新
git pull
mvn clean install

# 3. 在服务器上更新
ssh root@129.211.180.183
cd /opt/voicebox
git pull  # 如果服务器使用 Git
# 或者上传新的 pom.xml
mvn clean install
./restart-all.sh
```

#### 前端依赖（npm）

**在任一环境修改 package.json 后**：
```bash
# 1. 提交到版本控制
git add package.json package-lock.json
git commit -m "更新 npm 依赖: [描述]"
git push

# 2. 在另一环境拉取并更新
git pull
cd app-web
npm install

# 3. 在服务器上更新
ssh root@129.211.180.183
cd /opt/voicebox/app-web
npm install
npm run build
# 重启 Nginx 或前端服务
```

---

### 5. 环境变量同步

#### 环境变量文件

**本地开发环境**：
- `config.properties` - 本地配置
- `app-web/.env.development` - 前端开发配置

**云服务器环境**：
- `config.properties` - 生产配置
- `app-web/.env.production` - 前端生产配置

#### 同步流程

```bash
# 1. 维护环境变量模板
# env-example.properties - 包含所有配置项的示例

# 2. 本地开发环境
cp env-example.properties config.properties
# 编辑 config.properties，填入本地环境的值

# 3. 服务器环境
scp env-example.properties root@129.211.180.183:/opt/voicebox/
ssh root@129.211.180.183
cd /opt/voicebox
cp env-example.properties config.properties
# 编辑 config.properties，填入生产环境的值
```

---

## 同步检查清单

### 每次在云服务器修改后

- [ ] 记录修改的内容和原因
- [ ] 下载变更的文件到本地
- [ ] 在本地测试变更
- [ ] 提交到版本控制
- [ ] 更新相关文档
- [ ] 通知团队成员

### 每次在本地开发后

- [ ] 在本地完整测试
- [ ] 提交到版本控制
- [ ] 部署到服务器
- [ ] 在服务器上测试
- [ ] 验证服务正常运行
- [ ] 更新部署文档

---

## 同步工具和脚本

### 快速同步脚本

创建 `scripts/dev/sync-from-server.sh`：
```bash
#!/bin/bash
# 从服务器同步代码到本地

SERVER="root@129.211.180.183"
SERVER_DIR="/opt/voicebox"
LOCAL_DIR="."

echo "从服务器同步代码..."

# 同步指定文件或目录
if [ -z "$1" ]; then
  echo "用法: ./sync-from-server.sh [文件或目录路径]"
  echo "例如: ./sync-from-server.sh app-device/src/main/java/com/example/voicebox/app/device/controller/"
  exit 1
fi

scp -r $SERVER:$SERVER_DIR/$1 $LOCAL_DIR/$1

echo "同步完成！"
echo "请检查变更并提交到 Git"
```

创建 `scripts/dev/sync-to-server.sh`：
```bash
#!/bin/bash
# 同步本地代码到服务器

SERVER="root@129.211.180.183"
SERVER_DIR="/opt/voicebox"
LOCAL_DIR="."

echo "同步代码到服务器..."

# 同步指定文件或目录
if [ -z "$1" ]; then
  echo "用法: ./sync-to-server.sh [文件或目录路径]"
  echo "例如: ./sync-to-server.sh app-device/src/main/java/com/example/voicebox/app/device/controller/"
  exit 1
fi

scp -r $LOCAL_DIR/$1 $SERVER:$SERVER_DIR/$1

echo "同步完成！"
echo "请在服务器上重启相关服务"
```

---

## 同步记录

### 创建同步日志

在 `docs/SYNC_LOG.md` 中记录每次同步：

```markdown
## 同步记录

### 2024-11-28 15:30 - 修复聊天功能 Bug

**方向**: 云服务器 → 本地
**变更文件**:
- app-device/src/main/java/.../ChatController.java

**变更内容**: 修复消息发送时的空指针异常

**同步步骤**:
1. 从服务器下载文件
2. 本地测试通过
3. 提交到 Git (commit: abc123)

---

### 2024-11-27 10:00 - 添加用户画像功能

**方向**: 本地 → 云服务器
**变更文件**:
- app-device/src/main/java/.../PersonalityController.java
- app-web/src/views/PersonalityProfile.vue

**变更内容**: 实现用户画像展示功能

**同步步骤**:
1. 本地开发完成
2. 本地测试通过
3. 提交到 Git (commit: def456)
4. 部署到服务器
5. 服务器测试通过
```

---

## 避免同步冲突

### 最佳实践

1. **优先在本地开发**
   - 所有新功能在本地开发和测试
   - 只在紧急情况下直接修改服务器代码

2. **及时同步**
   - 每次修改后立即同步
   - 不要积累多个未同步的变更

3. **使用版本控制**
   - 所有变更都提交到 Git
   - 使用分支管理不同的功能

4. **文档记录**
   - 记录每次同步的内容和原因
   - 维护同步日志

5. **通知团队**
   - 重要变更通知团队成员
   - 避免多人同时修改同一文件

---

## 紧急情况处理

### 服务器紧急修复

如果必须在服务器上直接修复问题：

```bash
# 1. 在服务器上修复
ssh root@129.211.180.183
cd /opt/voicebox
# 修改代码
./restart-all.sh

# 2. 立即备份修改的文件
cp [修改的文件] [修改的文件].backup

# 3. 尽快同步到本地
# 在本地执行
scp root@129.211.180.183:/opt/voicebox/[修改的文件] ./[修改的文件]

# 4. 提交到版本控制
git add [修改的文件]
git commit -m "紧急修复: [描述]"
git push

# 5. 记录到同步日志
echo "紧急修复: [描述]" >> docs/SYNC_LOG.md
```

---

## 注意事项

1. **配置文件差异**
   - 本地和服务器的配置文件可能不同（数据库地址、端口等）
   - 不要直接覆盖，要手动合并

2. **数据库数据**
   - 同步数据库结构，不要同步数据（除非必要）
   - 生产数据不要下载到本地

3. **敏感信息**
   - 不要将生产环境的密码、密钥提交到 Git
   - 使用环境变量或配置文件管理敏感信息

4. **测试充分**
   - 同步后必须在目标环境测试
   - 确保功能正常再继续

5. **备份重要**
   - 修改前备份重要文件
   - 定期备份数据库

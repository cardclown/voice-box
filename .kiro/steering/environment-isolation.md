# 环境隔离规范

## 核心原则

**开发环境和生产环境必须完全隔离**，确保：
1. 开发测试不影响线上服务
2. 数据库独立，避免数据污染
3. 配置分离，环境切换简单
4. 部署流程标准化

## ⚠️ 重要：不逃避问题原则

**当开发环境出现问题时，必须在本地解决，不要逃避到云环境运行**

### 为什么不能逃避

1. **问题会累积** - 今天逃避的问题，明天会变成更大的问题
2. **环境不一致** - 开发环境的问题可能在生产环境更严重
3. **团队协作** - 其他开发者也会遇到同样的问题
4. **技能提升** - 解决问题是提升能力的最好方式
5. **生产风险** - 未经本地测试的代码不应该上生产

### 正确的做法

#### ❌ 错误示范

```bash
# 本地编译失败
mvn clean install
# [ERROR] 编译失败

# 错误做法：直接在服务器上运行
ssh root@129.211.180.183
cd /opt/voicebox
mvn clean install  # 在服务器上编译
./start-all.sh     # 在服务器上测试
```

**问题**：
- 生产环境变成了测试环境
- 可能影响线上服务
- 问题没有真正解决
- 下次还会遇到同样的问题

#### ✅ 正确做法

```bash
# 本地编译失败
mvn clean install
# [ERROR] 编译失败

# 正确做法：分析并解决问题
# 1. 查看详细错误信息
mvn clean install -X

# 2. 分析错误原因
# - 依赖问题？
# - 代码语法错误？
# - 配置问题？
# - 环境问题？

# 3. 解决问题
# - 修复代码错误
# - 更新依赖
# - 调整配置
# - 安装缺失的工具

# 4. 验证修复
mvn clean install
./start-all.sh dev

# 5. 测试功能
# 在本地完整测试

# 6. 提交代码
git add .
git commit -m "修复: [描述问题和解决方案]"

# 7. 部署到生产
./deploy/deploy-prod.sh
```

### 常见问题及解决方案

#### 问题 1: 本地编译失败

**症状**：
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin
```

**不要**：去服务器上编译

**应该**：
```bash
# 1. 查看详细错误
mvn clean install -X

# 2. 检查 Java 版本
java -version
# 确保与项目要求一致

# 3. 清理并重试
mvn clean
rm -rf ~/.m2/repository/com/example/voicebox
mvn install

# 4. 检查代码错误
# 使用 IDE 查看编译错误
# 修复语法错误、导入错误等

# 5. 更新依赖
mvn dependency:resolve
```

#### 问题 2: 数据库连接失败

**症状**：
```
Communications link failure
```

**不要**：直接连到生产数据库测试

**应该**：
```bash
# 1. 检查本地数据库是否运行
# macOS
brew services list | grep mysql

# Linux
systemctl status mysqld

# 2. 启动本地数据库
brew services start mysql  # macOS
systemctl start mysqld     # Linux

# 3. 测试连接
mysql -u voicebox_dev -pdev123 voicebox_dev

# 4. 检查配置
cat config-dev.properties
# 确保使用本地数据库配置

# 5. 初始化数据库（如果需要）
mysql -u root -p < deploy/db/init-database.sh
```

#### 问题 3: 前端启动失败

**症状**：
```
npm ERR! code ELIFECYCLE
```

**不要**：在服务器上测试前端

**应该**：
```bash
# 1. 查看详细错误
npm run dev --verbose

# 2. 清理并重装依赖
cd app-web
rm -rf node_modules package-lock.json
npm cache clean --force
npm install

# 3. 检查 Node 版本
node -v
npm -v
# 确保版本符合要求

# 4. 检查端口占用
lsof -ti:5173
# 如果有进程占用，杀掉它
kill -9 [PID]

# 5. 重新启动
npm run dev
```

#### 问题 4: 依赖冲突

**症状**：
```
Dependency convergence error
```

**不要**：忽略警告，直接部署

**应该**：
```bash
# 1. 分析依赖树
mvn dependency:tree

# 2. 查找冲突
mvn dependency:tree | grep -A 5 "conflict"

# 3. 解决冲突
# 在 pom.xml 中明确指定版本
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>...</groupId>
      <artifactId>...</artifactId>
      <version>指定版本</version>
    </dependency>
  </dependencies>
</dependencyManagement>

# 4. 验证
mvn clean install
```

#### 问题 5: 测试失败

**症状**：
```
Tests run: 10, Failures: 2, Errors: 0, Skipped: 0
```

**不要**：跳过测试部署（-DskipTests）

**应该**：
```bash
# 1. 运行失败的测试
mvn test -Dtest=FailedTestClass

# 2. 查看测试日志
cat target/surefire-reports/FailedTestClass.txt

# 3. 修复测试或代码
# 分析失败原因
# 修复问题

# 4. 重新运行所有测试
mvn test

# 5. 确保全部通过后再部署
```

### 问题排查流程

```
遇到问题
    ↓
不要慌张，不要逃避
    ↓
1. 记录错误信息
   - 截图
   - 复制错误日志
   - 记录操作步骤
    ↓
2. 分析问题
   - 查看完整错误信息
   - 搜索错误信息
   - 查看相关文档
    ↓
3. 尝试解决
   - 根据错误信息修复
   - 参考类似问题的解决方案
   - 逐步排查
    ↓
4. 验证修复
   - 重现问题
   - 确认已解决
   - 测试相关功能
    ↓
5. 记录解决方案
   - 更新文档
   - 提交代码
   - 分享经验
    ↓
6. 部署到生产
```

### 寻求帮助的正确方式

如果确实无法解决，应该：

1. **整理问题信息**
   - 完整的错误日志
   - 已尝试的解决方案
   - 环境信息（OS、版本等）

2. **寻求帮助**
   - 询问团队成员
   - 搜索 Stack Overflow
   - 查看官方文档
   - 提问时提供完整信息

3. **不要**
   - 直接说"不行"就放弃
   - 在生产环境上试错
   - 跳过问题继续开发

### 培养解决问题的能力

1. **保持耐心** - 解决问题需要时间
2. **系统思考** - 从根本原因入手
3. **记录经验** - 建立个人知识库
4. **持续学习** - 提升技术能力
5. **不怕失败** - 失败是学习的机会

### 环境问题检查清单

遇到问题时，按顺序检查：

- [ ] 错误信息是什么？
- [ ] 最近改了什么代码？
- [ ] 配置文件正确吗？
- [ ] 依赖版本匹配吗？
- [ ] 数据库连接正常吗？
- [ ] 端口被占用了吗？
- [ ] 权限设置正确吗？
- [ ] 日志里有什么线索？
- [ ] 能在新环境重现吗？
- [ ] 其他人遇到过吗？

---

## 环境架构

### 环境定义

| 环境 | 用途 | 数据库 | 代码分支 |
|------|------|--------|----------|
| **开发环境 (dev)** | 本地开发、功能测试 | 本地 MySQL 或开发服务器 | `develop` |
| **生产环境 (prod)** | 线上服务 | 生产服务器 MySQL | `main` |
| **测试环境 (staging)** | 预发布测试（可选） | 测试服务器 MySQL | `staging` |

---

## 方案 A: 本地 MySQL（推荐）

### 优点
- 完全隔离，开发不影响生产
- 无需网络连接即可开发
- 数据库操作更快
- 可以随意测试和重置

### 配置步骤

#### 1. 安装本地 MySQL

**macOS**:
```bash
# 使用 Homebrew 安装
brew install mysql

# 启动 MySQL 服务
brew services start mysql

# 设置 root 密码
mysql_secure_installation
```

**Linux**:
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install mysql-server

# CentOS/RHEL
sudo yum install mysql-server

# 启动服务
sudo systemctl start mysqld
sudo systemctl enable mysqld
```

#### 2. 创建开发数据库

```bash
# 连接到本地 MySQL
mysql -u root -p

# 创建数据库和用户
CREATE DATABASE voicebox_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'voicebox_dev'@'localhost' IDENTIFIED BY 'dev123';
GRANT ALL PRIVILEGES ON voicebox_dev.* TO 'voicebox_dev'@'localhost';
FLUSH PRIVILEGES;
EXIT;

# 初始化数据库结构
mysql -u voicebox_dev -pdev123 voicebox_dev < deploy/db/init-database.sh
```

#### 3. 配置开发环境

**创建 `config-dev.properties`**:
```properties
# 开发环境配置
db.host=localhost
db.port=3306
db.name=voicebox_dev
db.username=voicebox_dev
db.password=dev123
db.url=jdbc:mysql://${db.host}:${db.port}/${db.name}?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai

# 其他开发配置
server.port=8080
debug.enabled=true
log.level=DEBUG
```

**创建 `config-prod.properties`**:
```properties
# 生产环境配置
db.host=localhost
db.port=3306
db.name=voicebox_db
db.username=voicebox
db.password=voicebox123
db.url=jdbc:mysql://${db.host}:${db.port}/${db.name}?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai

# 其他生产配置
server.port=8080
debug.enabled=false
log.level=INFO
```

#### 4. 修改启动脚本

**`start-all.sh`** - 支持环境参数:
```bash
#!/bin/bash

# 默认使用开发环境
ENV=${1:-dev}

echo "启动环境: $ENV"

# 根据环境选择配置文件
if [ "$ENV" = "prod" ]; then
    CONFIG_FILE="config-prod.properties"
else
    CONFIG_FILE="config-dev.properties"
fi

# 检查配置文件
if [ ! -f "$CONFIG_FILE" ]; then
    echo "错误: 配置文件 $CONFIG_FILE 不存在"
    exit 1
fi

echo "使用配置: $CONFIG_FILE"

# 启动后端服务
cd app-device
mvn spring-boot:run -Dspring.config.location=../$CONFIG_FILE &
echo $! > ../backend.pid

# 启动前端服务
cd ../app-web
if [ "$ENV" = "prod" ]; then
    npm run build
    # 生产环境使用 nginx 或其他服务器
else
    npm run dev &
    echo $! > ../frontend.pid
fi

cd ..
echo "服务启动完成"
```

**使用方式**:
```bash
# 开发环境
./start-all.sh dev

# 生产环境
./start-all.sh prod
```

---

## 方案 B: 开发服务器数据库

### 适用场景
- 团队协作，需要共享开发数据
- 本地无法安装 MySQL
- 需要模拟生产环境

### 配置步骤

#### 1. 在生产服务器创建开发数据库

```bash
# 连接到生产服务器
ssh root@129.211.180.183

# 创建开发数据库
mysql -u root -proot123

CREATE DATABASE voicebox_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'voicebox_dev'@'%' IDENTIFIED BY 'dev123';
GRANT ALL PRIVILEGES ON voicebox_dev.* TO 'voicebox_dev'@'%';
FLUSH PRIVILEGES;
EXIT;

# 初始化开发数据库
mysql -u voicebox_dev -pdev123 voicebox_dev < /opt/voicebox/deploy/db/init-database.sh
```

#### 2. 配置文件

**`config-dev.properties`**:
```properties
# 开发环境 - 连接到服务器的开发数据库
db.host=129.211.180.183
db.port=3306
db.name=voicebox_dev
db.username=voicebox_dev
db.password=dev123
```

**`config-prod.properties`**:
```properties
# 生产环境 - 连接到生产数据库
db.host=localhost
db.port=3306
db.name=voicebox_db
db.username=voicebox
db.password=voicebox123
```

---

## Git 分支管理

### 分支策略

```
main (生产)
  ↑
  merge
  ↑
develop (开发)
  ↑
  merge
  ↑
feature/* (功能分支)
```

### 工作流程

#### 1. 日常开发

```bash
# 从 develop 创建功能分支
git checkout develop
git pull origin develop
git checkout -b feature/user-profile

# 开发和测试
# 使用开发环境: ./start-all.sh dev

# 提交代码
git add .
git commit -m "feat: 添加用户画像功能"
git push origin feature/user-profile

# 合并到 develop
git checkout develop
git merge feature/user-profile
git push origin develop
```

#### 2. 发布到生产

```bash
# 从 develop 合并到 main
git checkout main
git pull origin main
git merge develop

# 打标签
git tag -a v1.2.0 -m "发布用户画像功能"
git push origin main --tags

# 部署到生产服务器
./deploy/deploy-to-server.sh
```

#### 3. 紧急修复

```bash
# 从 main 创建 hotfix 分支
git checkout main
git checkout -b hotfix/critical-bug

# 修复问题
# 测试

# 合并回 main 和 develop
git checkout main
git merge hotfix/critical-bug
git push origin main

git checkout develop
git merge hotfix/critical-bug
git push origin develop

# 部署
./deploy/deploy-to-server.sh
```

---

## 部署流程

### 开发环境部署

```bash
# 本地开发环境
./start-all.sh dev

# 或指定配置文件
./start-all.sh dev config-dev.properties
```

### 生产环境部署

**方式 1: 自动化部署脚本**

创建 `deploy/deploy-prod.sh`:
```bash
#!/bin/bash

set -e

SERVER="root@129.211.180.183"
REMOTE_DIR="/opt/voicebox"
BRANCH="main"

echo "=========================================="
echo "部署到生产环境"
echo "=========================================="

# 1. 检查当前分支
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "$BRANCH" ]; then
    echo "错误: 必须在 $BRANCH 分支上部署"
    echo "当前分支: $CURRENT_BRANCH"
    exit 1
fi

# 2. 检查是否有未提交的变更
if [ -n "$(git status --porcelain)" ]; then
    echo "错误: 有未提交的变更"
    git status
    exit 1
fi

# 3. 确认部署
read -p "确认部署到生产环境? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "取消部署"
    exit 0
fi

# 4. 打包代码
echo "打包代码..."
tar -czf voicebox-prod.tar.gz \
    --exclude=node_modules \
    --exclude=target \
    --exclude=.git \
    --exclude=*.log \
    --exclude=*.pid \
    --exclude=config-dev.properties \
    .

# 5. 上传到服务器
echo "上传到服务器..."
scp voicebox-prod.tar.gz $SERVER:/tmp/

# 6. 在服务器上部署
echo "在服务器上部署..."
ssh $SERVER << 'ENDSSH'
set -e

cd /opt/voicebox

# 停止服务
./stop-all.sh

# 备份当前版本
BACKUP_DIR="/opt/voicebox-backup/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR
cp -r /opt/voicebox/* $BACKUP_DIR/

# 解压新版本
tar -xzf /tmp/voicebox-prod.tar.gz

# 确保使用生产配置
if [ ! -f config-prod.properties ]; then
    echo "错误: config-prod.properties 不存在"
    exit 1
fi
cp config-prod.properties config.properties

# 编译
mvn clean install -DskipTests

# 构建前端
cd app-web
npm install
npm run build
cd ..

# 启动服务
./start-all.sh prod

# 等待服务启动
sleep 10

# 检查服务状态
./status.sh

echo "部署完成！"
echo "备份位置: $BACKUP_DIR"
ENDSSH

# 7. 清理
rm voicebox-prod.tar.gz

echo "=========================================="
echo "生产环境部署完成"
echo "=========================================="
```

**使用方式**:
```bash
# 部署到生产
./deploy/deploy-prod.sh
```

---

## 数据库迁移管理

### 迁移脚本规范

**文件命名**: `deploy/db/migrations/YYYYMMDD_HHMMSS_description.sql`

**示例**:
```sql
-- deploy/db/migrations/20241129_100000_add_user_preferences.sql

-- 添加用户偏好表
CREATE TABLE IF NOT EXISTS user_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    preference_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_user_preference (user_id, preference_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 应用迁移

**开发环境**:
```bash
# 应用到本地数据库
mysql -u voicebox_dev -pdev123 voicebox_dev < deploy/db/migrations/20241129_100000_add_user_preferences.sql

# 验证
mysql -u voicebox_dev -pdev123 voicebox_dev -e "SHOW TABLES;"
```

**生产环境**:
```bash
# 上传迁移脚本
scp deploy/db/migrations/20241129_100000_add_user_preferences.sql root@129.211.180.183:/tmp/

# 在服务器上应用
ssh root@129.211.180.183 "mysql -u voicebox -pvoicebox123 voicebox_db < /tmp/20241129_100000_add_user_preferences.sql"

# 验证
ssh root@129.211.180.183 "mysql -u voicebox -pvoicebox123 voicebox_db -e 'SHOW TABLES;'"
```

---

## 配置管理

### 配置文件结构

```
项目根目录/
├── config-dev.properties          # 开发环境配置
├── config-prod.properties         # 生产环境配置
├── config-staging.properties      # 测试环境配置（可选）
├── env-example.properties         # 配置模板
└── .gitignore                     # 忽略敏感配置
```

### .gitignore 配置

```gitignore
# 环境配置文件（包含敏感信息）
config-dev.properties
config-prod.properties
config-staging.properties
config.properties

# 但保留模板
!env-example.properties
```

### 配置模板

**`env-example.properties`**:
```properties
# 数据库配置
db.host=YOUR_DB_HOST
db.port=3306
db.name=YOUR_DB_NAME
db.username=YOUR_DB_USER
db.password=YOUR_DB_PASSWORD

# 服务器配置
server.port=8080

# 日志配置
log.level=INFO

# 其他配置
debug.enabled=false
```

### 首次设置

```bash
# 开发环境
cp env-example.properties config-dev.properties
# 编辑 config-dev.properties，填入开发环境的值

# 生产环境（在服务器上）
cp env-example.properties config-prod.properties
# 编辑 config-prod.properties，填入生产环境的值
```

---

## 环境切换

### 本地开发

```bash
# 启动开发环境
./start-all.sh dev

# 或显式指定配置
./start-all.sh dev config-dev.properties
```

### 生产部署

```bash
# 部署到生产
./deploy/deploy-prod.sh

# 或手动部署
ssh root@129.211.180.183
cd /opt/voicebox
./start-all.sh prod config-prod.properties
```

---

## 数据同步（可选）

### 从生产同步数据到开发

**仅同步结构**:
```bash
# 导出生产数据库结构
ssh root@129.211.180.183 "mysqldump -u voicebox -pvoicebox123 --no-data voicebox_db > /tmp/prod-schema.sql"

# 下载到本地
scp root@129.211.180.183:/tmp/prod-schema.sql ./

# 应用到开发数据库
mysql -u voicebox_dev -pdev123 voicebox_dev < prod-schema.sql
```

**同步测试数据**:
```bash
# 导出生产数据（脱敏后）
ssh root@129.211.180.183 "mysqldump -u voicebox -pvoicebox123 voicebox_db > /tmp/prod-data.sql"

# 下载并导入
scp root@129.211.180.183:/tmp/prod-data.sql ./
mysql -u voicebox_dev -pdev123 voicebox_dev < prod-data.sql
```

---

## 监控和日志

### 开发环境

```bash
# 查看日志
tail -f logs/dev.log

# 查看服务状态
./status.sh
```

### 生产环境

```bash
# 远程查看日志
ssh root@129.211.180.183 "tail -f /opt/voicebox/logs/prod.log"

# 查看服务状态
ssh root@129.211.180.183 "cd /opt/voicebox && ./status.sh"
```

---

## 最佳实践

### 1. 开发流程

1. 在 `develop` 分支开发
2. 使用开发环境测试（`./start-all.sh dev`）
3. 提交代码到功能分支
4. 合并到 `develop` 分支
5. 在测试环境验证（可选）
6. 合并到 `main` 分支
7. 部署到生产环境

### 2. 数据库变更

1. 创建迁移脚本
2. 在开发环境测试
3. 提交到版本控制
4. 在测试环境验证（可选）
5. 在生产环境应用

### 3. 配置管理

1. 敏感配置不提交到 Git
2. 使用配置模板
3. 环境变量优先
4. 文档记录配置项

### 4. 部署流程

1. 使用自动化脚本
2. 部署前备份
3. 部署后验证
4. 记录部署日志

---

## 故障恢复

### 回滚到上一版本

```bash
# 在服务器上
ssh root@129.211.180.183

# 停止服务
cd /opt/voicebox
./stop-all.sh

# 恢复备份
BACKUP_DIR="/opt/voicebox-backup/20241129_100000"  # 最新备份
rm -rf /opt/voicebox/*
cp -r $BACKUP_DIR/* /opt/voicebox/

# 启动服务
./start-all.sh prod

# 验证
./status.sh
```

---

## 快速参考

### 常用命令

```bash
# 开发环境
./start-all.sh dev                    # 启动开发环境
./stop-all.sh                         # 停止服务
./status.sh                           # 查看状态

# 生产部署
./deploy/deploy-prod.sh               # 部署到生产

# 数据库迁移
mysql -u voicebox_dev -pdev123 voicebox_dev < migration.sql  # 开发
ssh root@129.211.180.183 "mysql -u voicebox -pvoicebox123 voicebox_db < /tmp/migration.sql"  # 生产

# Git 操作
git checkout develop                  # 切换到开发分支
git checkout main                     # 切换到生产分支
git merge develop                     # 合并开发到生产
```

---

## 检查清单

### 开发前

- [ ] 确认在 `develop` 分支
- [ ] 拉取最新代码
- [ ] 启动开发环境
- [ ] 数据库连接正常

### 部署前

- [ ] 所有测试通过
- [ ] 代码已合并到 `main`
- [ ] 配置文件正确
- [ ] 备份已创建

### 部署后

- [ ] 服务启动成功
- [ ] 功能测试通过
- [ ] 日志无错误
- [ ] 性能正常

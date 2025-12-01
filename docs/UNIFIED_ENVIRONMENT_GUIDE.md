# 统一环境配置指南

## 📋 环境要求

为了避免本地和线上环境差异导致的编译和运行问题，我们统一使用以下环境：

| 组件 | 版本 | 说明 |
|------|------|------|
| **Java** | 1.8 (JDK 1.8.0) | 统一使用Java 8 |
| **Maven** | 3.6.3 | 统一使用Maven 3.6.3 |
| **编码** | UTF-8 | 统一使用UTF-8编码 |

---

## 🔍 环境检查

### 本地环境检查

```bash
# 检查Java版本
java -version
# 应该显示: java version "1.8.0_xxx"

# 检查Maven版本
mvn -version
# 应该显示: Apache Maven 3.6.3
```

### 服务器环境检查

```bash
# SSH连接到服务器
ssh root@129.211.180.183

# 检查Java版本
java -version

# 检查Maven版本
mvn -version
```

---

## 🚀 快速部署流程

### 方式1：自动化部署（推荐）

使用统一环境部署脚本，自动配置服务器环境并部署：

```bash
# 1. 赋予执行权限
chmod +x scripts/deploy-with-unified-env.sh

# 2. 执行部署
./scripts/deploy-with-unified-env.sh
```

**脚本会自动完成**：
1. ✅ 检查本地环境（Java 1.8 + Maven 3.6.3）
2. ✅ 上传环境配置脚本到服务器
3. ✅ 在服务器上配置统一环境
4. ✅ 在本地编译项目
5. ✅ 部署到服务器并启动服务

### 方式2：手动配置

#### 步骤1：配置服务器环境

```bash
# 1. 上传环境配置脚本
scp scripts/server/setup-unified-environment.sh root@129.211.180.183:/tmp/

# 2. SSH连接到服务器
ssh root@129.211.180.183

# 3. 执行环境配置脚本
chmod +x /tmp/setup-unified-environment.sh
/tmp/setup-unified-environment.sh

# 4. 重新加载环境变量
source /etc/profile

# 5. 验证环境
java -version
mvn -version
```

#### 步骤2：编译和部署

```bash
# 1. 在本地编译
mvn clean package -DskipTests

# 2. 停止服务器服务
ssh root@129.211.180.183 "cd /opt/voicebox && ./stop-all.sh"

# 3. 上传jar包
scp app-device/target/app-device-0.0.1-SNAPSHOT.jar root@129.211.180.183:/opt/voicebox/app-device/target/

# 4. 启动服务
ssh root@129.211.180.183 "cd /opt/voicebox && ./start-all.sh"
```

---

## 🔧 环境配置脚本说明

### setup-unified-environment.sh

**功能**：
- 清理旧的Java和Maven环境
- 安装Java 1.8 (OpenJDK)
- 安装Maven 3.6.3
- 配置环境变量
- 验证安装

**使用方法**：
```bash
# 必须使用root权限执行
sudo /tmp/setup-unified-environment.sh
```

**脚本会做什么**：
1. 备份现有配置到 `/root/environment-backup-YYYYMMDD_HHMMSS/`
2. 清理旧的Maven安装（/opt/maven, /usr/local/maven）
3. 安装OpenJDK 1.8
4. 下载并安装Maven 3.6.3到 `/opt/maven`
5. 配置环境变量到 `/etc/profile`
6. 验证安装结果

---

## 📝 Maven配置说明

### 根pom.xml

```xml
<properties>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

### app-device/pom.xml

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.1</version>
            <configuration>
                <source>1.8</source>
                <target>1.8</target>
                <encoding>UTF-8</encoding>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**关键配置**：
- `source` 和 `target` 都设置为 `1.8`
- `encoding` 设置为 `UTF-8`
- Maven编译器插件版本：`3.8.1`（兼容Maven 3.6.3）

---

## ⚠️ 常见问题

### 问题1：编译时出现"找不到符号"错误

**原因**：Lombok注解处理器在某些环境下不工作

**解决方案**：
- 已在代码中手动添加getter/setter方法
- 不依赖Lombok注解处理器

### 问题2：服务器Maven版本不对

**检查**：
```bash
ssh root@129.211.180.183 "mvn -version"
```

**解决**：
```bash
# 重新运行环境配置脚本
ssh root@129.211.180.183
/tmp/setup-unified-environment.sh
source /etc/profile
```

### 问题3：环境变量不生效

**解决**：
```bash
# 重新加载环境变量
source /etc/profile

# 或者退出并重新登录
exit
ssh root@129.211.180.183
```

### 问题4：Maven下载失败

**原因**：网络问题或下载源不可用

**解决**：
```bash
# 手动下载Maven
cd /tmp
wget https://archive.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz

# 或使用国内镜像
wget https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
```

---

## 📊 环境验证清单

部署前检查：

- [ ] 本地Java版本是1.8
- [ ] 本地Maven版本是3.6.3
- [ ] 本地编译成功（`mvn clean package -DskipTests`）
- [ ] 服务器Java版本是1.8
- [ ] 服务器Maven版本是3.6.3
- [ ] 服务器环境变量已生效（`source /etc/profile`）

部署后检查：

- [ ] 服务器服务启动成功
- [ ] 后端API可访问（http://129.211.180.183:8080）
- [ ] 前端页面可访问（http://129.211.180.183）
- [ ] 日志无错误
- [ ] 数据库连接正常

---

## 🔄 回滚方案

如果部署出现问题，可以快速回滚：

```bash
# 1. SSH连接到服务器
ssh root@129.211.180.183

# 2. 停止服务
cd /opt/voicebox
./stop-all.sh

# 3. 恢复备份
BACKUP_DIR="/opt/voicebox-backup/YYYYMMDD_HHMMSS"  # 替换为实际备份目录
rm -rf /opt/voicebox/*
cp -r $BACKUP_DIR/* /opt/voicebox/

# 4. 启动服务
./start-all.sh

# 5. 验证
./status.sh
```

---

## 📚 相关文档

- [快速开始指南](../QUICK_START.md)
- [部署指南](../deploy/README.md)
- [环境隔离规范](../.kiro/steering/environment-isolation.md)
- [数据库配置](../.kiro/steering/database-configuration.md)

---

## 💡 最佳实践

1. **始终在本地测试**
   - 在本地完整编译和测试
   - 确保所有测试通过
   - 验证功能正常

2. **使用统一环境**
   - 本地和服务器使用相同的Java和Maven版本
   - 避免环境差异导致的问题

3. **定期备份**
   - 每次部署前自动备份
   - 保留最近3-5个版本的备份

4. **验证部署**
   - 部署后立即验证服务状态
   - 检查日志确认无错误
   - 测试关键功能

5. **记录变更**
   - 在 `docs/SYNC_LOG.md` 中记录每次部署
   - 记录环境变更和配置修改

---

## 🆘 获取帮助

如果遇到问题：

1. 查看日志：
   ```bash
   ssh root@129.211.180.183 "tail -100 /opt/voicebox/logs/app.log"
   ```

2. 检查服务状态：
   ```bash
   ssh root@129.211.180.183 "cd /opt/voicebox && ./status.sh"
   ```

3. 查看环境配置：
   ```bash
   ssh root@129.211.180.183 "cat /etc/profile | grep -E 'JAVA|MAVEN'"
   ```

4. 联系团队成员或查看相关文档

---

**最后更新**：2024-11-30  
**维护者**：VoiceBox Team

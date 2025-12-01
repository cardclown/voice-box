# 数据库优先级规则

## 🔴 强制规则：优先使用云服务器数据库

**所有数据库相关操作必须优先使用云服务器的 MySQL 数据库**

---

## 核心规则

### 规则 1: 连接地址强制要求

**本地开发环境**：
- ✅ **必须使用**: `129.211.180.183:3306`
- ❌ **禁止使用**: `localhost`, `127.0.0.1`, 本地 MySQL

**服务器环境**：
- ✅ **可以使用**: `localhost`（仅在服务器上运行时）

### 规则 2: 配置文件检查

所有配置文件中的数据库地址必须是：

```properties
# ✅ 正确配置（本地开发）
spring.datasource.url=jdbc:mysql://129.211.180.183:3306/voicebox_db?...
db.host=129.211.180.183

# ❌ 错误配置（本地开发）
spring.datasource.url=jdbc:mysql://localhost:3306/voicebox_db?...
db.host=localhost
```

### 规则 3: 数据库操作检查

任何数据库操作前，必须确认：

```bash
# 1. 检查连接地址
echo "当前数据库地址: 129.211.180.183"

# 2. 测试连接
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 -e "SELECT 1;"

# 3. 确认连接成功后再执行操作
```

---

## 适用场景

### ✅ 必须使用云服务器数据库的场景

1. **本地开发**
   - 启动 Spring Boot 应用
   - 运行测试
   - 调试代码
   - 查询数据

2. **数据库操作**
   - 创建表
   - 修改表结构
   - 插入数据
   - 查询数据
   - 删除数据

3. **数据库迁移**
   - 应用迁移脚本
   - 初始化数据库
   - 更新数据库结构

4. **测试**
   - 单元测试
   - 集成测试
   - 端到端测试

### ⚠️ 例外情况（仅在服务器上）

仅当代码在云服务器 `129.211.180.183` 上运行时，可以使用 `localhost`：

```bash
# 在服务器上执行
ssh root@129.211.180.183
cd /opt/voicebox

# 此时可以使用 localhost
mysql -h localhost -u voicebox -pvoicebox123 voicebox_db
```

---

## 配置检查清单

### 启动应用前检查

- [ ] `application.properties` 中的数据库地址是 `129.211.180.183`
- [ ] `config.properties` 中的 `db.host` 是 `129.211.180.183`
- [ ] 环境变量 `DB_HOST` 是 `129.211.180.183`（如果使用）
- [ ] 测试配置中的数据库地址是 `129.211.180.183`

### 数据库操作前检查

- [ ] 确认要连接的是云服务器数据库
- [ ] 测试连接是否正常
- [ ] 确认有足够的权限
- [ ] 备份重要数据（如果是修改操作）

---

## 常见错误和修复

### 错误 1: 使用了 localhost

```properties
# ❌ 错误
spring.datasource.url=jdbc:mysql://localhost:3306/voicebox_db?...

# ✅ 修复
spring.datasource.url=jdbc:mysql://129.211.180.183:3306/voicebox_db?...
```

### 错误 2: 连接本地 MySQL

```bash
# ❌ 错误
mysql -u root -p voicebox_db

# ✅ 修复
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db
```

### 错误 3: 测试使用了本地数据库

```java
// ❌ 错误
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/test_db"
})

// ✅ 修复
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://129.211.180.183:3306/voicebox_db"
})
```

---

## 验证方法

### 1. 检查配置文件

```bash
# 检查所有配置文件中的数据库地址
grep -r "localhost:3306" app-device/src/main/resources/
grep -r "127.0.0.1:3306" app-device/src/main/resources/
grep -r "db.host=localhost" .

# 应该没有结果，或者只在服务器部署脚本中出现
```

### 2. 测试连接

```bash
# 测试云服务器数据库连接
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 -e "SELECT DATABASE();"

# 应该返回: voicebox_db
```

### 3. 启动应用检查

```bash
# 启动应用
cd app-device
mvn spring-boot:run

# 查看日志，确认连接的是云服务器
# 应该看到类似：
# HikariPool-1 - Starting...
# HikariPool-1 - Added connection com.mysql.cj.jdbc.ConnectionImpl@... (129.211.180.183:3306)
```

---

## 为什么要这样做

### 优点

1. **数据一致性** - 所有开发者使用同一数据源
2. **简化配置** - 无需在本地安装 MySQL
3. **避免环境差异** - 开发环境和生产环境使用相同的数据库
4. **团队协作** - 数据实时同步，便于协作
5. **问题排查** - 问题更容易复现和排查

### 注意事项

1. **网络依赖** - 需要稳定的网络连接
2. **数据安全** - 谨慎操作，避免误删数据
3. **性能考虑** - 远程连接可能比本地稍慢
4. **并发操作** - 多人同时操作时注意数据冲突

---

## 快速参考

```bash
# ✅ 正确的数据库连接
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 voicebox_db

# ✅ 正确的配置
spring.datasource.url=jdbc:mysql://129.211.180.183:3306/voicebox_db?...

# ✅ 正确的测试连接
mysql -h 129.211.180.183 -u voicebox -pvoicebox123 -e "SELECT 1;"

# ❌ 错误的连接（本地开发时）
mysql -h localhost -u root -p
mysql -u root -p voicebox_db
spring.datasource.url=jdbc:mysql://localhost:3306/...
```

---

## 记住

**所有数据库操作，优先使用云服务器数据库 `129.211.180.183`**

如果你发现自己在使用 `localhost` 或本地 MySQL，立即停止并切换到云服务器数据库。

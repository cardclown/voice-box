# 数据库迁移脚本

本目录包含数据库结构变更的迁移脚本。

## 命名规范

迁移脚本使用以下命名格式：

```
YYYYMMDD_HHMMSS_description.sql
```

例如：
- `20241129_100000_add_user_preferences.sql`
- `20241130_143000_add_sentiment_analysis.sql`

## 创建迁移脚本

### 1. 复制模板

```bash
cp template.sql YYYYMMDD_HHMMSS_your_description.sql
```

### 2. 编辑脚本

在新文件中添加你的 SQL 变更：

```sql
-- 迁移说明
-- 日期: 2024-11-29
-- 作者: Your Name
-- 描述: 添加用户偏好字段

USE voicebox_db;

-- 添加新字段
ALTER TABLE users ADD COLUMN theme_preference VARCHAR(20) DEFAULT 'light';

-- 创建索引
CREATE INDEX idx_theme ON users(theme_preference);
```

### 3. 执行迁移

```bash
# 本地执行
mysql -u voicebox -pvoicebox123 voicebox_db < YYYYMMDD_HHMMSS_your_description.sql

# 服务器执行
scp YYYYMMDD_HHMMSS_your_description.sql root@server:/tmp/
ssh root@server "mysql -u voicebox -pvoicebox123 voicebox_db < /tmp/YYYYMMDD_HHMMSS_your_description.sql"
```

### 4. 记录迁移

在本文件末尾的迁移历史表中添加记录。

## 迁移脚本规范

### 必须包含的内容

1. **注释说明**
   - 迁移日期
   - 作者
   - 变更描述
   - 影响范围

2. **USE 语句**
   ```sql
   USE voicebox_db;
   ```

3. **安全检查**
   ```sql
   -- 检查表是否存在
   -- 检查字段是否已存在
   -- 使用 IF NOT EXISTS
   ```

4. **回滚说明**
   ```sql
   -- 回滚方法:
   -- ALTER TABLE users DROP COLUMN theme_preference;
   ```

### 最佳实践

1. **向后兼容**
   - 添加字段时使用 DEFAULT 值
   - 不要删除正在使用的字段
   - 使用 ALTER TABLE 而不是 DROP + CREATE

2. **分步执行**
   - 大型变更分多个小步骤
   - 每个步骤可以独立回滚
   - 避免长时间锁表

3. **测试验证**
   - 在测试环境先执行
   - 验证数据完整性
   - 检查应用兼容性

4. **备份数据**
   ```bash
   mysqldump -u voicebox -pvoicebox123 voicebox_db > backup_before_migration.sql
   ```

## 迁移模板

```sql
-- ============================================
-- 迁移脚本模板
-- ============================================

-- 迁移信息
-- 日期: YYYY-MM-DD
-- 作者: Your Name
-- 描述: 简要描述本次变更
-- 影响: 列出受影响的表和功能

USE voicebox_db;

-- 开始事务（如果支持）
START TRANSACTION;

-- ============================================
-- 变更内容
-- ============================================

-- 示例：添加新字段
ALTER TABLE table_name 
ADD COLUMN new_column VARCHAR(100) DEFAULT 'default_value' COMMENT '字段说明';

-- 示例：创建索引
CREATE INDEX idx_new_column ON table_name(new_column);

-- 示例：修改字段
ALTER TABLE table_name 
MODIFY COLUMN existing_column VARCHAR(200);

-- 示例：删除字段（谨慎使用）
-- ALTER TABLE table_name DROP COLUMN old_column;

-- ============================================
-- 数据迁移（如果需要）
-- ============================================

-- 示例：更新现有数据
-- UPDATE table_name SET new_column = 'value' WHERE condition;

-- ============================================
-- 验证
-- ============================================

-- 检查变更是否成功
SELECT COUNT(*) FROM table_name;
DESCRIBE table_name;

-- 提交事务
COMMIT;

-- ============================================
-- 回滚方法
-- ============================================

-- 如果需要回滚，执行以下语句：
-- ALTER TABLE table_name DROP COLUMN new_column;
-- DROP INDEX idx_new_column ON table_name;
```

## 常见迁移场景

### 1. 添加新表

```sql
CREATE TABLE IF NOT EXISTS new_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2. 添加字段

```sql
ALTER TABLE users 
ADD COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT '手机号';
```

### 3. 修改字段类型

```sql
ALTER TABLE users 
MODIFY COLUMN email VARCHAR(320);  -- 扩大字段长度
```

### 4. 添加索引

```sql
CREATE INDEX idx_email ON users(email);
```

### 5. 添加外键

```sql
ALTER TABLE child_table 
ADD CONSTRAINT fk_parent 
FOREIGN KEY (parent_id) REFERENCES parent_table(id) 
ON DELETE CASCADE;
```

### 6. 数据迁移

```sql
-- 迁移旧数据到新结构
INSERT INTO new_table (id, name, created_at)
SELECT id, old_name, created_at FROM old_table;
```

## 迁移历史

| 日期 | 文件名 | 描述 | 作者 | 状态 |
|------|--------|------|------|------|
| 2024-11-29 | 初始化 | 创建迁移脚本体系 | AI Assistant | ✅ 完成 |

## 注意事项

1. **生产环境迁移前必须备份**
2. **在低峰期执行大型迁移**
3. **监控迁移过程，准备回滚方案**
4. **迁移后验证应用功能**
5. **更新相关文档和代码**

## 相关文档

- [数据库架构文档](../../../app-device/DATABASE_SCHEMA.md)
- [部署指南](../../README.md)

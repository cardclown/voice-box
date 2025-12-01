---
inclusion: always
---

# 项目组织规范

## 文档组织规则

### 文档目录结构
```
docs/
├── README.md                    # 文档索引和导航
├── FEATURE_ROADMAP.md          # 功能路线图
├── versions/                    # 版本化功能文档
│   ├── v1.0-feature-name/
│   │   ├── requirements.md     # 需求文档
│   │   ├── design.md          # 设计文档
│   │   ├── implementation-plan.md
│   │   └── README.md
│   └── v2.0-feature-name/
└── archive/                     # 已归档的旧文档
    └── README.md               # 归档说明
```

### 文档命名规范

#### 版本化功能文档
- **位置**: `docs/versions/vX.Y-feature-name/`
- **命名**: 使用小写字母和连字符，如 `v2.0-personality-analysis`
- **必需文件**:
  - `requirements.md` - 需求文档
  - `design.md` - 设计文档
  - `implementation-plan.md` - 实施计划
  - `README.md` - 功能概述和索引

#### 临时文档和总结
- **禁止**: 在 `docs/` 根目录创建 `V2.0_XXX.md` 这样的临时文档
- **替代方案**: 
  - 放入对应的 `docs/versions/vX.Y-feature-name/` 目录
  - 或放入 `docs/archive/` 如果已过时

#### 模块级文档
- **位置**: 在各模块目录内，如 `app-web/PROJECT_STRUCTURE.md`
- **命名**: 使用大写字母和下划线，如 `README_API.md`, `DATABASE_SCHEMA.md`
- **范围**: 仅限该模块相关的文档

### 文档创建规则

1. **新功能文档**
   - 必须创建在 `docs/versions/vX.Y-feature-name/` 目录
   - 遵循标准的文档结构（requirements, design, implementation-plan）
   - 在 `docs/README.md` 中添加索引链接

2. **临时工作文档**
   - 如果是短期使用，创建在对应版本目录的 `notes/` 子目录
   - 完成后移动到 `docs/archive/` 或删除

3. **更新现有文档**
   - 优先更新现有文档，而不是创建新文档
   - 如果文档已过时，移动到 `docs/archive/`

## 脚本和工具文件组织

### 脚本目录结构
```
scripts/              # 开发和维护脚本
├── dev/             # 开发环境脚本
├── deploy/          # 部署相关脚本（已有 deploy/ 目录）
├── test/            # 测试脚本
└── utils/           # 工具脚本

根目录/              # 仅保留常用的启动脚本
├── start-all.sh
├── stop-all.sh
├── restart-all.sh
└── status.sh
```

### 脚本命名和位置规则

1. **根目录脚本** - 仅保留最常用的操作脚本
   - `start-all.sh` - 启动所有服务
   - `stop-all.sh` - 停止所有服务
   - `restart-all.sh` - 重启所有服务
   - `status.sh` - 查看服务状态

2. **开发脚本** - 放入 `scripts/dev/`
   - `dev-watch.sh` → `scripts/dev/watch.sh`
   - `setup-env.sh` → `scripts/dev/setup-env.sh`

3. **部署脚本** - 使用现有的 `deploy/` 目录
   - `deploy-to-server.sh` → `deploy/deploy-to-server.sh`
   - `scp-upload.exp` → `deploy/scp-upload.exp`
   - `ssh-connect.exp` → `deploy/ssh-connect.exp`

4. **配置文件** - 保持在根目录
   - `config.properties`
   - `env-example.properties`
   - `docker-compose.yml`

### 临时文件处理

**禁止在根目录保留**:
- `*.log` 文件 → 应该在 `.gitignore` 中忽略
- `*.pid` 文件 → 应该在 `.gitignore` 中忽略
- 测试文件如 `test-stream.html` → 移到 `scripts/test/` 或删除
- 临时媒体文件如 `song_632886.mp3` → 移到适当位置或删除

## 创建新文件时的检查清单

在创建任何新文档或脚本前，必须：

1. ✅ 检查是否已有类似文档/脚本可以更新
2. ✅ 确认文件应该放在哪个目录
3. ✅ 使用规范的命名方式
4. ✅ 如果是版本化功能，创建完整的目录结构
5. ✅ 更新相关的索引文档（如 `docs/README.md`）

## 文档清理原则

定期检查并清理：
- 将过时的文档移到 `docs/archive/`
- 删除重复的文档
- 合并内容相似的文档
- 更新文档索引

## 示例：正确的文档创建流程

### 场景：添加新功能 "智能推荐系统"

```bash
# 1. 创建版本目录
docs/versions/v3.0-smart-recommendation/

# 2. 创建标准文档
docs/versions/v3.0-smart-recommendation/requirements.md
docs/versions/v3.0-smart-recommendation/design.md
docs/versions/v3.0-smart-recommendation/implementation-plan.md
docs/versions/v3.0-smart-recommendation/README.md

# 3. 更新索引
# 在 docs/README.md 中添加链接

# 4. 如需临时笔记
docs/versions/v3.0-smart-recommendation/notes/brainstorm.md
```

### 场景：添加部署脚本

```bash
# ❌ 错误：放在根目录
./deploy-new-feature.sh

# ✅ 正确：放在 deploy 目录
deploy/deploy-new-feature.sh
```

## 强制规则

1. **禁止在根目录创建散乱的文档** - 必须放入 `docs/` 的适当子目录
2. **禁止在根目录创建开发/测试脚本** - 必须放入 `scripts/` 或 `deploy/`
3. **禁止创建临时文档而不清理** - 完成后必须归档或删除
4. **必须遵循命名规范** - 版本目录用小写连字符，文档用大写下划线
5. **创建新文档前必须检查现有文档** - 避免重复和冗余

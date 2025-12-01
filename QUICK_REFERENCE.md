# 🚀 VoiceBox 快速参考

**最后更新**: 2024-01-15

---

## 📁 项目结构

```
voice-box/
├── docs/                          # 📚 文档中心
│   ├── README.md                  # 文档主页
│   ├── FEATURE_ROADMAP.md        # 功能路线图
│   ├── ISSUES_AND_FIXES.md       # 问题排查
│   ├── FIXES_COMPLETED.md        # 修复完成报告
│   ├── versions/                  # 版本文档
│   └── archive/                   # 归档文档
├── app-web/                       # 前端应用
├── app-device/                    # 后端应用
├── scripts/                       # 工具脚本
└── QUICK_REFERENCE.md            # 本文件
```

---

## 🔗 快速链接

### 📖 文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 文档中心 | [docs/README.md](docs/README.md) | 文档导航和索引 |
| 功能路线图 | [docs/FEATURE_ROADMAP.md](docs/FEATURE_ROADMAP.md) | 产品规划和版本计划 |
| 使用指南 | [docs/HOW_TO_USE_VERSION_DOCS.md](docs/HOW_TO_USE_VERSION_DOCS.md) | 文档系统使用说明 |
| 问题排查 | [docs/ISSUES_AND_FIXES.md](docs/ISSUES_AND_FIXES.md) | 已知问题和解决方案 |
| 修复报告 | [docs/FIXES_COMPLETED.md](docs/FIXES_COMPLETED.md) | 已完成的修复 |
| **版本修正** | [docs/VERSION_FIX_SUMMARY.md](docs/VERSION_FIX_SUMMARY.md) | **环境版本修正说明** |

### 🎯 版本文档

| 版本 | 路径 | 状态 |
|------|------|------|
| v1.0 用户身份识别 | [docs/versions/v1.0-user-identity/](docs/versions/v1.0-user-identity/) | 📝 规划中 |

### 📦 归档文档

所有历史文档已移至 [docs/archive/](docs/archive/)

---

## 🛠️ 常用命令

### 启动项目

```bash
# 启动所有服务
./start-all.sh

# 启动前端
cd app-web && npm run dev

# 启动后端
cd app-device && mvn spring-boot:run
```

### 文档管理

```bash
# 创建新版本文档
./scripts/create-version-docs.sh v1.1 personality-analysis

# 查看文档中心
cat docs/README.md

# 查看功能路线图
cat docs/FEATURE_ROADMAP.md
```

---

## 🎨 最近修复

### ✅ 已完成 (2024-01-15)

1. **文档结构整理**
   - 创建统一的文档目录
   - 归档历史文档
   - 建立清晰的层级结构

2. **UI问题修复**
   - 修复按钮重叠问题
   - 优化移动端触摸区域
   - 改进按钮事件处理

3. **功能优化**
   - 移除删除功能（避免误操作）
   - 优化编辑为"重新编辑"
   - 实现重新生成功能

4. **⚠️ 版本要求修正**
   - 修正为JDK 1.8 (之前错误写成JDK 11)
   - 修正为MySQL 5.7 (之前错误写成MySQL 8.0)
   - 修正为Maven 3.6.3 (之前错误写成Maven 3.8+)
   - 详见：[docs/VERSION_FIX_SUMMARY.md](docs/VERSION_FIX_SUMMARY.md)

---

## 📋 待办事项

### 高优先级 (P0)

- [ ] 测试所有修复的功能
- [ ] 验证移动端体验
- [ ] 完善错误处理

### 中优先级 (P1)

- [ ] 创建v1.1版本文档
- [ ] 添加消息搜索功能
- [ ] 优化长对话性能

### 低优先级 (P2)

- [ ] 添加消息评分功能
- [ ] 支持消息导出
- [ ] 实现对话分支

---

## 🐛 已知问题

目前无已知严重问题。

如发现问题，请在 [docs/ISSUES_AND_FIXES.md](docs/ISSUES_AND_FIXES.md) 中记录。

---

## 💡 快速提示

### 查看文档

```bash
# 查看所有文档
ls -la docs/

# 查看版本文档
ls -la docs/versions/

# 查看归档文档
ls -la docs/archive/
```

### 创建新功能

1. 使用脚本创建版本文档
2. 填写需求、设计、计划
3. 团队评审确认
4. 开始开发实施

### 报告问题

1. 在 `docs/ISSUES_AND_FIXES.md` 中记录
2. 描述问题现象和复现步骤
3. 提供截图或日志
4. 标注优先级

---

## 📞 获取帮助

- **文档问题**: 查看 [docs/README.md](docs/README.md)
- **功能规划**: 查看 [docs/FEATURE_ROADMAP.md](docs/FEATURE_ROADMAP.md)
- **技术问题**: 查看项目代码注释
- **其他问题**: 联系团队成员

---

**维护**: VoiceBox团队  
**版本**: 1.0  
**更新**: 2024-01-15

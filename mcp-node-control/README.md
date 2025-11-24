## Node 本地控制 MCP 服务

这是一个使用 Node.js 编写的 **Model Context Protocol (MCP) 服务器**，可以让支持 MCP 的客户端（例如 Claude Desktop 等）通过一组工具来“远程操作”你的这台 macOS 电脑。

### 功能概览

- **run_shell**：执行本机 shell 命令（谨慎使用）
- **read_file**：读取本地文件内容
- **write_file**：写入/创建本地文件
- **list_directory**：列出目录内容
- **get_system_info**：查看系统信息
- **open_on_macos**：在 macOS 上用 `open` 打开应用 / 文件 / URL

> ⚠️ 安全提醒：启用此 MCP 后，接入它的 AI 有能力在你的电脑上执行命令、读写文件。**务必只在你完全信任的环境和模型下启用，且不要暴露到公网。**

---

### 1. 安装依赖

在项目根目录（当前 voice-box）下执行：

```bash
cd /Users/jd/mydesk/cursorFiles/voice-box/mcp-node-control
npm install
```

这会安装：

- `@modelcontextprotocol/sdk`：MCP 官方 Node SDK
- `zod`：用于定义工具的参数 schema

---

### 2. 本地启动 MCP 服务器（测试用）

可以先在命令行简单启动测试：

```bash
cd /Users/jd/mydesk/cursorFiles/voice-box/mcp-node-control
node server.mjs
```

正常情况下，此进程会挂起等待 MCP 客户端通过 stdin/stdout 连接；直接在终端中不会看到太多输出，这是正常的。

---

### 3. 在 MCP 客户端中接入（以 Claude Desktop 为例）

以下是以 Claude Desktop 为例的配置方式，其他支持 MCP 的客户端配置方式类似，只是配置文件位置和字段名称可能不同。

1. 找到/创建 Claude Desktop 的配置文件（例如 `claude_desktop_config.json`）。
2. 在其中新增一个 MCP 服务器配置，例如：

```json
{
  "mcpServers": {
    "node-computer-control": {
      "command": "node",
      "args": [
        "/Users/jd/mydesk/cursorFiles/voice-box/mcp-node-control/server.mjs"
      ]
    }
  }
}
```

保存重启 Claude Desktop 之后，你应该能在工具列表里看到 `node-computer-control`（或者类似名称）的 MCP 服务器，并能调用其中的工具。

---

### 4. 各工具用法说明

- **run_shell**
  - **功能**：执行任意 shell 命令。
  - **参数**：
    - `command`：要执行的命令字符串（如 `ls -la /`）。
    - `cwd`（可选）：工作目录（不填则是进程当前目录）。
    - `timeoutMs`（可选）：超时毫秒数，超时会中止命令。

- **read_file**
  - **功能**：读取 UTF-8 文本文件。
  - **参数**：
    - `filePath`：文件路径（支持绝对/相对路径）。
    - `maxBytes`（可选）：最大读取字节数，超出会被截断。

- **write_file**
  - **功能**：将文本写入到文件（默认覆盖）。
  - **参数**：
    - `filePath`：目标文件路径。
    - `content`：要写入的文本。
    - `createDirectories`（可选）：是否自动创建上级目录。

- **list_directory**
  - **功能**：列出目录下的文件和子目录。
  - **参数**：
    - `dirPath`：目录路径。
    - `withFileTypes`（可选，默认 true）：是否返回类型信息（文件/目录/链接）。

- **get_system_info**
  - **功能**：查看当前系统的一些基本信息（平台、CPU、内存等）。
  - **参数**：无。

- **open_on_macos**
  - **功能**：在 macOS 上通过 `open` 打开目标（应用 / 文件 / URL）。
  - **参数**：
    - `target`：目标路径或 URL（如 `/Applications/Notes.app` 或 `https://www.google.com`）。
    - `args`（可选）：传给 `open` 的其它参数（如 `["-n"]`）。

---

### 5. 权限与安全建议

- **只在本机使用**，不要给此脚本做任何网络暴露或端口转发。
- MCP 客户端一旦连接成功，它就能：执行命令、读写文件、打开应用，所以请**只让你信任的模型/会话使用它**。
- 如需限制访问范围，可以在 `server.mjs` 里：
  - 对 `run_shell` 做白名单/黑名单过滤；
  - 对 `read_file` / `write_file` / `list_directory` 限制到某个根目录；
  - 对敏感目录（如 `~/.ssh`、密码管理器数据目录等）显式禁止访问。



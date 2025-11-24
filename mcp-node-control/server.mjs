import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import fs from "node:fs/promises";
import path from "node:path";
import os from "node:os";
import { exec as execCallback } from "node:child_process";
import { promisify } from "node:util";

const exec = promisify(execCallback);

const server = new McpServer({
  name: "node-computer-control",
  version: "0.1.0"
});

// 在本机执行 shell 命令
server.registerTool(
  "run_shell",
  {
    title: "运行 Shell 命令",
    description: "在本机上执行 shell 命令（请谨慎使用，仅在你信任的环境中启用）",
    inputSchema: z.object({
      command: z
        .string()
        .describe("要执行的完整 shell 命令，例如：ls -la / 或者 echo 'hello'"),
      cwd: z
        .string()
        .optional()
        .describe("可选：在此目录下执行命令，未提供时使用当前工作目录"),
      timeoutMs: z
        .number()
        .int()
        .positive()
        .optional()
        .describe("可选：超时时间（毫秒），例如 30000 表示 30 秒")
    })
  },
  async ({ command, cwd, timeoutMs }) => {
    const options = {
      cwd: cwd || process.cwd(),
      maxBuffer: 10 * 1024 * 1024
    };
    if (timeoutMs) {
      options.timeout = timeoutMs;
    }

    const { stdout, stderr } = await exec(command, options);

    return {
      content: [
        {
          type: "text",
          text:
            `命令：${command}\n` +
            `工作目录：${options.cwd}\n\n` +
            `======= STDOUT =======\n${stdout}\n\n` +
            `======= STDERR =======\n${stderr}`
        }
      ]
    };
  }
);

// 读取本地文件
server.registerTool(
  "read_file",
  {
    title: "读取文件",
    description: "读取本地文件内容（UTF-8 文本），可限制最大读取大小",
    inputSchema: z.object({
      filePath: z
        .string()
        .describe("要读取的文件路径，可以是绝对路径，也可以是相对路径"),
      maxBytes: z
        .number()
        .int()
        .positive()
        .optional()
        .describe("可选：最大读取字节数，超出时会被截断")
    })
  },
  async ({ filePath, maxBytes }) => {
    const fullPath = path.resolve(filePath);
    let data = await fs.readFile(fullPath, "utf8");

    if (maxBytes && Buffer.byteLength(data, "utf8") > maxBytes) {
      const buf = Buffer.from(data, "utf8").subarray(0, maxBytes);
      data =
        buf.toString("utf8") +
        `\n\n[内容已被截断，原始字节数=${Buffer.byteLength(
          data,
          "utf8"
        )}]`;
    }

    return {
      content: [
        {
          type: "text",
          text: `文件路径：${fullPath}\n\n${data}`
        }
      ]
    };
  }
);

// 写入本地文件（覆盖写入）
server.registerTool(
  "write_file",
  {
    title: "写入文件",
    description: "写入文本到本地文件（UTF-8，默认覆盖），可自动创建上级目录",
    inputSchema: z.object({
      filePath: z
        .string()
        .describe("要写入的文件路径，可以是绝对路径，也可以是相对路径"),
      content: z.string().describe("要写入的文本内容"),
      createDirectories: z
        .boolean()
        .optional()
        .describe("可选：是否自动创建上级目录（默认为 false）")
    })
  },
  async ({ filePath, content, createDirectories }) => {
    const fullPath = path.resolve(filePath);

    if (createDirectories) {
      await fs.mkdir(path.dirname(fullPath), { recursive: true });
    }

    await fs.writeFile(fullPath, content, "utf8");

    return {
      content: [
        {
          type: "text",
          text: `已写入文件：${fullPath}\n写入长度：${content.length} 字符`
        }
      ]
    };
  }
);

// 列出目录内容
server.registerTool(
  "list_directory",
  {
    title: "列出目录",
    description: "列出某个目录下的文件和子目录",
    inputSchema: z.object({
      dirPath: z
        .string()
        .describe("要列出的目录路径，可以是绝对路径，也可以是相对路径"),
      withFileTypes: z
        .boolean()
        .optional()
        .describe("是否返回文件类型信息（文件 / 目录），默认 true")
    })
  },
  async ({ dirPath, withFileTypes = true }) => {
    const fullPath = path.resolve(dirPath);
    const entries = await fs.readdir(fullPath, {
      withFileTypes: true
    });

    const result = entries.map((entry) => {
      const entryPath = path.join(fullPath, entry.name);
      if (!withFileTypes) {
        return entryPath;
      }
      return {
        name: entry.name,
        path: entryPath,
        isFile: entry.isFile(),
        isDirectory: entry.isDirectory(),
        isSymbolicLink: entry.isSymbolicLink()
      };
    });

    return {
      content: [
        {
          type: "text",
          text:
            `目录：${fullPath}\n` +
            `条目数量：${result.length}\n\n` +
            JSON.stringify(result, null, 2)
        }
      ]
    };
  }
);

// 获取系统信息
server.registerTool(
  "get_system_info",
  {
    title: "系统信息",
    description: "获取当前系统的基本信息（平台、CPU、内存等）",
    inputSchema: z.object({})
  },
  async () => {
    const info = {
      platform: os.platform(),
      release: os.release(),
      arch: os.arch(),
      cpus: os.cpus().map((c) => c.model),
      totalMem: os.totalmem(),
      freeMem: os.freemem(),
      homedir: os.homedir(),
      hostname: os.hostname()
    };

    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(info, null, 2)
        }
      ]
    };
  }
);

// 在 macOS 上通过 open 命令打开应用或文件
server.registerTool(
  "open_on_macos",
  {
    title: "macOS 打开目标",
    description:
      "在 macOS 上使用 open 命令打开应用、文件或 URL（仅在 macOS 下可用）",
    inputSchema: z.object({
      target: z
        .string()
        .describe(
          "要打开的目标，可以是 .app 路径、文件路径或 URL，例如 /Applications/Notes.app 或 https://www.google.com"
        ),
      args: z
        .array(z.string())
        .optional()
        .describe("传递给 open 的额外参数，例如 ['-n'] 表示新建实例")
    })
  },
  async ({ target, args = [] }) => {
    if (os.platform() !== "darwin") {
      return {
        content: [
          {
            type: "text",
            text: "open_on_macos 仅在 macOS (darwin) 平台上可用。"
          }
        ]
      };
    }

    const cmdParts = ["open", ...args.map((a) => `"${a}"`), `"${target}"`];
    const command = cmdParts.join(" ");

    const { stdout, stderr } = await exec(command);

    return {
      content: [
        {
          type: "text",
          text:
            `已执行命令：${command}\n\n` +
            `======= STDOUT =======\n${stdout}\n\n` +
            `======= STDERR =======\n${stderr}`
        }
      ]
    };
  }
);

const transport = new StdioServerTransport();
await server.connect(transport);



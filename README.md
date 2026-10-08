# Tell Shell 🐚✨

**用自然语言指挥 Android。**  
Tell Shell 是一个 AI 辅助的 Shell 命令执行器，输入自然语言 → 调用 AI API 翻译为 Shell 命令 → 通过 Shizuku/Sui 以 root/shell 权限执行。

默认对接 DeepSeek，也支持 OpenCode Zen / Go 以及任意 OpenAI 兼容服务。

---

## 截图

| 主页（Material3） | 主页（Miuix） | 设置页 |
|:---:|:---:|:---:|
| *(待补充)* | *(待补充)* | *(待补充)* |

---

## 功能

- **📱 应用列表** — 显示所有桌面应用（含复选框多选），选中信息作为命令上下文
- **🤖 AI 命令生成** — 输入自然语言，DeepSeek API 输出纯 Shell 命令（提示词约束仅输出可执行命令）
- **🧠 思考深度可调** — 关闭 / 低 / 高 / 最高四档，对齐 DeepSeek 官方 `reasoning_effort` 枚举
- **⚡ Shizuku/Sui 执行** — 双通道权限申请：Phase 1 Shizuku → Phase 2 Sui fallback
- **📋 命令输出** — 实时显示 stdout + stderr，支持点击复制
- **🕘 历史记录** — 自动保存每次输入/命令/输出与思考过程，可多选后交给 AI 分析
- **🔌 多协议 API** — OpenAI 兼容 / OpenAI Responses / Anthropic / Gemini 四种格式
- **🎨 双主题 UI** — Google Material3 / Xiaomi Miuix 可切换
- **⚙️ 设置页** — BaseURL / API Key / 模型 / 采样参数 / 提示词 / 思考深度 / OpenCode 兼容

---

## 思考深度

不同服务商的思考参数并不一致，设置页提供四档思考深度与一个 OpenCode 兼容开关。

**档位**（默认「高」）：

| 档位 | `reasoning_effort` | 说明 |
|------|--------------------|------|
| 关闭 | `none` | 不启用思考模式，最快最省 token |
| 低 | `low` | 轻量思考 |
| 高 | `high` | 官方默认档 |
| 最高 | `max` | 最深思考，耗时与消耗最高 |

**请求体结构**（对齐 [DeepSeek 官方规范](https://api-docs.deepseek.com/api/create-chat-completion)）——`thinking` 与 `reasoning_effort` 是两个**并列的顶层字段**：

```json
{
  "model": "deepseek-flash",
  "messages": [ ... ],
  "thinking": { "type": "enabled" },
  "reasoning_effort": "high",
  "max_tokens": 2000
}
```

选择「关闭」时会显式发送 `"thinking": {"type": "disabled"}` 与 `"reasoning_effort": "none"`——因为 DeepSeek 侧 `thinking` 默认即为 `enabled`，省略字段并不会关闭思考。

**OpenCode 兼容开关**：OpenCode 的档位枚举与 DeepSeek 不同（为 `none`/`minimal`/`low`/`medium`/`high`/`xhigh`，没有 `max`）。开启「使用 OpenCode 思考档位」后，最高档会映射为 `xhigh`，避免网关因非法枚举报错。

> 另有「发送 OpenCode 会话头」开关：OpenCode Zen / Go 网关要求每个会话携带稳定的 `x-opencode-session` 头，缺失会返回 `400 MissingSessionID`。该开关默认关闭，遇到此报错时打开即可（应用也会在错误提示中引导）。

---

## 环境要求

### 运行环境

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| **Android** | 13+ (API 33) | minSdk 33，targetSdk 36 |
| **Shizuku** | v12+ | [下载](https://shizuku.rikka.app/download/) |
| **Sui** | v12+ | [下载](https://github.com/RikkaApps/Sui)（Magisk 模块） |
| **API Key** | — | DeepSeek / OpenCode Zen 或任意 OpenAI 兼容服务，在设置页配置 |

> 无需 Root 即可使用 Shizuku（通过 ADB 启动）；Sui 需要 Magisk/KernelSU + Root。

### 构建环境

| 工具 | 版本 |
|------|------|
| Android Studio | Ladybug+ (2025.1+) |
| JDK | 17+ |
| Gradle | 9.4.1（Wrapper 自带） |
| AGP | 9.2.1 |
| Kotlin | 2.3.0（内置） |

---

## 快速开始

### 1. 配置 API

打开应用 → 点击右上角 ⚙️ → 填入：
- **BaseURL**: `https://api.deepseek.com`（默认）
- **API Key**: 你的 API Key
- **API 格式**: 按服务商选择（DeepSeek 选「OpenAI 兼容」）
- **模型**: 有 API Key 时会自动拉取列表

使用 OpenCode Zen / Go 时，再按需打开「发送 OpenCode 会话头」与「使用 OpenCode 思考档位」两个开关（见上文「思考深度」）。

### 2. 授权 Shizuku/Sui

点击顶栏 **⚪ 授权** 按钮，应用会自动：
1. 尝试 Shizuku 通道
2. 超时后 fallback 到 Sui
3. 显示当前权限来源（如 `sui(root)` / `shizuku(shell)`）

### 3. 使用

1. 勾选要操作的应用（可选）
2. 在输入框写入自然语言，如：
   - `禁用 kindle`
   - `清除 chrome 缓存`
   - `查看当前 WiFi 密码`
3. 点击 **生成命令** → 确认命令 → 点击 **执行**

---

## 手动构建

> ⚠️ 本项目约定：**不要用命令行 Gradle 编译**，构建统一交给 Android Studio。

在 Android Studio 中打开项目根目录，Gradle Sync 后：

- **Run** 直接安装到设备调试；
- **Build → Generate Signed App Bundle / APK** 打包发布版。

发布版使用 `app/build.gradle.kts` 中的 `release` 签名配置，可通过环境变量覆盖：
`KEYSTORE_PATH` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`。

---

## 技术架构

```
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│   Material3 ─── Theme Switch ─── Miuix          │
├─────────────────────────────────────────────────┤
│              ViewModel + StateFlow              │
├─────────────────────────────────────────────────┤
│  DeepSeek API  │  Shizuku/Sui  │  DataStore    │
│  (OkHttp)      │  (Shell Exec) │  (Settings)   │
└─────────────────────────────────────────────────┘
```

| 层级 | 技术 |
|------|------|
| **UI** | Jetpack Compose + Material3 / Miuix 0.9.2 |
| **导航** | Navigation Compose 2.8.7 |
| **状态管理** | ViewModel + StateFlow |
| **AI** | 多协议客户端（OkHttp 4.12.0 + Gson 2.11.0） |
| **权限提升** | Shizuku API 12.2.0 + Sui |
| **持久化** | DataStore Preferences 1.1.4 |
| **构建** | Gradle 9.4.1 + AGP 9.2.1 + Kotlin 2.3.0 |

---

## 项目结构

```
tell-shell/
├── app/
│   └── src/main/java/com/tellshell/app/
│       ├── TellShellApp.kt           # Application（Sui 初始化）
│       ├── MainActivity.kt           # 单 Activity + NavHost
│       ├── data/                     # 数据层
│       │   ├── Models.kt             # AppInfo 数据类
│       │   ├── ThemeMode.kt          # 主题枚举
│       │   ├── HistoryItem.kt        # 历史记录数据类
│       │   ├── HistoryStore.kt       # 历史记录持久化
│       │   └── SettingsStore.kt      # DataStore 封装
│       ├── network/
│       │   ├── ApiFormat.kt          # API 格式枚举（OpenAI/Responses/Anthropic/Gemini）
│       │   ├── ReasoningEffort.kt    # 思考深度枚举（none/low/high/max）
│       │   └── AIClient.kt           # 多协议 AI API 客户端
│       ├── shell/
│       │   └── ShizukuExecutor.kt    # Shizuku/Sui 执行引擎
│       ├── viewmodel/
│       │   ├── HomeViewModel.kt      # 主页状态管理
│       │   ├── HistoryViewModel.kt   # 历史记录状态管理
│       │   └── SettingsViewModel.kt  # 设置状态管理
│       └── ui/
│           ├── theme/AppTheme.kt     # 主题路由
│           ├── material3/            # Material3 版 Screens
│           └── miuix/                # Miuix 版 Screens
├── gradle/libs.versions.toml         # 依赖版本集中管理
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 相关项目

- [Shizuku](https://github.com/RikkaApps/Shizuku) — 以普通/ADB/Root 身份运行代码
- [Sui](https://github.com/RikkaApps/Sui) — Magisk 模块，通过 Shizuku API 通信
- [Miuix](https://github.com/compose-miuix-ui/miuix) — HyperOS 风格 Compose 组件库
- [DeepSeek](https://platform.deepseek.com/) — AI API

---

## License

MIT

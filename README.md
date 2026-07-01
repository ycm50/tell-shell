# Tell Shell 🐚✨

**用自然语言指挥 Android。**  
Tell Shell 是一个 AI 辅助的 Shell 命令执行器，输入自然语言 → 调用 DeepSeek API 翻译为 Shell 命令 → 通过 Shizuku/Sui 以 root/shell 权限执行。

---

## 截图

| 主页（Material3） | 主页（Miuix） | 设置页 |
|:---:|:---:|:---:|
| *(待补充)* | *(待补充)* | *(待补充)* |

---

## 功能

- **📱 应用列表** — 显示所有桌面应用（含复选框多选），选中信息作为命令上下文
- **🤖 AI 命令生成** — 输入自然语言，DeepSeek API 输出纯 Shell 命令（提示词约束仅输出可执行命令）
- **⚡ Shizuku/Sui 执行** — 双通道权限申请：Phase 1 Shizuku → Phase 2 Sui fallback
- **📋 命令输出** — 实时显示 stdout + stderr，支持点击复制
- **🎨 双主题 UI** — Google Material3 / Xiaomi Miuix 可切换
- **⚙️ 设置页** — BaseURL / API Key 配置、主题切换、系统提示词展示

---

## 环境要求

### 运行环境

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| **Android** | 14+ (API 34) | minSdk 33，推荐 34+ |
| **Shizuku** | v12+ | [下载](https://shizuku.rikka.app/download/) |
| **Sui** | v12+ | [下载](https://github.com/RikkaApps/Sui)（Magisk 模块） |
| **DeepSeek API Key** | — | 在设置页配置 |

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
- **API Key**: 你的 DeepSeek API Key

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

使用项目根目录的 `scripts/build-tell-shell.bat`：

```bat
scripts\build-tell-shell.bat <标签> <架构>
```

示例：
```bat
scripts\build-tell-shell.bat v1.0.0 arm64-v8a
scripts\build-tell-shell.bat v1.0.1 armeabi-v7a
scripts\build-tell-shell.bat v1.0.0 x86_64
```

构建产物输出到 `build-output/<标签>/` 目录，不修改仓库代码。

> 也可以直接在 Android Studio 中打开 `test/` 目录，Gradle Sync 后 Run/Debug。

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
| **导航** | Navigation Compose |
| **状态管理** | ViewModel + StateFlow |
| **AI** | DeepSeek Chat Completion API（OkHttp） |
| **权限提升** | Shizuku API 12.2.0 + Sui |
| **持久化** | DataStore Preferences |
| **构建** | Gradle 9.4.1 + AGP 9.2.1 |

---

## 项目结构

```
test/
├── app/
│   └── src/main/java/com/tellshell/app/
│       ├── TellShellApp.kt           # Application（Sui 初始化）
│       ├── MainActivity.kt           # 单 Activity + NavHost
│       ├── data/                     # 数据层
│       │   ├── Models.kt             # AppInfo 数据类
│       │   ├── ThemeMode.kt          # 主题枚举
│       │   └── SettingsStore.kt      # DataStore 封装
│       ├── network/
│       │   └── DeepSeekClient.kt     # DeepSeek API 客户端
│       ├── shell/
│       │   └── ShizukuExecutor.kt    # Shizuku/Sui 执行引擎
│       ├── viewmodel/
│       │   ├── HomeViewModel.kt      # 主页状态管理
│       │   └── SettingsViewModel.kt  # 设置状态管理
│       └── ui/
│           ├── theme/AppTheme.kt     # 主题路由
│           ├── material3/            # Material3 版 Screens
│           └── miuix/                # Miuix 版 Screens
├── scripts/
│   └── build-tell-shell.bat          # 手动构建脚本
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml
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

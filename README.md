# 不忘 (BuWang) — 虚拟人格生成与互动 App

## 项目简介

「不忘」是一款基于大语言模型的虚拟人格生成与互动 Android 应用。用户可以通过 MBTI 类型、聊天文本导入、ST角色卡导入或手动参数调节来创建具有独特人格的 AI 角色，并与他们进行自然对话。

### V1.0「人格初现」核心功能

- **4种角色创建方式**：手动配置、MBTI JSON导入、聊天文本导入、ST角色卡导入
- **人格参数化**：基于 Big Five 五大人格模型 + 语言风格 + 行为模式
- **智能对话**：支持流式SSE对话，DeepSeek V3 / 通义千问 / 自定义API
- **纯本地存储**：所有数据存储在设备本地，Room + DataStore，保障隐私安全
- **数据可移植**：.bwbackup 加密导出/导入，支持单角色ST格式导出

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin 1.9+ |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture |
| 数据库 | Room 2.6+ |
| 键值存储 | DataStore 1.0+ |
| 加密 | Tink + Android Keystore + PBKDF2 |
| 网络 | OkHttp 4.12 + Retrofit 2.9 + SSE |
| DI | Hilt 2.50 |
| 异步 | Coroutines + Flow |
| JSON | Moshi + Gson |
| 最低 SDK | API 26 (Android 8.0) |
| 目标 SDK | API 34 (Android 14) |

## 项目结构

```
app/src/main/java/com/buwang/app/
├── data/                          # 数据层
│   ├── local/
│   │   ├── entity/                # Room Entity (4张表)
│   │   │   ├── PersonaEntity      # 角色表
│   │   │   ├── ConversationEntity # 会话表
│   │   │   ├── MessageEntity      # 消息表
│   │   │   └── UserSettingsEntity # 设置表(单例)
│   │   ├── dao/                   # DAO 接口
│   │   └── database/              # BuWangDatabase
│   ├── remote/
│   │   ├── api/
│   │   │   ├── LlmApi             # 统一LLM API接口
│   │   │   ├── LlmApiFactory      # 多模型适配器工厂
│   │   │   └── SseParser          # SSE流式响应解析器
│   │   ├── dto/                   # ChatRequest/ChatResponse/ChatStreamChunk
│   │   └── interceptor/           # API Key注入 + 错误处理拦截器
│   └── repository/                # Repository 实现
│       ├── PersonaRepositoryImpl
│       ├── ChatRepositoryImpl     # 含流式/非流式对话
│       └── UserSettingsRepositoryImpl
├── domain/                        # 领域层
│   ├── model/                     # Persona, Message, UserSettings, Conversation
│   └── repository/                # Repository 接口
├── core/                          # 核心工具
│   ├── crypto/                    # CryptoManager (Tink + Keystore + PBKDF2)
│   ├── export/                    # ExportEngine (加密导出/导入)
│   └── personality/               # 人格引擎（预留）
├── di/                            # Hilt 模块
│   ├── DatabaseModule             # Room + DAO
│   ├── NetworkModule              # OkHttp + Retrofit + LlmApiFactory
│   ├── CryptoModule               # CryptoManager
│   └── RepositoryModule           # Repository Binds
├── presentation/                  # UI层（目录已建）
│   ├── chat/                      # 聊天界面
│   ├── persona/                   # 角色管理
│   ├── settings/                  # 设置界面
│   └── components/                # 共享组件
├── BuWangApplication.kt           # @HiltAndroidApp
└── MainActivity.kt                # @AndroidEntryPoint

personality-engine/                # 人格引擎设计文档
├── bigfive-model.md               # Big Five 人格参数模型
├── mbti-mapping.json              # MBTI→Big Five 映射表
├── mbti-mapping.md                # MBTI映射设计文档
├── system-prompt-design.md        # 三层Prompt架构设计
├── preset-characters.json         # 4个预设角色参数
├── preset-characters.md           # 预设角色设计文档
├── chat-analysis-prompt.md        # 聊天文本人格分析Prompt
└── personality-drift-monitor.md   # 人格漂移监测方案
```

## 架构设计

### 数据流

```
UI (Compose) → ViewModel → UseCase → Repository → DAO / LlmApi
     ↑                                       ↓
     └─────────── StateFlow ◄── Flow ◄───────┘
```

### 对话流程

```
用户输入 → 乐观更新UI → 保存用户消息
    → 构建Prompt(System + History + User)
    → LlmApi.chatCompletionStream(stream=true)
    → SseParser → Flow<token>
    → 逐字更新UI → 保存完整回复 → 更新会话时间
```

### 多模型适配

```
LlmApiFactory.create(modelKey, apiKey, customBaseUrl?)
    ├── tokens-box (默认) → https://tokens-box.com/v1/  (MiniMax-M2.7 / MiniMax-M3)
    ├── deepseek-v3 → https://api.deepseek.com/v1/
    ├── qwen-turbo  → https://dashscope.aliyuncs.com/compatible-mode/v1/
    ├── silicon-*   → https://api.siliconflow.cn/v1/
    └── custom      → 用户自定义 Base URL
```

> **联调说明**：默认模型为 Tokens-Box（MiniMax-M2.7 / MiniMax-M3，推理模型会在流式内容中输出 `` 标签），
> 该模型目前仅用于联调测试，落地阶段将按 PRD 替换为正式选型（默认 DeepSeek V3）。
>
> **密钥已外置（安全交付）**：内置 Key 不再硬编码于源码，改由构建期从 `local.properties` 的
> `buwang.builtin.api.key`（或环境变量 `BUWANG_BUILTIN_API_KEY`）注入 `BuildConfig.BUILTIN_API_KEY`，
> `local.properties` 已被 `.gitignore` 排除，不会进入版本库；运行时由 `LlmApiFactory` / 拦截器 / 仓库统一读取。

## 构建

```bash
# 克隆项目
git clone <repo-url>
cd buwang-app

# 配置 Android SDK 路径（Android Studio 同步时会自动补写 sdk.dir）
# 若需使用内置联调模型，在 local.properties 中追加：
#   buwang.builtin.api.key=sk-xxxx

# 构建 Debug APK
./gradlew assembleDebug

# 运行测试
./gradlew test
```

## 开发规范

- **分支策略**：`main` (生产) / `develop` (开发) / `feature/*` (功能分支)
- **代码规范**：遵循 Kotlin 官方编码规范
- **Commit 规范**：`<type>: <description>` (feat/fix/docs/style/refactor/test/chore)
- **Code Review**：所有 PR 需要至少 1 人审核通过方可合并

## 团队

| 角色 | 职责 |
|------|------|
| 产品负责人 | 拉拉肥 — 需求决策、优先级裁定、验收签字 |
| 高级项目经理 | PM — 项目计划、进度跟踪、风险管理 |
| 工作室运营管理者 | Studio Lead — 环境搭建、CI/CD、技术架构 |
| 设计师 | Designer — 交互设计、视觉设计、设计规范 |
| 原型工程师 | Prototyper — 可交互原型、动效设计 |
| 移动开发工程师 | Android Dev — 数据层、网络层、UI开发、联调 |
| UX架构师 | UX Architect — 人格引擎、Prompt架构、对话体验 |

## 许可证

Copyright 2024 BuWang Team. All rights reserved.

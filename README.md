# GlesTranslate Android

一个基于 Jetpack Compose 的轻量 AI 翻译与识别应用。支持：
- 文本翻译（内置翻译服务/自定义 OpenAI 兼容服务，支持流式输出）
- 图片文字识别（OCR）：可拍照或选择图片
- 语音转文字（ASR）：选择本地音频文件识别
- 语言管理（预设语言 + 自定义语言）与翻译历史记录

网页版仓库链接：`https://github.com/Glassous/GlesTranslate`

## 功能特性
- 内置翻译服务：`https://api.jkyai.top/API/depsek3.1.php`
- 内置 OCR/ASR 服务：
  - 图片识别：`https://api.pearktrue.cn/api/ocr/`
  - 音频识别：`https://api.pearktrue.cn/api/audiocr/`
- 自定义 AI 服务：OpenAI 兼容 Chat Completions 流式接口（SSE），可配置 `Base URL / API Key / Model`，并可开启多模态（图片/音频识别）
- Jetpack DataStore 持久化：保存语言列表、选中语言、AI 配置、多模态开关、历史记录
- Jetpack Compose Material 3 UI，集成 Google Fonts（Aladin）

## 架构概要
- `viewmodel/TranslationViewModel.kt`：业务状态与逻辑中心，负责翻译、识别、历史记录管理、设置同步
- `network/`：
  - `BuiltInTranslationService.kt`：内置文本翻译 HTTP 调用
  - `OcrService.kt`：内置图片/音频识别（multipart 上传）
  - `CustomOpenAIService.kt`：OpenAI 兼容流式 Chat Completions（文本翻译、图片/音频识别）
- `data/`：
  - `TranslationAppData.kt/Serializer`：DataStore 序列化模型与默认值
  - `AppDataStore.kt`：应用 DataStore 入口
- `ui/screens/`：
  - `MainScreen.kt`：主界面（输入、翻译结果、识别入口、历史与语言选择等）
  - `SettingsScreen.kt`：设置界面（AI 配置、多模态开关、语言管理）
- `MainActivity.kt`：应用入口与导航（`main`/`settings`）

## 开发环境
- `compileSdk = 36`，`targetSdk = 36`，`minSdk = 33`
- Kotlin `2.0.21`，AGP `8.11.2`
- 主要依赖：
  - Jetpack Compose BOM、Material3（含 `material3:1.5.0-alpha07` 用于 Expressive 组件）
  - Navigation Compose、Lifecycle ViewModel Compose
  - Kotlinx Serialization JSON
  - Preferences DataStore
  - Ktor Client（Android、Content Negotiation、Kotlinx JSON）
  - Google Fonts、Material Icons Extended

## 构建与运行
- Android Studio 打开本项目后，直接运行 `app` 模块即可。
- 命令行构建：
  - Windows：`gradlew.bat assembleDebug`
  - macOS/Linux：`./gradlew assembleDebug`
- 设备要求：Android 13（API 33）及以上。

## 使用说明
- 文本翻译：在主界面输入文本，选择目标语言后点击发送；若开启自定义 AI，将以流式输出显示翻译增量。
- 图片识别：
  - 点击“拍照”拍摄并识别图片（需相机权限）
  - 点击“图片”从本地选择图片识别
- 音频识别：点击“语音”从本地选择音频文件识别（无需麦克风权限）
- 历史记录：侧边抽屉展示最近翻译记录，可点击回填或删除
- 语言管理：在“设置”中管理自定义语言，预设语言在 `data/PredefinedLanguages` 中定义

## 自定义 AI 配置
- 在“设置 → API配置”中：
  - 勾选“使用自定义AI API”
  - 填写 `Base URL`、`API Key`、`Model`（文本翻译）
  - 开启“多模态”并填写 `MultiModal Model`（图片/音频识别）
- 接口兼容说明：
  - 若 `Base URL` 以 `/v1` 结尾，将自动使用 `/v1/chat/completions`
  - 若未包含路径，将追加 `/v1/chat/completions`
  - SSE 流式返回会按增量解析展示结果

## 权限
- `INTERNET`：网络请求（翻译与识别）
- `CAMERA`：拍照识别图片（运行时动态授权）
- 音频识别使用系统文件选择器，不需要 `RECORD_AUDIO` 权限。

## 目录结构（简要）
```
app/
  src/main/java/com/glassous/glestranslate/
    data/               # DataStore模型与序列化
    network/            # 内置/自定义网络服务
    ui/screens/         # 主界面与设置界面
    ui/theme/           # 主题与字体
    MainActivity.kt     # 入口与导航
```

## 许可证
本项目采用 Apache License 2.0，详情见仓库根目录的 `LICENSE` 文件。

## 致谢
- 本项目使用的第三方服务与接口仅用于演示与学习，实际生产使用请遵循相应服务条款与限速策略。
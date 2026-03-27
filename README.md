# 科梦奇机器人 SDK 示例应用（KmqSampleBody）

![Android](https://img.shields.io/badge/Android-API%2026%2B-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue)

---

## 目录

- [一、项目说明](#一项目说明)
- [二、产品型号说明](#二产品型号说明)
- [三、适用版本说明](#三适用版本说明)
- [四、开发注意事项](#四开发注意事项)
  - [4.4 AndroidManifest 权限配置](#44-androidmanifest-权限配置)
  - [4.5 Application 中的 SDK 连接初始化](#45-application-中的-sdk-连接初始化)
- [五、功能模块总览](#五功能模块总览)
  - [5.1 语音能力](#51-语音能力) 
    -  [5.1.1 TTS 播报](#511-tts-播报) 
    - · [5.1.2 TTS 流式播报](#512-tts-流式播报)
    - · [5.1.3 ASR 语音识别](#513-asr-语音识别)
    - · [5.1.4 Action 机制](#514-action-机制)
  - [5.2 底盘 & 导航能力](#52-底盘--导航能力) 
    - [5.2.1 底盘控制](#521-底盘控制) 
    - [5.2.2 云台控制](#522-云台控制仅豹二小宝支持)
    - [5.2.3 定位状态与坐标获取](#523-定位状态与坐标获取)
    - [5.2.4 点位管理](#524-点位管理)
    - [5.2.5 导航](#525-导航)
    - [5.2.6 充电管理](#526-充电管理)
  - [5.3 视觉能力](#53-视觉能力)
    - [5.3.1 Camera2 原生摄像头](#531-camera2-原生摄像头)
    - [5.3.2 摄像头数据流共享（SurfaceShare）](#532-摄像头数据流共享surfaceshare)
    - [5.3.3 人脸检测与识别](#533-人脸检测与识别)
  - [5.4 系统信息与控制](#54-系统信息与控制)
    - [5.4.1 系统版本与 SN 获取](#541-系统版本与-sn-获取)
    - [5.4.2 电池信息与监听](#542-电池信息与监听)
    - [5.4.3 禁止系统功能](#543-禁止系统功能)
    - [5.4.4 休眠功能](#544-休眠功能)
    - [5.4.5 唤醒](#545-唤醒)
    - [5.4.6 充电控制](#546-充电控制)
    - [5.4.7 通用错误码速查](#547-通用错误码速查)
- [六、项目架构](#六项目架构)
- [七、核心概念](#七核心概念)
  - [7.1 已消费 vs 未消费](#71-已消费-vs-未消费asr-结果处理模式)
  - [7.2 AgentCore 关键属性](#72-agentcore-关键属性)
  - [7.3 PageAgent 生命周期](#73-pageagent-生命周期)
  - [7.4 TTSCallback 回调机制](#74-ttscallback-回调机制)
  - [7.5 RobotApi 连接生命周期](#75-robotapi-连接生命周期)
  - [7.6 reqId 请求标识](#76-reqid-请求标识)
  - [7.7 三种监听器模式](#77-三种监听器模式)
  - [7.8 Definition 常量体系](#78-definition-常量体系)
  - [7.9 线程模型与 UI 更新](#79-线程模型与-ui-更新)
- [八、API 速查表](#八api-速查表)
- [九、项目依赖](#九项目依赖)

---

## 一、项目说明

本项目是科梦奇机器人 Agent/Robot SDK 的官方示例应用，旨在为开发者提供完整的 SDK 功能演示和二次开发参考。

**项目定位：**

- 演示机器人 Agent/Robot SDK 的核心能力，包括 TTS 语音播报、ASR 语音识别、底盘控制、定位导航、充电管理等
- 提供清晰的代码示例和最佳实践，帮助开发者快速上手 SDK 二次开发
- 覆盖 Agent SDK（语音交互）和 CoreService SDK（硬件控制）两大核心模块

**应用概述：**

应用首页（[`MainActivity`](app/src/main/java/com/kmq/kmqsamplebody/MainActivity.kt)）以卡片网格展示所有功能模块，点击即可进入对应演示页面。每个页面独立演示一项 SDK 功能，代码结构清晰，便于参考和复用。

---

## 二、产品型号说明

本示例应用适用于以下科梦奇机器人产品型号：

| 产品型号 | 支持状态 |
|---------|---------|
| 小宝 | 支持 |
| 小科 | 支持 |
| 小鱼二代 | 支持 |
| 豹二 | 支持 |

> 以上型号均需安装 **Robot OS 11.3 以上** 或 **Agent OS** 系统方可运行本示例应用。

---

## 三、适用版本说明

| 项目 | 版本要求 |
|------|---------|
| 机器人操作系统 | Robot OS >= 11.3 或 Agent OS |
| Android minSdk | 26（Android 8.0） |
| Android targetSdk | 36 |
| Android compileSdk | 36 |
| JDK | 11+ |
| Gradle | 8.x（项目自带 Wrapper，无需手动安装） |

---

## 四、开发注意事项

### 4.1 开发方式

- 本项目为 **Android 原生开发**，使用 Kotlin 语言
- 开发工具：**Android Studio**（推荐 Ladybug 或更高版本）

### 4.2 Gradle 依赖配置

项目仅需引入科梦奇机器人 Agent SDK 依赖。以下分别展示 Kotlin DSL（`.gradle.kts`）和 Groovy DSL（`.gradle`）两种写法：

**项目级仓库配置** — [`settings.gradle.kts`](settings.gradle.kts) / `settings.gradle`：

```kotlin
// settings.gradle.kts (Kotlin DSL)
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            credentials.username = "agentMaven"
            credentials.password = "agentMaven"
            url = uri("https://npm.ainirobot.com/repository/maven-public/")
        }
    }
}
```

```groovy
// settings.gradle (Groovy DSL)
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            credentials {
                username 'agentMaven'
                password 'agentMaven'
            }
            url 'https://npm.ainirobot.com/repository/maven-public/'
        }
    }
}
```

**模块级依赖声明** — [`app/build.gradle.kts`](app/build.gradle.kts) / `app/build.gradle`：

```kotlin
// app/build.gradle.kts (Kotlin DSL)
dependencies {
    implementation("com.orionstar.agent:sdk:0.4.5-SNAPSHOT")
}
```

```groovy
// app/build.gradle (Groovy DSL)
dependencies {
    implementation 'com.orionstar.agent:sdk:0.4.5-SNAPSHOT'
}
```

### 4.3 assets 配置说明

项目中 [`app/src/main/assets/`](app/src/main/assets) 目录下包含 Agent 框架所需的配置文件：

**[`actionRegistry.json`](app/src/main/assets/actionRegistry.json)** — Agent Action 注册配置文件：

```json
{
  "appId": "app_ebbd1e6e22d6499eb9c220daf095d465",
  "platform": "apk",
  "actionList": []
}
```

| 字段 | 说明                                                         |
|------|------------------------------------------------------------|
| `appId` | 应用在 Agent 平台上的唯一标识，由平台分配，开发者需替换为自己的 appId。开发者可以联系售前、售后进行获取 |
| `platform` | 应用平台类型，固定为 `"apk"`                                         |
| `actionList` | 应用注册的 Action 列表，当前为空数组，可根据业务需求添加自定义 Action                 |

> **注意：** 每个应用必须配置正确的 `appId`，否则 Agent 框架无法正常识别和管理该应用。`appId` 需从科梦奇机器人获取。

### 4.4 AndroidManifest 权限配置

[`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) 中需声明以下权限，确保 SDK 各项功能正常运行：

```xml
<!-- 网络访问（Agent SDK 通信必需） -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- 机器人设置提供者权限（底盘控制、休眠、导航设置等必需） -->
<uses-permission android:name="com.ainirobot.coreservice.robotSettingProvider" />

<!-- 开机自启动权限（需要时取消注释）
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
-->
```

**权限说明：**

| 权限 | 必需 | 说明 |
|------|------|------|
| `android.permission.INTERNET` | 是 | 网络访问，Agent SDK 与云端通信 |
| `com.ainirobot.coreservice.robotSettingProvider` | 是 | 访问机器人系统设置（电量查询、导航加减速模式、休眠等），缺少此权限会抛出 `SecurityException` |
| `android.permission.RECEIVE_BOOT_COMPLETED` | 否 | 开机自启动，配合 intent-filter 使用 |
| `android.permission.WRITE_EXTERNAL_STORAGE` | 否 | 外部存储写入（如需保存人脸照片等） |
| `android.permission.READ_EXTERNAL_STORAGE` | 否 | 外部存储读取 |

**开机自启动配置：**

如需应用在机器人开机后自动启动，需要以下两步：

**步骤 1：Manifest 配置**

取消注释 `RECEIVE_BOOT_COMPLETED` 权限，并在 `MainActivity` 中添加 `intent-filter`：

```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<activity android:name=".MainActivity" android:exported="true">
    <!-- 标准启动入口 -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <!-- 开机自启动入口 -->
    <intent-filter>
        <action android:name="action.orionstar.default.app" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

**步骤 2：系统设置**

在机器人上完成配置：
1. 三指下拉进入系统设置
2. 选择"开发者设置"
3. 在"开机启动程序"中配置您的应用

> **注意**：开机启动程序功能需要 OTA3 及以上版本支持。

### 4.5 Application 中的 SDK 连接初始化

应用启动时需要在自定义 `Application` 类中完成两项 SDK 初始化：

1. **Agent SDK 初始化**：创建 `AppAgent` 实例，设置人设和注册 Action
2. **CoreService SDK 连接**：调用 `RobotApi.getInstance().connectServer()` 建立与机器人底盘服务的连接

```kotlin
class MainApplication : Application() {

    lateinit var appAgent: AppAgent
    var isRobotApiConnected = false
        private set

    override fun onCreate() {
        super.onCreate()

        // ① Agent SDK 初始化
        appAgent = object : AppAgent(this@MainApplication) {
            override fun onCreate() {
                setPersona("你是科梦奇机器人的示例应用助手，负责展示各项SDK功能。")
                registerAction(Actions.SAY)
            }

            override fun onExecuteAction(action: Action, params: Bundle?): Boolean {
                return false
            }
        }

        // ② CoreService SDK 连接（底盘/导航/充电等硬件 API 必需）
        RobotApi.getInstance().connectServer(this, object : ApiListener {
            override fun handleApiConnected() {
                isRobotApiConnected = true
                Log.i(TAG, "RobotApi 连接成功")
            }

            override fun handleApiDisconnected() {
                isRobotApiConnected = false
                Log.w(TAG, "RobotApi 连接断开")
            }

            override fun handleApiDisabled() {
                isRobotApiConnected = false
                Log.w(TAG, "RobotApi 不可用")
            }
        })
    }
}
```

**连接回调说明：**

| 回调方法 | 触发时机 | 说明 |
|---------|---------|------|
| `handleApiConnected` | 连接成功 | 此后所有 `RobotApi` 调用生效，底盘控制权移交给当前 APP |
| `handleApiDisconnected` | 连接断开 | 系统接管、发生故障或启动了其他带 SDK 的程序时触发 |
| `handleApiDisabled` | API 不可用 | SDK 被禁用，所有 API 调用无效 |

**关键注意事项：**

- **所有 `RobotApi` 调用必须在 `handleApiConnected` 回调之后才能生效**，在连接前调用不会有任何效果
- **只有前台进程**才能正确连接并获取底盘控制权限，后台服务进程不工作
- 当系统接管结束后，会自动重新 connect；其他原因（如其他 APP 抢占）导致的断开不会自动恢复
- 建议通过 `isRobotApiConnected` 标志位在各 Activity 中检查连接状态，未连接时给予用户提示

> **参考**：本项目中各 Activity（如 `ChassisActivity`）在 `onCreate` 中会检查 `(application as MainApplication).isRobotApiConnected`，若未连接则显示提示信息。

---

## 五、功能模块总览

应用首页以卡片网格展示所有功能模块，按能力类别分为以下四大类：

### 5.1 语音能力

| 模块 | Activity | 说明 |
|------|----------|------|
| **TTS 播报** | [`TtsActivity`](app/src/main/java/com/kmq/kmqsamplebody/tts/TtsActivity.kt) | 文字转语音播报，支持显示/隐藏语音条，支持已消费/未消费两种 ASR 语音录入模式 |
| **TTS 流式播报** | [`TtsStreamActivity`](app/src/main/java/com/kmq/kmqsamplebody/tts/TtsStreamActivity.kt) | 模拟大模型流式文本输出效果，逐字显示文本并同步 TTS 播报，支持 `TTSCallback` 回调 |
| **ASR 语音识别** | [`AsrActivity`](app/src/main/java/com/kmq/kmqsamplebody/asr/AsrActivity.kt) | 语音转文字识别，支持已消费/未消费两种模式，实时显示识别结果 |
| **Action 机制** | [`MainApplication`](app/src/main/java/com/kmq/kmqsamplebody/MainApplication.kt) / [`actionRegistry.json`](app/src/main/assets/actionRegistry.json) | 大模型意图识别后调用的技能模块，支持动态注册（App/Page 级）和静态注册，详见 [下方说明](#514-action-机制) |

#### 5.1.1 TTS 播报

> 源码：[`TtsActivity.kt`](app/src/main/java/com/kmq/kmqsamplebody/tts/TtsActivity.kt) | 布局：[`activity_tts.xml`](app/src/main/res/layout/activity_tts.xml)

**功能说明：**

文字转语音播报演示页面，左侧为文本输入区和状态显示，右侧为功能按钮组。同时集成了 ASR 语音录入功能，支持已消费/未消费两种模式。

**页面功能：**

| 按钮 | 功能 | 关键 API |
|------|------|----------|
| 播报(显示语音条) | 将输入框文本通过 TTS 播报，同时显示语音条动画 | `AgentCore.tts(text)` + `isEnableVoiceBar = true` |
| 播报(不显示语音条) | 将输入框文本通过 TTS 播报，不显示语音条 | `AgentCore.tts(text)` + `isEnableVoiceBar = false` |
| 语音录入(已消费) | 开启麦克风，识别结果由页面处理，大模型不回复 | `onASRResult` 返回 `true` |
| 语音录入(未消费) | 开启麦克风，识别结果传递给大模型，大模型自动回复 | `onASRResult` 返回 `false` + `isDisablePlan = false` |
| 停止 | 停止 TTS 播报和语音录入，清除上下文 | `AgentCore.stopTTS()` + `clearContext()` |
| 返回首页 | 返回 `MainActivity` | `FLAG_ACTIVITY_CLEAR_TOP` |

**SDK TTS API 说明：**

本页面使用的是 AgentOS SDK 的**异步 TTS 接口**。SDK 同时提供同步和异步两种调用方式：

```kotlin
// 异步调用（本页面使用）
AgentCore.tts(text: String, timeoutMillis: Long = 180000, callback: TTSCallback? = null)

// 同步调用（需在协程中使用）
suspend AgentCore.ttsSync(text: String, timeoutMillis: Long = 180000): TaskResult<String>
```

> **TTS 播放行为特性：**
> - **自动追加播放**：多次调用 `tts()` 会将音频追加到播放队列，不会中断前一次播放，适合连续播报多段内容
> - **语音打断机制**：用户在 TTS 播放过程中对机器人说话会触发语音识别，**自动打断当前 TTS 播放**，被打断时触发失败回调（`status=2`）
> - **流式播放**：TTS 采用流式播放机制，音频边生成边播放，非预生成文件模式。`timeoutMillis` 为完整播放超时时间，设置过短可能导致长文本被截断
> - **强制停止**：调用 `AgentCore.stopTTS()` 可立即打断播放

**核心实现：**

- 通过 `RecordMode` 枚举（`NONE`/`CONSUMED`/`UNCONSUMED`）管理录音状态
- `resetToIdle()` 统一重置所有 AgentCore 标志位和 UI 状态：

```kotlin
fun resetToIdle() {
    recordMode = RecordMode.NONE
    AgentCore.isMicrophoneMuted = true    // 静音麦克风
    AgentCore.isDisablePlan = true         // 禁用大模型自动规划
    AgentCore.isEnableVoiceBar = false     // 隐藏语音条
    // 恢复按钮状态...
}
```

- `PageAgent.setOnTranscribeListener` 监听 ASR/TTS 回调，实时更新输入框文本
- 按钮互斥：进入一种录音模式后，另一种模式按钮自动禁用

**语音条控制（`isEnableVoiceBar`）：**

语音条是系统默认的语音交互 UI 动画。本页面通过两个播报按钮分别演示显示和隐藏语音条的效果：

| 属性 | 说明 |
|------|------|
| `AgentCore.isEnableVoiceBar = true` | 播报时显示系统语音条动画 |
| `AgentCore.isEnableVoiceBar = false` | 播报时不显示语音条，适合自定义 UI 场景 |

**已消费 vs 未消费模式对比：**

这是 AgentOS SDK 中最核心的概念之一，决定了语音识别结果的流向和大模型是否参与处理：

```
已消费模式（Consumed）：
用户说话 → ASR识别 → onASRResult返回true → 文本显示在输入框 → 结束
                                            ↑                    ↑
                                       页面自行处理          大模型无感知
                                                          系统不显示字幕

适用场景：表单填写、搜索输入、语音指令等需要APP自行处理语音的场景

未消费模式（Unconsumed）：
用户说话 → ASR识别 → onASRResult返回false → 文本显示在输入框 → 大模型接收
                                            ↑                    ↓
                                        仅监听不拦截         自动规划Action
                                        系统正常显示字幕       → TTS语音回复

适用场景：对话交互、智能问答、情感响应等需要大模型参与的场景
```

| 对比项 | 已消费（Consumed） | 未消费（Unconsumed） |
|--------|-------------------|---------------------|
| `onASRResult` 返回值 | `true` | `false` |
| `isDisablePlan` | `true`（禁用规划） | `false`（启用规划） |
| 大模型是否收到语音 | 否 | 是 |
| 大模型是否自动回复 | 否 | 是（通过 Action 规划） |
| 系统字幕条 | 不显示 | 正常显示 |
| 典型场景 | 表单填写、搜索 | 对话交互、智能问答 |

---

#### 5.1.2 TTS 流式播报

> 源码：[`TtsStreamActivity.kt`](app/src/main/java/com/kmq/kmqsamplebody/tts/TtsStreamActivity.kt) | 布局：[`activity_tts_stream.xml`](app/src/main/res/layout/activity_tts_stream.xml)

**功能说明：**

模拟大模型流式文本输出效果的演示页面。点击播报后，左侧文本区域从空白开始逐字显示内容，同时调用 TTS 引擎进行语音播报，视觉上模拟大模型流式生成文本的效果。

**页面功能：**

| 按钮 | 功能 | 关键 API |
|------|------|----------|
| 播报(显示语音条) | 启动流式文字动画 + TTS 播报，显示语音条 | `AgentCore.tts(text, timeout, TTSCallback)` |
| 播报(不显示语音条) | 启动流式文字动画 + TTS 播报，隐藏语音条 | `AgentCore.tts(text, timeout, TTSCallback)` |
| 停止 | 停止流式动画和 TTS 播报，恢复完整文本 | `AgentCore.stopTTS()` + `stopStreamText()` |
| 返回首页 | 返回 `MainActivity` | `FLAG_ACTIVITY_CLEAR_TOP` |

**SDK TTSCallback 机制：**

本页面使用了带 `TTSCallback` 回调的异步 TTS 接口，可以精确监控播报完成状态：

```kotlin
AgentCore.tts(fullText, 180000, object : TTSCallback {
    override fun onTaskEnd(status: Int, result: String?) {
        // status == 1：播报成功完成
        // status == 2：播报失败（被打断、超时或出错）
        // result：失败时包含错误信息
    }
})
```

| 回调状态 | 含义 | 常见触发场景 |
|----------|------|-------------|
| `status = 1` | 播报成功 | 文本正常播报完毕 |
| `status = 2` | 播报失败 | 用户语音打断、调用 `stopTTS()`、超时 |

> **注意：** TTS 播放被任何方式终止或打断时，都会触发失败回调（`status=2`）。用户在播放过程中对机器人说话会自动打断当前播报。

**核心实现：**

- **流式文字动画**：通过 `Handler` + `Runnable` 定时任务实现，每隔固定间隔追加固定字符数
- **自动滚动**：`ScrollView.fullScroll(FOCUS_DOWN)` 保持视图跟随最新文本
- **双通道并行**：文字动画和 TTS 引擎同时启动，视觉效果与语音输出同步进行
- **生命周期安全**：`onDestroy` 中取消定时器、停止 TTS 并清除上下文

```kotlin
private fun startStreamText() {
    stopStreamText()           // 先停止之前的动画
    currentIndex = 0
    isStreaming = true
    tvContent.text = ""

    val runnable = object : Runnable {
        override fun run() {
            if (!isStreaming) return
            val end = (currentIndex + CHUNK_SIZE).coerceAtMost(fullText.length)
            tvContent.text = fullText.substring(0, end)
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
            currentIndex = end
            if (currentIndex < fullText.length) {
                handler.postDelayed(this, STREAM_INTERVAL_MS)
            } else {
                isStreaming = false
            }
        }
    }
    streamRunnable = runnable
    handler.post(runnable)
}
```

**流式输出流程：**

```
点击播报 → 清空文本区 → 启动定时器 ──→ 每60ms追加3字符 → 自动滚动 → 全部显示完毕
                  ↓ 同时
            AgentCore.tts() ──→ 流式音频播放 → TTSCallback.onTaskEnd
                                    ↑
                              用户说话可打断（触发 status=2）
```

**关键参数：**

| 参数 | 值 | 说明 |
|------|-----|------|
| `CHUNK_SIZE` | 3 | 每次追加的字符数，增大可加快文字出现速度 |
| `STREAM_INTERVAL_MS` | 60ms | 追加间隔，减小可加快文字出现速度 |
| TTS `timeoutMillis` | 180000ms | 流式播放的完整超时时间，需根据文本长度合理设置 |

> **SDK 超时说明：** TTS 采用流式播放机制，音频边生成边播放。`timeoutMillis` 是从调用开始到播报结束的总时长限制，设置过短会导致长文本播报被中断。本项目设为 180 秒以适应长篇公司介绍文本。

**与普通 TTS 播报的对比：**

| 对比项 | TTS 播报（TtsActivity） | TTS 流式播报（TtsStreamActivity） |
|--------|------------------------|----------------------------------|
| TTS 调用方式 | `AgentCore.tts(text)` 简单调用 | `AgentCore.tts(text, timeout, callback)` 带回调 |
| 文本显示 | 静态显示在输入框 | 逐字动态显示，模拟流式生成 |
| 播报状态感知 | 无回调 | `TTSCallback.onTaskEnd` 精确感知成功/失败 |
| ASR 录入 | 支持已消费/未消费模式 | 不支持（纯播报演示） |
| 适用场景 | 短文本播报 + 语音交互 | 长文本播报 + 流式视觉效果 |

---

#### 5.1.3 ASR 语音识别

> 源码：[`AsrActivity.kt`](app/src/main/java/com/kmq/kmqsamplebody/asr/AsrActivity.kt) | 布局：[`activity_asr.xml`](app/src/main/res/layout/activity_asr.xml)

**功能说明：**

语音转文字识别演示页面，左侧为识别结果展示区域，右侧为功能按钮组。支持已消费和未消费两种 ASR 模式，实时显示语音识别结果。本页面专注于 ASR 能力演示，不包含 TTS 播报功能。

**页面功能：**

| 按钮 | 功能 | 关键 API |
|------|------|----------|
| 语音录入(已消费) | 开启麦克风，识别结果由页面处理，大模型不回复 | `onASRResult` 返回 `true` + `isDisablePlan = true` |
| 语音录入(未消费) | 开启麦克风，识别结果传递给大模型，大模型自动回复 | `onASRResult` 返回 `false` + `isDisablePlan = false` |
| 停止 | 停止语音录入，静音麦克风，清除上下文 | `isMicrophoneMuted = true` + `clearContext()` |
| 返回首页 | 返回 `MainActivity` | `FLAG_ACTIVITY_CLEAR_TOP` |

**SDK ASR 监听机制：**

ASR 通过 `PageAgent.setOnTranscribeListener` 设置监听器，SDK 会在语音识别过程中持续回调：

```kotlin
pageAgent.setOnTranscribeListener(object : OnTranscribeListener {

    override fun onASRResult(transcription: Transcription): Boolean {
        // transcription.text  — 当前识别到的文本（中间结果会持续更新）
        // transcription.final — 是否为最终结果（true 表示一句话识别完毕）
        runOnUiThread {
            tvResult.text = transcription.text
            if (transcription.`final`) {
                // 最终结果到达，一句话识别完成
                resetToIdle()
            }
        }
        return isConsumed  // true=已消费, false=未消费
    }

    override fun onTTSResult(transcription: Transcription): Boolean {
        return false  // ASR 页面不处理 TTS 事件
    }
})
```

> **线程提醒：** `onASRResult` / `onTTSResult` 回调在**子线程**中执行，更新 UI 必须通过 `runOnUiThread` 切换到主线程。

**ASR 结果类型：**

| `transcription` 属性 | 说明 |
|----------------------|------|
| `.text` | 当前识别文本，中间结果会随着用户持续说话不断更新 |
| `.final = false` | 中间结果，用户仍在说话，文本可能继续变化 |
| `.final = true` | 最终结果，一句话识别完毕，文本不再变化 |

**麦克风控制与免唤醒：**

AgentOS SDK 集成的应用默认在进入前台时自动开启麦克风。本页面通过手动控制实现按需开启：

| API | 说明 |
|-----|------|
| `AgentCore.isMicrophoneMuted = true` | 静音麦克风，停止接收语音输入 |
| `AgentCore.isMicrophoneMuted = false` | 开启麦克风，开始接收语音输入 |
| `AgentCore.isEnableWakeFree` | 免唤醒开关（默认开启）。开启时通过视觉+声学融合算法，仅当用户正对机器人时才拾音，降低多人场景下的交叉干扰 |

**核心实现：**

- 与 `TtsActivity` 共享相同的 `RecordMode` 状态管理和 `resetToIdle()` 逻辑
- **麦克风开启时序至关重要**：必须先完成所有 AgentCore 属性设置，最后才开启麦克风

```kotlin
// ✅ 正确时序：先设置属性，最后开启麦克风
recordMode = RecordMode.CONSUMED
AgentCore.isDisablePlan = true          // 1. 先设置规划开关
AgentCore.isEnableVoiceBar = false      // 2. 再设置语音条
btnVoiceConsumed.text = "停止录入(已消费)" // 3. 更新 UI
AgentCore.isMicrophoneMuted = false     // 4. 最后开启麦克风 ← 必须最后执行

// ❌ 错误时序：先开麦克风，再设置属性
AgentCore.isMicrophoneMuted = false     // 麦克风已开，此时属性未就绪
AgentCore.isDisablePlan = true          // 设置可能不及时生效
```

> **踩坑经验：** 如果麦克风开启后语音录入无响应、无文本输出，首先检查 `isMicrophoneMuted = false` 是否在所有其他属性设置之后执行。

**AgentCore 关键属性联动：**

ASR 功能涉及三个 AgentCore 属性的协同配置，不同模式下的组合如下：

| 属性 | 已消费模式 | 未消费模式 | 空闲/停止 |
|------|-----------|-----------|----------|
| `isMicrophoneMuted` | `false`（开启） | `false`（开启） | `true`（静音） |
| `isDisablePlan` | `true`（禁用规划） | `false`（启用规划） | `true`（禁用） |
| `isEnableVoiceBar` | `false`（隐藏） | `false`（隐藏） | `false`（隐藏） |

**对话上下文管理：**

停止录入时调用 `AgentCore.clearContext()` 清空大模型对话历史，避免残留的上下文影响后续交互。这在切换录音模式或页面销毁时尤为重要：

```kotlin
btnStop.setOnClickListener {
    AgentCore.isDisablePlan = true
    AgentCore.isMicrophoneMuted = true
    AgentCore.clearContext()     // 清空对话历史，确保下次交互不受影响
    resetToIdle()
}
```

**开发注意事项：**

- **时序要求**：开启麦克风前必须先设置 `isDisablePlan` 和 `isEnableVoiceBar`，否则可能出现语音录入无响应
- **模式切换**：切换录音模式时需先调用 `resetToIdle()` 清除前一模式的状态，避免标志位残留
- **资源清理**：`onDestroy` 中需手动静音麦克风并清除上下文，防止后台持续拾音和上下文泄漏
- **子线程回调**：`onASRResult` 在子线程回调，所有 UI 操作必须通过 `runOnUiThread` 执行

---

#### 5.1.4 Action 机制

Action 是 AgentOS SDK 的核心概念，是大模型通过识别用户意图后调用的**技能模块**。当用户通过语音与机器人交互时，大模型会根据 Action 的名称、描述和参数来理解并规划最匹配的 Action 执行。

**本项目中的 Action 用法：**

项目在 [`MainApplication`](app/src/main/java/com/kmq/kmqsamplebody/MainApplication.kt) 中注册了系统内置的 `Actions.SAY`：

```kotlin
override fun onCreate() {
    setPersona("你是科梦奇机器人的示例应用助手，负责展示各项SDK功能。")
    registerAction(Actions.SAY)
}
```

`Actions.SAY` 是机器人的**兜底对话 Action**，注册后大模型可以通过 TTS 直接回答用户问题。如果不注册此 Action，机器人将无法通过语音回复用户。

**Action 注册方式：**

| 注册方式 | 适用范围 | 说明 |
|---------|---------|------|
| 动态注册（App 级） | 应用前台期间全局生效 | 在 `AppAgent.onCreate()` 中调用 `registerAction()` |
| 动态注册（Page 级） | 仅当前页面可见时生效 | 在 `PageAgent` 上调用 `registerAction()` |
| 静态注册 | 可被外部应用调用 | 在 [`actionRegistry.json`](app/src/main/assets/actionRegistry.json) 的 `actionList` 中声明 |

**自定义 Action 示例：**

如需扩展自定义 Action，可参考以下方式在页面中注册：

```kotlin
PageAgent(this).registerAction(
    Action(
        name = "com.kmq.sample.GREET_USER",
        displayName = "打招呼",
        desc = "当用户主动打招呼或问好时，做出友好的回应",
        parameters = listOf(
            Parameter(
                "sentence",
                ParameterType.STRING,
                "回复给用户的话",
                true
            )
        ),
        executor = object : ActionExecutor {
            override fun onExecute(action: Action, params: Bundle?): Boolean {
                AOCoroutineScope.launch {
                    params?.getString("sentence")?.let { AgentCore.ttsSync(it) }
                    action.notify(isTriggerFollowUp = false)
                }
                return true
            }
        }
    )
)
```

**Action 设计要点：**

- **单一职责**：每个 Action 只负责一个明确功能，避免通过参数区分不同业务逻辑
- **描述准确**：`desc` 必须清晰描述功能和使用场景，大模型主要依据此属性选择 Action
- **参数精简**：建议 1-3 个参数，每个参数需有明确描述
- **及时上报**：Action 执行完成后必须调用 `action.notify()` 通知系统

### 5.2 底盘 & 导航能力

| 模块 | Activity | 说明 |
|------|----------|------|
| 底盘移动 | `ChassisActivity` | 底盘前进、后退、左转、右转、停止 |
| 云台控制 | `ChassisActivity` | 云台上仰、下俯、复位（仅豹二、小宝支持） |
| 定位信息 | `LocationActivity` | 定位状态监听、获取当前坐标（x, y, theta） |
| 点位导航 | `PositionActivity` | 获取点位列表、Grid 展示、点击导航到目标点位 |
| 充电管理 | `ChargingActivity` | 自动回充、停止充电、离桩控制 |

#### 5.2.1 底盘控制

**参数说明：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `reqId` | Int | 请求ID，用于日志追踪，自增即可 |
| `speed` | Float | 前进/后退速度（m/s），范围 0~1.0，大于1.0按1.0执行 |
| `distance` | Float | 运动距离（m），值需大于0，到达后自动停止 |
| `avoid` | Boolean | 是否避障（仅 goForward 支持，后退无避障） |
| `speed`（旋转） | Float | 旋转速度（度/s），范围 0~50 |
| `angle` | Float | 旋转角度（度），值需大于0，到达后自动停止 |
| `listener` | CommandListener | 结果/状态回调 |

```kotlin
val listener = object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        // 处理运动结果
    }
}

// === 前进 goForward — 三个重载版本 ===

// 1. 持续前进（不限距离，需手动 stopMove 停止）
RobotApi.getInstance().goForward(reqId, 0.3f, listener)

// 2. 前进指定距离后自动停止（速度 0.3 m/s，距离 1 米）
RobotApi.getInstance().goForward(reqId, 0.3f, 1.0f, listener)

// 3. 前进指定距离 + 避障（avoid=true 遇障碍物自动避停）
RobotApi.getInstance().goForward(reqId, 0.3f, 1.0f, true, listener)

// === 后退 goBackward — 两个重载版本 ===

// 1. 持续后退（不限距离，需手动 stopMove 停止）
RobotApi.getInstance().goBackward(reqId, 0.3f, listener)

// 2. 后退指定距离后自动停止（注意：后退没有避障，请谨慎使用）
RobotApi.getInstance().goBackward(reqId, 0.3f, 1.0f, listener)

// === 左转 turnLeft — 两个重载版本 ===

// 1. 持续左转（不限角度，需手动 stopMove 停止）
//    speed: 旋转速度，单位 度/s，范围 0~50
RobotApi.getInstance().turnLeft(reqId, 20.0f, listener)

// 2. 左转指定角度后自动停止（速度 20 度/s，角度 50°）
RobotApi.getInstance().turnLeft(reqId, 20.0f, 50.0f, listener)

// === 右转 turnRight — 同 turnLeft，两个重载版本 ===
RobotApi.getInstance().turnRight(reqId, 20.0f, listener)
RobotApi.getInstance().turnRight(reqId, 20.0f, 50.0f, listener)

// === 立即停止所有底盘运动 ===
RobotApi.getInstance().stopMove(reqId, listener)
```

#### 5.2.2 云台控制（仅豹二、小宝支持）

```kotlin
// moveHead(reqId, hmode, vmode, hangle, vangle, listener)
// hmode/vmode: "relative"(相对角度) 或 "absolute"(绝对角度)
// 只控制垂直方向时，水平保持不变（hangle=0, hmode="relative"）

// 云台上仰（垂直方向相对上移 10°）
RobotApi.getInstance().moveHead(reqId, "relative", "relative", 0, 10, listener)

// 云台下俯（垂直方向相对下移 10°）
RobotApi.getInstance().moveHead(reqId, "relative", "relative", 0, -10, listener)

// 云台复位（水平和垂直均回到绝对 0° 位置）
RobotApi.getInstance().moveHead(reqId, "absolute", "absolute", 0, 0, listener)
```

> **注意**：云台控制仅适用于 **豹二** 和 **小宝** 机型，其他机型不具备云台硬件，调用将无效。

#### 5.2.3 定位状态与坐标获取

机器人导航的前提是已建图且定位成功。定位相关能力包括：判断是否已定位、监听定位状态变化、获取当前坐标。

##### 判断当前是否已定位

```kotlin
RobotApi.getInstance().isRobotEstimate(reqId, object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        if ("true" == message) {
            // 当前已定位
        } else {
            // 当前未定位
        }
    }
})
```

##### 定位状态监听

通过 `registerStatusListener` 注册 `STATUS_POSE_ESTIMATE` 事件，定位状态发生变化时自动回调：

```kotlin
val statusListener = object : StatusListener() {
    override fun onStatusUpdate(type: String?, data: String?) {
        // data = "0" → 未定位
        // data = "1" → 已定位
    }
}

// 注册监听（定位状态变化时触发）
RobotApi.getInstance().registerStatusListener(
    Definition.STATUS_POSE_ESTIMATE, statusListener
)

// 主动查询一次当前定位状态
RobotApi.getInstance().getRobotStatus(
    Definition.STATUS_POSE_ESTIMATE, statusListener
)

// 页面销毁时务必取消注册
RobotApi.getInstance().unregisterStatusListener(statusListener)
```

##### 获取当前坐标

调用前需确保机器人已定位，返回 JSON 包含 `x`、`y`、`theta`。

```kotlin
RobotApi.getInstance().getPosition(reqId, object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        val json = JSONObject(message ?: "{}")
        val x = json.optDouble("x", 0.0)      // X 坐标（米）
        val y = json.optDouble("y", 0.0)      // Y 坐标（米）
        val theta = json.optDouble("theta", 0.0) // 朝向（弧度）
    }
})
```

##### 设置机器人初始坐标点（手动定位）

当机器人无法自动定位时，可通过 `setPoseEstimate` 手动设置初始位置：

```kotlin
val params = JSONObject().apply {
    put(Definition.JSON_NAVI_POSITION_X, x)
    put(Definition.JSON_NAVI_POSITION_Y, y)
    put(Definition.JSON_NAVI_POSITION_THETA, theta)
}
RobotApi.getInstance().setPoseEstimate(reqId, params.toString(), object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        if ("succeed" == message) {
            // 定位成功
        }
    }
})
```

##### 位置持续上报

通过 `STATUS_POSE` 监听，可实时获取机器人坐标变化（持续上报）：

```kotlin
RobotApi.getInstance().registerStatusListener(
    Definition.STATUS_POSE,
    object : StatusListener() {
        override fun onStatusUpdate(type: String?, data: String?) {
            // data 为 JSON，包含 px, py, theta, name, status, distance 等
        }
    }
)
```

**可监听的状态类型汇总：**

| 状态常量 | 说明 |
|---------|------|
| `STATUS_POSE` | 机器人当前坐标，持续上报 |
| `STATUS_POSE_ESTIMATE` | 定位状态变化时上报（0=未定位，1=已定位） |
| `STATUS_BATTERY` | 电池状态（是否充电、电量、低电量报警等） |

> **注意**：`getPosition` 调用前必须确保已定位，否则返回的坐标无意义。

#### 5.2.4 点位管理

点位是预先在地图上标记的位置，用于导航目标。通过系统自带"地图工具"或 API 完成点位的设置与获取。

##### 获取当前地图所有点位

```kotlin
RobotApi.getInstance().getPlaceList(reqId, object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        val jsonArray = JSONArray(message ?: "[]")
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            val name = json.getString("name")       // 点位名称
            val x = json.getDouble("x")             // X 坐标
            val y = json.getDouble("y")             // Y 坐标
            val theta = json.getDouble("theta")     // 朝向（弧度）
            val id = json.getString("id")           // 点位 ID
            val status = json.getInt("status")      // 0=正常可达, 1=禁行区, 2=地图外
            val time = json.getLong("time")          // 更新时间戳
        }
    }
})
```

**点位状态（status）说明：**

| 值 | 含义 |
|----|------|
| 0 | 正常区域，可以导航到达 |
| 1 | 禁行区，不可到达 |
| 2 | 地图外，不可到达 |

##### 保存当前位置为点位

机器人在目标位置时，调用 `setLocation` 将当前坐标保存为命名点位：

```kotlin
RobotApi.getInstance().setLocation(reqId, "前台", object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        if ("succeed" == message) {
            // 点位保存成功
        }
    }
})
```

> **注意**：调用前需确保已定位。保存的点位与当前地图关联，切换地图后需重新设置。

##### 根据名称获取点位坐标

```kotlin
RobotApi.getInstance().getLocation(reqId, "前台", object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        val json = JSONObject(message ?: "{}")
        val isExist = json.getBoolean(Definition.JSON_NAVI_SITE_EXIST)
        if (isExist) {
            val x = json.getDouble(Definition.JSON_NAVI_POSITION_X)
            val y = json.getDouble(Definition.JSON_NAVI_POSITION_Y)
            val theta = json.getDouble(Definition.JSON_NAVI_POSITION_THETA)
        }
    }
})
```

##### 判断机器人是否在某个点位

```kotlin
val params = JSONObject().apply {
    put(Definition.JSON_NAVI_TARGET_PLACE_NAME, "前台")
    put(Definition.JSON_NAVI_COORDINATE_DEVIATION, 1.0) // 判断范围（米）
}
RobotApi.getInstance().isRobotInlocations(reqId, params.toString(), object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        val json = JSONObject(message ?: "{}")
        val isInLocation = json.getBoolean(Definition.JSON_NAVI_IS_IN_LOCATION)
    }
})
```

##### 获取当前地图名称

```kotlin
RobotApi.getInstance().getMapName(reqId, object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        if (!message.isNullOrEmpty()) {
            val mapName = message // 当前地图名称
        }
    }
})
```

#### 5.2.5 导航

导航是机器人从当前位置自动行走到目标点位的核心能力。行走过程中可自动规划路线、有效避开障碍物。

> **前提条件**：机器人已建图、在该地图上已定位成功、雷达处于开启状态。

##### 导航到指定点位（按名称）

```kotlin
// 方式1：默认导航速度
RobotApi.getInstance().startNavigation(
    reqId,
    "前台",           // destName: 目标点位名称
    0.2,              // coordinateDeviation: 到达判定范围（米），建议 0.2
    30000L,           // time: 避障超时时间（ms），建议 30000
    navigationListener
)

// 方式2：指定导航速度
RobotApi.getInstance().startNavigation(
    reqId,
    "前台",           // destName
    0.2,              // coordinateDeviation
    30000L,           // time
    0.7f,             // linearSpeed: 线速度 0.1~0.85 m/s，默认 0.7
    1.2f,             // angularSpeed: 角速度 0.4~1.4 m/s，默认 1.2
    navigationListener
)

// 方式3：指定避障距离（V10+ 支持）
RobotApi.getInstance().startNavigation(
    reqId,
    "前台",           // destName
    0.2,              // coordinateDeviation
    0.75,             // obsDistance: 最大避障距离（米），默认 0.75
    30000L,           // time
    navigationListener
)
```

**参数说明：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `destName` | String | 目标点位名称（必须通过 setLocation 或地图工具预设） |
| `coordinateDeviation` | Double | 到达判定范围（米），距目标在此范围内视为到达，建议 0.2 |
| `obsDistance` | Double | 最大避障距离，障碍物距目标小于该值时停止，默认 0.75 米 |
| `time` | Long | 避障超时（ms），在此时间内移动距离不超 0.1m 则判定失败，建议 30000 |
| `linearSpeed` | Float | 导航线速度，范围 0.1~0.85 m/s，默认 0.7 |
| `angularSpeed` | Float | 导航角速度，范围 0.4~1.4 m/s，默认 1.2 |

> **速度建议**：线速度和角速度应保持 `angularSpeed = 0.4 + (linearSpeed - 0.1) / 3 * 4` 的关系。

##### 导航回调监听

```kotlin
val navigationListener = object : ActionListener() {
    override fun onResult(status: Int, responseString: String?) {
        when (status) {
            Definition.RESULT_OK -> {
                if ("true" == responseString) { /* 导航成功 */ }
                else { /* 导航失败 */ }
            }
            Definition.ACTION_RESPONSE_STOP_SUCCESS -> { /* 取消导航成功 */ }
        }
    }

    override fun onStatusUpdate(status: Int, data: String?) {
        when (status) {
            Definition.STATUS_START_NAVIGATION -> { /* 开始导航 */ }
            Definition.STATUS_NAVI_AVOID -> { /* 遇障碍物，开始避障 */ }
            Definition.STATUS_NAVI_AVOID_END -> { /* 障碍物移除，避障结束 */ }
            Definition.STATUS_NAVI_GO_STRAIGHT -> { /* 直线行走中 */ }
            Definition.STATUS_NAVI_TURN_LEFT -> { /* 左转弯中 */ }
            Definition.STATUS_NAVI_TURN_RIGHT -> { /* 右转弯中 */ }
        }
    }

    override fun onError(errorCode: Int, errorString: String?) {
        when (errorCode) {
            Definition.ERROR_NOT_ESTIMATE -> { /* 当前未定位 */ }
            Definition.ERROR_IN_DESTINATION -> { /* 已在目的地范围内 */ }
            Definition.ERROR_DESTINATION_NOT_EXIST -> { /* 目的地不存在 */ }
            Definition.ERROR_DESTINATION_CAN_NOT_ARRAIVE -> { /* 避障超时，目的地不可达 */ }
            Definition.ACTION_RESPONSE_ALREADY_RUN -> { /* 导航已在运行，请先停止 */ }
            Definition.ACTION_RESPONSE_REQUEST_RES_ERROR -> { /* 底盘已被其他接口占用 */ }
        }
    }
}
```

**导航状态码：**

| 状态码 | 常量 | 说明 |
|--------|------|------|
| 1014 | `STATUS_START_NAVIGATION` | 开始导航 |
| 1018 | `STATUS_NAVI_AVOID` | 开始避障 |
| 1019 | `STATUS_NAVI_AVOID_END` | 避障结束 |
| 1020 | `STATUS_NAVI_OUT_MAP` | 走出地图，建议停止导航 |
| 1025 | `STATUS_NAVI_GLOBAL_PATH_FAILED` | 路径规划失败 |
| 1036 | `STATUS_NAVI_GO_STRAIGHT` | 直线行走 |
| 1037 | `STATUS_NAVI_TURN_LEFT` | 左转弯 |
| 1038 | `STATUS_NAVI_TURN_RIGHT` | 右转弯 |

**导航错误码：**

| 错误码 | 常量 | 说明 |
|--------|------|------|
| -108 | `ERROR_DESTINATION_NOT_EXIST` | 目的地不存在 |
| -109 | `ERROR_DESTINATION_CAN_NOT_ARRAIVE` | 避障超时，目的地不可达 |
| -113 | `ERROR_IN_DESTINATION` | 已在目的地范围内 |
| -116 | `ERROR_NOT_ESTIMATE` | 当前未定位 |
| -1 | `ACTION_RESPONSE_ALREADY_RUN` | 接口已调用，请先停止 |
| -6 | `ACTION_RESPONSE_REQUEST_RES_ERROR` | 底盘被其他接口占用 |

##### 停止导航

```kotlin
RobotApi.getInstance().stopNavigation(reqId)
```

> **注意**：`stopNavigation` 只能停止 `startNavigation` 启动的导航，不能停止 `goPosition` 启动的导航（两者的停止接口不可混用）。

##### 导航到指定坐标点

当目标位置不是预设点位而是坐标时，可使用 `goPosition`（注意：官方建议优先使用 `startNavigation`）：

```kotlin
val position = JSONObject().apply {
    put(Definition.JSON_NAVI_POSITION_X, x)
    put(Definition.JSON_NAVI_POSITION_Y, y)
    put(Definition.JSON_NAVI_POSITION_THETA, theta)
}
RobotApi.getInstance().goPosition(reqId, position.toString(), listener)

// 停止坐标导航（必须使用对应的停止方法）
RobotApi.getInstance().stopGoPosition(reqId)
```

##### 转向目标点方向

只转动到目标点的方向，不实际移动：

```kotlin
RobotApi.getInstance().resumeSpecialPlaceTheta(reqId, "前台", object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        // result: 0=执行, -1=未执行
    }
})
```

#### 5.2.6 充电管理

##### 获取当前电量

```kotlin
val batteryInfo = RobotSettingApi.getInstance()
    .getRobotString(Definition.ROBOT_SETTINGS_BATTERY_INFO)
```

##### 自动回充

导航至充电桩并开始充电：

```kotlin
RobotApi.getInstance().startNaviToAutoChargeAction(
    reqId,
    60000L, // timeout: 导航超时时间（ms），超时则回充失败
    object : ActionListener() {
        override fun onResult(status: Int, responseString: String?) {
            when (status) {
                Definition.RESULT_OK -> { /* 充电成功 */ }
                Definition.RESULT_FAILURE -> { /* 充电失败 */ }
            }
        }

        override fun onStatusUpdate(status: Int, data: String?) {
            when (status) {
                Definition.STATUS_NAVI_GLOBAL_PATH_FAILED -> { /* 路径规划失败 */ }
                Definition.STATUS_NAVI_OUT_MAP -> { /* 充电桩在地图外 */ }
                Definition.STATUS_NAVI_AVOID -> { /* 路线被障碍物堵死 */ }
                Definition.STATUS_NAVI_AVOID_END -> { /* 障碍物已移除 */ }
            }
        }
    }
)
```

##### 停止自动回充

```kotlin
RobotApi.getInstance().stopAutoChargeAction(reqId, true)
```

##### 停止充电并脱离充电桩

```kotlin
// 必须先禁用系统充电界面
RobotApi.getInstance().disableBattery()

// 然后离桩
RobotApi.getInstance().leaveChargingPile(
    reqId,
    0.7f,  // speed: 离桩前进速度，默认 0.7
    0.2f,  // distance: 离桩前进距离（米），默认 0.2
    object : CommandListener() {
        override fun onResult(result: Int, message: String?, extraData: String?) {
            if (result == Definition.RESULT_OK) {
                // 离桩成功
            }
        }

        override fun onError(errorCode: Int, errorString: String?, extraData: String?) {
            when (errorCode) {
                Definition.RESULT_FAILURE_MOTION_AVOID_STOP -> { /* 前方有障碍，离桩失败 */ }
                Definition.RESULT_FAILURE_TIMEOUT -> { /* 离桩超时（15s） */ }
            }
        }
    }
)
```

##### 监听电池状态

```kotlin
RobotApi.getInstance().registerStatusListener(
    Definition.STATUS_BATTERY,
    object : StatusListener() {
        override fun onStatusUpdate(type: String?, data: String?) {
            // data 包含充电状态、电量百分比、低电量报警等信息
        }
    }
)
```

> **注意**：线充方式的机器人无法使用 `leaveChargingPile` 脱离充电。

### 5.3 视觉能力

| 模块 | Activity | 说明 |
|------|----------|------|
| Camera2 原生摄像头 | `CameraActivity` | Android Camera2 API 打开摄像头预览、前后切换 |
| 摄像头数据流共享 | `CameraActivity` | 通过 SurfaceShareApi 获取 VisionSDK 共享流，不影响视觉能力 |
| 人脸检测与识别 | `CameraActivity` | PersonApi 检测视野内人员、获取人脸信息、注册识别 |

> **重要冲突说明**：使用 Android Camera2 API 打开摄像头期间，机器人视觉能力（人脸检测、人体检测等）将暂时不可用。释放摄像头后一段时间可自动恢复。如果既需要摄像头画面又不想影响视觉能力，请使用**数据流共享**方式。

#### 5.3.1 Camera2 原生摄像头

使用 Android 标准 Camera2 API 打开摄像头并在 `SurfaceView` 上显示预览画面，支持前后摄像头切换。

**机型摄像头说明：**

| 机型 | 前置摄像头 | 后置摄像头 |
|------|-----------|-----------|
| 豹二 / 小宝 / 小鱼二代 / 小科 | 双摄，支持 Camera2 或共享流 | 仅支持 Camera2 |

**使用注意事项：**

- 首次授权 APP 使用摄像头时可能崩溃，重新开启即可，之后可正常使用
- 横屏机器人通过 Camera2 打开后方向旋转 90°（机器人无陀螺仪，默认竖屏），需手动设置旋转 270° 纠正
- 使用 Camera2 期间视觉能力（人脸检测等）暂不可用

```kotlin
// Camera2 核心流程：
// 1. SurfaceView + SurfaceHolder.Callback 等待 Surface 就绪
// 2. CameraManager.openCamera() 打开摄像头
// 3. CameraDevice.createCaptureSession() 创建预览会话
// 4. CaptureRequest.Builder 配置预览参数并启动

val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

// 枚举前后摄像头
for (id in cameraManager.cameraIdList) {
    val facing = cameraManager.getCameraCharacteristics(id)
        .get(CameraCharacteristics.LENS_FACING)
    // LENS_FACING_FRONT = 前置, LENS_FACING_BACK = 后置
}

// 打开摄像头
cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
    override fun onOpened(camera: CameraDevice) {
        // 创建预览会话
        val surface = surfaceView.holder.surface
        val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        requestBuilder.addTarget(surface)
        camera.createCaptureSession(listOf(surface), sessionCallback, handler)
    }
    override fun onDisconnected(camera: CameraDevice) { camera.close() }
    override fun onError(camera: CameraDevice, error: Int) { camera.close() }
}, handler)

// 关闭摄像头
captureSession?.close()
cameraDevice?.close()
```

##### 通过 SDK 切换视觉摄像头

此方法切换的是 VisionSDK 使用的摄像头（影响人脸检测方向），不影响 Camera2：

```kotlin
// 切换到前置摄像头
RobotApi.getInstance().switchCamera(reqId, Definition.JSON_HEAD_FORWARD, listener)

// 切换到后置摄像头
RobotApi.getInstance().switchCamera(reqId, Definition.JSON_HEAD_BACKWARD, listener)
```

**参数说明：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `camera` | String | `Definition.JSON_HEAD_FORWARD`（前置）或 `Definition.JSON_HEAD_BACKWARD`（后置） |

#### 5.3.2 摄像头数据流共享（SurfaceShare）

通过 `SurfaceShareApi` 获取 VisionSDK 的摄像头数据流，**不占用摄像头、不影响机器人视觉能力**。适用于既需要获取摄像头画面又需要保留人脸检测能力的场景。

**核心 API：**

| API | 说明 |
|-----|------|
| `SurfaceShareApi.getInstance().requestImageFrame(surface, bean, listener)` | 开始共享数据流 |
| `SurfaceShareApi.getInstance().abandonImageFrame(bean)` | 停止共享数据流 |

**数据格式：** 共享流输出 `YUV_420_888` 格式图像，需转换为 Bitmap 才能渲染到 ImageView。

```kotlin
// 1. 创建 ImageReader 接收图像帧
val imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 4)
imageReader.setOnImageAvailableListener({ reader ->
    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
    try {
        // 将 YUV 数据转换为 Bitmap 显示
        val bitmap = yuvToBitmap(image)
        runOnUiThread { imageView.setImageBitmap(bitmap) }
    } finally {
        image.close() // 必须释放，否则极易引起 OOM
    }
}, backgroundHandler)

// 2. 配置共享参数
val surfaceShareBean = SurfaceShareBean().apply { name = "MyApp" }

// 3. 开始请求共享流
SurfaceShareApi.getInstance().requestImageFrame(
    imageReader.surface, surfaceShareBean,
    object : SurfaceShareListener() {
        override fun onError(error: Int, message: String?) {
            // error == SurfaceShareError.ERROR_SURFACE_SHARE_USED 表示共享流已被其他应用占用
        }
        override fun onStatusUpdate(status: Int, message: String?) {
            // 状态更新
        }
    }
)

// 4. 停止共享流（不使用时务必关闭，避免内存和 CPU 开销）
SurfaceShareApi.getInstance().abandonImageFrame(surfaceShareBean)
imageReader.close()
```

**YUV → Bitmap 转换核心逻辑：**

```kotlin
fun yuvImageToBitmap(image: Image): Bitmap {
    val width = image.width
    val height = image.height
    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]
    val argb = IntArray(width * height)

    for (j in 0 until height) {
        for (i in 0 until width) {
            val yIndex = j * yPlane.rowStride + i
            val uvIndex = (j / 2) * uPlane.rowStride + (i / 2) * uPlane.pixelStride
            val y = (yPlane.buffer.get(yIndex).toInt() and 0xFF) - 16
            val u = (uPlane.buffer.get(uvIndex).toInt() and 0xFF) - 128
            val v = (vPlane.buffer.get(uvIndex).toInt() and 0xFF) - 128
            val yy = maxOf(y, 0)
            val r = (1.164f * yy + 1.596f * v).toInt().coerceIn(0, 255)
            val g = (1.164f * yy - 0.813f * v - 0.391f * u).toInt().coerceIn(0, 255)
            val b = (1.164f * yy + 2.018f * u).toInt().coerceIn(0, 255)
            argb[j * width + i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    return Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)
}
```

> **注意**：共享流的内存和 CPU 开销较大，不使用时必须调用 `abandonImageFrame` 关闭并释放 `ImageReader`，避免 OOM 崩溃。

#### 5.3.3 人脸检测与识别

视觉能力包括**人员检测**（本地能力）和**人脸识别**（需联网）。API 主要集中在 `PersonApi` 和 `RobotApi`。

**检测原理：** 人站在机器人前方时，机器人可检测到人员信息。较远时人脸和人体均可检测到，较近时只能检测到人脸。当 `Person.id >= 0` 时，表示该人员的人脸信息完整，可获取人脸照片进行识别。

> **注意**：使用视觉能力时不可同时使用 Camera2 打开摄像头，否则视觉能力报错失效。

##### Person 主要属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `id` | Int | 人脸本地识别 ID，可用于焦点跟随等 |
| `distance` | Double | 人员距离机器人的距离（米） |
| `faceAngleX` | Double | 人脸 X 轴角度 |
| `faceAngleY` | Double | 人脸 Y 轴角度 |
| `faceX` / `faceY` | Int | 人脸在画面中的坐标 |
| `facewidth` / `faceheight` | Int | 人脸在画面中的尺寸 |
| `bodyX` / `bodyY` | Int | 身体在画面中的坐标 |
| `bodywidth` / `bodyheight` | Int | 身体在画面中的尺寸 |
| `angleInView` | Double | 人相对于机器人头部的角度 |
| `remoteFaceId` | String | 已注册的人脸远端 ID（未注册为空） |
| `age` | Int | 估算年龄（云端注册后返回） |
| `gender` | String | 性别（需授权注册后返回） |

##### 注册人员变化监听

```kotlin
val personListener = object : PersonListener() {
    override fun personChanged() {
        super.personChanged()
        // 人员变化时获取当前视野内所有人员
        val persons = PersonApi.getInstance().getAllPersons()
    }
}

// 注册监听
PersonApi.getInstance().registerPersonListener(personListener)

// 取消监听（页面销毁时务必调用）
PersonApi.getInstance().unregisterPersonListener(personListener)
```

##### 获取人员列表

```kotlin
// 获取视野内全部人员
val allPersons: List<Person> = PersonApi.getInstance().getAllPersons()

// 获取指定距离内的人员（单位：米，最佳识别距离 1~3 米）
val nearPersons: List<Person> = PersonApi.getInstance().getAllPersons(3)

// 获取有人脸信息的人员
val faceList: List<Person> = PersonApi.getInstance().getAllFaceList()

// 获取有人体信息的人员
val bodyList: List<Person> = PersonApi.getInstance().getAllBodyList()

// 获取有完整人脸信息的人员（id >= 0，可用于注册/识别）
val completeFaceList: List<Person> = PersonApi.getInstance().getCompleteFaceList()

// 获取正在焦点跟随的人员（仅在焦点跟随进行中有效）
val focusPerson: Person? = PersonApi.getInstance().getFocusPerson()
```

##### 获取人脸照片

```kotlin
RobotApi.getInstance().getPictureById(reqId, person.id, 1, object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        val json = JSONObject(message ?: "{}")
        if (Definition.RESPONSE_OK == json.optString("status")) {
            val pictures = json.optJSONArray("pictures")
            val picturePath = pictures?.optString(0) // 照片本地存储全路径
            // 使用完后需手动删除该文件
        }
    }
})
```

> **注意**：`getPictureById` 保存的图片在使用完后需要手动删除。

##### 自动注册人脸

从摄像头实时获取人脸并注册：

```kotlin
RobotApi.getInstance().startRegister(
    reqId,
    "张三",        // personName: 注册名称
    20000,         // timeout: 注册超时时间（ms）
    5,             // tryCount: 失败重试次数
    2,             // secondDelay: 重试间隔（秒）
    object : ActionListener() {
        override fun onResult(status: Int, response: String?) {
            if (Definition.RESULT_OK != status) {
                // 注册失败
                return
            }
            val json = JSONObject(response ?: "{}")
            val remoteType = json.optString(Definition.REGISTER_REMOTE_TYPE)
            when (remoteType) {
                Definition.REGISTER_REMOTE_SERVER_EXIST -> { /* 用户已存在 */ }
                Definition.REGISTER_REMOTE_SERVER_NEW -> { /* 新注册成功 */ }
            }
        }
    }
)
```

> **注意**：不要重复注册同一个人脸。

##### 远程识别

使用人脸照片向云端查询人员信息：

```kotlin
RobotApi.getInstance().getPersonInfoFromNet(
    reqId, person.id, facePictures,
    object : CommandListener() {
        override fun onResult(result: Int, message: String?) {
            val json = JSONObject(message ?: "{}")
            val info = json.optJSONObject("people")
            val name = info?.optString("name")       // 姓名
            val gender = info?.optString("gender")   // 性别
            val age = info?.optString("age")         // 年龄
        }
    }
)
```

##### 焦点跟随

根据指定的人脸 ID 持续跟踪目标，头部云台跟随转动，底盘联动：

```kotlin
RobotApi.getInstance().startFocusFollow(
    reqId,
    person.id,     // faceId: 跟随目标的人脸 ID
    10,            // lostTimeout: 丢失超时（秒），建议 5~10
    5,             // maxDistance: 超距报警距离（米），建议 5
    object : ActionListener() {
        override fun onStatusUpdate(status: Int, data: String?) {
            when (status) {
                Definition.STATUS_TRACK_TARGET_SUCCEED -> { /* 跟随目标成功 */ }
                Definition.STATUS_GUEST_LOST -> { /* 跟随目标丢失 */ }
                Definition.STATUS_GUEST_FARAWAY -> { /* 目标超出设定距离 */ }
                Definition.STATUS_GUEST_APPEAR -> { /* 目标重新进入范围 */ }
            }
        }
        override fun onError(errorCode: Int, errorString: String?) {
            when (errorCode) {
                Definition.ERROR_SET_TRACK_FAILED,
                Definition.ERROR_TARGET_NOT_FOUND -> { /* 目标未找到 */ }
                Definition.ACTION_RESPONSE_ALREADY_RUN -> { /* 已在跟随中，请先停止 */ }
                Definition.ACTION_RESPONSE_REQUEST_RES_ERROR -> { /* 底盘被其他接口占用 */ }
            }
        }
    }
)

// 停止焦点跟随
RobotApi.getInstance().stopFocusFollow(reqId)
```

> **注意**：焦点跟随会占用底盘资源，不可同时执行导航等底盘操作。不要反复调用来跟踪同一个人，只需在丢失或停止后重新启动。

### 5.4 系统信息与控制

| 模块 | Activity | 说明 |
|------|----------|------|
| 系统版本获取 | `SystemInfoActivity` | 获取系统版本号（`getVersion`） |
| 设备 SN 获取 | `SystemInfoActivity` | 获取设备唯一序列号（`getRobotSn`） |
| 电池信息 | `SystemInfoActivity` | 获取电池快照 + 注册实时电池状态监听 |
| 禁止系统功能 | — | 禁用急停画面 / 充电界面 / 功能键 |
| 休眠功能 | — | 低功耗待机模式的开始与结束 |
| 唤醒 | — | 根据声源方位转动到用户角度 |
| 充电控制 | — | 自动回充、停止回充、离桩 |

> 源码：[`SystemInfoActivity.kt`](app/src/main/java/com/kmq/kmqsamplebody/systeminfo/SystemInfoActivity.kt) | 布局：[`activity_system_info.xml`](app/src/main/res/layout/activity_system_info.xml)

#### 5.4.1 系统版本与 SN 获取

##### 获取系统版本号

**方法名称：** `getVersion`

同步方法，直接返回版本号字符串。

```kotlin
val version: String? = RobotApi.getInstance().version
```

##### 获取本机 SN

**方法名称：** `getRobotSn`

异步方法，通过 `CommandListener` 回调返回设备序列号。

```kotlin
RobotApi.getInstance().getRobotSn(object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        if (result == Definition.RESULT_OK) {
            val sn = message // 设备序列号
        }
    }
})
```

**返回值说明：**

| 字段 | 说明 |
|------|------|
| `result` | `1`（RESULT_OK）= 命令执行成功，`-1` = 未执行 |
| `message` | 设备序列号字符串 |

#### 5.4.2 电池信息与监听

##### 获取当前电量（快照）

```kotlin
val batteryInfo: String? = RobotSettingApi.getInstance()
    .getRobotString(Definition.ROBOT_SETTINGS_BATTERY_INFO)
```

返回 JSON 字符串，包含当前电量百分比、是否正在充电等信息。

##### 注册电池状态实时监听

通过 `STATUS_BATTERY` 监听，电池状态变化时（充电开始/结束、电量变化、低电量报警）自动回调：

```kotlin
val batteryListener = object : StatusListener() {
    override fun onStatusUpdate(type: String?, data: String?) {
        // data 包含充电状态、电量百分比、低电量报警等信息
    }
}

RobotApi.getInstance().registerStatusListener(Definition.STATUS_BATTERY, batteryListener)

// 页面销毁时务必取消注册
RobotApi.getInstance().unregisterStatusListener(batteryListener)
```

**可监听的状态类型汇总：**

| 状态常量 | 说明 |
|---------|------|
| `STATUS_BATTERY` | 电池状态（是否充电、电量、低电量报警等） |
| `STATUS_EMERGENCY` | 急停状态 |

#### 5.4.3 禁止系统功能

SDK 提供多个接口用于禁用机器人系统级默认行为，适用于需要自定义 UI 或接管系统行为的场景。

##### 禁用急停画面

系统不再接管急停事件，没有急停画面，可用于用户自定义急停画面。

**方法名称：** `disableEmergency`

```kotlin
RobotApi.getInstance().disableEmergency()
```

> **注意**：急停状态下，所有底盘相关功能 API 不可使用，唤醒、休眠等切换机器人状态的 API 也不生效。

**获取急停状态：**

```kotlin
// 主动查询
RobotApi.getInstance().getRobotStatus(Definition.STATUS_EMERGENCY, statusListener)

// 注册监听
RobotApi.getInstance().registerStatusListener(Definition.STATUS_EMERGENCY, statusListener)
```

##### 禁用电池界面

禁用当前电池界面，充电时可使用除底盘外 APP 任何能力。如果确认不需要充电接管，推荐在 APP 启动连上 RobotAPI 成功之后直接调用此接口禁用，此后直到 APP 退出，充电接管画面都会处于禁用状态。

**方法名称：** `disableBattery`

```kotlin
RobotApi.getInstance().disableBattery()
```

##### 禁用功能键

禁用机器人头部后面的物理按钮。

**方法名称：** `disableFunctionKey`

```kotlin
RobotApi.getInstance().disableFunctionKey()
```

#### 5.4.4 休眠功能

休眠是让机器人在没有任务或者低电量的时候，保持低功耗运行的一种模式。

> **注意**：使用休眠 API 需要添加权限：
> ```xml
> <uses-permission android:name="com.ainirobot.coreservice.robotSettingProvider" />
> ```

##### 开始休眠

**方法名称：** `robotStandby`

```kotlin
RobotApi.getInstance().robotStandby(0, object : CommandListener() {
    override fun onStatusUpdate(status: Int, data: String?, extraData: String?) {
        // 休眠状态更新
    }
})
```

##### 停止休眠

**方法名称：** `robotStandbyEnd`

```kotlin
RobotApi.getInstance().robotStandbyEnd(reqId)
```

#### 5.4.5 唤醒

唤醒场景指机器人根据唤醒词呼唤的声源方位，控制机器人转动到用户角度。

**不同机型策略：**

| 机型 | 唤醒策略 |
|------|---------|
| 豹二 | 角度 < 45° 只转动头部云台，> 45° 头部云台及底盘均会转动 |
| 小宝 | 根据声源方位转动底盘 |

##### 声源定位

每次唤醒机器人时，可获取唤醒人和机器人之间的夹角（声源定位角度）。在 `ModuleCallback` 的 `onSendRequest` 回调中获得，`reqType` 为 `Definition.REQ_SPEECH_WAKEUP` 时，`reqParams` 即为声源定位角度。

##### 开始唤醒

**方法名称：** `wakeUp`

```kotlin
RobotApi.getInstance().wakeUp(reqId, angle, object : ActionListener() {
    override fun onResult(status: Int, responseString: String?) {
        when (status) {
            Definition.RESULT_OK -> { /* 唤醒完成 */ }
            Definition.ACTION_RESPONSE_STOP_SUCCESS -> { /* 主动调用 stopWakeUp 停止 */ }
        }
    }

    override fun onError(errorCode: Int, errorString: String?) {
        when (errorCode) {
            Definition.ERROR_MOVE_HEAD_FAILED -> { /* 头部云台移动失败 */ }
            Definition.ACTION_RESPONSE_ALREADY_RUN -> { /* 当前正在唤醒中，需先停止 */ }
            Definition.ACTION_RESPONSE_REQUEST_RES_ERROR -> { /* 底盘被其他接口占用 */ }
        }
    }
})
```

**参数说明：**

| 参数 | 类型 | 说明 |
|------|------|------|
| `angle` | Int | 声源定位角度，从 `ModuleCallback` 回调获取 |

##### 停止唤醒

**方法名称：** `stopWakeUp`

```kotlin
RobotApi.getInstance().stopWakeUp(reqId)
```

#### 5.4.6 充电控制

##### 开始自动回充

导航至充电桩并开始充电。

**方法名称：** `startNaviToAutoChargeAction`

```kotlin
RobotApi.getInstance().startNaviToAutoChargeAction(reqId, timeout, object : ActionListener() {
    override fun onResult(status: Int, responseString: String?) {
        when (status) {
            Definition.RESULT_OK -> { /* 充电成功 */ }
            Definition.RESULT_FAILURE -> { /* 充电失败 */ }
        }
    }

    override fun onStatusUpdate(status: Int, data: String?) {
        when (status) {
            Definition.STATUS_NAVI_GLOBAL_PATH_FAILED -> { /* 路径规划失败 */ }
            Definition.STATUS_NAVI_OUT_MAP -> { /* 充电桩在地图外 */ }
            Definition.STATUS_NAVI_AVOID -> { /* 路线被障碍物堵死 */ }
            Definition.STATUS_NAVI_AVOID_END -> { /* 障碍物已移除 */ }
        }
    }
})
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `timeout` | Long | 导航超时时间（ms），超时则回充失败 |

##### 停止自动回充

**方法名称：** `stopAutoChargeAction`

```kotlin
RobotApi.getInstance().stopAutoChargeAction(reqId, true)
```

##### 停止充电并脱离充电桩

**方法名称：** `leaveChargingPile`

> **前置条件**：必须先调用 `RobotApi.getInstance().disableBattery()` 禁用系统充电界面，否则无法脱离。

```kotlin
RobotApi.getInstance().disableBattery()

RobotApi.getInstance().leaveChargingPile(reqId, speed, distance, object : CommandListener() {
    override fun onResult(result: Int, message: String?, extraData: String?) {
        when (result) {
            Definition.RESULT_OK -> { /* 离桩成功 */ }
        }
    }

    override fun onError(errorCode: Int, errorString: String?, extraData: String?) {
        when (errorCode) {
            Definition.RESULT_FAILURE_MOTION_AVOID_STOP -> { /* 前方有障碍，离桩失败 */ }
            Definition.RESULT_FAILURE_TIMEOUT -> { /* 离桩超时（15s） */ }
            Definition.STATUS_LEAVE_PILE_OPEN_RADAR_FAILURE -> { /* 雷达启动失败 */ }
        }
    }
})
```

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `speed` | Float | 0.7 | 离桩前进速度 |
| `distance` | Float | 0.2 | 离桩前进距离（米） |

> **注意**：线充方式的机器人无法使用此方法脱离充电。离桩成功后如需判断充电状态，注意充电状态更新有一定延迟。

#### 5.4.7 通用错误码速查

以下错误码在导航、充电、唤醒、焦点跟随等多个 API 中通用。

##### 通用 onResult 状态码

| 常量 | Code | 说明 |
|------|------|------|
| `RESULT_OK` | 1 | 操作成功 |
| `RESULT_FAILURE` | 2 | 操作失败 |
| `ACTION_RESPONSE_STOP_SUCCESS` | 3 | 主动停止成功 |
| `RESULT_NAVIGATION_ARRIVED` | 102 | 到达目的地 |
| `RESULT_DESTINATION_AVAILABLE` | 103 | 目的地可达，未被占用 |
| `RESULT_DESTINATION_IN_RANGE` | 104 | 到达目的地范围内 |

##### 通用 onError 错误码

| 常量 | Code | 说明 |
|------|------|------|
| `ACTION_RESPONSE_ALREADY_RUN` | -1 | 当前接口已在执行，请先停止再调用 |
| `ACTION_RESPONSE_REQUEST_RES_ERROR` | -6 | 底盘已被其他接口占用，请先停止 |
| `ACTION_RESPONSE_RES_UNAVAILBALE` | -9 | 请求不可用（如雷达未启动） |
| `ERROR_PARAMETER` | -102 | 参数错误 |
| `ERROR_TARGET_NOT_FOUND` | -107 | 未找到目标点位 |
| `ERROR_DESTINATION_NOT_EXIST` | -108 | 目的地不存在 |
| `ERROR_DESTINATION_CAN_NOT_ARRAIVE` | -109 | 避障超时，目的地不可达 |
| `ERROR_IN_DESTINATION` | -113 | 已在目的地范围内 |
| `ERROR_NOT_ESTIMATE` | -116 | 当前未定位 |
| `ERROR_NAVIGATION_FAILED` | -120 | 导航因其他原因失败（兜底） |
| `ERROR_WHEEL_OVER_CURRENT_RUN_OUT` | -124 | 轮子堵转重试次数用完 |
| `ERROR_MULTI_ROBOT_WAITING_TIMEOUT` | -125 | 多机调度等待超时 |
| `ERROR_MULTIPLE_MODE_ERROR` | -127 | 多机信息错误 |
| `ERROR_ESTIMATE_ERROR` | -128 | 重定位失败 |
| `ERROR_NO_AVAILABLE_DESTINATION` | -129 | 未找到可用的目的地 |
| `ERROR_NAVIGATION_AVOID_TIMEOUT` | -136 | 机器人避障超时 |
| `ERROR_MOVE_HEAD_FAILED` | — | 头部云台移动失败 |

##### 导航 onStatusUpdate 状态码

| 常量 | Code | 说明 |
|------|------|------|
| `STATUS_START_NAVIGATION` | 1014 | 导航开始 |
| `STATUS_START_CRUISE` | 1015 | 巡航开始 |
| `STATUS_GOAL_OCCLUDED` | 1016 | 避障开始 |
| `STATUS_GOAL_OCCLUDED_END` | 1017 | 避障结束 |
| `STATUS_NAVI_AVOID` | 1018 | 避障（路线被堵） |
| `STATUS_NAVI_AVOID_END` | 1019 | 避障结束（障碍物移除） |
| `STATUS_NAVI_OUT_MAP` | 1020 | 走出地图范围，建议停止导航 |
| `STATUS_NAVI_OBSTACLES_AVOID` | 1023 | 障碍物避障 |
| `STATUS_NAVI_GLOBAL_PATH_FAILED` | 1025 | 路径规划失败，建议停止导航 |
| `STATUS_NAVI_MULTI_ROBOT_WAITING` | 1034 | 多机调度等待 |
| `STATUS_NAVI_MULTI_ROBOT_WAITING_END` | 1035 | 多机调度等待结束 |
| `STATUS_NAVI_GO_STRAIGHT` | 1036 | 直线行走中 |
| `STATUS_NAVI_TURN_LEFT` | 1037 | 左转弯中 |
| `STATUS_NAVI_TURN_RIGHT` | 1038 | 右转弯中 |

##### 离桩 onError 错误码

| 常量 | Code | 说明 |
|------|------|------|
| `RESULT_FAILURE_MOTION_AVOID_STOP` | — | 前方有障碍物，离桩失败 |
| `RESULT_FAILURE_TIMEOUT` | — | 离桩超时（默认 15 秒） |
| `STATUS_LEAVE_PILE_OPEN_RADAR_FAILURE` | — | 雷达启动失败 |

---

## 六、项目架构

### 6.1 目录结构

```
KmqSampleBody/
├── app/
│   ├── build.gradle.kts              # 模块构建配置与依赖
│   ├── src/main/
│   │   ├── AndroidManifest.xml       # 应用清单
│   │   ├── assets/
│   │   │   └── actionRegistry.json   # Agent Action 注册配置
│   │   ├── java/com/kmq/kmqsamplebody/
│   │   │   ├── MainApplication.kt    # Application 入口，初始化 AppAgent
│   │   │   ├── MainActivity.kt       # 首页，卡片网格导航
│   │   │   ├── FeatureAdapter.kt     # RecyclerView 适配器
│   │   │   ├── tts/
│   │   │   │   ├── TtsActivity.kt        # TTS 播报 + ASR 录入
│   │   │   │   └── TtsStreamActivity.kt  # TTS 流式播报
│   │   │   ├── asr/
│   │   │   │   └── AsrActivity.kt        # ASR 语音识别
│   │   │   ├── chassis/
│   │   │   │   └── ChassisActivity.kt    # 底盘移动 + 云台上下控制
│   │   │   ├── location/
│   │   │   │   └── LocationActivity.kt   # 定位状态监听 + 坐标获取
│   │   │   ├── position/
│   │   │   │   └── PositionActivity.kt   # 点位列表 Grid 展示 + 点击导航
│   │   │   ├── camera/
│   │   │   │   └── CameraActivity.kt     # Camera2 + 数据流共享 + 人脸识别
│   │   │   └── systeminfo/
│   │   │       └── SystemInfoActivity.kt # 系统版本 + SN + 电池状态
│   │   └── res/
│   │       ├── layout/               # 各页面布局 XML
│   │       ├── drawable/             # 功能图标
│   │       └── values/               # 字符串、颜色、主题
├── build.gradle.kts                  # 根构建文件
├── settings.gradle.kts               # 项目设置与仓库配置
└── gradle/libs.versions.toml         # 版本目录
```

### 6.2 架构流程

```
┌───────────────────────────┐
│      MainApplication      │  应用启动时初始化
│  ├─ AppAgent              │  设置 Persona（机器人人设）
│  │  └─ Actions.SAY        │  注册语音播报 Action
│  └─ RobotApi.connectServer│  连接 CoreService 底盘服务
└─────────────┬─────────────┘
              │
              ▼
┌───────────────────────────┐
│        MainActivity       │  首页卡片网格（2列 RecyclerView）
│        FeatureAdapter     │  点击卡片 → startActivity
└─────────────┬─────────────┘
              │
  ┌───────┬───┴───┬────────┬────────┬────────┬────────┬───────┐
  ▼       ▼       ▼        ▼        ▼        ▼        ▼       ▼
TTS    TTS流式   ASR    底盘/云台  定位    点位导航  摄像头  系统信息
页面    页面     页面     页面     页面     页面    /人脸    页面
  │       │       │        │        │        │       │       │
Agent   Agent   Agent   Robot    Robot    Robot   Robot   Robot
 SDK     SDK     SDK     Api      Api      Api     Api     Api
                                                  Person  Setting
                                                   Api     Api
```

### 6.3 关键组件

| 组件 | 来源 | 职责 |
|------|------|------|
| `AppAgent` | Agent SDK | 应用级 Agent，管理 Persona 人设和 Action 注册 |
| `PageAgent` | Agent SDK | 页面级 Agent，管理 ASR/TTS 事件监听的生命周期 |
| `AgentCore` | Agent SDK | 静态工具类，提供 TTS 播报、麦克风控制、语音条控制等核心 API |
| `RobotApi` | CoreService SDK | 机器人硬件 API，提供底盘控制、定位、充电等能力 |

---

## 七、核心概念

本节梳理开发过程中需要理解的核心概念，分为 **Agent SDK（语音交互）** 和 **CoreService SDK（硬件控制）** 两部分。

### 7.1 已消费 vs 未消费（ASR 结果处理模式）

这是 Agent SDK 中最重要的概念，决定了语音识别结果的流向和大模型是否参与处理：

```
已消费模式（Consumed）：
用户说话 → ASR 识别 → onASRResult 返回 true → APP 自行处理 → 结束
                                                               大模型无感知

未消费模式（Unconsumed）：
用户说话 → ASR 识别 → onASRResult 返回 false → 大模型接收 → 自动规划 Action → TTS 回复
                                                仅监听不拦截
```

| 对比项 | 已消费（Consumed） | 未消费（Unconsumed） |
|--------|-------------------|---------------------|
| `onASRResult` 返回值 | `true` | `false` |
| `isDisablePlan` | `true`（禁用规划） | `false`（启用规划） |
| 大模型是否收到语音 | 否 | 是 |
| 大模型是否自动回复 | 否 | 是（通过 Action 规划） |
| 系统字幕条 | 不显示 | 正常显示 |
| 典型场景 | 表单填写、搜索输入、语音指令 | 对话交互、智能问答、情感响应 |

### 7.2 AgentCore 关键属性

`AgentCore` 是 Agent SDK 的静态工具类，通过以下三个属性控制语音交互行为。**三个属性需要配合使用**，不同模式下的正确组合如下：

| 属性 | 类型 | 说明 |
|------|------|------|
| `isMicrophoneMuted` | `Boolean` | 麦克风静音开关。`true` = 静音，`false` = 开启 |
| `isDisablePlan` | `Boolean` | 大模型自动规划开关。`true` = 禁用自动回复，`false` = 启用 |
| `isEnableVoiceBar` | `Boolean` | 语音条 UI 开关。`true` = 显示动画，`false` = 隐藏 |

**各模式下的属性组合：**

| 属性 | 已消费模式 | 未消费模式 | 空闲/停止 |
|------|-----------|-----------|----------|
| `isMicrophoneMuted` | `false`（开启） | `false`（开启） | `true`（静音） |
| `isDisablePlan` | `true`（禁用） | `false`（启用） | `true`（禁用） |
| `isEnableVoiceBar` | `false`（隐藏） | `false`（隐藏） | `false`（隐藏） |

> **关键时序要求**：开启麦克风 `isMicrophoneMuted = false` **必须在所有其他属性设置之后**，否则可能出现语音录入无响应。

### 7.3 PageAgent 生命周期

`PageAgent` 绑定到 Activity，负责管理**页面级**的 ASR/TTS 事件监听。与 `AppAgent`（应用级）的区别在于 `PageAgent` 仅在当前页面可见时生效。

```kotlin
val pageAgent = PageAgent(this)

pageAgent.setOnTranscribeListener(object : OnTranscribeListener {
    override fun onASRResult(transcription: Transcription): Boolean {
        // transcription.text  — 当前识别文本（中间结果会持续更新）
        // transcription.final — 是否为最终结果（true = 一句话识别完毕）
        runOnUiThread { textView.text = transcription.text }
        return isConsumed // true=已消费, false=未消费
    }

    override fun onTTSResult(transcription: Transcription): Boolean {
        return false
    }
})
```

**生命周期要点：**

| 时机 | 操作 | 说明 |
|------|------|------|
| `onCreate` | 创建 `PageAgent`，设置 `OnTranscribeListener` | 页面创建时初始化 |
| `onDestroy` | 静音麦克风、清除上下文 | 防止后台持续拾音和上下文泄漏 |
| 模式切换 | 先 `resetToIdle()` 再设置新模式 | 避免标志位残留 |

> **线程提醒**：`onASRResult` / `onTTSResult` 回调在**子线程**中执行，更新 UI 必须通过 `runOnUiThread`。

### 7.4 TTSCallback 回调机制

`AgentCore.tts()` 提供同步和异步两种调用方式，异步调用可通过 `TTSCallback` 精确感知播报完成状态：

```kotlin
// 异步调用（带回调）
AgentCore.tts(text, 180000, object : TTSCallback {
    override fun onTaskEnd(status: Int, result: String?) {
        // status == 1：播报成功完成
        // status == 2：播报失败（被打断、超时或出错）
    }
})

// 同步调用（需在协程中使用）
val taskResult = AgentCore.ttsSync(text, 180000)
```

**TTS 播放行为特性：**

| 特性 | 说明 |
|------|------|
| 自动追加播放 | 多次调用 `tts()` 音频追加到队列，不中断前一次播放 |
| 语音打断 | 用户说话自动打断当前播放，触发 `status=2` |
| 流式播放 | 音频边生成边播放，`timeout` 为完整播放超时 |
| 强制停止 | `AgentCore.stopTTS()` 立即打断 |

### 7.5 RobotApi 连接生命周期

所有硬件控制 API（底盘、导航、充电、视觉等）都依赖 `RobotApi` 与 CoreService 的连接。连接在 `Application.onCreate` 中建立：

```
APP 启动 → connectServer() → handleApiConnected → RobotApi 可用
                              handleApiDisconnected → 连接断开，API 失效
                              handleApiDisabled → SDK 被禁用
```

**连接状态与 API 可用性：**

| 连接状态 | API 调用效果 | 触发时机 |
|---------|-------------|---------|
| 已连接（`handleApiConnected`） | 正常执行 | 首次连接成功、系统接管结束后自动重连 |
| 断开（`handleApiDisconnected`） | 无效，静默失败 | 系统接管、其他 APP 抢占底盘控制权 |
| 禁用（`handleApiDisabled`） | 无效 | SDK 被系统禁用 |

**关键规则：**
- 所有 `RobotApi` 调用**必须在 `handleApiConnected` 之后**才能生效
- **只有前台进程**才能获取底盘控制权限，后台服务进程不工作
- 系统接管结束后会自动重连；其他 APP 抢占导致的断开**不会自动恢复**
- 建议通过全局标志位 `isRobotApiConnected` 在各 Activity 中检查，未连接时提示用户

### 7.6 reqId 请求标识

CoreService SDK 中几乎所有 API 都需要传入 `reqId` 参数：

```kotlin
private var reqId = 0

RobotApi.getInstance().goForward(reqId++, speed, distance, listener)
```

| 要点 | 说明 |
|------|------|
| 类型 | `Int` |
| 用途 | 用于日志追踪和请求-回调匹配 |
| 推荐做法 | 每个 Activity 维护一个自增计数器，每次调用 `reqId++` |
| 注意事项 | 不同 API 的 `reqId` 互不影响，主要用于调试定位问题 |

### 7.7 三种监听器模式

CoreService SDK 使用三种监听器处理不同类型的异步回调，理解它们的区别是正确使用 API 的关键：

#### CommandListener — 命令回调

用于**一次性命令**（底盘移动、获取信息等），回调后即结束：

```kotlin
object : CommandListener() {
    override fun onResult(result: Int, message: String?) {
        // 命令执行结果，result: Definition.RESULT_OK(1) / -1
    }
    override fun onStatusUpdate(status: Int, data: String?, extraData: String?) {
        // 命令执行过程中的状态更新（可选）
    }
    override fun onError(errorCode: Int, errorString: String?, extraData: String?) {
        // 命令执行错误（可选）
    }
}
```

**典型使用**：`goForward`、`getPosition`、`getRobotSn`、`getPlaceList`、`leaveChargingPile`

#### ActionListener — 动作回调

用于**持续性动作**（导航、充电、唤醒等），动作执行期间持续回调状态，结束时回调结果：

```kotlin
object : ActionListener() {
    override fun onResult(status: Int, responseString: String?) {
        // 动作最终结果
        // RESULT_OK(1): 成功
        // RESULT_FAILURE(2): 失败
        // ACTION_RESPONSE_STOP_SUCCESS(3): 主动停止成功
    }
    override fun onStatusUpdate(status: Int, data: String?) {
        // 动作进行中的状态更新（避障、转弯、直行等）
    }
    override fun onError(errorCode: Int, errorString: String?) {
        // 动作执行错误（未定位、目的地不存在等）
    }
}
```

**典型使用**：`startNavigation`、`startNaviToAutoChargeAction`、`wakeUp`、`startFocusFollow`

#### StatusListener — 状态监听

用于**持续订阅**系统状态变化，注册后持续回调直到反注册：

```kotlin
object : StatusListener() {
    override fun onStatusUpdate(type: String?, data: String?) {
        // type: 状态类型标识
        // data: 状态数据（通常为 JSON 字符串或简单值）
    }
}
```

**典型使用**：`registerStatusListener(STATUS_BATTERY, ...)`、`registerStatusListener(STATUS_POSE_ESTIMATE, ...)`

> **重要**：`StatusListener` 注册后会持续占用资源，页面销毁时**必须调用 `unregisterStatusListener`** 取消注册，否则会导致内存泄漏。

**三种监听器对比：**

| 对比项 | CommandListener | ActionListener | StatusListener |
|--------|----------------|----------------|----------------|
| 回调次数 | 一次（结果返回后结束） | 多次（过程 + 结果） | 持续（直到反注册） |
| 生命周期 | 随命令结束 | 随动作结束 | 需手动反注册 |
| 有 onError | 有 | 有 | 无 |
| 有 onStatusUpdate | 有 | 有 | 仅 onStatusUpdate |
| 适用场景 | 一次性命令 | 持续性动作 | 系统状态订阅 |

### 7.8 Definition 常量体系

`Definition` 类是 CoreService SDK 的常量中心，包含所有状态码、错误码、配置键名：

| 常量类别 | 前缀/示例 | 说明 |
|---------|----------|------|
| 结果码 | `RESULT_OK`(1)、`RESULT_FAILURE`(2) | API 调用结果 |
| 动作响应 | `ACTION_RESPONSE_STOP_SUCCESS`(3)、`ACTION_RESPONSE_ALREADY_RUN`(-1) | 动作控制响应 |
| 错误码 | `ERROR_NOT_ESTIMATE`(-116)、`ERROR_DESTINATION_NOT_EXIST`(-108) | 错误原因 |
| 状态类型 | `STATUS_BATTERY`、`STATUS_POSE`、`STATUS_POSE_ESTIMATE`、`STATUS_EMERGENCY` | 状态监听类型标识 |
| 导航状态 | `STATUS_NAVI_AVOID`(1018)、`STATUS_START_NAVIGATION`(1014) | 导航过程状态码 |
| JSON 键名 | `JSON_NAVI_POSITION_X`、`JSON_NAVI_TARGET_PLACE_NAME` | API 参数/返回值的 JSON 键 |
| 设置键名 | `ROBOT_SETTINGS_BATTERY_INFO` | RobotSettingApi 查询键 |
| 摄像头方向 | `JSON_HEAD_FORWARD`、`JSON_HEAD_BACKWARD` | SDK 摄像头切换参数 |

> 完整错误码和状态码列表详见 [5.4.7 通用错误码速查](#547-通用错误码速查)。

### 7.9 线程模型与 UI 更新

SDK 回调（无论是 Agent SDK 还是 CoreService SDK）均在**子线程**中执行，直接操作 UI 会导致崩溃：

```kotlin
// ❌ 错误：在回调中直接操作 UI
override fun onResult(result: Int, message: String?) {
    textView.text = message  // 崩溃：CalledFromWrongThreadException
}

// ✅ 正确：通过 runOnUiThread 切到主线程
override fun onResult(result: Int, message: String?) {
    runOnUiThread { textView.text = message }
}
```

**各回调的线程规则：**

| 回调来源 | 执行线程 | UI 更新方式 |
|---------|---------|------------|
| `OnTranscribeListener.onASRResult` | 子线程 | `runOnUiThread { }` |
| `OnTranscribeListener.onTTSResult` | 子线程 | `runOnUiThread { }` |
| `TTSCallback.onTaskEnd` | 子线程 | `runOnUiThread { }` |
| `CommandListener.onResult` | 子线程 | `runOnUiThread { }` |
| `ActionListener.onResult/onStatusUpdate/onError` | 子线程 | `runOnUiThread { }` |
| `StatusListener.onStatusUpdate` | 子线程 | `runOnUiThread { }` |
| `PersonListener.personChanged` | 子线程 | `runOnUiThread { }` |

> **通用规则**：所有 SDK 回调一律假定在子线程，任何 UI 操作都需要 `runOnUiThread`。

---

## 八、API 速查表

### Agent SDK（语音交互）

| API | 说明 |
|-----|------|
| `AgentCore.tts(text)` | 文字转语音播报 |
| `AgentCore.tts(text, timeout, callback)` | 带超时和回调的流式 TTS 播报 |
| `AgentCore.stopTTS()` | 停止当前 TTS 播报 |
| `AgentCore.clearContext()` | 清除对话上下文 |
| `AgentCore.isMicrophoneMuted` | 麦克风静音开关（`true`=静音） |
| `AgentCore.isDisablePlan` | 禁用大模型自动规划（`true`=禁用） |
| `AgentCore.isEnableVoiceBar` | 语音条 UI 显示开关（`true`=显示） |
| `PageAgent(activity)` | 创建页面级 Agent 实例 |
| `PageAgent.setOnTranscribeListener(listener)` | 设置 ASR/TTS 转写监听器 |
| `AppAgent.setPersona(description)` | 设置机器人人设描述 |
| `AppAgent.registerAction(action)` | 注册 Agent Action |

### CoreService SDK（硬件控制）

**底盘 & 云台：**

| API | 说明 |
|-----|------|
| `goForward(reqId, speed, listener)` | 持续前进（speed: m/s，范围 0~1.0） |
| `goForward(reqId, speed, distance, listener)` | 前进指定距离后自动停止 |
| `goForward(reqId, speed, distance, avoid, listener)` | 前进指定距离 + 避障 |
| `goBackward(reqId, speed, listener)` | 持续后退（无避障） |
| `goBackward(reqId, speed, distance, listener)` | 后退指定距离后自动停止 |
| `turnLeft(reqId, speed, listener)` | 持续左转（speed: 度/s，范围 0~50） |
| `turnLeft(reqId, speed, angle, listener)` | 左转指定角度后自动停止 |
| `turnRight(reqId, speed, listener)` | 持续右转 |
| `turnRight(reqId, speed, angle, listener)` | 右转指定角度后自动停止 |
| `stopMove(reqId, listener)` | 立即停止底盘运动（不可停止导航和头部运动） |
| `moveHead(reqId, hmode, vmode, hangle, vangle, listener)` | 云台俯仰控制（仅豹二、小宝） |
| `resetHead(reqId, listener)` | 恢复云台初始角度 |

**定位 & 坐标：**

| API | 说明 |
|-----|------|
| `isRobotEstimate(reqId, listener)` | 判断是否已定位（返回 "true"/"false"） |
| `getPosition(reqId, listener)` | 获取当前位姿（x, y, theta），需已定位 |
| `setPoseEstimate(reqId, params, listener)` | 手动设置初始定位坐标 |
| `registerStatusListener(STATUS_POSE_ESTIMATE, listener)` | 监听定位状态变化（0=未定位，1=已定位） |
| `registerStatusListener(STATUS_POSE, listener)` | 监听坐标持续上报 |
| `getRobotStatus(type, listener)` | 主动查询指定状态（如定位状态） |
| `unregisterStatusListener(listener)` | 取消状态监听 |

**点位管理：**

| API | 说明 |
|-----|------|
| `getPlaceList(reqId, listener)` | 获取当前地图所有点位（name, x, y, theta, status） |
| `setLocation(reqId, placeName, listener)` | 保存当前位置为命名点位 |
| `getLocation(reqId, placeName, listener)` | 根据名称获取点位坐标 |
| `isRobotInlocations(reqId, params, listener)` | 判断机器人是否在指定点位范围内 |
| `getMapName(reqId, listener)` | 获取当前地图名称 |
| `switchMap(reqId, mapName, listener)` | 切换地图（切换后需重新定位） |

**导航：**

| API | 说明 |
|-----|------|
| `startNavigation(reqId, destName, deviation, time, listener)` | 导航到指定点位（默认速度） |
| `startNavigation(reqId, destName, deviation, time, linearSpeed, angularSpeed, listener)` | 导航到指定点位（自定义速度） |
| `startNavigation(reqId, destName, deviation, obsDistance, time, listener)` | 导航 + 指定避障距离（V10+） |
| `stopNavigation(reqId)` | 停止 startNavigation 导航 |
| `goPosition(reqId, position, listener)` | 导航到指定坐标点（已废弃，建议用 startNavigation） |
| `stopGoPosition(reqId)` | 停止 goPosition 导航 |
| `resumeSpecialPlaceTheta(reqId, placeName, listener)` | 转向目标点方向（不移动） |

**充电：**

| API | 说明 |
|-----|------|
| `startNaviToAutoChargeAction(reqId, timeout, listener)` | 自动导航至充电桩 |
| `stopAutoChargeAction(reqId, force)` | 停止自动回充 |
| `leaveChargingPile(reqId, speed, distance, listener)` | 停止充电并脱离充电桩（需先 disableBattery） |
| `disableBattery()` | 禁用系统充电界面 |
| `RobotSettingApi.getRobotString(ROBOT_SETTINGS_BATTERY_INFO)` | 获取当前电量信息 |
| `registerStatusListener(STATUS_BATTERY, listener)` | 监听电池状态变化 |

**视觉 & 摄像头：**

| API | 说明 |
|-----|------|
| `switchCamera(reqId, camera, listener)` | 切换视觉 SDK 摄像头（前置 / 后置） |
| `getPictureById(reqId, faceId, count, listener)` | 获取人脸照片（使用完需手动删除） |
| `startRegister(reqId, name, timeout, tryCount, delay, listener)` | 自动注册人脸 |
| `remoteRegister(reqId, name, path, listener)` | 使用照片远程注册人脸 |
| `getPersonInfoFromNet(reqId, faceId, pictures, listener)` | 云端人脸识别 |
| `startFocusFollow(reqId, faceId, lostTimeout, maxDistance, listener)` | 开始焦点跟随 |
| `stopFocusFollow(reqId)` | 停止焦点跟随 |
| `PersonApi.getInstance().registerPersonListener(listener)` | 注册人员变化监听 |
| `PersonApi.getInstance().unregisterPersonListener(listener)` | 取消人员变化监听 |
| `PersonApi.getInstance().getAllPersons()` | 获取视野内全部人员 |
| `PersonApi.getInstance().getAllFaceList()` | 获取有人脸信息的人员 |
| `PersonApi.getInstance().getCompleteFaceList()` | 获取有完整人脸的人员 |
| `SurfaceShareApi.getInstance().requestImageFrame(surface, bean, listener)` | 开始摄像头数据流共享 |
| `SurfaceShareApi.getInstance().abandonImageFrame(bean)` | 停止数据流共享 |

**系统信息 & 控制：**

| API | 说明 |
|-----|------|
| `getVersion()` | 获取系统版本号（同步） |
| `getRobotSn(listener)` | 获取设备 SN 序列号（异步） |
| `RobotSettingApi.getRobotString(ROBOT_SETTINGS_BATTERY_INFO)` | 获取电池信息快照 |
| `registerStatusListener(STATUS_BATTERY, listener)` | 监听电池状态变化 |
| `disableEmergency()` | 禁用系统急停画面 |
| `disableBattery()` | 禁用系统充电界面 |
| `disableFunctionKey()` | 禁用机器人头部物理按钮 |
| `robotStandby(reqId, listener)` | 开始休眠（低功耗模式） |
| `robotStandbyEnd(reqId)` | 停止休眠 |
| `wakeUp(reqId, angle, listener)` | 根据声源角度唤醒（转向用户） |
| `stopWakeUp(reqId)` | 停止唤醒 |
| `startNaviToAutoChargeAction(reqId, timeout, listener)` | 自动导航至充电桩 |
| `stopAutoChargeAction(reqId, force)` | 停止自动回充 |
| `leaveChargingPile(reqId, speed, distance, listener)` | 脱离充电桩（需先 disableBattery） |
| `registerStatusListener(STATUS_EMERGENCY, listener)` | 监听急停状态 |

---

## 九、项目依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| `com.orionstar.agent:sdk` | — | 科梦奇机器人 Agent SDK（含 AgentCore、PageAgent 等） |
| `androidx.core:core-ktx` | — | AndroidX Kotlin 扩展 |
| `androidx.appcompat:appcompat` | 1.7.1 | AppCompat 兼容库 |
| `com.google.android.material:material` | 1.10.0 | Material Design 组件 |
| `androidx.recyclerview:recyclerview` | 1.3.2 | RecyclerView 列表组件 |
| `androidx.cardview:cardview` | 1.0.0 | CardView 卡片组件 |
| `androidx.activity:activity` | — | Activity 基础库 |
| `androidx.constraintlayout:constraintlayout` | — | 约束布局 |

---

## 许可证

本项目为科梦奇机器人 SDK 示例代码，仅供学习和开发参考。

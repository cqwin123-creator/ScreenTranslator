# ScreenTranslator - 屏幕翻译APP

一款支持屏幕文字识别和AI翻译的Android应用，可以翻译游戏、图片中的日语和英语文字。

## 功能特性

- **屏幕文字识别**：使用Google ML Kit识别屏幕上的日语和英语文字
- **AI翻译**：使用Kimi API进行智能翻译
- **悬浮窗显示**：翻译结果以悬浮窗形式显示
- **两种显示模式**：
  - 覆盖在原文表面显示
  - 显示在屏幕下方固定区域

## 下载APK

每次推送代码后，GitHub Actions会自动构建APK。

1. 点击上方 **Actions** 标签
2. 选择最新的工作流运行
3. 在 **Artifacts** 部分下载 `debug-apk` 或 `release-apk`

## 自行构建

### 环境要求

- Android Studio Arctic Fox (2020.3.1) 或更高版本
- JDK 11 或更高版本
- Android SDK 24-34

### 构建步骤

1. 克隆仓库
```bash
git clone https://github.com/yourusername/ScreenTranslator.git
cd ScreenTranslator
```

2. 使用Android Studio打开项目

3. 等待Gradle同步完成

4. 构建APK
```bash
./gradlew assembleDebug
```

APK输出位置：`app/build/outputs/apk/debug/app-debug.apk`

## 配置Kimi API

1. 访问 [Kimi开放平台](https://platform.moonshot.cn)
2. 注册并登录账号
3. 进入"API Key管理"页面创建新Key
4. 在APP设置中粘贴API Key

## 使用说明

1. 安装APK并打开应用
2. 配置Kimi API Key
3. 授予**悬浮窗权限**和**屏幕录制权限**
4. 点击"启动翻译"
5. 屏幕上会出现悬浮翻译按钮
6. 点击悬浮按钮即可翻译当前屏幕内容

## 权限说明

- **悬浮窗权限**：用于显示悬浮按钮和翻译结果
- **屏幕录制权限**：用于捕获屏幕内容进行OCR识别
- **网络权限**：用于调用翻译API

## 技术栈

- Kotlin
- Android SDK 34
- Google ML Kit Text Recognition
- MediaProjection API
- OkHttp + Gson
- Kotlin Coroutines

## License

MIT License

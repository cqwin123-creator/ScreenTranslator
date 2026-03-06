# GitHub Actions 自动打包说明

## 项目结构

```
ScreenTranslator-GitHub/
├── .github/
│   └── workflows/
│       └── build.yml          # GitHub Actions 工作流配置
├── app/                        # Android应用模块
│   ├── src/main/              # 源代码
│   ├── build.gradle           # 模块构建配置
│   └── proguard-rules.pro     # 混淆规则
├── gradle/wrapper/            # Gradle Wrapper
├── build.gradle               # 项目构建配置
├── settings.gradle            # 项目设置
├── gradlew                    # Gradle脚本(Linux/Mac)
├── gradlew.bat                # Gradle脚本(Windows)
├── .gitignore                 # Git忽略文件
└── README.md                  # 项目说明
```

## 上传到GitHub步骤

### 1. 创建GitHub仓库

1. 登录GitHub
2. 点击右上角 **+** → **New repository**
3. 仓库名称填 `ScreenTranslator`
4. 选择 **Public** 或 **Private**
5. 点击 **Create repository**

### 2. 上传代码

#### 方法一：使用命令行

```bash
# 解压 tar.gz 文件
tar -xzvf ScreenTranslator-GitHub.tar.gz
cd ScreenTranslator-GitHub

# 初始化Git仓库
git init
git add .
git commit -m "Initial commit"

# 关联远程仓库（替换为你的仓库地址）
git remote add origin https://github.com/你的用户名/ScreenTranslator.git

# 推送代码
git branch -M main
git push -u origin main
```

#### 方法二：使用GitHub网页上传

1. 解压 `ScreenTranslator-GitHub.tar.gz`
2. 在GitHub仓库页面点击 **Add file** → **Upload files**
3. 拖拽整个项目文件夹到上传区域
4. 点击 **Commit changes**

### 3. 启用GitHub Actions

1. 进入仓库的 **Actions** 标签
2. 如果提示启用Actions，点击 **I understand my workflows, go ahead and enable them**
3. 工作流会自动运行

## 自动打包流程

每次以下操作会触发自动打包：
- 推送代码到 `main` 或 `master` 分支
- 创建Pull Request到 `main` 或 `master` 分支
- 手动点击 **Run workflow**

### 下载APK

1. 进入仓库的 **Actions** 标签
2. 点击最新的工作流运行
3. 等待工作流完成（约5-10分钟）
4. 在 **Artifacts** 部分下载：
   - `debug-apk` - Debug版本（推荐测试用）
   - `release-apk` - Release版本（未签名）

## 常见问题

### 1. Actions运行失败

**检查点：**
- 确保所有文件已正确上传
- 检查 `.github/workflows/build.yml` 是否存在
- 查看Actions日志获取详细错误信息

**常见错误解决：**

```bash
# 错误：Permission denied
# 解决：确保gradlew有执行权限
chmod +x gradlew
git add gradlew
git commit -m "Fix gradlew permission"
git push
```

### 2. 构建成功但APK无法安装

- Debug APK可以直接安装
- Release APK未签名，需要自行签名后才能安装

### 3. 如何签名Release APK

在GitHub Actions中添加签名步骤（需要配置Secrets）：

```yaml
- name: Sign Release APK
  uses: r0adkll/sign-android-release@v1
  with:
    releaseDirectory: app/build/outputs/apk/release
    signingKeyBase64: ${{ secrets.SIGNING_KEY }}
    alias: ${{ secrets.ALIAS }}
    keyStorePassword: ${{ secrets.KEY_STORE_PASSWORD }}
    keyPassword: ${{ secrets.KEY_PASSWORD }}
```

## 配置Kimi API

1. 访问 https://platform.moonshot.cn
2. 注册并创建API Key
3. 在APP设置中输入API Key

## 技术栈

- **语言**: Kotlin
- **minSdk**: 24, **targetSdk**: 34
- **OCR**: Google ML Kit Text Recognition
- **屏幕捕获**: MediaProjection API
- **网络**: OkHttp + Gson
- **异步**: Kotlin Coroutines

## 文件清单

共 **31个文件**：

| 类型 | 数量 | 说明 |
|------|------|------|
| Kotlin源码 | 7个 | 主要功能代码 |
| XML布局 | 4个 | 界面布局 |
| XML资源 | 7个 | 字符串、颜色、主题等 |
| Gradle配置 | 5个 | 构建配置 |
| GitHub Actions | 1个 | 自动打包工作流 |
| 其他 | 7个 | README、gitignore等 |

## 下一步

1. ✅ 上传代码到GitHub
2. ✅ 等待Actions自动打包
3. ✅ 下载APK安装测试
4. 🔧 配置Kimi API Key
5. 🎉 开始使用屏幕翻译功能

# Tesseract OCR 配置说明

## 概述

项目已改用 **Tesseract OCR** 替代 ML Kit，支持离线识别日语和英语文字。

## 配置步骤

### 1. 下载语言数据文件

需要下载以下 `.traineddata` 文件：

| 文件名 | 语言 | 大小 |
|--------|------|------|
| `eng.traineddata` | 英语 | 约 4MB |
| `jpn.traineddata` | 日语 | 约 15MB |
| `jpn_vert.traineddata` | 日语竖排 | 约 15MB |

**下载地址**：https://github.com/tesseract-ocr/tessdata

### 2. 创建 assets 目录

```
app/src/main/assets/
└── tessdata/
    ├── eng.traineddata
    ├── jpn.traineddata
    └── jpn_vert.traineddata
```

### 3. 放置文件

将下载的 `.traineddata` 文件放入 `app/src/main/assets/tessdata/` 目录。

### 4. 重新构建

```bash
./gradlew assembleDebug
```

## 注意事项

1. **文件大小**：语言数据文件较大（总计约 34MB），会增加 APK 体积
2. **首次运行**：应用首次启动时会将语言数据复制到设备存储
3. **存储空间**：需要额外的存储空间存放语言数据

## 替代方案

如果 APK 体积太大，可以考虑：

1. **只保留英语识别**：删除 `jpn.traineddata` 和 `jpn_vert.traineddata`
2. **运行时下载**：首次使用时从服务器下载语言数据

## 测试

构建完成后，应用应该能够：
- ✅ 识别屏幕上的英语文字
- ✅ 识别屏幕上的日语文字（包括横排和竖排）
- ✅ 离线工作，无需网络连接

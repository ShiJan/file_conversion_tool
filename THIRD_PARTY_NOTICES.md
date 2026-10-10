# 第三方许可声明

本项目源代码采用 MIT 许可（见 `LICENSE`）。便携包中包含以下第三方组件；发布时应随包保留本声明。Maven 依赖中还可能包含传递依赖，发布者须以构建时依赖清单再次核对各组件的许可原文。

| 组件 | 用途 | 许可与来源 |
| --- | --- | --- |
| OpenJFX / JavaFX | 桌面界面 | GPL v2 with Classpath Exception；https://openjfx.io/ |
| Tesseract OCR | 本机离线文字识别 | Apache License 2.0；https://github.com/tesseract-ocr/tesseract |
| `tessdata_fast/chi_sim.traineddata` | 简体中文 OCR 模型 | Apache License 2.0；https://github.com/tesseract-ocr/tessdata_fast |
| JavaCPP Presets | Tesseract 原生库的 Java 封装与平台二进制 | Apache License 2.0；https://github.com/bytedeco/javacpp-presets |
| Leptonica | Tesseract 图像处理依赖 | BSD 风格许可；https://github.com/DanBloomberg/leptonica |
| Eclipse Temurin 21 | CI 生成的随包 Java 运行时 | GPL v2 with Classpath Exception；https://adoptium.net/ |

本项目不使用微信官方接口，不隶属于微信或腾讯。用户截图与识别过程完全在本机进行。

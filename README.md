# 微信聊天截图批量整理工具

一个离线的 JavaFX 桌面工具：批量导入微信聊天截图，从截图顶部聊天标题区域尝试识别好友备注、昵称或群聊名称，核对后按界面顺序编号，导出一个 ZIP。**不会上传图片；不会修改原图片。** 本项目与微信、腾讯无隶属关系。

## 使用方法

1. 解压对应系统的便携包，在包内启动 `WeChatScreenshotOrganizer`（macOS 为 `WeChatScreenshotOrganizer.app`）。不需安装 Java、Tesseract 或配置 `JAVA_HOME`。
2. 点击“导入图片”，或把多张 PNG/JPG/JPEG 文件拖入窗口。缩略图旁显示原文件名和 OCR 状态；重复路径只导入一次。
3. 逐张核对聊天名称。点击缩略图可打开原图预览，滚动查看长截图，并用缩放按钮核对顶部昵称；按 Escape 或“关闭预览”返回。OCR 只读取顶部标题裁剪区，识别失败或不准确时直接在输入框修改。使用 ↑ ↓ 调整顺序，也可以移除误导入图片。
4. 设置文件名前缀、两处连接符、ZIP 名称和保存位置，检查右侧每一个最终文件名，再点“导出 ZIP”。已有同名 ZIP 不会被覆盖，请换一个名称。完成一批后点击“全部清除”，确认后即可导入下一批；原图和已导出的 ZIP 不受影响。

图片命名默认是 `前缀-名称序号.原扩展名`，例如 `记录-张三1.png`、`记录-张三2.png`。“前缀与名称之间”和“名称与序号之间”可分别选择不添加、`-` 或 `_`，例如可改为 `记录_张三_1.png`。所有图片依界面顺序统一从 1 开始编号，导入 10 张时序号为 1 到 10，不按好友名称重置。非法路径字符被替换成 `_`，末尾空格和点会移除。空名称或生成的重名会阻止导出。ZIP 是标准 ZIP，图片原始字节保持不变。

## 平台与便携包

稳定版请从 [GitHub Releases](https://github.com/ShiJan/file_conversion_tool/releases/latest) 下载与系统对应的压缩包，解压后直接启动包内程序。

| 平台 | 构建产物 | 启动方式 | 本仓库当前验证 |
| --- | --- | --- | --- |
| Windows x64 | `WeChatScreenshotOrganizer-windows-x64.zip` | 解压后运行 `WeChatScreenshotOrganizer.exe` | 需 Windows CI / 实机验证 |
| Linux x64 | `WeChatScreenshotOrganizer-linux-x86_64.tar.gz` | 解压后运行 `WeChatScreenshotOrganizer/bin/WeChatScreenshotOrganizer` | 在 Ubuntu 22.04 构建；需 CI / 实机验证 |
| macOS Intel | `WeChatScreenshotOrganizer-macos-x86_64.zip` | 解压后打开 `.app` | 用户已完成应用操作测试；发布包仍需核对 |
| macOS Apple Silicon | `WeChatScreenshotOrganizer-macos-arm64.zip` | 解压后打开 `.app` | 需 Apple Silicon CI / 实机验证 |

每个平台必须在该平台上运行 `jpackage --type app-image`。这是包含专用 Java 运行时的应用目录，不是安装器，也不会调用系统 `java` 命令。macOS Intel 和 Apple Silicon 分别构建，不承诺单一通用二进制。未签名的 macOS 应用首次打开可能被 Gatekeeper 拦截；确认来源后可在“系统设置 → 隐私与安全性”中允许打开。Linux 桌面环境须有可用图形会话。当前实际构建与验证情况见 [DELIVERY.md](DELIVERY.md)。

## 开发与构建

需要 JDK 21、Maven 3.6+ 和该平台的 `jpackage`。首次构建需要联网下载 Maven 依赖；**用户运行便携包不需要联网**。仓库内已包含经 SHA-256 校验的模型文件 `src/main/resources/chi_sim.traineddata`，它随应用 JAR 打包，不需要单独安装。

macOS / Linux：

```bash
bash scripts/download-model.sh # 仅模型文件缺失时
bash scripts/package-unix.sh
```

Windows PowerShell：

```powershell
./scripts/download-model.ps1
./scripts/package-windows.ps1
```

仅运行测试：`mvn -s scripts/maven-settings.xml -gs scripts/maven-settings.xml test`。`scripts/maven-settings.xml` 只为这个项目选择官方 Maven Central，不更改用户 Maven 或 Java 的全局设置。GitHub Actions 工作流 `.github/workflows/portable.yml` 对四种目标系统分别执行测试和应用目录构建，成功后生成带校验值的 Release 草稿。发布步骤见 [RELEASE.md](RELEASE.md)。

应用与窗口使用原创深色“截图叠层 + ZIP 拉链”图标，平台文件位于 `assets/`：macOS `.icns` 包含 16–1024 像素的 Retina 尺寸，Windows `.ico` 包含 16、32、48、64、128、256 像素的方形图，Linux `.png` 为 512×512。图标源码是 `scripts/IconGenerator.java`；macOS 上可先运行 `javac -d target/icon-generator scripts/IconGenerator.java`、`java -cp target/icon-generator IconGenerator`，再运行 `iconutil -c icns assets/icon.iconset -o assets/icon.icns` 重新生成。

## OCR 范围与局限

按每张图片的原始宽度计算标题候选区，不采用固定像素高度：横向为图片宽度的 16%–84%，纵向限定在距顶部约 2.5%–26% 图片宽度的范围。对偏上、常规、偏下的重叠区域分别识别；长截图只解码顶部，不把正文送入 OCR。它适用于常见竖屏聊天页；平板、横屏、标题被裁掉或被拼接软件严重缩小、状态栏异常及不同微信版本可能需要手动输入。OCR 分数低于 50 时不会自动填入名称。真实脱敏微信截图尚需验证准确率和裁剪参数。

## 隐私与许可

所有文件读取、OCR 和 ZIP 创建都在本机完成。工具没有上传接口或遥测。请勿向仓库提交真实聊天截图、个人信息或密钥。项目代码见 [LICENSE](LICENSE)，依赖和模型许可见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

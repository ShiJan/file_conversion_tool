# v1.0.0 稳定版发布流程

发布目标是 GitHub Releases。用户按系统下载一个压缩包，解压后直接运行，无需安装 Java 或 OCR。四个平台必须分别在对应系统上构建；macOS Intel 此前构建已由用户完成界面测试，此版各系统包仍需实机验证完整界面流程。

## 发布前

1. 使用 GitHub 公开仓库 `ShiJan/file_conversion_tool`，确认本地 `origin` 指向该仓库。
2. 检查仓库内容，确认没有真实聊天截图、个人信息和密钥。`dist/`、`target/` 已被 `.gitignore` 排除。
3. 运行 `git diff --check` 和 `JAVA_TOOL_OPTIONS=-Djava.awt.headless=true mvn -s scripts/maven-settings.xml -gs scripts/maven-settings.xml test`。
4. 提交源码与文档并推送 `main`。主分支只运行 Windows 测试和打包；确认通过后，再创建并推送指向该提交的 `v1.0.0` 标签。

## 构建与发布

推送 `v1.0.0` 标签会触发 [Portable builds](.github/workflows/portable.yml)。四个作业分别在 Windows x64、Linux x64、macOS Intel 和 macOS Apple Silicon 运行测试并打包，再通过包内启动程序执行 `--self-test`，验证内置运行时、OCR 标题识别和 ZIP 导出。四个作业全成功后，工作流检查压缩包完整性与启动文件、生成 `SHA256SUMS.txt`，并发布附有五个文件的稳定版 Release。

发布后逐一核对文件名、大小和校验值。四个平台在各自运行器上执行包内自检，完整图形界面操作仍需对应系统的实机验证。发布说明按实情标注验证范围，源文件为 [.github/release-notes/v1.0.0.md](.github/release-notes/v1.0.0.md)。

| 系统 | Release 文件 |
| --- | --- |
| Windows x64 | `WeChatScreenshotOrganizer-windows-x64.zip` |
| Linux x64 | `WeChatScreenshotOrganizer-linux-x86_64.tar.gz` |
| macOS Intel | `WeChatScreenshotOrganizer-macos-x86_64.zip` |
| macOS Apple Silicon | `WeChatScreenshotOrganizer-macos-arm64.zip` |

macOS 应用目前未签名和公证。首次打开从网络下载的包可能需要在“系统设置 → 隐私与安全性”中确认来源；若要求无安全提示地双击打开，还需 Apple Developer 签名与公证流程。

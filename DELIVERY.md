# 当前交付记录

日期：2026-10-10。当前开发机为 macOS Intel（x86_64）。

## 已执行验证

- `JAVA_TOOL_OPTIONS=-Djava.awt.headless=true bash scripts/package-unix.sh`：运行 JUnit 测试、构建 JAR、复制目标平台依赖、生成 `jpackage --type app-image`；随后从应用自带启动程序运行 `--self-test`，验证 JavaFX 类、OCR 原生库与模型、ZIP 导出，再归档。headless 设置用于当前无图形测试环境中的 AWT 测试进程。
- JUnit：16 项通过，含模拟窄长图、顶部偏高标题、深色标题栏和中文昵称的离线 OCR、标题区域隔离、10 张图片全局顺序编号、重排、连接符组合、无连接符时的重名拦截、非法文件名处理、ZIP 文件名和原始字节校验，以及三平台图标尺寸校验。默认导出命名为 `前缀-名称序号.原扩展名`。
- macOS Intel：ZIP 完整性通过检查；包内启动程序自检已确认 OCR 能识别测试标题并正确导出 ZIP，JAR 内包含 OCR 模型，包内含专用 Java 运行时。
- 用户已反馈此前 macOS Intel 包的界面操作无问题；本次为适配新的 Maven 构件名重新打包。真实微信截图识别准确率及其他系统的界面操作尚未验证。

## 产物与平台状态

| 平台 | 当前产物 | 状态 |
| --- | --- | --- |
| macOS Intel | `dist/WeChatScreenshotOrganizer-macos-x86_64.zip` | 本机已重新构建；包内为 Oracle JDK 21.0.12 运行时，当前包待人工复测 |
| macOS Apple Silicon | CI 工作流配置 | 当前开发机无法执行，尚未构建或验证 |
| Windows x64 | CI 工作流配置 | 当前开发机无法执行，尚未构建或验证 |
| Linux x64 | CI 工作流配置 | 当前开发机无法执行，尚未构建或验证 |

macOS Intel ZIP：69,860,286 字节；SHA-256 `ae409bfc09440497ce6436b7e72d29a0e9628b39d1a6870902a1dabea734dbec`。

四个平台的构建步骤见 `README.md` 和 `.github/workflows/portable.yml`。用户运行构建好的便携包无需预装 Java 或 OCR 工具。Oracle 官方目前将 JDK 21 截至 2026 年 9 月的更新列为 NFTC 授权；后续更新的授权可能变化，正式发布建议统一使用 GitHub Actions 中的 Temurin 产物，并逐平台实机验证。

本仓库没有真实聊天截图、个人信息或密钥。发布前需再核对提交内容。

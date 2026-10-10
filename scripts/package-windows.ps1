$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
if (-not (Test-Path "src/main/resources/chi_sim.traineddata")) { throw "缺少中文 OCR 模型。先运行 scripts/download-model.ps1" }
$modelHash = (Get-FileHash "src/main/resources/chi_sim.traineddata" -Algorithm SHA256).Hash.ToLowerInvariant()
if ($modelHash -ne "a5fcb6f0db1e1d6d8522f39db4e848f05984669172e584e8d76b6b3141e1f730") { throw "OCR 模型 SHA-256 不匹配" }
mvn -s scripts/maven-settings.xml -gs scripts/maven-settings.xml -B clean test package dependency:copy-dependencies "-Dnative.classifier=windows-x86_64" "-DincludeScope=runtime" "-DoutputDirectory=target/package-input/lib"
if ($LASTEXITCODE -ne 0) { throw "Maven 构建失败" }
Copy-Item target/file_conversion_tool-1.0.0.jar target/package-input/
Copy-Item LICENSE,THIRD_PARTY_NOTICES.md target/package-input/
New-Item -ItemType Directory -Force dist | Out-Null
New-Item -ItemType Directory -Force target/portable | Out-Null
jpackage --type app-image --name WeChatScreenshotOrganizer --app-version 1.0.0 --dest target/portable --input target/package-input --icon assets/icon.ico --main-jar file_conversion_tool-1.0.0.jar --main-class dev.wechatarchive.Main
if ($LASTEXITCODE -ne 0) { throw "jpackage 构建失败" }
$check = Start-Process -FilePath "target/portable/WeChatScreenshotOrganizer/WeChatScreenshotOrganizer.exe" -ArgumentList "--self-test" -Wait -PassThru
if ($check.ExitCode -ne 0) { throw "便携包自检失败，退出码：$($check.ExitCode)" }
Compress-Archive -Path target/portable/WeChatScreenshotOrganizer -DestinationPath dist/WeChatScreenshotOrganizer-windows-x64.zip -Force

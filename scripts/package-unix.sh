#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ ! -s src/main/resources/chi_sim.traineddata ]]; then
  echo "缺少 src/main/resources/chi_sim.traineddata；先运行 scripts/download-model.sh" >&2
  exit 1
fi
model_hash="$(shasum -a 256 src/main/resources/chi_sim.traineddata | cut -d ' ' -f 1)"
if [[ "$model_hash" != "a5fcb6f0db1e1d6d8522f39db4e848f05984669172e584e8d76b6b3141e1f730" ]]; then
  echo "OCR 模型 SHA-256 不匹配" >&2
  exit 1
fi

if [[ "$(uname -s)" == Darwin ]]; then native_classifier="macosx-$(uname -m)"; else native_classifier="linux-$(uname -m)"; fi
native_classifier="${native_classifier/aarch64/arm64}"
mvn -s scripts/maven-settings.xml -gs scripts/maven-settings.xml -B clean test package dependency:copy-dependencies \
  -Dnative.classifier="$native_classifier" -DincludeScope=runtime -DoutputDirectory=target/package-input/lib
cp target/file_conversion_tool-1.0.0.jar target/package-input/
cp LICENSE THIRD_PARTY_NOTICES.md target/package-input/
mkdir -p dist
mkdir -p target/portable
if [[ "$(uname -s)" == Darwin ]]; then icon_file="assets/icon.icns"; else icon_file="assets/icon.png"; fi
jpackage --type app-image --name WeChatScreenshotOrganizer --app-version 1.0.0 \
  --dest target/portable --input target/package-input \
  --icon "$icon_file" \
  --main-jar file_conversion_tool-1.0.0.jar \
  --main-class dev.wechatarchive.Main

if [[ "$(uname -s)" == Darwin ]]; then
  target/portable/WeChatScreenshotOrganizer.app/Contents/MacOS/WeChatScreenshotOrganizer --self-test
  arch="$(uname -m)"
  ditto -c -k --sequesterRsrc --keepParent target/portable/WeChatScreenshotOrganizer.app "dist/WeChatScreenshotOrganizer-macos-${arch}.zip"
else
  target/portable/WeChatScreenshotOrganizer/bin/WeChatScreenshotOrganizer --self-test
  arch="$(uname -m)"
  tar -C target/portable -czf "dist/WeChatScreenshotOrganizer-linux-${arch}.tar.gz" WeChatScreenshotOrganizer
fi

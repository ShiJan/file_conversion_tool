#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
curl --fail --location --retry 3 --output src/main/resources/chi_sim.traineddata \
  'https://cdn.jsdelivr.net/gh/tesseract-ocr/tessdata_fast@main/chi_sim.traineddata'
test -s src/main/resources/chi_sim.traineddata
actual="$(shasum -a 256 src/main/resources/chi_sim.traineddata | cut -d ' ' -f 1)"
expected="a5fcb6f0db1e1d6d8522f39db4e848f05984669172e584e8d76b6b3141e1f730"
if [[ "$actual" != "$expected" ]]; then echo "OCR 模型 SHA-256 不匹配" >&2; exit 1; fi

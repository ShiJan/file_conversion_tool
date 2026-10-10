$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
Invoke-WebRequest -Uri "https://cdn.jsdelivr.net/gh/tesseract-ocr/tessdata_fast@main/chi_sim.traineddata" -OutFile "src/main/resources/chi_sim.traineddata"
if ((Get-Item "src/main/resources/chi_sim.traineddata").Length -lt 1000000) { throw "OCR 模型下载不完整" }
$actual = (Get-FileHash "src/main/resources/chi_sim.traineddata" -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne "a5fcb6f0db1e1d6d8522f39db4e848f05984669172e584e8d76b6b3141e1f730") { throw "OCR 模型 SHA-256 不匹配" }

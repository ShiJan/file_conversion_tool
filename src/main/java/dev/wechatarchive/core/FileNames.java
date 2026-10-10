package dev.wechatarchive.core;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

public final class FileNames {
    private static final Set<String> RESERVED = Set.of("CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9");

    private FileNames() {}

    public static String clean(String value) {
        if (value == null) return "";
        String result = Normalizer.normalize(value, Normalizer.Form.NFC)
            .replaceAll("[\\p{Cntrl}<>:\"/\\\\|?*]", "_")
            .replaceAll("\\s+", " ").strip()
            .replaceAll("[. ]+$", "");
        if (result.equals(".") || result.equals("..")) return "";
        String stem = result.split("\\.", 2)[0];
        if (RESERVED.contains(stem.toUpperCase(Locale.ROOT))) result = stem + "_" + result.substring(stem.length());
        int count = result.codePointCount(0, result.length());
        if (count > 50) result = result.substring(0, result.offsetByCodePoints(0, 50)).replaceAll("[. ]+$", "");
        return result;
    }

    public static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) throw new IllegalArgumentException("图片缺少扩展名：" + filename);
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!Set.of("png", "jpg", "jpeg").contains(ext)) throw new IllegalArgumentException("只支持 PNG、JPG、JPEG 图片：" + filename);
        return filename.substring(dot);
    }

    public static String zipName(String value) {
        String clean = clean(value);
        if (clean.toLowerCase(Locale.ROOT).endsWith(".zip")) clean = clean(clean.substring(0, clean.length() - 4));
        if (clean.isBlank()) throw new IllegalArgumentException("请填写 ZIP 压缩包名称");
        return clean + ".zip";
    }
}

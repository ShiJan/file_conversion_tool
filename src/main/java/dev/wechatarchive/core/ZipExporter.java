package dev.wechatarchive.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipExporter {
    private ZipExporter() {}

    public static Path export(List<ExportPlan.Item> items, Path destination) throws IOException {
        if (items.isEmpty()) throw new IllegalArgumentException("没有可导出的图片");
        if (Files.exists(destination)) throw new IOException("目标 ZIP 已存在，请更改压缩包名称：" + destination.getFileName());
        Path parent = destination.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".wechat-export-", ".zip.tmp");
        try {
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(temporary))) {
                for (ExportPlan.Item item : items) {
                    out.putNextEntry(new ZipEntry(item.entryName()));
                    Files.copy(item.source(), out);
                    out.closeEntry();
                }
            }
            // Both paths are in one directory; this move refuses to replace an existing ZIP.
            Files.move(temporary, destination);
            return destination;
        } catch (IOException error) {
            throw new IOException("导出失败，未覆盖原文件：" + error.getMessage(), error);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}

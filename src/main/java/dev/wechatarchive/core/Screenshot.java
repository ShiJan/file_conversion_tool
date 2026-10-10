package dev.wechatarchive.core;

import java.nio.file.Path;

public record Screenshot(Path path, String displayName) {
    public Screenshot {
        if (path == null) throw new IllegalArgumentException("图片路径不能为空");
        displayName = displayName == null ? "" : displayName.strip();
    }

    public Screenshot withName(String name) {
        return new Screenshot(path, name);
    }
}

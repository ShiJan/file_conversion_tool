package dev.wechatarchive.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class ExportPlan {
    public record Item(Path source, String entryName) {}
    public enum Separator {
        NONE("不添加", ""), HYPHEN("-", "-"), UNDERSCORE("_", "_");

        private final String label;
        private final String value;

        Separator(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String value() { return value; }
        @Override public String toString() { return label; }
    }

    private ExportPlan() {}

    public static List<Item> create(List<Screenshot> screenshots, String prefix) {
        return create(screenshots, prefix, Separator.HYPHEN, Separator.NONE);
    }

    public static List<Item> create(List<Screenshot> screenshots, String prefix,
                                    Separator prefixNameSeparator, Separator nameNumberSeparator) {
        if (screenshots.isEmpty()) throw new IllegalArgumentException("请先导入图片");
        Objects.requireNonNull(prefixNameSeparator);
        Objects.requireNonNull(nameNumberSeparator);
        String safePrefix = FileNames.clean(prefix);
        if (safePrefix.isBlank()) throw new IllegalArgumentException("请填写文件名前缀");
        List<Item> result = new ArrayList<>();
        Set<String> entryNames = new HashSet<>();
        for (int index = 0; index < screenshots.size(); index++) {
            Screenshot screenshot = screenshots.get(index);
            String name = FileNames.clean(screenshot.displayName());
            if (name.isBlank()) throw new IllegalArgumentException("请填写图片名称：" + screenshot.path().getFileName());
            String extension = FileNames.extension(screenshot.path().getFileName().toString());
            String entry = safePrefix + prefixNameSeparator.value() + name
                + nameNumberSeparator.value() + (index + 1) + extension;
            if (!entryNames.add(entry.toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("生成了重复文件名，请调整名称或连接符：" + entry);
            result.add(new Item(screenshot.path(), entry));
        }
        return List.copyOf(result);
    }
}

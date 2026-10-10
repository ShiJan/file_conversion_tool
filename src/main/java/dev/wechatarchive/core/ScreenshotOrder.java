package dev.wechatarchive.core;

import java.util.Collections;
import java.util.List;

public final class ScreenshotOrder {
    private ScreenshotOrder() {}

    public static <T> void move(List<T> items, int from, int to) {
        if (from < 0 || from >= items.size() || to < 0 || to >= items.size()) return;
        Collections.swap(items, from, to);
    }
}

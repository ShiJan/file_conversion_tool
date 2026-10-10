package dev.wechatarchive.ocr;

import java.awt.Rectangle;
import java.util.List;

public final class TitleRegion {
    private TitleRegion() {}

    // The chat header keeps roughly the same pixel size as screenshots grow vertically.
    // Use screen width as its scale; a percentage of height drifts into message text on long captures.
    public static Rectangle bounds(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("图片尺寸无效");
        int x = Math.max(0, (int) (width * 0.16));
        int y = Math.min(height - 1, Math.max(0, (int) (width * 0.07)));
        int right = Math.min(width, (int) (width * 0.84));
        int bottom = Math.max(y + 1, Math.min(height, (int) (width * 0.26)));
        return new Rectangle(x, y, Math.max(1, right - x), Math.max(1, bottom - y));
    }

    public static List<Rectangle> candidates(int width, int height) {
        Rectangle main = bounds(width, height);
        return List.of(main,
            band(main, width, height, 0.025, 0.16),
            band(main, width, height, 0.115, 0.26));
    }

    private static Rectangle band(Rectangle main, int width, int height, double top, double bottom) {
        int y = Math.min(height - 1, Math.max(0, (int) (width * top)));
        int end = Math.max(y + 1, Math.min(height, (int) (width * bottom)));
        return new Rectangle(main.x, y, main.width, end - y);
    }
}

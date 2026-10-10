package dev.wechatarchive.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

final class IconAssetsTest {
    @Test void dockAndTaskbarIconsContainSquarePlatformSizes() throws Exception {
        var linux = ImageIO.read(Path.of("assets/icon.png").toFile());
        assertEquals(512, linux.getWidth());
        assertEquals(512, linux.getHeight());
        byte[] mac = Files.readAllBytes(Path.of("assets/icon.icns"));
        assertEquals("icns", new String(mac, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));

        byte[] windows = Files.readAllBytes(Path.of("assets/icon.ico"));
        ByteBuffer ico = ByteBuffer.wrap(windows).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, ico.getShort());
        assertEquals(1, ico.getShort());
        int count = Short.toUnsignedInt(ico.getShort());
        Set<Integer> sizes = new HashSet<>();
        for (int index = 0; index < count; index++) {
            int entry = 6 + index * 16;
            int width = Byte.toUnsignedInt(windows[entry]);
            int height = Byte.toUnsignedInt(windows[entry + 1]);
            width = width == 0 ? 256 : width;
            height = height == 0 ? 256 : height;
            assertEquals(width, height);
            sizes.add(width);
        }
        assertEquals(Set.of(16, 32, 48, 64, 128, 256), sizes);
    }
}

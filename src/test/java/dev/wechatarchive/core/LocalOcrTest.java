package dev.wechatarchive.core;

import static org.junit.jupiter.api.Assertions.*;

import dev.wechatarchive.ocr.LocalOcr;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class LocalOcrTest {
    @TempDir Path temp;

    @Test void runsBundledOcrOnSyntheticTitleWithoutReadingBody() throws Exception {
        copyModel();
        LocalOcr.Result normal = LocalOcr.readTitle(createScreenshot(1600), temp);
        LocalOcr.Result longCapture = LocalOcr.readTitle(createScreenshot(8000), temp);
        assertTrue(normal.name().contains("ALICE"), "模拟标题应能识别：" + normal);
        assertEquals(normal.name(), longCapture.name(), "长截图应识别出同一顶部标题");
        assertFalse(longCapture.name().contains("WRONG"), "正文不能成为聊天名称");
    }

    @Test void readsAnEarlySmallTitleInANarrowLongCapture() throws Exception {
        copyModel();
        BufferedImage image = new BufferedImage(300, 9000, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 300, 9000);
        graphics.drawImage(titleFixture("narrow-light.png"), 0, 0, null);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 26));
        graphics.drawString("WRONG", 105, 180);
        graphics.dispose();
        Path screenshot = temp.resolve("narrow-long.png");
        ImageIO.write(image, "png", screenshot.toFile());
        LocalOcr.Result result = LocalOcr.readTitle(screenshot, temp);
        assertTrue(result.name().contains("ALICE"), "窄长图的顶部小字应能识别：" + result);
        assertFalse(result.name().contains("WRONG"), "正文不能成为聊天名称");
    }

    @Test void readsADarkHeaderInANarrowLongCapture() throws Exception {
        copyModel();
        BufferedImage image = new BufferedImage(300, 9000, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 300, 9000);
        graphics.drawImage(titleFixture("narrow-dark.png"), 0, 0, null);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 26));
        graphics.drawString("WRONG", 105, 180);
        graphics.dispose();
        Path screenshot = temp.resolve("dark-narrow-long.png");
        ImageIO.write(image, "png", screenshot.toFile());
        LocalOcr.Result result = LocalOcr.readTitle(screenshot, temp);
        assertTrue(result.name().contains("ALICE"), "深色标题栏应能识别：" + result);
        assertFalse(result.name().contains("WRONG"), "正文不能成为聊天名称");
    }

    @Test void readsAChineseNameInANarrowLongCapture() throws Exception {
        copyModel();
        BufferedImage image = new BufferedImage(300, 9000, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 300, 9000);
        graphics.drawImage(titleFixture("narrow-chinese.png"), 0, 0, null);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 28));
        graphics.drawString("错误正文", 95, 180);
        graphics.dispose();
        Path screenshot = temp.resolve("chinese-narrow-long.png");
        ImageIO.write(image, "png", screenshot.toFile());
        LocalOcr.Result result = LocalOcr.readTitle(screenshot, temp);
        assertEquals("张三", result.name().replace(" ", ""), "窄长图上的中文昵称应能识别：" + result);
    }

    private void copyModel() throws Exception {
        try (var source = getClass().getResourceAsStream("/chi_sim.traineddata")) {
            assertNotNull(source, "便携包必须包含 OCR 模型");
            Files.copy(source, temp.resolve("chi_sim.traineddata"));
        }
    }

    private BufferedImage titleFixture(String name) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/" + name)) {
            assertNotNull(input, "缺少固定像素的 OCR 测试标题：" + name);
            return ImageIO.read(input);
        }
    }

    private Path createScreenshot(int height) throws Exception {
        BufferedImage image = new BufferedImage(900, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 900, height);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 60));
        graphics.drawString("ALICE", 340, 135);
        graphics.drawString("WRONG BODY NAME", 150, 500);
        graphics.dispose();
        Path screenshot = temp.resolve("synthetic-" + height + ".png");
        ImageIO.write(image, "png", screenshot.toFile());
        return screenshot;
    }
}

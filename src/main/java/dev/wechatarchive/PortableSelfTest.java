package dev.wechatarchive;

import dev.wechatarchive.core.ExportPlan;
import dev.wechatarchive.core.Screenshot;
import dev.wechatarchive.core.ZipExporter;
import dev.wechatarchive.ocr.LocalOcr;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/** Exercises the packaged launcher, bundled model, native OCR, and ZIP export. */
public final class PortableSelfTest {
    private PortableSelfTest() {}

    public static void run() throws Exception {
        Class.forName("javafx.scene.control.ComboBox", false, PortableSelfTest.class.getClassLoader());
        Path directory = Files.createTempDirectory("wechat-portable-check-");
        Path model = directory.resolve("chi_sim.traineddata");
        Path image = directory.resolve("sample.png");
        Path archive = directory.resolve("sample.zip");
        try {
            try (InputStream input = PortableSelfTest.class.getResourceAsStream("/chi_sim.traineddata")) {
                if (input == null) throw new IllegalStateException("缺少内置 OCR 模型");
                Files.copy(input, model);
            }
            BufferedImage sample = new BufferedImage(900, 300, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = sample.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 900, 300);
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 60));
            graphics.drawString("ALICE", 340, 135);
            graphics.dispose();
            if (!ImageIO.write(sample, "png", image.toFile()))
                throw new IllegalStateException("无法生成测试图片");

            LocalOcr.Result title = LocalOcr.readTitle(image, directory);
            if (!title.name().contains("ALICE"))
                throw new IllegalStateException("OCR 未能识别测试标题：" + title);
            ZipExporter.export(ExportPlan.create(List.of(new Screenshot(image, "测试")), "记录"), archive);
            try (ZipFile zip = new ZipFile(archive.toFile())) {
                var entry = zip.getEntry("记录-测试1.png");
                if (entry == null || !Arrays.equals(Files.readAllBytes(image), zip.getInputStream(entry).readAllBytes()))
                    throw new IllegalStateException("ZIP 导出内容不正确");
            }
            System.out.println("便携包自检通过");
        } finally {
            Files.deleteIfExists(archive);
            Files.deleteIfExists(image);
            Files.deleteIfExists(model);
            Files.deleteIfExists(directory);
        }
    }
}

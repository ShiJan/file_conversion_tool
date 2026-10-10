import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Generates the original screenshot-stack icon without external build dependencies. */
public final class IconGenerator {
    private static final Path ASSETS = Path.of("assets");
    private static final int[] ICO_SIZES = {16, 32, 48, 64, 128, 256};
    private static final double MAC_DOCK_SCALE = 0.86;

    public static void main(String[] args) throws Exception {
        Files.createDirectories(ASSETS);
        Path iconset = ASSETS.resolve("icon.iconset");
        Files.createDirectories(iconset);
        save(ASSETS.resolve("icon.png"), 512);
        save(Path.of("src/main/resources/icon.png"), 256);
        saveMac(iconset.resolve("icon_16x16.png"), 16);
        saveMac(iconset.resolve("icon_16x16@2x.png"), 32);
        saveMac(iconset.resolve("icon_32x32.png"), 32);
        saveMac(iconset.resolve("icon_32x32@2x.png"), 64);
        saveMac(iconset.resolve("icon_128x128.png"), 128);
        saveMac(iconset.resolve("icon_128x128@2x.png"), 256);
        saveMac(iconset.resolve("icon_256x256.png"), 256);
        saveMac(iconset.resolve("icon_256x256@2x.png"), 512);
        saveMac(iconset.resolve("icon_512x512.png"), 512);
        saveMac(iconset.resolve("icon_512x512@2x.png"), 1024);
        writeIco(ASSETS.resolve("icon.ico"));
    }

    private static void save(Path path, int size) throws IOException {
        Files.createDirectories(path.getParent());
        ImageIO.write(render(size), "png", path.toFile());
    }

    private static void saveMac(Path path, int size) throws IOException {
        Files.createDirectories(path.getParent());
        ImageIO.write(render(size, MAC_DOCK_SCALE), "png", path.toFile());
    }

    private static BufferedImage render(int size) {
        return render(size, 1.0);
    }

    private static BufferedImage render(int size, double scale) {
        int master = 1024;
        BufferedImage image = new BufferedImage(master, master, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(4, 4);
        g.translate(128, 128);
        g.scale(scale, scale);
        g.translate(-128, -128);
        g.setPaint(new GradientPaint(0, 0, new Color(24, 48, 54), 256, 256, new Color(8, 23, 32)));
        g.fill(new RoundRectangle2D.Double(8, 8, 240, 240, 52, 52));

        g.setColor(new Color(0, 0, 0, 110));
        g.fill(new RoundRectangle2D.Double(61, 50, 142, 170, 20, 20));
        g.setColor(new Color(48, 112, 108));
        g.fill(new RoundRectangle2D.Double(48, 39, 140, 170, 18, 18));
        g.setColor(new Color(34, 64, 70));
        g.fill(new RoundRectangle2D.Double(63, 50, 140, 170, 18, 18));
        g.setColor(new Color(133, 199, 185));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new RoundRectangle2D.Double(64, 51, 138, 168, 17, 17));

        g.setColor(new Color(70, 199, 158));
        g.fillRoundRect(79, 69, 108, 27, 8, 8);
        g.setColor(new Color(124, 171, 166));
        g.fillRoundRect(79, 112, 72, 8, 4, 4);
        g.fillRoundRect(97, 132, 54, 8, 4, 4);
        g.fillRoundRect(79, 152, 62, 8, 4, 4);
        g.fillRoundRect(105, 172, 36, 8, 4, 4);

        // A gold zipper marks the export action without using any WeChat brand asset.
        g.setColor(new Color(238, 183, 79));
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(174, 112, 174, 184);
        for (int y = 117; y <= 168; y += 13) {
            g.fillRoundRect(166, y, 16, 5, 3, 3);
        }
        g.fillRoundRect(164, 180, 20, 22, 7, 7);
        g.setColor(new Color(255, 237, 185));
        g.fillOval(171, 186, 6, 6);
        g.dispose();

        if (size == master) return image;
        BufferedImage smaller = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D down = smaller.createGraphics();
        down.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        down.drawImage(image, 0, 0, size, size, null);
        down.dispose();
        return smaller;
    }

    private static void writeIco(Path output) throws IOException {
        byte[][] encoded = new byte[ICO_SIZES.length][];
        int total = 6 + 16 * ICO_SIZES.length;
        for (int i = 0; i < ICO_SIZES.length; i++) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(render(ICO_SIZES[i]), "png", bytes);
            encoded[i] = bytes.toByteArray();
            total += encoded[i].length;
        }
        ByteBuffer ico = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        ico.putShort((short) 0).putShort((short) 1).putShort((short) ICO_SIZES.length);
        int offset = 6 + 16 * ICO_SIZES.length;
        for (int i = 0; i < ICO_SIZES.length; i++) {
            int size = ICO_SIZES[i];
            ico.put((byte) (size == 256 ? 0 : size));
            ico.put((byte) (size == 256 ? 0 : size));
            ico.put((byte) 0).put((byte) 0);
            ico.putShort((short) 1).putShort((short) 32);
            ico.putInt(encoded[i].length).putInt(offset);
            offset += encoded[i].length;
        }
        for (byte[] bytes : encoded) ico.put(bytes);
        Files.write(output, ico.array());
    }
}

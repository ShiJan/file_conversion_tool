package dev.wechatarchive.ocr;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixRead;
import static org.bytedeco.tesseract.global.tesseract.OEM_LSTM_ONLY;
import static org.bytedeco.tesseract.global.tesseract.PSM_SINGLE_LINE;

public final class LocalOcr {
    public record Result(String name, int confidence) {}

    private LocalOcr() {}

    public static Result readTitle(Path image, Path modelDirectory) throws IOException {
        if (!Files.isRegularFile(modelDirectory.resolve("chi_sim.traineddata"))) {
            throw new IOException("缺少内置 OCR 模型 chi_sim.traineddata");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(image.toFile())) {
            if (input == null) throw new IOException("无法读取图片：" + image.getFileName());
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("无法读取图片：" + image.getFileName());
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                return readCandidates(reader, modelDirectory);
            } finally {
                reader.dispose();
            }
        }
    }

    private static Result readCandidates(ImageReader reader, Path modelDirectory) throws IOException {
        Path temp = Files.createTempFile("wechat-title-", ".png");
        try {
            Loader.load(org.bytedeco.tesseract.global.tesseract.class);
            try (TessBaseAPI api = new TessBaseAPI()) {
                if (api.Init(modelDirectory.toString(), "chi_sim", OEM_LSTM_ONLY) != 0) {
                    throw new IOException("OCR 模型加载失败，请检查应用文件是否完整");
                }
                api.SetPageSegMode(PSM_SINGLE_LINE);
                Result best = new Result("", 0);
                for (Rectangle region : TitleRegion.candidates(reader.getWidth(0), reader.getHeight(0))) {
                    ImageReadParam parameters = reader.getDefaultReadParam();
                    parameters.setSourceRegion(region);
                    BufferedImage cropped = reader.read(0, parameters);
                    BufferedImage scaled = upscale(cropped);
                    ImageIO.write(scaled, "png", temp.toFile());
                    PIX pixels = pixRead(temp.toString());
                    if (pixels == null) throw new IOException("标题区域无法读取");
                    try {
                        api.SetImage(pixels);
                        try (BytePointer text = api.GetUTF8Text()) {
                            String value = text == null ? "" : text.getString().replaceAll("[\\r\\n]+", " ").strip();
                            int confidence = api.MeanTextConf();
                            if (!value.isBlank() && confidence > best.confidence()) {
                                best = new Result(value, confidence);
                            }
                        }
                    } finally {
                        pixDestroy(pixels);
                    }
                }
                api.End();
                // A doubtful guess must not silently become the exported filename.
                return best.confidence() >= 50 ? best : new Result("", best.confidence());
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static BufferedImage upscale(BufferedImage cropped) {
        BufferedImage scaled = new BufferedImage(cropped.getWidth() * 3, cropped.getHeight() * 3, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(cropped, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        graphics.dispose();
        return scaled;
    }
}

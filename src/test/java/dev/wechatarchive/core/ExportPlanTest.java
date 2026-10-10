package dev.wechatarchive.core;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ExportPlanTest {
    @TempDir Path temp;

    @Test void numbersAllImagesInDisplayOrderRegardlessOfName() {
        List<Screenshot> files = List.of(
            new Screenshot(temp.resolve("a.png"), "张三"),
            new Screenshot(temp.resolve("b.jpg"), "李四"),
            new Screenshot(temp.resolve("c.png"), "张三"));
        assertEquals(List.of("记录-张三1.png", "记录-李四2.jpg", "记录-张三3.png"),
            ExportPlan.create(files, "记录").stream().map(ExportPlan.Item::entryName).toList());
    }

    @Test void reorderingChangesNumbersAndZipOrder() {
        List<Screenshot> files = new ArrayList<>(List.of(
            new Screenshot(temp.resolve("first.png"), "甲"),
            new Screenshot(temp.resolve("second.png"), "甲"),
            new Screenshot(temp.resolve("third.png"), "乙")));
        ScreenshotOrder.move(files, 1, 0);
        List<ExportPlan.Item> plan = ExportPlan.create(files, "记录");
        assertEquals("second.png", plan.getFirst().source().getFileName().toString());
        assertEquals(List.of("记录-甲1.png", "记录-甲2.png", "记录-乙3.png"),
            plan.stream().map(ExportPlan.Item::entryName).toList());
    }

    @Test void cleansIllegalCharactersAndRejectsBlankNames() {
        assertEquals("张_三_测试", FileNames.clean(" 张/三:测试. "));
        assertEquals("CON_", FileNames.clean("CON"));
        assertEquals("CON_.txt", FileNames.clean("CON.txt"));
        assertThrows(IllegalArgumentException.class,
            () -> ExportPlan.create(List.of(new Screenshot(temp.resolve("a.png"), "  ")), "记录"));
    }

    @Test void sanitizedDuplicateNamesStillGetUniqueNumbers() {
        List<Screenshot> files = List.of(
            new Screenshot(temp.resolve("a.png"), "张/三"),
            new Screenshot(temp.resolve("b.png"), "张:三"),
            new Screenshot(temp.resolve("c.png"), "张_三"));
        assertEquals(List.of("记录-张_三1.png", "记录-张_三2.png", "记录-张_三3.png"),
            ExportPlan.create(files, "记录").stream().map(ExportPlan.Item::entryName).toList());
    }

    @Test void tenImagesUseOneThroughTenEvenWithDifferentNames() {
        List<Screenshot> files = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            files.add(new Screenshot(temp.resolve("image-" + i + ".png"), i % 2 == 0 ? "张三" : "李四"));
        }
        List<ExportPlan.Item> plan = ExportPlan.create(files, "记录");
        for (int i = 0; i < 10; i++) {
            assertTrue(plan.get(i).entryName().endsWith((i + 1) + ".png"));
        }
    }

    @Test void supportsIndependentSeparatorChoices() {
        List<Screenshot> files = List.of(new Screenshot(temp.resolve("a.png"), "张三"));
        for (ExportPlan.Separator prefixName : ExportPlan.Separator.values()) {
            for (ExportPlan.Separator nameNumber : ExportPlan.Separator.values()) {
                String expected = "记录" + prefixName.value() + "张三" + nameNumber.value() + "1.png";
                assertEquals(expected, ExportPlan.create(files, "记录", prefixName, nameNumber).getFirst().entryName());
            }
        }
    }

    @Test void rejectsCollisionsWhenSeparatorIsOmitted() {
        List<Screenshot> files = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            files.add(new Screenshot(temp.resolve(i + ".png"), i == 2 ? "甲1" : "甲"));
        }
        assertThrows(IllegalArgumentException.class,
            () -> ExportPlan.create(files, "记录", ExportPlan.Separator.HYPHEN, ExportPlan.Separator.NONE));
    }

    @Test void zipContainsExpectedNamesAndUnchangedBytes() throws Exception {
        byte[] first = {0, 1, 2, (byte) 255};
        byte[] second = {9, 8, 7};
        Path a = temp.resolve("a.png"), b = temp.resolve("b.jpg");
        Files.write(a, first);
        Files.write(b, second);
        Path output = temp.resolve("聊天.zip");
        ZipExporter.export(ExportPlan.create(List.of(new Screenshot(a, "甲"), new Screenshot(b, "乙")), "记录"), output);
        try (ZipFile zip = new ZipFile(output.toFile())) {
            assertEquals(2, zip.size());
            assertArrayEquals(first, zip.getInputStream(zip.getEntry("记录-甲1.png")).readAllBytes());
            assertArrayEquals(second, zip.getInputStream(zip.getEntry("记录-乙2.jpg")).readAllBytes());
        }
        assertArrayEquals(first, Files.readAllBytes(a));
        assertThrows(Exception.class,
            () -> ZipExporter.export(ExportPlan.create(List.of(new Screenshot(a, "甲")), "记录"), output));
    }
}

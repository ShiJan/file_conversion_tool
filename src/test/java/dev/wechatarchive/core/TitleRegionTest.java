package dev.wechatarchive.core;

import static org.junit.jupiter.api.Assertions.*;

import dev.wechatarchive.ocr.TitleRegion;
import org.junit.jupiter.api.Test;

final class TitleRegionTest {
    @Test void neverIncludesMessageBody() {
        var region = TitleRegion.bounds(1170, 2532);
        assertTrue(region.y > 0);
        assertTrue(region.y + region.height <= 1170 * 0.27);
        assertTrue(region.x >= 1170 * 0.15);
    }

    @Test void longCaptureKeepsTheSameHeaderRegion() {
        assertEquals(TitleRegion.bounds(1170, 2532), TitleRegion.bounds(1170, 18000));
    }

    @Test void allCandidatesStayWithinTheTopOfNarrowLongCaptures() {
        var shortImage = TitleRegion.candidates(300, 900);
        var longImage = TitleRegion.candidates(300, 9000);
        assertEquals(shortImage, longImage);
        assertTrue(longImage.stream().allMatch(region -> region.y + region.height <= 300 * 0.27));
        assertTrue(longImage.stream().allMatch(region -> region.x >= 300 * 0.15));
    }
}

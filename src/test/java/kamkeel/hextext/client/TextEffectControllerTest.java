package kamkeel.hextext.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TextEffectControllerTest {

    @Test
    public void testIgniteBrightnessWithinBounds() {
        long interval = 100L;
        for (int step = 0; step < 10; step++) {
            float value = TextEffectController.computeIgniteBrightness(step * 25L, interval);
            assertTrue("Brightness should not exceed 1", value <= 1.0f + 1e-4f);
            assertTrue("Brightness should not drop below minimum", value >= 0.35f - 1e-4f);
        }
    }

    @Test
    public void testIgniteBrightnessPeaksAndValleys() {
        long interval = 80L;
        float peak = TextEffectController.computeIgniteBrightness(0L, interval);
        float valley = TextEffectController.computeIgniteBrightness(interval, interval);
        assertEquals("Expected ignite to reach full brightness", 1.0f, peak, 1e-4f);
        assertEquals("Expected ignite to dip to minimum brightness", 0.35f, valley, 1e-4f);
    }

    @Test
    public void testRainbowSofteningBringsUpDarkColours() {
        int deepBlue = 0x000033;
        int softened = TextEffectController.softenRainbowColor(deepBlue);
        int originalLuma = (deepBlue >> 16 & 0xFF) + (deepBlue >> 8 & 0xFF) + (deepBlue & 0xFF);
        int softenedLuma = (softened >> 16 & 0xFF) + (softened >> 8 & 0xFF) + (softened & 0xFF);
        assertTrue("Softened rainbow colour should be lighter", softenedLuma > originalLuma);
    }

    @Test
    public void testRainbowSofteningLeavesBrightColoursUntouched() {
        int brightYellow = 0xFFF000;
        int softened = TextEffectController.softenRainbowColor(brightYellow);
        assertEquals("Bright colours should remain unchanged", brightYellow, softened);
    }

    @Test
    public void testScaleBrightnessRespectsFactor() {
        int color = 0x6699CC;
        int darker = TextEffectController.scaleBrightness(color, 0.5f);
        assertEquals(Math.round(0x66 * 0.5f), darker >> 16 & 0xFF);
        assertEquals(Math.round(0x99 * 0.5f), darker >> 8 & 0xFF);
        assertEquals(Math.round(0xCC * 0.5f), darker & 0xFF);
    }
}

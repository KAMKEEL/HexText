package kamkeel.hextext.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TextEffectMathTest {

    @Test
    public void rainbowColourKeepsBlueReadable() {
        int color = TextEffectMath.computeRainbowColor(0L, 3000.0, 20, 0, 12.0f);
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        assertTrue("Expected blue component to remain bright", blue >= 240);
        assertTrue("Rainbow colour should retain some red for readability", red > 0);
        assertTrue("Rainbow colour should retain some green for readability", green > 0);
    }

    @Test
    public void igniteBrightnessFormsTriangleWave() {
        float minFactor = 0.35f;
        long interval = 100L;
        assertEquals(1.0f, TextEffectMath.computeIgniteBrightness(0L, interval, minFactor), 0.0001f);
        assertEquals(minFactor, TextEffectMath.computeIgniteBrightness(interval, interval, minFactor), 0.0001f);
        assertEquals(1.0f, TextEffectMath.computeIgniteBrightness(interval * 2L, interval, minFactor), 0.0001f);
        float midpoint = TextEffectMath.computeIgniteBrightness(interval / 2L, interval, minFactor);
        float expectedMidpoint = minFactor + (1.0f - minFactor) * 0.5f;
        assertEquals(expectedMidpoint, midpoint, 0.0001f);
    }

    @Test
    public void shakeSeedChangesWithTimeWindow() {
        long seedA = TextEffectMath.computeShakeSeed(2, 1000L, 100L);
        long seedB = TextEffectMath.computeShakeSeed(2, 1200L, 100L);
        long seedC = TextEffectMath.computeShakeSeed(3, 1000L, 100L);
        assertTrue(seedA != seedB);
        assertTrue(seedA != seedC);
    }
}

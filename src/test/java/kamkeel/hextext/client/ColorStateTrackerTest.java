package kamkeel.hextext.client;

import kamkeel.hextext.util.ColorCodeUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ColorStateTrackerTest {

    @Test
    public void testResetToBaseRestoresInitialColour() {
        ColorStateTracker tracker = new ColorStateTracker();
        tracker.begin(0xFFFFFF, false);
        tracker.applyRgb(0xFF0000, false, null, false);
        assertEquals(0xFF0000, tracker.getCurrentColor());
        int restored = tracker.resetToBase(null, false);
        assertEquals(0xFFFFFF, restored);
        assertEquals(0xFFFFFF, tracker.getCurrentColor());
    }

    @Test
    public void testPushAndPopRoundTrip() {
        ColorStateTracker tracker = new ColorStateTracker();
        tracker.begin(0xFFFFFF, false);
        tracker.applyRgb(0x00FF00, false, null, false);
        tracker.push(0x0000FF, null, false);
        assertEquals(0x0000FF, tracker.getCurrentColor());
        int popped = tracker.pop(null, false);
        assertEquals(0x00FF00, popped);
        assertEquals(0x00FF00, tracker.getCurrentColor());
    }

    @Test
    public void testApplyRgbClearsStackWhenRequested() {
        ColorStateTracker tracker = new ColorStateTracker();
        tracker.begin(0xFFFFFF, false);
        tracker.push(0xFF0000, null, false);
        tracker.applyRgb(0x00FF00, true, null, false);
        int popped = tracker.pop(null, false);
        assertEquals(0xFFFFFF, popped);
    }

    @Test
    public void testVanillaColorUsesPalette() {
        ColorStateTracker tracker = new ColorStateTracker();
        tracker.begin(0xFFFFFF, false);
        int[] palette = new int[32];
        palette[5] = 0x123456;
        int applied = tracker.applyVanillaColor(5, palette, false, null, false);
        assertEquals(0x123456, applied);
    }

    @Test
    public void testShadowRgbConversion() {
        ColorStateTracker tracker = new ColorStateTracker();
        tracker.begin(0xFFFFFF, true);
        int rgb = 0x336699;
        int applied = tracker.applyRgb(rgb, false, null, true);
        assertEquals(ColorCodeUtils.calculateShadowColor(rgb), applied);
    }
}

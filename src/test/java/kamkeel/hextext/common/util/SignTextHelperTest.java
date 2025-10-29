package kamkeel.hextext.common.util;

import org.junit.Assert;
import org.junit.Test;

public class SignTextHelperTest {

    @Test
    public void clampPreservesFormatting() {
        String text = "\u00A7aHello\u00A7r World!"; // 12 visible characters
        String clamped = SignTextHelper.clampToVisibleLimit(text);
        Assert.assertEquals(text, clamped);
    }

    @Test
    public void clampTruncatesBeyondLimit() {
        String text = "\u00A7b1234567890123456"; // 16 visible characters
        String clamped = SignTextHelper.clampToVisibleLimit(text);
        Assert.assertEquals("\u00A7b123456789012345", clamped);
        Assert.assertEquals(15, SignTextHelper.visibleLength(clamped));
    }
}

package kamkeel.hextext.client.render;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.client.support.SimpleCharWidthFunction;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FormattedTextMetricsTest {

    @Before
    public void setUp() {
        HexText.proxy = new CommonProxy();
    }

    @Test
    public void calculateMaxLineWidthSkipsFormattingCodes() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        float width = FormattedTextMetrics.calculateMaxLineWidth("&aAB", false, widthFunction, 0.0f, 1.0f);
        assertEquals(10.0f, width, 0.0001f);
    }

    @Test
    public void calculateMaxLineWidthAccountsForBold() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        float width = FormattedTextMetrics.calculateMaxLineWidth("&lAB&rC", false, widthFunction, 0.0f, 1.0f);
        assertEquals(17.0f, width, 0.0001f);
    }

    @Test
    public void computeLineBreakHonoursSafePositions() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        widthFunction.setWidth(' ', 2.0f);
        int breakIndex = FormattedTextMetrics.computeLineBreakIndex("AB CD", 12, false, widthFunction, 0.0f, 1.0f);
        assertEquals(3, breakIndex);
    }

    @Test
    public void formattingCodesDoNotIncreaseWidth() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        float width = FormattedTextMetrics.calculateMaxLineWidth("&hFlip&i&j", false, widthFunction, 0.0f, 1.0f);
        assertEquals(20.0f, width, 0.0001f);
    }

    @Test
    public void boldSurvivesEffects() {
        SimpleCharWidthFunction widthFunction = new SimpleCharWidthFunction(5.0f);
        float width = FormattedTextMetrics.calculateMaxLineWidth("&lA&jB", false, widthFunction, 0.0f, 1.0f);
        assertEquals(12.0f, width, 0.0001f);
    }
}

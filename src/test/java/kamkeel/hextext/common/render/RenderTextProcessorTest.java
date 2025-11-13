package kamkeel.hextext.common.render;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.config.HexTextConfig;

import org.junit.Test;
import org.junit.Before;

import kamkeel.hextext.api.rendering.RenderDirective;
import kamkeel.hextext.api.rendering.RenderPlan;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RenderTextProcessorTest {

    @Before
    public void setUp() {
        HexText.proxy = new CommonProxy();
        HexTextConfig.resetToDefaults();
        HexTextConfig.setUniversalAmpersandEnabled(true);
        HexTextConfig.setEnableRgbHtmlFormat(true);
    }

    @Test
    public void testNonRawHexProcessing() {
        RenderPlan data = RenderTextProcessor.prepare("&#FFAA11Hello", false);
        assertTrue(data.shouldReplaceText());
        assertEquals("Hello", data.getDisplayText());
        assertTrue(data.hasInstructions());
        Map<Integer, List<RenderDirective>> instructions = data.getInstructions();
        assertNotNull(instructions);
        List<RenderDirective> atZero = instructions.get(0);
        assertEquals(1, atZero.size());
        RenderDirectiveImpl instruction = (RenderDirectiveImpl) atZero.get(0);
        assertEquals(RenderDirectiveImpl.Type.APPLY_RGB, instruction.getType());
        assertEquals(0xFFAA11, instruction.getRgb());
        assertTrue(instruction.shouldClearStack());
    }

    @Test
    public void testRawVanillaFormatting() {
        RenderPlan data = RenderTextProcessor.prepare("&aHello", true);
        assertFalse(data.shouldReplaceText());
        assertNull(data.getDisplayText());
        assertTrue(data.hasInstructions());
        Map<Integer, List<RenderDirective>> instructions = data.getInstructions();
        assertNotNull(instructions);
        List<RenderDirective> atZero = instructions.get(0);
        assertNotNull(atZero);
        assertEquals(1, atZero.size());
        RenderDirectiveImpl instruction = (RenderDirectiveImpl) atZero.get(0);
        assertEquals(RenderDirectiveImpl.Type.APPLY_VANILLA_COLOR, instruction.getType());
        assertEquals(10, instruction.getParameter());
    }

    @Test
    public void testRawHexColor() {
        RenderPlan data = RenderTextProcessor.prepare("&#ABCDEFWorld", true);
        assertFalse(data.shouldReplaceText());
        assertNull(data.getDisplayText());
        Map<Integer, List<RenderDirective>> instructions = data.getInstructions();
        assertNotNull(instructions);
        assertEquals(1, instructions.size());
        RenderDirectiveImpl instruction = (RenderDirectiveImpl) instructions.get(0).get(0);
        assertEquals(RenderDirectiveImpl.Type.APPLY_RGB, instruction.getType());
        assertEquals(0xABCDEF, instruction.getRgb());
    }

    @Test
    public void testSectionSignHexProcessing() {
        RenderPlan data = RenderTextProcessor.prepare("§#123456Hello", false);
        assertTrue(data.shouldReplaceText());
        assertEquals("Hello", data.getDisplayText());
        assertTrue(data.hasInstructions());
        Map<Integer, List<RenderDirective>> instructions = data.getInstructions();
        assertNotNull(instructions);
        List<RenderDirective> atZero = instructions.get(0);
        assertNotNull(atZero);
        assertFalse(atZero.isEmpty());
        RenderDirectiveImpl instruction = (RenderDirectiveImpl) atZero.get(0);
        assertEquals(RenderDirectiveImpl.Type.APPLY_RGB, instruction.getType());
        assertEquals(0x123456, instruction.getRgb());
        assertTrue(instruction.shouldClearStack());
    }

    @Test
    public void testRawBoldFormatting() {
        RenderPlan data = RenderTextProcessor.prepare("&lBold", true);
        assertFalse(data.shouldReplaceText());
        assertNull(data.getDisplayText());
        Map<Integer, List<RenderDirective>> instructions = data.getInstructions();
        assertNotNull(instructions);
        List<RenderDirective> atZero = instructions.get(0);
        assertNotNull(atZero);
        assertEquals(1, atZero.size());
        RenderDirectiveImpl instruction = (RenderDirectiveImpl) atZero.get(0);
        assertEquals(RenderDirectiveImpl.Type.SET_BOLD, instruction.getType());
        assertTrue(instruction.isEnabled());
    }

    @Test
    public void testRawFormattingAfterPlainText() {
        String input = "Pre Stuff &lPost";
        RenderPlan data = RenderTextProcessor.prepare(input, true);
        assertFalse(data.shouldReplaceText());
        assertTrue(data.hasInstructions());
        Map<Integer, List<RenderDirective>> instructions = data.getInstructions();
        assertNotNull(instructions);
        int tokenIndex = input.indexOf('&');
        assertTrue("Expected instruction entry for formatting token", instructions.containsKey(tokenIndex));
        List<RenderDirective> atIndex = instructions.get(tokenIndex);
        assertNotNull(atIndex);
        assertFalse(atIndex.isEmpty());
        assertEquals(RenderDirectiveImpl.Type.SET_BOLD, ((RenderDirectiveImpl) atIndex.get(0)).getType());
    }

    @Test
    public void testResetInstruction() {
        RenderPlan data = RenderTextProcessor.prepare("&rReset", false);
        assertTrue(data.shouldReplaceText());
        assertEquals("§rReset", data.getDisplayText());
        Map<Integer, List<RenderDirective>> instructions = data.getInstructions();
        assertNotNull(instructions);
        RenderDirectiveImpl instruction = (RenderDirectiveImpl) instructions.get(0).get(0);
        assertEquals(RenderDirectiveImpl.Type.RESET_TO_BASE, instruction.getType());
    }

    @Test
    public void testRgbTagClearsFormatting() {
        RenderPlan data = RenderTextProcessor.prepare("&lBold <123456>World", false);
        assertTrue(data.hasInstructions());
        boolean foundPush = false;
        for (List<RenderDirective> instructionList : data.getInstructions().values()) {
            for (RenderDirective instruction : instructionList) {
                RenderDirectiveImpl renderInstruction = (RenderDirectiveImpl) instruction;
                if (renderInstruction.getType() == RenderDirectiveImpl.Type.PUSH_RGB) {
                    foundPush = true;
                    assertTrue("Expected RGB push to clear formatting", renderInstruction.resetsFormatting());
                }
            }
        }
        assertTrue("Expected PUSH_RGB instruction", foundPush);
    }

    @Test
    public void testRainbowInstructionNonRaw() {
        RenderPlan data = RenderTextProcessor.prepare("&gRainbow", false);
        assertTrue(data.shouldReplaceText());
        assertEquals("Rainbow", data.getDisplayText());
        assertTrue(data.hasInstructions());
        List<RenderDirective> instructions = data.getInstructions().get(0);
        assertNotNull(instructions);
        assertFalse(instructions.isEmpty());
        RenderDirectiveImpl instruction = (RenderDirectiveImpl) instructions.get(0);
        assertEquals(RenderDirectiveImpl.Type.SET_RAINBOW, instruction.getType());
        assertTrue(instruction.isEnabled());
        assertEquals(0, instruction.getParameter());
        assertTrue(instruction.resetsFormatting());
    }

    @Test
    public void testNonRawVanillaColorProducesInstruction() {
        RenderPlan data = RenderTextProcessor.prepare("&aGreen", false);
        assertTrue(data.shouldReplaceText());
        assertEquals("§aGreen", data.getDisplayText());
        assertTrue(data.hasInstructions());
        List<RenderDirective> instructions = data.getInstructions().get(0);
        assertNotNull(instructions);
        assertFalse(instructions.isEmpty());
        RenderDirectiveImpl instruction = (RenderDirectiveImpl) instructions.get(0);
        assertEquals(RenderDirectiveImpl.Type.APPLY_VANILLA_COLOR, instruction.getType());
        assertEquals(10, instruction.getParameter());
    }

    @Test
    public void testSectionSignEffectInstruction() {
        RenderPlan data = RenderTextProcessor.prepare("§gWave", false);
        assertFalse(data.shouldReplaceText());
        assertTrue(data.hasInstructions());
        List<RenderDirective> instructions = data.getInstructions().get(0);
        assertNotNull(instructions);
        assertFalse(instructions.isEmpty());
        assertEquals(RenderDirectiveImpl.Type.SET_RAINBOW, ((RenderDirectiveImpl) instructions.get(0)).getType());
    }

    @Test
    public void testDinnerboneInstructionRaw() {
        RenderPlan data = RenderTextProcessor.prepare("&hFlip", true);
        assertFalse(data.shouldReplaceText());
        assertTrue(data.hasInstructions());
        List<RenderDirective> instructions = data.getInstructions().get(0);
        assertNotNull(instructions);
        boolean found = false;
        for (RenderDirective instruction : instructions) {
            RenderDirectiveImpl renderInstruction = (RenderDirectiveImpl) instruction;
            if (renderInstruction.getType() == RenderDirectiveImpl.Type.SET_DINNERBONE) {
                assertTrue(renderInstruction.isEnabled());
                found = true;
            }
        }
        assertTrue("Expected dinnerbone instruction", found);
    }

    @Test
    public void testRawResetInstructionProducesInstruction() {
        RenderPlan data = RenderTextProcessor.prepare("&rTest", true);
        assertFalse(data.shouldReplaceText());
        assertTrue(data.hasInstructions());
        List<RenderDirective> instructions = data.getInstructions().get(0);
        assertNotNull(instructions);
        assertFalse(instructions.isEmpty());
        RenderDirectiveImpl instruction = (RenderDirectiveImpl) instructions.get(0);
        assertEquals(RenderDirectiveImpl.Type.RESET_TO_BASE, instruction.getType());
        assertTrue(instruction.resetsFormatting());
    }

    @Test
    public void testIgniteAndShakeInstructionsExist() {
        RenderPlan data = RenderTextProcessor.prepare("&iFire &jShake", false);
        assertTrue(data.shouldReplaceText());
        assertEquals("Fire Shake", data.getDisplayText());
        assertTrue(data.hasInstructions());
        assertTrue(data.getInstructions().containsKey(0));
        boolean sawIgnite = false;
        for (RenderDirective instruction : data.getInstructions().get(0)) {
            RenderDirectiveImpl renderInstruction = (RenderDirectiveImpl) instruction;
            if (renderInstruction.getType() == RenderDirectiveImpl.Type.SET_IGNITE) {
                sawIgnite = true;
            }
        }
        assertTrue("Expected ignite instruction", sawIgnite);
        int shakeIndex = data.getDisplayText().indexOf('S');
        assertTrue(data.getInstructions().containsKey(shakeIndex));
        boolean sawShake = false;
        for (RenderDirective instruction : data.getInstructions().get(shakeIndex)) {
            RenderDirectiveImpl renderInstruction = (RenderDirectiveImpl) instruction;
            if (renderInstruction.getType() == RenderDirectiveImpl.Type.SET_SHAKE) {
                sawShake = true;
            }
        }
        assertTrue("Expected shake instruction", sawShake);
    }

    @Test
    public void testEffectsStackWithStylesAndColours() {
        RenderPlan data = RenderTextProcessor.prepare("&4&l&jTest", false);
        assertTrue(data.hasInstructions());

        boolean sawColor = false;
        boolean sawBold = false;
        boolean sawShake = false;

        for (List<RenderDirective> instructionList : data.getInstructions().values()) {
            for (RenderDirective instruction : instructionList) {
                RenderDirectiveImpl renderInstruction = (RenderDirectiveImpl) instruction;
                switch (renderInstruction.getType()) {
                    case APPLY_VANILLA_COLOR:
                        sawColor = true;
                        break;
                    case SET_BOLD:
                        sawBold |= renderInstruction.isEnabled();
                        break;
                    case SET_SHAKE:
                        sawShake |= renderInstruction.isEnabled();
                        break;
                    default:
                        break;
                }
            }
        }

        assertTrue("Expected vanilla colour instruction", sawColor);
        assertTrue("Expected bold instruction", sawBold);
        assertTrue("Expected shake instruction", sawShake);
    }

    @Test
    public void testHtmlRgbDisabledTreatsTagsAsText() {
        HexTextConfig.setEnableRgbHtmlFormat(false);
        RenderPlan data = RenderTextProcessor.prepare("<123456>World", false);
        assertFalse(data.hasInstructions());
        assertFalse(data.shouldReplaceText());
        assertNull(data.getDisplayText());
    }

    @Test
    public void testAmpersandDisabledTreatsTokensAsText() {
        HexTextConfig.setUniversalAmpersandEnabled(false);
        RenderPlan data = RenderTextProcessor.prepare("&aGreen", false);
        assertFalse(data.hasInstructions());
        assertFalse(data.shouldReplaceText());
        assertNull(data.getDisplayText());
        RenderPlan rgbData = RenderTextProcessor.prepare("&#FFAA11Text", false);
        assertFalse(rgbData.hasInstructions());
        assertFalse(rgbData.shouldReplaceText());
        assertNull(rgbData.getDisplayText());
    }
}

package kamkeel.hextext.client;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class RenderTextProcessorTest {

    @Test
    public void testNonRawHexProcessing() {
        RenderTextData data = RenderTextProcessor.prepare("&FFAA11Hello", false);
        assertTrue(data.shouldReplaceText());
        assertEquals("Hello", data.getDisplayText());
        assertTrue(data.hasInstructions());
        Map<Integer, List<RenderInstruction>> instructions = data.getInstructions();
        assertNotNull(instructions);
        List<RenderInstruction> atZero = instructions.get(0);
        assertEquals(1, atZero.size());
        RenderInstruction instruction = atZero.get(0);
        assertEquals(RenderInstruction.Type.APPLY_RGB, instruction.getType());
        assertEquals(0xFFAA11, instruction.getRgb());
        assertTrue(instruction.shouldClearStack());
    }

    @Test
    public void testRawVanillaFormatting() {
        RenderTextData data = RenderTextProcessor.prepare("&aHello", true);
        assertFalse(data.shouldReplaceText());
        assertNull(data.getDisplayText());
        assertTrue(data.hasInstructions());
        Map<Integer, List<RenderInstruction>> instructions = data.getInstructions();
        assertNotNull(instructions);
        List<RenderInstruction> atZero = instructions.get(0);
        assertNotNull(atZero);
        assertEquals(1, atZero.size());
        RenderInstruction instruction = atZero.get(0);
        assertEquals(RenderInstruction.Type.APPLY_VANILLA_COLOR, instruction.getType());
        assertEquals(10, instruction.getParameter());
    }

    @Test
    public void testRawHexColor() {
        RenderTextData data = RenderTextProcessor.prepare("&ABCDEFWorld", true);
        assertFalse(data.shouldReplaceText());
        assertNull(data.getDisplayText());
        Map<Integer, List<RenderInstruction>> instructions = data.getInstructions();
        assertNotNull(instructions);
        assertEquals(1, instructions.size());
        RenderInstruction instruction = instructions.get(0).get(0);
        assertEquals(RenderInstruction.Type.APPLY_RGB, instruction.getType());
        assertEquals(0xABCDEF, instruction.getRgb());
    }

    @Test
    public void testRawBoldFormatting() {
        RenderTextData data = RenderTextProcessor.prepare("&lBold", true);
        assertFalse(data.shouldReplaceText());
        assertNull(data.getDisplayText());
        Map<Integer, List<RenderInstruction>> instructions = data.getInstructions();
        assertNotNull(instructions);
        List<RenderInstruction> atZero = instructions.get(0);
        assertNotNull(atZero);
        assertEquals(1, atZero.size());
        RenderInstruction instruction = atZero.get(0);
        assertEquals(RenderInstruction.Type.SET_BOLD, instruction.getType());
        assertTrue(instruction.isEnabled());
    }

    @Test
    public void testRawFormattingAfterPlainText() {
        String input = "Pre Stuff &lPost";
        RenderTextData data = RenderTextProcessor.prepare(input, true);
        assertFalse(data.shouldReplaceText());
        assertTrue(data.hasInstructions());
        Map<Integer, List<RenderInstruction>> instructions = data.getInstructions();
        assertNotNull(instructions);
        int tokenIndex = input.indexOf('&');
        assertTrue("Expected instruction entry for formatting token", instructions.containsKey(tokenIndex));
        List<RenderInstruction> atIndex = instructions.get(tokenIndex);
        assertNotNull(atIndex);
        assertFalse(atIndex.isEmpty());
        assertEquals(RenderInstruction.Type.SET_BOLD, atIndex.get(0).getType());
    }

    @Test
    public void testResetInstruction() {
        RenderTextData data = RenderTextProcessor.prepare("&rReset", false);
        assertTrue(data.shouldReplaceText());
        assertEquals("§rReset", data.getDisplayText());
        Map<Integer, List<RenderInstruction>> instructions = data.getInstructions();
        assertNotNull(instructions);
        RenderInstruction instruction = instructions.get(0).get(0);
        assertEquals(RenderInstruction.Type.RESET_TO_BASE, instruction.getType());
    }

    @Test
    public void testRgbTagClearsFormatting() {
        RenderTextData data = RenderTextProcessor.prepare("&lBold <123456>World", false);
        assertTrue(data.hasInstructions());
        boolean foundPush = false;
        for (List<RenderInstruction> instructionList : data.getInstructions().values()) {
            for (RenderInstruction instruction : instructionList) {
                if (instruction.getType() == RenderInstruction.Type.PUSH_RGB) {
                    foundPush = true;
                    assertTrue("Expected RGB push to clear formatting", instruction.resetsFormatting());
                }
            }
        }
        assertTrue("Expected PUSH_RGB instruction", foundPush);
    }

    @Test
    public void testRainbowInstructionNonRaw() {
        RenderTextData data = RenderTextProcessor.prepare("&gRainbow", false);
        assertTrue(data.shouldReplaceText());
        assertEquals("Rainbow", data.getDisplayText());
        assertTrue(data.hasInstructions());
        List<RenderInstruction> instructions = data.getInstructions().get(0);
        assertNotNull(instructions);
        assertFalse(instructions.isEmpty());
        RenderInstruction instruction = instructions.get(0);
        assertEquals(RenderInstruction.Type.SET_RAINBOW, instruction.getType());
        assertTrue(instruction.isEnabled());
        assertEquals(0, instruction.getParameter());
    }

    @Test
    public void testNonRawVanillaColorProducesInstruction() {
        RenderTextData data = RenderTextProcessor.prepare("&aGreen", false);
        assertTrue(data.shouldReplaceText());
        assertEquals("§aGreen", data.getDisplayText());
        assertTrue(data.hasInstructions());
        List<RenderInstruction> instructions = data.getInstructions().get(0);
        assertNotNull(instructions);
        assertFalse(instructions.isEmpty());
        assertEquals(RenderInstruction.Type.APPLY_VANILLA_COLOR, instructions.get(0).getType());
        assertEquals(10, instructions.get(0).getParameter());
    }

    @Test
    public void testSectionSignEffectInstruction() {
        RenderTextData data = RenderTextProcessor.prepare("§gWave", false);
        assertFalse(data.shouldReplaceText());
        assertTrue(data.hasInstructions());
        List<RenderInstruction> instructions = data.getInstructions().get(0);
        assertNotNull(instructions);
        assertFalse(instructions.isEmpty());
        assertEquals(RenderInstruction.Type.SET_RAINBOW, instructions.get(0).getType());
    }

    @Test
    public void testDinnerboneInstructionRaw() {
        RenderTextData data = RenderTextProcessor.prepare("&hFlip", true);
        assertFalse(data.shouldReplaceText());
        assertTrue(data.hasInstructions());
        List<RenderInstruction> instructions = data.getInstructions().get(0);
        assertNotNull(instructions);
        boolean found = false;
        for (RenderInstruction instruction : instructions) {
            if (instruction.getType() == RenderInstruction.Type.SET_DINNERBONE) {
                assertTrue(instruction.isEnabled());
                found = true;
            }
        }
        assertTrue("Expected dinnerbone instruction", found);
    }
}

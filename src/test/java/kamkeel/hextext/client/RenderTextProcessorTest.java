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
    public void testResetInstruction() {
        RenderTextData data = RenderTextProcessor.prepare("&rReset", false);
        assertTrue(data.shouldReplaceText());
        assertEquals("§rReset", data.getDisplayText());
        Map<Integer, List<RenderInstruction>> instructions = data.getInstructions();
        assertNotNull(instructions);
        RenderInstruction instruction = instructions.get(0).get(0);
        assertEquals(RenderInstruction.Type.RESET_TO_BASE, instruction.getType());
    }
}

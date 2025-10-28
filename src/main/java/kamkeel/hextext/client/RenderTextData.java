package kamkeel.hextext.client;

import java.util.List;
import java.util.Map;

/**
 * Holds the sanitized string and deferred render instructions calculated for font rendering.
 */
public final class RenderTextData {

    private final String displayText;
    private final boolean replaceText;
    private final Map<Integer, List<RenderInstruction>> instructions;

    private RenderTextData(String displayText, boolean replaceText, Map<Integer, List<RenderInstruction>> instructions) {
        this.displayText = displayText;
        this.replaceText = replaceText;
        this.instructions = instructions;
    }

    public static RenderTextData unchanged() {
        return new RenderTextData(null, false, null);
    }

    public static RenderTextData withDisplayText(String displayText) {
        return new RenderTextData(displayText, true, null);
    }

    public static RenderTextData withDisplayText(String displayText, Map<Integer, List<RenderInstruction>> instructions) {
        return new RenderTextData(displayText, true, instructions);
    }

    public static RenderTextData withInstructions(Map<Integer, List<RenderInstruction>> instructions) {
        return new RenderTextData(null, false, instructions);
    }

    public String getDisplayText() {
        return displayText;
    }

    public boolean shouldReplaceText() {
        return replaceText;
    }

    public Map<Integer, List<RenderInstruction>> getInstructions() {
        return instructions;
    }

    public boolean hasInstructions() {
        return instructions != null && !instructions.isEmpty();
    }
}

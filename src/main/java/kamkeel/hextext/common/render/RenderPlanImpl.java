package kamkeel.hextext.common.render;

import kamkeel.hextext.api.rendering.RenderDirective;
import kamkeel.hextext.api.rendering.RenderPlan;

import java.util.List;
import java.util.Map;

/**
 * Holds the sanitized string and deferred render instructions calculated for font rendering.
 */
public final class RenderPlanImpl implements RenderPlan {

    private final String displayText;
    private final boolean replaceText;
    private final Map<Integer, List<RenderDirective>> instructions;

    private RenderPlanImpl(String displayText, boolean replaceText, Map<Integer, List<RenderDirective>> instructions) {
        this.displayText = displayText;
        this.replaceText = replaceText;
        this.instructions = instructions;
    }

    public static RenderPlanImpl unchanged() {
        return new RenderPlanImpl(null, false, null);
    }

    public static RenderPlanImpl withDisplayText(String displayText) {
        return new RenderPlanImpl(displayText, true, null);
    }

    public static RenderPlanImpl withDisplayText(String displayText, Map<Integer, List<RenderDirective>> instructions) {
        return new RenderPlanImpl(displayText, true, instructions);
    }

    public static RenderPlanImpl withInstructions(Map<Integer, List<RenderDirective>> instructions) {
        return new RenderPlanImpl(null, false, instructions);
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    @Override
    public boolean shouldReplaceText() {
        return replaceText;
    }

    @Override
    public Map<Integer, List<RenderDirective>> getInstructions() {
        return instructions;
    }

    @Override
    public boolean hasInstructions() {
        return instructions != null && !instructions.isEmpty();
    }
}

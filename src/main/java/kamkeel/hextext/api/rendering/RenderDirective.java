package kamkeel.hextext.api.rendering;

/**
 * Describes a deferred render operation emitted while preparing text for the HexText font rendering pipeline.
 */
public interface RenderDirective {

    /**
     * Enumerates the operations that can be applied to the font renderer.
     */
    interface InstructionType {
    }

    /**
     * Returns the logical type of instruction that should be executed.
     */
    InstructionType getType();

    /**
     * Returns the RGB colour associated with the instruction, when relevant.
     */
    int getRgb();

    /**
     * Indicates whether the instruction clears the colour stack before
     * applying itself.
     */
    boolean shouldClearStack();

    /**
     * Returns an integer parameter associated with the instruction.
     */
    int getParameter();

    /**
     * Returns {@code true} if the instruction enables a toggled effect.
     */
    boolean isEnabled();

    /**
     * Returns {@code true} if executing the instruction should reset other
     * formatting state before applying its own changes.
     */
    boolean resetsFormatting();
}

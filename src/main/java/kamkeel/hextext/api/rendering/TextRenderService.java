package kamkeel.hextext.api.rendering;

/**
 * Entry point for preparing text and render directives compatible with HexText's font renderer hooks.
 */
public interface TextRenderService {

    /**
     * Processes the supplied text and returns sanitized output plus any deferred render directives that should run
     * alongside it.
     *
     * @param text    the input text to process
     * @param rawMode whether the caller is rendering in raw edit mode
     * @return the processed text result
     */
    RenderPlan prepare(String text, boolean rawMode);
}

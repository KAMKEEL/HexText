package kamkeel.hextext.api.rendering;

/**
 * Exposes contextual information about HexText's font rendering pipeline.
 */
public interface RenderingEnvironmentService {

    /**
     * Returns {@code true} when HexText is currently rendering raw text without vanilla formatting
     * interpretation.
     */
    boolean isRawTextRendering();

    /**
     * Increments the raw text rendering depth for the current thread.
     */
    void pushRawTextRendering();

    /**
     * Decrements the raw text rendering depth for the current thread.
     */
    void popRawTextRendering();
}

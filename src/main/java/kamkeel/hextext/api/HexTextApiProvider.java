package kamkeel.hextext.api;

import kamkeel.hextext.api.rendering.TextRenderService;
import kamkeel.hextext.api.rendering.TokenHighlightService;
import kamkeel.hextext.api.sign.SignInteractionRegistry;
import kamkeel.hextext.api.sign.SignStateService;
import kamkeel.hextext.api.text.SignTextService;
import kamkeel.hextext.api.text.TextFormatter;
import kamkeel.hextext.api.text.TextSanitizer;

/**
 * Internal hook that allows the HexText mod to provide the backing implementation for the public API.
 */
public interface HexTextApiProvider {

    /**
     * Returns the running HexText mod version.
     */
    String modVersion();

    /**
     * Returns the configured sign interaction registry implementation.
     */
    SignInteractionRegistry signInteractions();

    /**
     * Returns the text helper implementation.
     */
    SignTextService signText();

    /**
     * Returns the formatting helper implementation.
     */
    TextFormatter textFormatter();

    /**
     * Returns the text normalisation helper implementation.
     */
    TextSanitizer textSanitizer();

    /**
     * Returns the text rendering helper implementation.
     */
    TextRenderService textRenderer();

    /**
     * Returns the token highlighting helper implementation.
     */
    TokenHighlightService tokenHighlighter();

    /**
     * Returns the sign state helper implementation.
     */
    SignStateService signState();
}

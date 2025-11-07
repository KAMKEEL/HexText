package kamkeel.hextext.api;

import kamkeel.hextext.api.sign.SignInteractionRegistry;
import kamkeel.hextext.api.sign.SignStateApi;
import kamkeel.hextext.api.text.SignTextApi;
import kamkeel.hextext.api.text.TextFormattingApi;

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
    SignTextApi signText();

    /**
     * Returns the formatting helper implementation.
     */
    TextFormattingApi textFormatting();

    /**
     * Returns the sign state helper implementation.
     */
    SignStateApi signState();
}

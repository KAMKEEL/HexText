package kamkeel.hextext.api.text;

/**
 * Utility helpers that operate on sign text while respecting HexText formatting rules.
 */
public interface SignTextApi {

    /**
     * Maximum number of visible characters supported on a sign line.
     */
    int visibleCharacterLimit();

    /**
     * Calculates the visible length of the supplied sign line.
     */
    int visibleLength(CharSequence text);

    /**
     * Clamps the text to the visible character limit while preserving formatting codes.
     */
    String clampToVisibleLimit(String text);

    /**
     * Copies text from {@code src} to {@code dst} without modification.
     */
    void copyText(String[] src, String[] dst);

    /**
     * Copies text from {@code src} to {@code dst} while respecting the visible character limit.
     */
    void copyTextClamped(String[] src, String[] dst);

    /**
     * Sanitises text for server use before copying it into {@code dst}.
     */
    void copyTextSanitizedClamped(String[] src, String[] dst);

    /**
     * Filters the input string so it only contains characters accepted by HexText signs.
     */
    String filterAllowedCharacters(String input);
}

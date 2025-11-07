package kamkeel.hextext.api.text;

/**
 * Entry points for HexText's colour and formatting parsing helpers.
 */
public interface TextFormattingApi {

    /**
     * Captures the formatting environment currently enforced by HexText.
     *
     * @param rawMode when {@code true}, raw formatting parsing rules are applied
     * @return an immutable description of the environment
     */
    FormattingEnvironment captureEnvironment(boolean rawMode);

    /**
     * Computes the length of the formatting token that begins at {@code index}.
     *
     * @param text   string to inspect
     * @param index  position to test
     * @param raw    whether raw mode rules should be applied
     * @param environment optional cached environment, may be {@code null}
     * @return number of characters consumed by a formatting token, or {@code 0}
     */
    int detectColorCodeLength(CharSequence text, int index, boolean raw, FormattingEnvironment environment);

    /**
     * Computes the length of the formatting token that begins at {@code index} using normal rules.
     */
    int detectColorCodeLength(CharSequence text, int index);

    /**
     * Determines whether the string contains any recognised formatting codes.
     */
    boolean containsFormattingCodes(CharSequence text);

    /**
     * Returns the index of the next formatting code after {@code start}, or {@code -1}.
     */
    int indexOfNextFormattingCode(CharSequence text, int start);

    /**
     * Validates the supplied RGB hex string.
     */
    boolean isValidHexString(String hex);

    /**
     * Parses the supplied RGB hex string into a 24-bit colour.
     */
    int parseHexColor(String hex);

    /**
     * Converts HSV components to a 24-bit RGB colour.
     */
    int hsvToRgb(float hue, float saturation, float value);

    /**
     * Immutable description of the formatting rules currently active.
     */
    interface FormattingEnvironment {
        boolean allowsHtmlFormatting();

        boolean allowsUniversalAmpersand();
    }
}

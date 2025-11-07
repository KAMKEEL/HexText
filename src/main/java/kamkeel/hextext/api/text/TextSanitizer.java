package kamkeel.hextext.api.text;

/**
 * Exposes common text normalisation helpers used throughout HexText.
 */
public interface TextSanitizer {

    String normalizeForRawDisplay(String text);

    String convertAmpersandsToSectionSigns(String text);

    String convertSectionSignsToAmpersands(String text);

    String stripColorCodes(CharSequence input);

    String stripExtras(CharSequence input);

    String stripHexColors(CharSequence input);

    String stripColors(CharSequence input);

    String stripStyles(CharSequence input);

    boolean containsFormattingCodes(CharSequence input);
}

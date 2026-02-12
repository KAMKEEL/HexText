package kamkeel.hextext.common.sign;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.api.sign.IHexTextSign;
import kamkeel.hextext.api.sign.SignSide;
import kamkeel.hextext.common.util.StringUtils;
import net.minecraft.tileentity.TileEntitySign;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Checks sign text against server-configured banned regex patterns.
 * Signs whose text matches any pattern are treated as uneditable.
 */
public final class SignBanHelper {

    private static volatile Pattern[] compiledPatterns = new Pattern[0];

    private SignBanHelper() {
    }

    /**
     * Compiles and caches the provided regex pattern strings.
     * Invalid patterns are logged and skipped.
     */
    public static void setPatterns(String[] patterns) {
        if (patterns == null || patterns.length == 0) {
            compiledPatterns = new Pattern[0];
            return;
        }

        Pattern[] compiled = new Pattern[patterns.length];
        int count = 0;

        for (String raw : patterns) {
            if (raw == null || raw.isEmpty()) {
                continue;
            }
            try {
                compiled[count++] = Pattern.compile(raw, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                CommonProxy.LOGGER.warn("Invalid banned sign pattern '{}': {}", raw, e.getMessage());
            }
        }

        if (count < compiled.length) {
            Pattern[] trimmed = new Pattern[count];
            System.arraycopy(compiled, 0, trimmed, 0, count);
            compiled = trimmed;
        }

        compiledPatterns = compiled;
    }

    /**
     * Returns {@code true} if any line on either side of the sign matches a banned pattern.
     */
    public static boolean isSignBanned(TileEntitySign sign) {
        Pattern[] patterns = compiledPatterns;
        if (patterns.length == 0) {
            return false;
        }

        IHexTextSign hexSign = (IHexTextSign) sign;

        for (String line : sign.signText) {
            if (isLineBanned(line, patterns)) {
                return true;
            }
        }

        String[] backLines = hexSign.getLines(SignSide.BACK);
        for (String line : backLines) {
            if (isLineBanned(line, patterns)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isLineBanned(String line, Pattern[] patterns) {
        if (line == null || line.isEmpty()) {
            return false;
        }

        String stripped = StringUtils.stripColorCodes(line);
        if (stripped == null || stripped.isEmpty()) {
            return false;
        }

        for (Pattern pattern : patterns) {
            if (pattern.matcher(stripped).find()) {
                return true;
            }
        }
        return false;
    }
}

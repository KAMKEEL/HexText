package kamkeel.hextext.client;

import java.util.List;
import java.util.Map;

/**
 * Holds the sanitized string and deferred render actions calculated for legacy font rendering.
 */
public final class LegacyRenderTextData {

    private final String sanitized;
    private final boolean modified;
    private final Map<Integer, List<LegacyRenderAction>> actions;

    private LegacyRenderTextData(String sanitized, boolean modified, Map<Integer, List<LegacyRenderAction>> actions) {
        this.sanitized = sanitized;
        this.modified = modified;
        this.actions = actions;
    }

    public static LegacyRenderTextData unmodified(String text) {
        return new LegacyRenderTextData(text, false, null);
    }

    public static LegacyRenderTextData modified(String sanitized, Map<Integer, List<LegacyRenderAction>> actions) {
        return new LegacyRenderTextData(sanitized, true, actions);
    }

    public String getSanitized() {
        return sanitized;
    }

    public boolean isModified() {
        return modified;
    }

    public Map<Integer, List<LegacyRenderAction>> getActions() {
        return actions;
    }
}

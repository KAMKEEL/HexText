package kamkeel.hextext.api;

import kamkeel.hextext.api.rendering.TextRenderService;
import kamkeel.hextext.api.rendering.TokenHighlightService;
import kamkeel.hextext.api.sign.SignInteractionRegistry;
import kamkeel.hextext.api.sign.SignStateService;
import kamkeel.hextext.api.text.SignTextService;
import kamkeel.hextext.api.text.TextFormatter;
import kamkeel.hextext.api.text.TextSanitizer;

/**
 * Central entry point for consumers of the HexText API.
 */
public final class HexTextApi {

    public static final String API_VERSION = "1.0.0";

    private static volatile HexTextApiProvider provider = UninitializedProvider.INSTANCE;

    private HexTextApi() {
    }

    /**
     * Installs the backing implementation that powers the exposed API surface.
     * <p>
     * This is intended for HexText's internal bootstrap and should not be invoked by other mods.
     */
    public static void installProvider(HexTextApiProvider implementation) {
        if (implementation == null) {
            throw new NullPointerException("HexTextApiProvider cannot be null");
        }
        if (provider != UninitializedProvider.INSTANCE) {
            throw new IllegalStateException("HexText API provider has already been installed");
        }
        provider = implementation;
    }

    /**
     * Returns the semantic version of the exposed API.
     */
    public static String apiVersion() {
        return API_VERSION;
    }

    /**
     * Returns the currently running HexText mod version.
     */
    public static String modVersion() {
        return provider().modVersion();
    }

    /**
     * Provides access to sign interaction registration hooks.
     */
    public static SignInteractionRegistry signInteractions() {
        return provider().signInteractions();
    }

    /**
     * Provides helpers for manipulating sign text in a HexText-friendly manner.
     */
    public static SignTextService signText() {
        return provider().signText();
    }

    /**
     * Provides colour and formatting parsing utilities.
     */
    public static TextFormatter textFormatter() {
        return provider().textFormatter();
    }

    /**
     * Provides common text normalisation helpers used across the mod.
     */
    public static TextSanitizer textSanitizer() {
        return provider().textSanitizer();
    }

    /**
     * Provides helpers for preparing text for the HexText render pipeline.
     */
    public static TextRenderService textRenderer() {
        return provider().textRenderer();
    }

    /**
     * Provides helpers for computing token highlight metadata.
     */
    public static TokenHighlightService tokenHighlighter() {
        return provider().tokenHighlighter();
    }

    /**
     * Provides helpers for interacting with HexText-enhanced sign tile entities.
     */
    public static SignStateService signState() {
        return provider().signState();
    }

    private static HexTextApiProvider provider() {
        HexTextApiProvider result = provider;
        if (result == UninitializedProvider.INSTANCE) {
            throw new IllegalStateException("HexText API has not been initialized yet");
        }
        return result;
    }

    private enum UninitializedProvider implements HexTextApiProvider {
        INSTANCE;

        @Override
        public String modVersion() {
            throw failure();
        }

        @Override
        public SignInteractionRegistry signInteractions() {
            throw failure();
        }

        @Override
        public SignTextService signText() {
            throw failure();
        }

        @Override
        public TextFormatter textFormatter() {
            throw failure();
        }

        @Override
        public TextSanitizer textSanitizer() {
            throw failure();
        }

        @Override
        public TextRenderService textRenderer() {
            throw failure();
        }

        @Override
        public TokenHighlightService tokenHighlighter() {
            throw failure();
        }

        @Override
        public SignStateService signState() {
            throw failure();
        }

        private IllegalStateException failure() {
            return new IllegalStateException("HexText API accessed before provider installation");
        }
    }
}

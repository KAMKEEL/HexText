package kamkeel.hextext.api;

import kamkeel.hextext.api.sign.SignInteractionRegistry;
import kamkeel.hextext.api.sign.SignStateApi;
import kamkeel.hextext.api.text.SignTextApi;
import kamkeel.hextext.api.text.TextFormattingApi;

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
    public static SignTextApi signText() {
        return provider().signText();
    }

    /**
     * Provides colour and formatting parsing utilities.
     */
    public static TextFormattingApi textFormatting() {
        return provider().textFormatting();
    }

    /**
     * Provides helpers for interacting with HexText-enhanced sign tile entities.
     */
    public static SignStateApi signState() {
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
        public SignTextApi signText() {
            throw failure();
        }

        @Override
        public TextFormattingApi textFormatting() {
            throw failure();
        }

        @Override
        public SignStateApi signState() {
            throw failure();
        }

        private IllegalStateException failure() {
            return new IllegalStateException("HexText API accessed before provider installation");
        }
    }
}

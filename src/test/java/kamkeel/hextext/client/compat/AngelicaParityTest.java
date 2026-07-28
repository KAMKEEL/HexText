package kamkeel.hextext.client.compat;

import kamkeel.hextext.CommonProxy;
import kamkeel.hextext.HexText;
import kamkeel.hextext.common.util.ColorCodeUtils;
import kamkeel.hextext.common.util.ColorMath;
import kamkeel.hextext.config.HexTextConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Proves that a HexText-authored string renders the same under Angelica as it does natively, by
 * modelling both pipelines and comparing their styled output for the translated text.
 *
 * <p>{@link HexTextModel} encodes the native semantics: colour directives (inline hex, vanilla
 * colours, span open, reset) clear styles and dynamic effects, span close restores the enclosing
 * colour but keeps styles. {@link AngelicaModel} encodes the batching renderer with the font
 * effect registry active: {@code §x} keeps styles and clears rainbow plus registry effects,
 * vanilla colours clear styles, {@code §z}/{@code §v} toggle independently of colours.
 *
 * <p>The one intentional divergence — {@code &g&#..&#..} renders as an Angelica gradient instead
 * of rainbow-then-colour — is excluded from generated inputs.
 */
public class AngelicaParityTest {

    private static final int FUZZ_SEED = 20260719;
    private static final int FUZZ_ITERATIONS = 400;
    private static final int BASE_COLOR = -1;

    @Before
    public void setUp() {
        HexText.proxy = new CommonProxy();
        HexTextConfig.resetToDefaults();
        HexTextConfig.setUniversalAmpersandEnabled(true);
        HexTextConfig.setEnableRgbHtmlFormat(true);
        AngelicaClientCompat.setGlyphEffectsRegistered(true);
    }

    @After
    public void tearDown() {
        AngelicaClientCompat.setGlyphEffectsRegistered(false);
    }

    @Test
    public void handPickedCorpusRendersIdentically() {
        String[] corpus = {
            "plain text",
            "&#FF0000red &#00FF00green",
            "§#123456section hex",
            "&lBold&#FF0000hex kills bold",
            "&l&n&kstyles&#FFFFFFgone",
            "<FF0000>span</FF0000>after",
            "<FF0000>a<00FF00>b</00FF00>c</FF0000>d",
            "&cVanilla<FF0000>Red</FF0000>Back",
            "<FF0000>&lbold inside</FF0000>still bold",
            "&lBold<FF0000>reset by open</FF0000>gone",
            "&gRainbow&#FF0000colour stops it",
            "&hFlip&#FF0000colour stops flip",
            "&iIgnite&jShake&rPlain",
            "&i&j&h&geverything&#FFFFFFcleared",
            "&ofoo&r&mbar&0baz",
            "</FF0000>unbalanced close",
            "&#GGGGGGinvalid & lone <FF00>short",
            "&lB&mC&nD&oE&kF&rG",
            "<AABBCC>&iignite in span</AABBCC>out",
            "text ends with token &#ABCDEF",
        };
        for (String input : corpus) {
            assertParity(input);
        }
    }

    @Test
    public void fuzzedInputsRenderIdentically() {
        Random random = new Random(FUZZ_SEED);
        for (int iteration = 0; iteration < FUZZ_ITERATIONS; iteration++) {
            assertParity(generate(random));
        }
    }

    private static void assertParity(String input) {
        String translated = AngelicaTextTranslator.translate(input);
        List<String> expected = HexTextModel.render(input);
        List<String> actual = AngelicaModel.render(translated);
        assertEquals("input: " + visible(input) + "\ntranslated: " + visible(translated), expected, actual);
    }

    private static String visible(String s) {
        return s.replace('§', '$');
    }

    // ------------------------------------------------------------------ input generation

    private static String generate(Random random) {
        StringBuilder sb = new StringBuilder();
        int tokens = random.nextInt(26);
        boolean lastWasRainbow = false;
        for (int i = 0; i < tokens; i++) {
            int pick = random.nextInt(12);
            boolean isHexToken = pick == 1 || pick == 2;
            if (lastWasRainbow && isHexToken) {
                sb.append('w'); // spacer: &g directly followed by &# would form Angelica's gradient
            }
            lastWasRainbow = false;
            switch (pick) {
                case 0: sb.append(randomLiteral(random)); break;
                case 1: sb.append('&').append('#').append(randomHex(random)); break;
                case 2: sb.append('§').append('#').append(randomHex(random)); break;
                case 3: sb.append('<').append(randomHex(random)).append('>'); break;
                case 4: sb.append('<').append('/').append(randomHex(random)).append('>'); break;
                case 5: sb.append(prefix(random)).append("0123456789abcdef".charAt(random.nextInt(16))); break;
                case 6: sb.append(prefix(random)).append("klmno".charAt(random.nextInt(5))); break;
                case 7: sb.append(prefix(random)).append('r'); break;
                case 8: sb.append(prefix(random)).append('g'); lastWasRainbow = true; break;
                case 9: sb.append(prefix(random)).append("hij".charAt(random.nextInt(3))); break;
                case 10: sb.append('&').append(randomLiteral(random)); break; // often an invalid token
                case 11: sb.append('<').append(randomLiteral(random)); break;
                default: break;
            }
        }
        return sb.toString();
    }

    private static char prefix(Random random) {
        return random.nextBoolean() ? '&' : '§';
    }

    private static String randomHex(Random random) {
        StringBuilder sb = new StringBuilder(6);
        String digits = random.nextBoolean() ? "0123456789abcdef" : "0123456789ABCDEF";
        for (int i = 0; i < 6; i++) {
            sb.append(digits.charAt(random.nextInt(16)));
        }
        return sb.toString();
    }

    private static char randomLiteral(Random random) {
        String pool = "wxyzWXYZ ?!.,+-";
        return pool.charAt(random.nextInt(pool.length()));
    }

    // ------------------------------------------------------------------ shared state helpers

    /** Mutable per-character render state shared by both models. */
    private abstract static class RenderModel {

        int color = BASE_COLOR;
        boolean obfuscated;
        boolean bold;
        boolean strikethrough;
        boolean underline;
        boolean italic;
        boolean rainbow;
        boolean ignite;
        boolean shake;
        boolean wave;
        boolean dinnerbone;

        final List<String> output = new ArrayList<>();

        void clearStyles() {
            obfuscated = bold = strikethrough = underline = italic = false;
        }

        void clearDynamicEffects() {
            rainbow = ignite = shake = wave = dinnerbone = false;
        }

        void emit(char ch) {
            StringBuilder sig = new StringBuilder(16);
            sig.append(ch).append('|');
            sig.append(color == BASE_COLOR ? "base" : String.format("%06x", color)).append('|');
            if (obfuscated) sig.append('k');
            if (bold) sig.append('l');
            if (strikethrough) sig.append('m');
            if (underline) sig.append('n');
            if (italic) sig.append('o');
            if (rainbow) sig.append('R');
            if (ignite) sig.append('I');
            if (shake) sig.append('S');
            if (wave) sig.append('W');
            if (dinnerbone) sig.append('D');
            output.add(sig.toString());
        }
    }

    // ------------------------------------------------------------------ native HexText semantics

    private static final class HexTextModel extends RenderModel {

        private final Deque<Integer> colorStack = new ArrayDeque<>();

        static List<String> render(String text) {
            HexTextModel model = new HexTextModel();
            model.run(text);
            return model.output;
        }

        private void run(String text) {
            for (int i = 0; i < text.length(); i++) {
                char current = text.charAt(i);

                if ((current == '§' || current == '&') && i + 1 < text.length()) {
                    char next = text.charAt(i + 1);
                    char lower = Character.toLowerCase(next);

                    if (next == '#' && ColorCodeUtils.isValidHexString(text, i + 2)) {
                        applyColor(ColorCodeUtils.parseHexColor(text, i + 2));
                        i += 7;
                        continue;
                    }
                    if (ColorCodeUtils.isMinecraftColorCode(lower)) {
                        applyColor(ColorMath.vanillaColorRgb(ColorCodeUtils.getMinecraftColorIndex(lower)));
                        i++;
                        continue;
                    }
                    if (lower == 'r') {
                        color = BASE_COLOR;
                        colorStack.clear();
                        clearStyles();
                        clearDynamicEffects();
                        i++;
                        continue;
                    }
                    if (ColorCodeUtils.isStyleCode(lower)) {
                        switch (lower) {
                            case 'k': obfuscated = true; break;
                            case 'l': bold = true; break;
                            case 'm': strikethrough = true; break;
                            case 'n': underline = true; break;
                            default: italic = true; break;
                        }
                        i++;
                        continue;
                    }
                    if (ColorCodeUtils.isEffectCode(lower)) {
                        switch (lower) {
                            case 'g': rainbow = true; break;
                            case 'h': dinnerbone = true; break;
                            case 'i': ignite = true; break;
                            // Wave is HexText's own effect now rather than something
                            // borrowed from Angelica, so the native model has to carry
                            // it too or the two sides disagree about the same letter.
                            case 'z': wave = true; break;
                            default: shake = true; break;
                        }
                        i++;
                        continue;
                    }
                }

                if (current == '<') {
                    if (i + 8 <= text.length() && text.charAt(i + 7) == '>'
                        && ColorCodeUtils.isValidHexString(text, i + 1)) {
                        colorStack.push(color);
                        color = ColorCodeUtils.parseHexColor(text, i + 1);
                        clearStyles();
                        clearDynamicEffects();
                        i += 7;
                        continue;
                    }
                    if (i + 9 <= text.length() && text.charAt(i + 1) == '/' && text.charAt(i + 8) == '>'
                        && ColorCodeUtils.isValidHexString(text, i + 2)) {
                        color = colorStack.isEmpty() ? BASE_COLOR : colorStack.pop();
                        clearDynamicEffects();
                        i += 8;
                        continue;
                    }
                }

                emit(current);
            }
        }

        private void applyColor(int rgb) {
            color = rgb;
            colorStack.clear();
            clearStyles();
            clearDynamicEffects();
        }
    }

    // ------------------------------------------------------------------ Angelica batcher semantics

    private static final class AngelicaModel extends RenderModel {

        static List<String> render(String text) {
            AngelicaModel model = new AngelicaModel();
            model.run(text);
            return model.output;
        }

        private void run(String text) {
            for (int i = 0; i < text.length(); i++) {
                char current = text.charAt(i);

                if (current == '§' && i + 1 < text.length()) {
                    char lower = Character.toLowerCase(text.charAt(i + 1));

                    if (lower == 'x' && isSectionX(text, i)) {
                        color = parseSectionX(text, i);
                        rainbow = false;
                        ignite = false;
                        shake = false;
                        i += 13;
                        continue;
                    }
                    if (ColorCodeUtils.isMinecraftColorCode(lower)) {
                        color = ColorMath.vanillaColorRgb(ColorCodeUtils.getMinecraftColorIndex(lower));
                        clearStyles();
                        rainbow = false;
                        ignite = false;
                        shake = false;
                        // wave and dinnerbone deliberately survive colour codes in the batcher
                        i++;
                        continue;
                    }
                    switch (lower) {
                        case 'r':
                            color = BASE_COLOR;
                            clearStyles();
                            clearDynamicEffects();
                            wave = false;
                            i++;
                            continue;
                        case 'k': obfuscated = true; i++; continue;
                        case 'l': bold = true; i++; continue;
                        case 'm': strikethrough = true; i++; continue;
                        case 'n': underline = true; i++; continue;
                        case 'o': italic = true; i++; continue;
                        case 'q': rainbow = true; i++; continue;
                        case 'z': wave = !wave; i++; continue;
                        case 'v': dinnerbone = !dinnerbone; i++; continue;
                        case 'i': ignite = true; i++; continue;
                        case 'j': shake = true; i++; continue;
                        default: i++; continue; // unknown codes are consumed silently
                    }
                }

                // §z used to be reachable only as the pre-registry fallback for shake,
                // and was treated as a regression. It is HexText's own wave now, so it
                // is expected output and carries the same marker the native model uses.
                emit(current);
            }
        }

        private static boolean isSectionX(String text, int start) {
            return parseSectionX(text, start) != Integer.MIN_VALUE;
        }

        private static int parseSectionX(String text, int start) {
            if (start + 14 > text.length()) {
                return Integer.MIN_VALUE;
            }
            int rgb = 0;
            for (int pos = start + 2; pos < start + 14; pos += 2) {
                if (text.charAt(pos) != '§') {
                    return Integer.MIN_VALUE;
                }
                int digit = Character.digit(text.charAt(pos + 1), 16);
                if (digit == -1) {
                    return Integer.MIN_VALUE;
                }
                rgb = (rgb << 4) | digit;
            }
            return rgb;
        }
    }
}

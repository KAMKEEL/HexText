package kamkeel.hextext.client.render.font;

import net.minecraft.client.gui.FontRenderer;

public interface FontRendererBridge {

    FontRenderer hexText$getFontRenderer();

    void hexText$setRandomStyle(boolean enabled);

    void hexText$setBoldStyle(boolean enabled);

    void hexText$setStrikethroughStyle(boolean enabled);

    void hexText$setUnderlineStyle(boolean enabled);

    void hexText$setItalicStyle(boolean enabled);

    void hexText$setTextColor(int color);

    int hexText$getTextColor();

    float hexText$getAlpha();

    float hexText$getRedComponent();

    float hexText$getBlueComponent();

    float hexText$getGreenComponent();

    int[] hexText$getColorCodePalette();

    float hexText$getPosX();

    float hexText$getPosY();

    int hexText$getFontHeight();

    void hexText$applyColorComponents(float red, float green, float blue, float alpha);

    void hexText$resetFormattingStyles();
}

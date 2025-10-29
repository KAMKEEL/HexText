package kamkeel.hextext.client.render.font;

import net.minecraft.client.gui.FontRenderer;

public interface FontRendererBridge {

    FontRenderer getFontRenderer();

    void setRandomStyle(boolean enabled);

    void setBoldStyle(boolean enabled);

    void setStrikethroughStyle(boolean enabled);

    void setUnderlineStyle(boolean enabled);

    void setItalicStyle(boolean enabled);

    void setTextColor(int color);

    int getTextColor();

    boolean isBoldStyle();

    float getAlpha();

    float getRedComponent();

    float getBlueComponent();

    float getGreenComponent();

    int[] getColorCodePalette();

    float getPosX();

    float getPosY();

    int getFontHeight();

    void applyColorComponents(float red, float green, float blue, float alpha);

    void resetFormattingStyles();
}

package kamkeel.hextext.client.config;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import kamkeel.hextext.HexText;
import kamkeel.hextext.config.HexTextConfig;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SideOnly(Side.CLIENT)
public class HexTextConfigGui extends GuiConfig {

    public HexTextConfigGui(GuiScreen parent) {
        super(parent, getConfigElements(), HexText.ID, false, false, "HexText Configuration");
    }

    private static List<IConfigElement> getConfigElements() {
        Configuration config = HexTextConfig.getConfiguration();
        if (config == null) {
            return Collections.emptyList();
        }

        List<IConfigElement> elements = new ArrayList<IConfigElement>();
        elements.add(new ConfigElement(config.getCategory(HexTextConfig.CATEGORY_EFFECTS)));
        return elements;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        if (this.entryList != null) {
            this.entryList.saveConfigElements();
        }
        Configuration configuration = HexTextConfig.getConfiguration();
        if (configuration != null && configuration.hasChanged()) {
            configuration.save();
        }
        HexTextConfig.sync();
    }
}

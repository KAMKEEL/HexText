package kamkeel.hextext.mixin.early;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kamkeel.hextext.common.compat.AngelicaCompatibility;

public final class HexTextEarlyMixins implements IMixinConfigPlugin {

    private static final MixinEnvironment.Side SIDE = MixinEnvironment.getCurrentEnvironment().getSide();

    @Override
    public void onLoad(String mixinPackage) {
        // No-op
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No-op
    }

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();
        if (SIDE == MixinEnvironment.Side.CLIENT) {
            if (!AngelicaCompatibility.shouldDisableHexTextFontRendererMixin()) {
                mixins.add("client.MixinFontRenderer");
            }
            mixins.add("client.MixinGuiNewChat");
            mixins.add("client.MixinGuiTextField");
            mixins.add("client.MixinGuiChat");
            mixins.add("client.MixinGuiRepair");

            mixins.add("client.sign.MixinGuiEditSign");
            mixins.add("client.sign.MixinNetHandlerPlayClient");
            mixins.add("client.sign.MixinTileEntitySignRenderer");
            mixins.add("client.sign.MixinTileEntitySignClient");
        }

        mixins.add("sign.MixinBlockSign");
        mixins.add("sign.MixinC12PacketUpdateSign");
        mixins.add("sign.MixinS33PacketUpdateSign");
        mixins.add("sign.MixinNetHandlerPlayServer");
        mixins.add("sign.MixinTileEntitySign");
        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No-op
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // No-op
    }
}

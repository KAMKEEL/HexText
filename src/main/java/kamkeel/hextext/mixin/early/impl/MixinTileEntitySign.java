package kamkeel.hextext.mixin.early.impl;

import kamkeel.hextext.common.util.SignTextHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntitySign.class)
public abstract class MixinTileEntitySign extends TileEntity {


    @Unique
    private boolean hextext$isWaxed = false;

    // Front Sign Text
    @Shadow
    public String[] signText;

    @Unique
    private boolean hextext$isGlowingFront = false;

    @Unique
    private int hextext$defaultColorFront = -1;


    // Back Sign Text
    @Unique
    public String[] hextext$backSignText = new String[] {"", "", "", ""};;

    @Unique
    private boolean hextext$isGlowingBack = false;

    @Unique
    private int hextext$defaultColorBack = -1;

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void hextext$clampLoadedLines(NBTTagCompound compound, CallbackInfo ci) {
        if (signText == null) {
            return;
        }
        for (int i = 0; i < signText.length; i++) {
            signText[i] = SignTextHelper.clampToVisibleLimit(signText[i]);
        }
    }
}

package kamkeel.hextext.mixin.early.impl.client.sign;

import kamkeel.hextext.HexText;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntitySign;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntitySign.class)
public abstract class MixinTileEntitySignClient {

    @Shadow
    public abstract void setEditable(boolean editable);

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void hextext$enableClientEdit(NBTTagCompound compound, CallbackInfo ci) {
        if (!HexText.getActiveProxy().isRemoteHexTextPresent()) {
            return;
        }

        boolean waxed = compound.getBoolean("HexTextWaxed");
        setEditable(!waxed);
    }
}

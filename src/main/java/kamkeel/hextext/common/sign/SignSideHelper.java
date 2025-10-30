package kamkeel.hextext.common.sign;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.MathHelper;

public final class SignSideHelper {

    private SignSideHelper() {
    }

    public static SignSide determineSide(TileEntitySign sign, double playerX, double playerZ) {
        double centerX = sign.xCoord + 0.5D;
        double centerZ = sign.zCoord + 0.5D;

        double vecX = playerX - centerX;
        double vecZ = playerZ - centerZ;

        if (vecX == 0.0D && vecZ == 0.0D) {
            return SignSide.FRONT;
        }

        double[] normal = computeFrontNormal(sign.getBlockType(), sign.getBlockMetadata());
        double dot = vecX * normal[0] + vecZ * normal[1];
        return dot >= 0.0D ? SignSide.FRONT : SignSide.BACK;
    }

    private static double[] computeFrontNormal(Block block, int metadata) {
        if (block == Blocks.wall_sign) {
            switch (metadata) {
                case 2:
                    return new double[] {0.0D, -1.0D};
                case 3:
                    return new double[] {0.0D, 1.0D};
                case 4:
                    return new double[] {-1.0D, 0.0D};
                case 5:
                    return new double[] {1.0D, 0.0D};
                default:
                    return new double[] {0.0D, 1.0D};
            }
        }

        float orientation = (metadata * 360.0F) / 16.0F;
        float yaw = MathHelper.wrapAngleTo180_float(orientation - 180.0F);
        double radians = Math.toRadians(yaw);
        return new double[] {-MathHelper.sin((float) radians), MathHelper.cos((float) radians)};
    }
}

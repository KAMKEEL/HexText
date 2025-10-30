package kamkeel.hextext.common.sign;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntitySign;
import org.junit.Assert;
import org.junit.Test;

public class SignSideHelperTest {

    @Test
    public void standingSignSouthFacesPositiveZ() {
        TileEntitySign sign = createSign(null, 0);
        Assert.assertEquals(SignSide.FRONT, SignSideHelper.determineSide(sign, 0.5D, 1.5D));
        Assert.assertEquals(SignSide.BACK, SignSideHelper.determineSide(sign, 0.5D, -0.5D));
    }

    @Test
    public void standingSignWestFacesNegativeX() {
        TileEntitySign sign = createSign(null, 4);
        Assert.assertEquals(SignSide.FRONT, SignSideHelper.determineSide(sign, -0.5D, 0.5D));
        Assert.assertEquals(SignSide.BACK, SignSideHelper.determineSide(sign, 1.5D, 0.5D));
    }

    private static TileEntitySign createSign(Block block, int metadata) {
        return new TestTileEntitySign(block, metadata);
    }

    private static final class TestTileEntitySign extends TileEntitySign {
        private final Block block;
        private final int metadata;

        private TestTileEntitySign(Block block, int metadata) {
            this.block = block;
            this.metadata = metadata;
            this.xCoord = 0;
            this.zCoord = 0;
            this.blockType = block;
            this.blockMetadata = metadata;
        }

        @Override
        public Block getBlockType() {
            return block;
        }

        @Override
        public int getBlockMetadata() {
            return metadata;
        }
    }
}

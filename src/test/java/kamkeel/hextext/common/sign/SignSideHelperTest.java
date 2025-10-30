package kamkeel.hextext.common.sign;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import org.junit.Assert;
import org.junit.Test;

public class SignSideHelperTest {

    @Test
    public void determinesFrontForStandingSignUsingHitCoordinates() {
        TestSign sign = new TestSign(Blocks.standing_sign, 0, 10, 10);

        SignSide frontHit = SignSideHelper.determineSide(sign, 10.5D, 12.0D, 0.0F, 0.5F, 0.9F);
        SignSide backHit = SignSideHelper.determineSide(sign, 10.5D, 12.0D, 0.0F, 0.5F, 0.1F);

        Assert.assertNotEquals(frontHit, backHit);
    }

    @Test
    public void fallsBackToPlayerPositionWhenHitIsCentered() {
        TestSign sign = new TestSign(Blocks.standing_sign, 8, 5, 5);

        SignSide fromSouth = SignSideHelper.determineSide(sign, 4.0D, 4.0D, 180.0F, 0.5F, 0.5F);
        SignSide fromNorth = SignSideHelper.determineSide(sign, 6.0D, 6.0D, 0.0F, 0.5F, 0.5F);

        Assert.assertNotEquals(fromSouth, fromNorth);
    }

    @Test
    public void wallSignsContinueToUsePlayerPosition() {
        TestSign sign = new TestSign(Blocks.wall_sign, 2, 3, 3);

        SignSide wallFront = SignSideHelper.determineSide(sign, 3.5D, 1.0D, 0.0F, 0.5F, 0.5F);
        SignSide wallBack = SignSideHelper.determineSide(sign, 3.5D, 5.0D, 180.0F, 0.5F, 0.5F);

        Assert.assertNotEquals(wallFront, wallBack);
    }

    @Test
    public void usesPlayerFacingWhenStandingInsideSign() {
        TestSign sign = new TestSign(Blocks.standing_sign, 4, 5, 5);

        SignSide facingFront = SignSideHelper.determineSide(sign, 5.5D, 5.5D, 90.0F, 0.5F, 0.5F);
        SignSide facingBack = SignSideHelper.determineSide(sign, 5.5D, 5.5D, -90.0F, 0.5F, 0.5F);

        Assert.assertNotEquals(facingFront, facingBack);
    }

    private static final class TestSign extends TileEntitySign {
        private final Block block;
        private final int metadata;

        private TestSign(Block block, int metadata, int x, int z) {
            this.block = block;
            this.metadata = metadata;
            this.xCoord = x;
            this.yCoord = 0;
            this.zCoord = z;
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

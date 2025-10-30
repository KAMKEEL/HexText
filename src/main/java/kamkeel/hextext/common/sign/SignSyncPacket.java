package kamkeel.hextext.common.sign;

public interface SignSyncPacket {

    void hextext$setBackText(String[] lines);

    String[] hextext$getBackText();

    void hextext$setGlowing(SignSide side, boolean glowing);

    boolean hextext$isGlowing(SignSide side);

    void hextext$setOutlined(SignSide side, boolean outlined);

    boolean hextext$isOutlined(SignSide side);

    void hextext$setWaxed(boolean waxed);

    boolean hextext$isWaxed();
}

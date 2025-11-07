package kamkeel.hextext.api.sign;

/**
 * Represents one of the editable faces of a sign.
 */
public enum SignSide {
    FRONT,
    BACK;

    public static SignSide fromBoolean(boolean front) {
        return front ? FRONT : BACK;
    }

    public boolean isFront() {
        return this == FRONT;
    }
}

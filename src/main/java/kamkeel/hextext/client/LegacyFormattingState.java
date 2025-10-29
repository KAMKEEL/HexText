package kamkeel.hextext.client;

final class LegacyFormattingState {
    private boolean bold;
    private boolean expectingLegacyCode;

    boolean isBold() {
        return bold;
    }

    void setBold(boolean bold) {
        this.bold = bold;
    }

    boolean isExpectingLegacyCode() {
        return expectingLegacyCode;
    }

    void setExpectingLegacyCode(boolean expectingLegacyCode) {
        this.expectingLegacyCode = expectingLegacyCode;
    }
}

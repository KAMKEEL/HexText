package kamkeel.hextext.client;

@FunctionalInterface
public interface CharWidthFunction {
    float getWidth(char character);
}

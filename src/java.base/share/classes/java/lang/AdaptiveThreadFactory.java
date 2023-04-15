package java.lang;

/**
 * Comment
 */
public class AdaptiveThreadFactory {

    private static native void registerNatives();
    static {
        registerNatives();
    }

    /**
    * Comment
    */
    public AdaptiveThreadFactory() {}

    /**
    * Comment
    * @return Comment
    */
    public static native int adaptiveThreadFactoryTest();

}
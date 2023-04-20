package java.lang;

import java.util.concurrent.ThreadFactory;

/**
 * Comment
 */
public class AdaptiveThreadFactory implements ThreadFactory {

    private static native void registerNatives();
    static {
        registerNatives();
    }

    private int adaptiveThreadFactoryId;
    private ThreadFactory platformThreadFactory;
    private ThreadFactory virtualThreadFactory;

    /**
     * Comment 
     * 
     * @param   adaptiveThreadFactoryId
     *          Comment
     */
    public AdaptiveThreadFactory(int adaptiveThreadFactoryId) {
        this.adaptiveThreadFactoryId = adaptiveThreadFactoryId;
        addMonitor(this.adaptiveThreadFactoryId);
        this.platformThreadFactory = Thread.ofPlatform().factory();
        this.virtualThreadFactory = Thread.ofVirtual().factory();
    }

    /**
    * Comment
    * @return Comment
    */
    //public static native int adaptiveThreadFactoryTest();

    /**
     * Comment
     * @return Comment
     */
    public Thread newThread(Runnable originalTask) {
        final boolean response = queryMonitor(this.adaptiveThreadFactoryId);
        final Runnable augmentedTask = augmentTask(originalTask);
        if(response) {
            return virtualThreadFactory.newThread(augmentedTask);
        } else {
            return platformThreadFactory.newThread(augmentedTask);
        }
    }

    private Runnable augmentTask(Runnable originalTask) {
        final Runnable augmentedTask = () -> {
            registerWithMonitor(this.adaptiveThreadFactoryId);
            originalTask.run();
        };
        return augmentedTask;
    }

    private native void addMonitor(int adaptiveThreadFactoryId);

    private native boolean queryMonitor(int adaptiveThreadFactoryId);

    private native void registerWithMonitor(int adaptiveThreadFactoryId);


}
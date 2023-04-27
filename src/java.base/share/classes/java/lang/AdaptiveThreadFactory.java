package java.lang;

import java.util.concurrent.ThreadFactory;

/**
 * Comment
 */
public class AdaptiveThreadFactory implements ThreadFactory, AutoCloseable {

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
        Thread newThread;
        if(response) {
            newThread = virtualThreadFactory.newThread(augmentedTask);
        } else {
            newThread = platformThreadFactory.newThread(augmentedTask);
        }
        newThread.associateWithAdaptiveThreadFactory(this.adaptiveThreadFactoryId);
        return newThread;
    }

    /*
     * Comment
     */
    public void close() {
        // TO DO: remove monitor
    }

    private Runnable augmentTask(Runnable originalTask) {
        final Runnable augmentedTask = () -> {
            registerJavaThreadAndAssociateOSThreadWithMonitor(
                Thread.currentThread().getAdaptiveThreadFactoryId(), 
                Thread.currentThread().threadId()
            );
            originalTask.run();
            deregisterJavaThreadAndDisassociateOSThreadFromMonitor(
                Thread.currentThread().getAdaptiveThreadFactoryId(), 
                Thread.currentThread().threadId() 
            );
        };
        return augmentedTask;
    }

    /* Native methods */

    private native void addMonitor(int adaptiveThreadFactoryId);
    private native boolean queryMonitor(int adaptiveThreadFactoryId);
    static native void registerJavaThreadAndAssociateOSThreadWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId); // called by platform and virtual threads
    static native void deregisterJavaThreadAndDisassociateOSThreadFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId); // called by platform and virtual threads
    static native void associateOSThreadWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId); // called by virtual threads only
    static native void disassociateOSThreadFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId); // called by virtual threads only

}
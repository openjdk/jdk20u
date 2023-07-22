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

    private enum ExecutionMode {
        HETEROGENEOUS, HOMOGENEOUS
    }

    private enum CurrentThreadState {
        PLATFORM, VIRTUAL
    }

    // parameters set by user
    private int adaptiveThreadFactoryId;
    private long parkingTimeWindowLength; // in milliseconds
    private long threadCreationTimeWindowLength; // in milliseconds
    private long numberParkingsThreshold;
    private long numberThreadCreationsThreshold;
    private ThreadFactory platformThreadFactory;
    private ThreadFactory virtualThreadFactory;
    private ExecutionMode executionMode;
    private int stateQueryInterval; // in milliseconds
    private int numberStateRepetitionsUntilTransition;
    private Runnable threadCreationHandler;

    // for internal use
    private Thread stateQueryingThread;
    private LinkedList<Boolean> queryResults;
    private LinkedList<Thread> threads;

    /**
     * Comment 
     * 
     * @param   adaptiveThreadFactoryId Comment
     * @param   parkingTimeWindowLength Comment
     * @param   threadCreationTimeWindowLength Comment
     * @param   numberParkingsThreshold Comment
     * @param   numberThreadCreationsThreshold Comment
     */
    public AdaptiveThreadFactory(
        int adaptiveThreadFactoryId, 
        long parkingTimeWindowLength, 
        long threadCreationTimeWindowLength,
        long numberParkingsThreshold,
        long numberThreadCreationsThreshold
    ) {
        this.adaptiveThreadFactoryId = adaptiveThreadFactoryId;
        addMonitor(this.adaptiveThreadFactoryId);
        setParameters(
            parkingTimeWindowLength,
            threadCreationTimeWindowLength,
            numberParkingsThreshold,
            numberThreadCreationsThreshold
        );
        this.platformThreadFactory = Thread.ofPlatform().factory();
        this.virtualThreadFactory = Thread.ofVirtual().factory();
        this.executionMode = ExecutionMode.STANDARD;
    }

    /**
     * Comment 
     * 
     * @param   adaptiveThreadFactoryId Comment
     * @param   parkingTimeWindowLength Comment
     * @param   threadCreationTimeWindowLength Comment
     * @param   numberParkingsThreshold Comment
     * @param   numberThreadCreationsThreshold Comment
     * @param   stateQueryInterval Comment
     * @param   numberStateRepetitionsUntilTransition Comment
     * @param   threadCreationHandler Comment 
     */
    public AdaptiveThreadFactory(
        int adaptiveThreadFactoryId, 
        long parkingTimeWindowLength, 
        long threadCreationTimeWindowLength,
        long numberParkingsThreshold,
        long numberThreadCreationsThreshold,
        int stateQueryInterval,
        int numberStateRepetitionsUntilTransition,
        Runnable threadCreationHandler 
    ) {
        this(
            adaptiveThreadFactoryId,
            parkingTimeWindowLength,
            threadCreationTimeWindowLength,
            numberParkingsThreshold,
            numberThreadCreationsThreshold
        );
        this.stateQueryInterval = stateQueryInterval;
        this.numberStateRepetitionsUntilTransition = numberStateRepetitionsUntilTransition;
        this.threadCreationHandler = threadCreationHandler;
        this.executionMode = ExecutionMode.PERIODIC;
    }

    private boolean doTransition(boolean newQueryResult) {
        queryResults.add(newQueryResult);
        if(queryResults.size() > this.numberStateRepetitionsUntilTransition) {
            queryResults.poll();
        }
        
    }

    private void startQueryingThread(
    ) {
        this.startQueryingThread = () -> {
            while(!Thread.currentThread().isInterrupted()) {
                final boolean useVirtualThread = queryMonitor(this.adaptiveThreadFactoryId);
                
                Thread.sleep(this.stateQueryInterval);
            }
        };
        this.startQueryingThread.setDaemon(true);
        this.startQueryingThread.start();
    }

    /**
     * Comment 
     * 
     * @param   parkingTimeWindowLength Comment
     * @param   threadCreationTimeWindowLength Comment
     * @param   numberParkingsThreshold Comment
     * @param   numberThreadCreationsThreshold Comment
     */
    public void setParameters(
        long parkingTimeWindowLength, 
        long threadCreationTimeWindowLength,
        long numberParkingsThreshold,
        long numberThreadCreationsThreshold
    ) {
        this.parkingTimeWindowLength = parkingTimeWindowLength;
        this.threadCreationTimeWindowLength = threadCreationTimeWindowLength;
        this.numberParkingsThreshold = numberParkingsThreshold;
        this.numberThreadCreationsThreshold = numberThreadCreationsThreshold;
        setMonitorParameters(
            this.adaptiveThreadFactoryId, 
            this.parkingTimeWindowLength, 
            this.threadCreationTimeWindowLength,
            this.numberParkingsThreshold,
            this.numberThreadCreationsThreshold
        );
    }

    /**
     * Comment 
     * 
     * @param   parkingTimeWindowLength Comment
     * @param   threadCreationTimeWindowLength Comment
     * @param   numberParkingsThreshold Comment
     * @param   numberThreadCreationsThreshold Comment
     * @param   stateQueryInterval Comment
     * @param   numberStateRepetitionsUntilTransition Comment
     * @param   threadCreationHandler Comment
     */
    public void setParameters(
        long parkingTimeWindowLength, 
        long threadCreationTimeWindowLength,
        long numberParkingsThreshold,
        long numberThreadCreationsThreshold,
        int stateQueryInterval,
        int numberStateRepetitionsUntilTransition,
        Runnable threadCreationHandler 
    ) {
        setParameters(
            parkingTimeWindowLength,
            threadCreationTimeWindowLength,
            numberParkingsThreshold,
            numberThreadCreationsThreshold
        );
        this.stateQueryInterval = stateQueryInterval;
        this.numberStateRepetitionsUntilTransition = numberStateRepetitionsUntilTransition;
        this.threadCreationHandler = threadCreationHandler;
        
    }

    /**
     * Comment
     * @return Comment
     */
    public Thread newThread(Runnable originalTask) {
        final Runnable augmentedTask = augmentTask(originalTask);
        Thread newThread;
        if(executionMode.equals(ExecutionMode.STANDARD)) {

        }
        newThread.associateWithAdaptiveThreadFactory(this.adaptiveThreadFactoryId);
        return newThread;

        final boolean useVirtualThread = queryMonitor(this.adaptiveThreadFactoryId);
        final Runnable augmentedTask = augmentTask(originalTask);
        Thread newThread;
        if(useVirtualThread) {
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
        removeMonitor(this.adaptiveThreadFactoryId);
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
            threads.remove(Thread.currentThread());
        };
        return augmentedTask;
    }

    /* Methods for testing */

    /**
     * Comment
     * @return Comment
     */
    public long getNumberParkingsInTimeWindow() {
        long numberParkingsInTimeWindow = countParkingsInTimeWindow(this.adaptiveThreadFactoryId);
        return numberParkingsInTimeWindow;
    }

    /**
     * Comment
     * @return Comment
     */
    public long getNumberThreadCreationsInTimeWindow() {
        long numberThreadCreationsInTimeWindow = countThreadCreationsInTimeWindow(this.adaptiveThreadFactoryId);
        return numberThreadCreationsInTimeWindow;
    }

    /**
     * Comment
     * @return Comment
     */
    public long getNumberThreads() {
        long numberThreads = countNumberThreads(this.adaptiveThreadFactoryId);
        return numberThreads;
    }

    /* Native methods */

    private native void addMonitor(int adaptiveThreadFactoryId);
    private native void removeMonitor(int adaptiveThreadFactoryId);
    private native void setMonitorParameters(
        int adaptiveThreadFactoryId, 
        long parkingTimeWindowLength, 
        long threadCreationTimeWindowLength,
        long numberParkingsThreshold,
        long numberThreadCreationsThreshold
    );
    private native boolean queryMonitor(int adaptiveThreadFactoryId);
    static native void registerJavaThreadAndAssociateOSThreadWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId); // called by platform and virtual threads
    static native void deregisterJavaThreadAndDisassociateOSThreadFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId); // called by platform and virtual threads
    static native void associateOSThreadWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId); // called by virtual threads only
    static native void disassociateOSThreadFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId); // called by virtual threads only
    
    /* Native methods for testing */
    static native long countParkingsInTimeWindow(int adaptiveThreadFactoryId);
    static native long countThreadCreationsInTimeWindow(int adaptiveThreadFactoryId);
    static native long countNumberThreads(int adaptiveThreadFactoryId);
}
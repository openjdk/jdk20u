package java.lang;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;

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

    /**
    * Comment
    */
    public enum ThreadType {
        /** Comment */ PLATFORM, 
        /** Comment */ VIRTUAL
    }

    /**
     * Comment
     */
    @FunctionalInterface
    public interface Discriminator {
        /**
        * Comment 
        * 
        * @param   numberThreadCreationsInTimeWindow Comment
        * @param   numberParkingsInTimeWindow Comment
        * @param   cpuUsage Comment
        * @param   numberThreads Comment
        * @param   currentThreadType Comment
        * @return Comment
        */
        ThreadType discriminate(
            long numberThreadCreationsInTimeWindow,
            long numberParkingsInTimeWindow,
            double cpuUsage, 
            long numberThreads,
            Optional<ThreadType> currentThreadType
        );
    }

    // user specification
    private int adaptiveThreadFactoryId;
    private long parkingTimeWindowLength; // in milliseconds
    private long threadCreationTimeWindowLength; // in milliseconds
    /*
    private long numberParkingsThreshold;
    private long numberThreadCreationsThreshold;
    */
    private int stateQueryInterval; // in milliseconds
    private int numberRecurrencesUntilTransition;
    private Optional<Runnable> threadCreationHandler;
    private Discriminator discriminator;
    private Supplier<Double> cpuUsageProvider;

    // internal use

    private ExecutionMode executionMode;
    private ThreadFactory platformThreadFactory;
    private ThreadFactory virtualThreadFactory;
    private final ConcurrentLinkedQueue<Thread> threads;

    // ExecutionMode.HOMOGENEOUS
    private Thread transitionManager;
    private ThreadType currentThreadType;
    private LinkedList<ThreadType> queryResults;
    private int maximalNumberTerminationAttempts;
    private int threadTerminationWaitingTimeInMilliseconds;

    private void setDefaultValues() {
        this.maximalNumberTerminationAttempts = 10;
        this.threadTerminationWaitingTimeInMilliseconds = 50;
    }

    /**
     * Comment 
     * 
     * @param   adaptiveThreadFactoryId Comment
     * @param   parkingTimeWindowLength Comment
     * @param   threadCreationTimeWindowLength Comment
     * @param   discriminator Comment
     * @param   cpuUsageProvider Comment
     */
    public AdaptiveThreadFactory(
        int adaptiveThreadFactoryId, 
        long parkingTimeWindowLength, 
        long threadCreationTimeWindowLength,
        Discriminator discriminator,
        Supplier<Double> cpuUsageProvider
    ) {
        // user specification
        this.adaptiveThreadFactoryId = adaptiveThreadFactoryId;
        addMonitor(this.adaptiveThreadFactoryId);
        setParameters(
            parkingTimeWindowLength,
            threadCreationTimeWindowLength,
            discriminator
        );
        this.cpuUsageProvider = cpuUsageProvider;
        // internal use
        this.platformThreadFactory = Thread.ofPlatform().factory();
        this.virtualThreadFactory = Thread.ofVirtual().factory();
        this.threads = new ConcurrentLinkedQueue<Thread>();
        this.executionMode = ExecutionMode.HETEROGENEOUS;
        setDefaultValues();
    }

    /**
     * Comment 
     * 
     * @param   adaptiveThreadFactoryId Comment
     * @param   parkingTimeWindowLength Comment
     * @param   threadCreationTimeWindowLength Comment
     * @param   discriminator Comment
     * @param   cpuUsageProvider Comment
     * @param   stateQueryInterval Comment
     * @param   numberRecurrencesUntilTransition Comment 
     */
    public AdaptiveThreadFactory(
        int adaptiveThreadFactoryId, 
        long parkingTimeWindowLength, 
        long threadCreationTimeWindowLength,
        Discriminator discriminator,
        Supplier<Double> cpuUsageProvider,
        int stateQueryInterval,
        int numberRecurrencesUntilTransition 
    ) {
        // user specification
        this(
            adaptiveThreadFactoryId,
            parkingTimeWindowLength,
            threadCreationTimeWindowLength,
            discriminator,
            cpuUsageProvider
        );
        this.stateQueryInterval = stateQueryInterval;
        this.numberRecurrencesUntilTransition = numberRecurrencesUntilTransition;
        this.threadCreationHandler = Optional.empty();
        // internal use
        this.queryResults = new LinkedList<ThreadType>();
        this.currentThreadType = ThreadType.PLATFORM;
        this.executionMode = ExecutionMode.HOMOGENEOUS;
        setDefaultValues();
        // start background thread
        startTransitionManager();
    }

    /**
     * Comment 
     * 
     * @param   adaptiveThreadFactoryId Comment
     * @param   parkingTimeWindowLength Comment
     * @param   threadCreationTimeWindowLength Comment
     * @param   discriminator Comment
     * @param   cpuUsageProvider Comment
     * @param   stateQueryInterval Comment
     * @param   numberRecurrencesUntilTransition Comment 
     * @param   threadCreationHandler Comment
     */
    public AdaptiveThreadFactory(
        int adaptiveThreadFactoryId, 
        long parkingTimeWindowLength, 
        long threadCreationTimeWindowLength,
        Discriminator discriminator,
        Supplier<Double> cpuUsageProvider,
        int stateQueryInterval,
        int numberRecurrencesUntilTransition,
        Runnable threadCreationHandler
    ) {
        this(
            adaptiveThreadFactoryId,
            parkingTimeWindowLength,
            threadCreationTimeWindowLength,
            discriminator,
            cpuUsageProvider,
            stateQueryInterval,
            numberRecurrencesUntilTransition
        );
        this.threadCreationHandler = Optional.of(threadCreationHandler);
    }

    
    /**
     * Comment 
     * 
     * @param   adaptiveThreadFactoryId Comment
     * @param   parkingTimeWindowLength Comment
     * @param   threadCreationTimeWindowLength Comment
     * @param   numberParkingsThreshold Comment
     * @param   numberThreadCreationsThreshold Comment
     */
    /*
    public AdaptiveThreadFactory(
        int adaptiveThreadFactoryId, 
        long parkingTimeWindowLength, 
        long threadCreationTimeWindowLength,
        long numberParkingsThreshold,
        long numberThreadCreationsThreshold
    ) {
        // user specification
        this.adaptiveThreadFactoryId = adaptiveThreadFactoryId;
        addMonitor(this.adaptiveThreadFactoryId);
        setParameters(
            parkingTimeWindowLength,
            threadCreationTimeWindowLength,
            numberParkingsThreshold,
            numberThreadCreationsThreshold
        );
        // internal use
        this.platformThreadFactory = Thread.ofPlatform().factory();
        this.virtualThreadFactory = Thread.ofVirtual().factory();
        this.threads = new ConcurrentLinkedQueue<Thread>();
        this.executionMode = ExecutionMode.HETEROGENEOUS;
    }
    */

    /**
     * Comment 
     * 
     * @param   adaptiveThreadFactoryId Comment
     * @param   parkingTimeWindowLength Comment
     * @param   threadCreationTimeWindowLength Comment
     * @param   numberParkingsThreshold Comment
     * @param   numberThreadCreationsThreshold Comment
     * @param   stateQueryInterval Comment
     * @param   numberRecurrencesUntilTransition Comment
     * @param   threadCreationHandler Comment 
     */
    /*
    public AdaptiveThreadFactory(
        int adaptiveThreadFactoryId, 
        long parkingTimeWindowLength, 
        long threadCreationTimeWindowLength,
        long numberParkingsThreshold,
        long numberThreadCreationsThreshold,
        int stateQueryInterval,
        int numberRecurrencesUntilTransition,
        Runnable threadCreationHandler 
    ) {
        // user specification
        this(
            adaptiveThreadFactoryId,
            parkingTimeWindowLength,
            threadCreationTimeWindowLength,
            numberParkingsThreshold,
            numberThreadCreationsThreshold
        );
        this.stateQueryInterval = stateQueryInterval;
        this.numberRecurrencesUntilTransition = numberRecurrencesUntilTransition;
        this.threadCreationHandler = threadCreationHandler;
        // internal use
        this.queryResults = new LinkedList<ThreadType>();
        this.currentThreadType = ThreadType.PLATFORM;
        this.executionMode = ExecutionMode.HOMOGENEOUS;
        // start background thread
        startTransitionManager();
    }
    */

    private boolean shallTransition(ThreadType newQueryResult) {
        this.queryResults.add(newQueryResult);
        if(queryResults.size() > this.numberRecurrencesUntilTransition) {
            queryResults.poll();
        }
        final boolean identicalQueryResults = this.queryResults.stream().distinct().count() == 1;
        if(identicalQueryResults) {
            if(!newQueryResult.equals(this.currentThreadType)) {
                this.currentThreadType = newQueryResult;
                return true;
            }
        }
        return false;
    }

    private void performTransition() {
        LinkedList<Thread> busyThreads = new LinkedList<Thread>();
        Iterator<Thread> iterator = this.threads.iterator();
        while(iterator.hasNext()) {
            Thread busyThread = iterator.next();
            busyThreads.add(busyThread);
        }
        int terminationAttemptNumber = 0;
        LinkedList<Thread> persistentBusyThreads = new LinkedList<Thread>();
        for(final Thread busyThread : busyThreads) {
            busyThread.setAsInterruptedByAdaptiveThreadFactory();
            try {
                busyThread.join(this.threadTerminationWaitingTimeInMilliseconds);
            } catch(InterruptedException interruptedException) {
                throw new RuntimeException(interruptedException.getMessage());
            }
            if(busyThread.isAlive()) {
                persistentBusyThreads.add(busyThread);
            } else {
                this.threadCreationHandler.ifPresent((Runnable runnable) -> runnable.run());
            }
        }
        busyThreads = persistentBusyThreads;
        persistentBusyThreads = new LinkedList<Thread>();
        terminationAttemptNumber += 1;
        while(
            terminationAttemptNumber < this.maximalNumberTerminationAttempts &&
            busyThreads.size() > 0
        ) {
            for(final Thread busyThread : busyThreads) {
                try {
                    busyThread.join(this.threadTerminationWaitingTimeInMilliseconds);
                } catch(InterruptedException interruptedException) {
                    throw new RuntimeException(interruptedException.getMessage());
                }
                if(busyThread.isAlive()) {
                    persistentBusyThreads.add(busyThread);
                } else {
                    this.threadCreationHandler.ifPresent((Runnable runnable) -> runnable.run());
                }
            }
            busyThreads = persistentBusyThreads;
            persistentBusyThreads = new LinkedList<Thread>();
            terminationAttemptNumber += 1;
        }
    }

    private void startTransitionManager() {
        this.transitionManager = new Thread(() -> {
            while(!Thread.currentThread().isInterrupted()) {
                final ThreadType newQueryResult = queryMonitor();
                final boolean shallTransition = shallTransition(newQueryResult);
                if(shallTransition) {
                    performTransition();
                }
                try {
                    Thread.sleep(this.stateQueryInterval);
                } catch(InterruptedException interruptedException) {
                    return;
                }
            }
        });
        this.transitionManager.setDaemon(true);
        this.transitionManager.start();
    }

    private void stopTransitionManager() {
        this.transitionManager.interrupt();
        try {
            this.transitionManager.join();
        } catch(InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException.getMessage());
        }
    }

    /**
     * Comment 
     * 
     * @param   parkingTimeWindowLength Comment
     * @param   threadCreationTimeWindowLength Comment
     * @param   discriminator Comment
     */
    public void setParameters(
        long parkingTimeWindowLength, 
        long threadCreationTimeWindowLength,
        Discriminator discriminator
    ) {
        this.parkingTimeWindowLength = parkingTimeWindowLength;
        this.threadCreationTimeWindowLength = threadCreationTimeWindowLength;
        this.discriminator = discriminator;
        setMonitorParameters(
            this.adaptiveThreadFactoryId, 
            this.parkingTimeWindowLength, 
            this.threadCreationTimeWindowLength
        );
    }

    /**
     * Comment 
     * 
     * @param   parkingTimeWindowLength Comment
     * @param   threadCreationTimeWindowLength Comment
     * @param   numberParkingsThreshold Comment
     * @param   numberThreadCreationsThreshold Comment
     */
    /*
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
    */

    /**
     * Comment
     * @return Comment
     */
    public Thread newThread(Runnable originalTask) {
        final Runnable augmentedTask = augmentTask(originalTask);
        Thread newThread;
        if(executionMode.equals(ExecutionMode.HETEROGENEOUS)) {
            final ThreadType newQueryResult = queryMonitor();
            if(newQueryResult.equals(ThreadType.PLATFORM)) {
                newThread = platformThreadFactory.newThread(augmentedTask);
            } else {
                newThread = virtualThreadFactory.newThread(augmentedTask);
            }
        } else {
            if(this.currentThreadType.equals(ThreadType.PLATFORM)) {
                newThread = platformThreadFactory.newThread(augmentedTask);
            } else {
                newThread = virtualThreadFactory.newThread(augmentedTask);
            }
        }
        newThread.associateWithAdaptiveThreadFactory(this.adaptiveThreadFactoryId);
        threads.add(newThread);
        return newThread;
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
            this.threads.remove(Thread.currentThread());
        };
        return augmentedTask;
    }

    private ThreadType queryMonitor() {
        if(this.executionMode.equals(ExecutionMode.HETEROGENEOUS)) {
            return this.discriminator.discriminate(
                getNumberThreadCreationsInTimeWindow(),
                getNumberParkingsInTimeWindow(),
                this.cpuUsageProvider.get(),
                getNumberThreads(),
                Optional.empty()
            ); 
        }
        else {
            return this.discriminator.discriminate(
                getNumberThreadCreationsInTimeWindow(),
                getNumberParkingsInTimeWindow(),
                this.cpuUsageProvider.get(),
                getNumberThreads(),
                Optional.of(this.currentThreadType)
            );
        }
    }

    /*
    private ThreadType queryMonitor() {
        final boolean useVirtualThread = queryMonitor(this.adaptiveThreadFactoryId);
        if(useVirtualThread) {
            return ThreadType.VIRTUAL;
        } else {
            return ThreadType.PLATFORM;
        }
    }
    */

    /*
     * Comment
     */
    public void close() {
        if(this.executionMode.equals(ExecutionMode.HOMOGENEOUS)) {
            stopTransitionManager();
        }
        removeMonitor(this.adaptiveThreadFactoryId);
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
        long threadCreationTimeWindowLength
    );
    /*
    private native void setMonitorParameters(
        int adaptiveThreadFactoryId, 
        long parkingTimeWindowLength, 
        long threadCreationTimeWindowLength,
        long numberParkingsThreshold,
        long numberThreadCreationsThreshold
    );
    */
    /*
    private native boolean queryMonitor(int adaptiveThreadFactoryId);
    */
    static native void registerJavaThreadAndAssociateOSThreadWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId); // called by platform and virtual threads
    static native void deregisterJavaThreadAndDisassociateOSThreadFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId); // called by platform and virtual threads
    static native void associateOSThreadWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId); // called by virtual threads only
    static native void disassociateOSThreadFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId); // called by virtual threads only
    
    /* Native methods for testing */
    static native long countParkingsInTimeWindow(int adaptiveThreadFactoryId);
    static native long countThreadCreationsInTimeWindow(int adaptiveThreadFactoryId);
    static native long countNumberThreads(int adaptiveThreadFactoryId);
}
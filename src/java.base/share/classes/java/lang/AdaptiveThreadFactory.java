package java.lang;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Comment
 */
public class AdaptiveThreadFactory implements ThreadFactory, AutoCloseable {

  private static native void registerNatives();

  static {
    registerNatives();
  }

  private enum ExecutionMode {
    HETEROGENEOUS,
    HOMOGENEOUS,
  }

  /**
   * Comment
   */
  public enum ThreadType {
    /** Comment */PLATFORM,
    /** Comment */VIRTUAL,
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
      long numberParkingsInTimeWindow,
      long numberThreadCreationsInTimeWindow,
      double cpuUsage,
      long numberThreads,
      Optional<ThreadType> currentThreadType
    );
  }

  // factory identification

  private static AtomicInteger adaptiveThreadFactoryCounter;

  static {
    adaptiveThreadFactoryCounter = new AtomicInteger(0);
  }

  // user specification
  private int adaptiveThreadFactoryId;
  private long parkingTimeWindowLength; // in milliseconds
  private long threadCreationTimeWindowLength; // in milliseconds
  private Discriminator discriminator;
  private int cpuUsageSamplingPeriod; // in milliseconds
  private int numberRelevantCpuUsageSamples;
  private Optional<Integer> stateQueryInterval; // in milliseconds
  private Optional<Integer> numberRecurrencesUntilTransition;
  private Optional<Runnable> threadCreationHandler;

  // internal use

  private ExecutionMode executionMode;
  private ThreadFactory platformThreadFactory;
  private ThreadFactory virtualThreadFactory;
  private ConcurrentLinkedQueue<Thread> threads;
  private Object operatingSystemMXBeanObject;
  private Method getCpuLoadMethod;
  private Supplier<Double> systemCpuUsageSupplier;
  private Method getProcessCpuLoadMethod;
  private Supplier<Double> processCpuUsageSupplier;
  private Thread cpuUsageSampler;
  private ConcurrentLinkedQueue<Double> cpuUsageSamples;

  // ExecutionMode.HOMOGENEOUS
  private Thread transitionManager;
  private ThreadType currentThreadType;
  private LinkedList<ThreadType> queryResults;
  private Integer maximalNumberTerminationAttempts;
  private Integer threadTerminationWaitingTimeInMilliseconds;

  /**
   * Comment
   */
  public static class AdaptiveThreadFactoryBuilder {

    private long parkingTimeWindowLength; // in milliseconds
    private long threadCreationTimeWindowLength; // in milliseconds
    private Discriminator discriminator;
    private int cpuUsageSamplingPeriod;
    private int numberRelevantCpuUsageSamples;
    private Optional<Integer> stateQueryInterval; // in milliseconds
    private Optional<Integer> numberRecurrencesUntilTransition;
    private Optional<Runnable> threadCreationHandler;

    /**
     * Comment
     */
    public AdaptiveThreadFactoryBuilder() {
      this.parkingTimeWindowLength = getDefaultParkingTimeWindowLength();
      this.threadCreationTimeWindowLength = getDefaultThreadCreationTimeWindowLength();
      this.discriminator = getDefaultDiscriminator();
      this.cpuUsageSamplingPeriod = getDefaultCpuUsageSamplingPeriod();
      this.numberRelevantCpuUsageSamples = getDefaultNumberRelevantCpuUsageSamples();
      this.stateQueryInterval = Optional.empty();
      this.numberRecurrencesUntilTransition = Optional.empty();
      this.threadCreationHandler = Optional.empty();
    }

    /**
     * Comment
     *
     * @param parkingTimeWindowLength Comment
     * @return Comment
     */
    public AdaptiveThreadFactoryBuilder setParkingTimeWindowLength(
      long parkingTimeWindowLength
    ) {
      this.parkingTimeWindowLength = parkingTimeWindowLength;
      return this;
    }

    /**
     * Comment
     *
     * @param threadCreationTimeWindowLength Comment
     * @return Comment
     */
    public AdaptiveThreadFactoryBuilder setThreadCreationTimeWindowLength(
      long threadCreationTimeWindowLength
    ) {
      this.threadCreationTimeWindowLength = threadCreationTimeWindowLength;
      return this;
    }

    /**
     * Comment
     *
     * @param discriminator Comment
     * @return Comment
     */
    public AdaptiveThreadFactoryBuilder setDiscriminator(
      Discriminator discriminator
    ) {
      this.discriminator = discriminator;
      return this;
    }

    /**
     * Comment
     *
     * @param cpuUsageSamplingPeriod Comment
     * @return Comment
     */
    public AdaptiveThreadFactoryBuilder setCpuUsageSamplingPeriod(
      int cpuUsageSamplingPeriod
    ) {
      this.cpuUsageSamplingPeriod = cpuUsageSamplingPeriod;
      return this;
    }

    /**
     * Comment
     *
     * @param numberRelevantCpuUsageSamples Comment
     * @return Comment
     */
    public AdaptiveThreadFactoryBuilder setNumberRelevantCpuUsageSamples(
      int numberRelevantCpuUsageSamples
    ) {
      this.numberRelevantCpuUsageSamples = numberRelevantCpuUsageSamples;
      return this;
    }

    /**
     * Comment
     *
     * @param stateQueryInterval Comment
     * @return Comment
     */
    public AdaptiveThreadFactoryBuilder setStateQueryInterval(
      int stateQueryInterval
    ) {
      this.stateQueryInterval = Optional.of(stateQueryInterval);
      return this;
    }

    /**
     * Comment
     *
     * @param numberRecurrencesUntilTransition Comment
     * @return Comment
     */
    public AdaptiveThreadFactoryBuilder setNumberRecurrencesUntilTransition(
      int numberRecurrencesUntilTransition
    ) {
      this.numberRecurrencesUntilTransition =
        Optional.of(numberRecurrencesUntilTransition);
      return this;
    }

    /**
     * Comment
     *
     * @param threadCreationHandler Comment
     * @return Comment
     */
    public AdaptiveThreadFactoryBuilder setThreadCreationHandler(
      Runnable threadCreationHandler
    ) {
      this.threadCreationHandler = Optional.of(threadCreationHandler);
      return this;
    }

    /**
     * Comment
     *
     * @return Comment
     */
    public AdaptiveThreadFactory build() {
      AdaptiveThreadFactory adaptiveThreadFactory = new AdaptiveThreadFactory(
        this.parkingTimeWindowLength,
        this.threadCreationTimeWindowLength,
        this.discriminator,
        this.cpuUsageSamplingPeriod,
        this.numberRelevantCpuUsageSamples,
        this.stateQueryInterval,
        this.numberRecurrencesUntilTransition,
        this.threadCreationHandler
      );
      return adaptiveThreadFactory;
    }
  }

  private class UserSpecificationRequirement {

    private boolean condition;
    private String message;

    public UserSpecificationRequirement setCondition(boolean condition) {
      this.condition = condition;
      return this;
    }

    public UserSpecificationRequirement setMessage(String message) {
      this.message = message;
      return this;
    }

    public void check() {
      if (!this.condition) {
        throw new IllegalArgumentException(this.message);
      }
    }
  }

  private void validateUserSpecification() {
    LinkedList<UserSpecificationRequirement> requirements = new LinkedList<UserSpecificationRequirement>();
    requirements.add(
      new UserSpecificationRequirement()
        .setCondition(
          (
            this.stateQueryInterval.isPresent() &&
            this.numberRecurrencesUntilTransition.isPresent()
          ) ||
          (
            this.stateQueryInterval.isEmpty() &&
            this.numberRecurrencesUntilTransition.isEmpty()
          )
        )
        .setMessage(
          "Either all or none of the parameters {stateQueryInterval, numberRecurrencesUntilTransition} must be set."
        )
    );
    for (final UserSpecificationRequirement requirement : requirements) {
      requirement.check();
    }
  }

  private boolean homogeneousExecutionModeInEffect() {
    final boolean homogeneousExecutionModeInEffect =
      this.stateQueryInterval.isPresent() &&
      this.numberRecurrencesUntilTransition.isPresent();
    return homogeneousExecutionModeInEffect;
  }

  private void setDefaultInternalValues() {
    this.maximalNumberTerminationAttempts = 10;
    this.threadTerminationWaitingTimeInMilliseconds = 50;
  }

  private void startPeriodicallyActiveThreads() {
    startCpuUsageSampler();
    if (homogeneousExecutionModeInEffect()) {
      startTransitionManager();
    }
  }

  private void createCpuUsageSupplier() {
    try {
      ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
      Class<?> operatingSystemMXBeanClass = systemClassLoader.loadClass(
        "com.sun.management.OperatingSystemMXBean"
      );
      Class<?> managementFactoryClass = systemClassLoader.loadClass(
        "java.lang.management.ManagementFactory"
      );
      Method getOperatingSystemMXBeanMethod = managementFactoryClass.getDeclaredMethod(
        "getOperatingSystemMXBean"
      );
      this.operatingSystemMXBeanObject =
        operatingSystemMXBeanClass.cast(
          getOperatingSystemMXBeanMethod.invoke(null)
        );
      this.getCpuLoadMethod =
        operatingSystemMXBeanClass.getDeclaredMethod("getCpuLoad");
      this.getProcessCpuLoadMethod =
        operatingSystemMXBeanClass.getDeclaredMethod("getProcessCpuLoad");
      this.systemCpuUsageSupplier =
        () -> {
          try {
            return (double) this.getCpuLoadMethod.invoke(
                this.operatingSystemMXBeanObject
              );
          } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
          }
        };
      this.processCpuUsageSupplier =
        () -> {
          try {
            return (double) this.getProcessCpuLoadMethod.invoke(
                this.operatingSystemMXBeanObject
              );
          } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
          }
        };
    } catch (Exception exception) {
      throw new RuntimeException(exception.getMessage());
    }
  }

  private void initialise() {
    this.platformThreadFactory = Thread.ofPlatform().factory();
    this.virtualThreadFactory = Thread.ofVirtual().factory();
    this.threads = new ConcurrentLinkedQueue<Thread>();
    createCpuUsageSupplier();
    this.cpuUsageSamples = new ConcurrentLinkedQueue<Double>();
    if (homogeneousExecutionModeInEffect()) {
      this.executionMode = ExecutionMode.HOMOGENEOUS;
      this.queryResults = new LinkedList<ThreadType>();
      this.currentThreadType = ThreadType.PLATFORM;
    } else {
      this.executionMode = ExecutionMode.HETEROGENEOUS;
    }
    setDefaultInternalValues();
    startPeriodicallyActiveThreads();
  }

  private static long getDefaultParkingTimeWindowLength() {
    return 200;
  }

  private static long getDefaultThreadCreationTimeWindowLength() {
    return 200;
  }

  private static Discriminator getDefaultDiscriminator() {
    Discriminator defaultDiscriminator = (
        long numberParkingsInTimeWindow,
        long numberThreadCreationsInTimeWindow,
        double cpuUsage,
        long numberFactoryThreads,
        Optional<AdaptiveThreadFactory.ThreadType> currentThreadType
      ) -> {
        if (numberParkingsInTimeWindow < 600) {
          return AdaptiveThreadFactory.ThreadType.PLATFORM;
        } else if (numberParkingsInTimeWindow >= 1100) {
          return AdaptiveThreadFactory.ThreadType.VIRTUAL;
        } else {
          if (
            currentThreadType
              .get()
              .equals(AdaptiveThreadFactory.ThreadType.PLATFORM)
          ) {
            if (cpuUsage >= 0.5) {
              return AdaptiveThreadFactory.ThreadType.PLATFORM;
            } else {
              return AdaptiveThreadFactory.ThreadType.VIRTUAL;
            }
          } else {
            if (cpuUsage >= 0.425) {
              return AdaptiveThreadFactory.ThreadType.PLATFORM;
            } else {
              return AdaptiveThreadFactory.ThreadType.VIRTUAL;
            }
          }
        }
      };
      return defaultDiscriminator;
  }

  private static int getDefaultCpuUsageSamplingPeriod() {
    return 100;
  }

  private static int getDefaultNumberRelevantCpuUsageSamples() {
    return 5;
  }

  private static Optional<Integer> getDefaultStateQueryInterval() {
    return Optional.of(1500);
  }

  private static Optional<Integer> getDefaultNumberRecurrencesUntilTransition() {
    return Optional.of(5);
  }

  private static Optional<Runnable> getDefaultThreadCreationHandler() {
    return Optional.empty();
  }

  /**
   * Comment
   *
   * @param   parkingTimeWindowLength Comment
   * @param   threadCreationTimeWindowLength Comment
   * @param   discriminator Comment
   * @param   cpuUsageSamplingPeriod Comment
   * @param   numberRelevantCpuUsageSamples Comment
   * @param   stateQueryInterval Comment
   * @param   numberRecurrencesUntilTransition Comment
   * @param   threadCreationHandler Comment
   */
  private AdaptiveThreadFactory(
    long parkingTimeWindowLength,
    long threadCreationTimeWindowLength,
    Discriminator discriminator,
    int cpuUsageSamplingPeriod,
    int numberRelevantCpuUsageSamples,
    Optional<Integer> stateQueryInterval,
    Optional<Integer> numberRecurrencesUntilTransition,
    Optional<Runnable> threadCreationHandler
  ) {
    this.adaptiveThreadFactoryId = adaptiveThreadFactoryCounter.incrementAndGet();
    this.parkingTimeWindowLength = parkingTimeWindowLength;
    this.threadCreationTimeWindowLength = threadCreationTimeWindowLength;
    this.discriminator = discriminator;
    this.cpuUsageSamplingPeriod = cpuUsageSamplingPeriod;
    this.numberRelevantCpuUsageSamples = numberRelevantCpuUsageSamples;
    this.stateQueryInterval = stateQueryInterval;
    this.numberRecurrencesUntilTransition = numberRecurrencesUntilTransition;
    this.threadCreationHandler = threadCreationHandler;
    validateUserSpecification();
    addMonitor(this.adaptiveThreadFactoryId);
    setMonitorParameters(
      this.adaptiveThreadFactoryId,
      this.parkingTimeWindowLength,
      this.threadCreationTimeWindowLength
    );
    initialise();
  }

  /**
   * Comment
   *
   */
  public AdaptiveThreadFactory() {
    this(
      getDefaultParkingTimeWindowLength(),
      getDefaultThreadCreationTimeWindowLength(),
      getDefaultDiscriminator(),
      getDefaultCpuUsageSamplingPeriod(),
      getDefaultNumberRelevantCpuUsageSamples(),
      getDefaultStateQueryInterval(),
      getDefaultNumberRecurrencesUntilTransition(),
      getDefaultThreadCreationHandler()
    );
  }

  private boolean shallTransition(ThreadType newQueryResult) {
    this.queryResults.add(newQueryResult);
    if (queryResults.size() > this.numberRecurrencesUntilTransition.get()) {
      queryResults.poll();
    }
    final boolean identicalQueryResults =
      this.queryResults.stream().distinct().count() == 1;
    if (identicalQueryResults) {
      if (!newQueryResult.equals(this.currentThreadType)) {
        this.currentThreadType = newQueryResult;
        return true;
      }
    }
    return false;
  }

  private void performTransition() {
    LinkedList<Thread> oldThreads = new LinkedList<Thread>();
    Iterator<Thread> iterator = this.threads.iterator();
    while (iterator.hasNext()) {
      Thread oldThread = iterator.next();
      oldThreads.add(oldThread);
    }
    for (final Thread oldThread : oldThreads) {
      oldThread.setAsInterruptedByAdaptiveThreadFactory();
    }
  }

  /*
  private void performTransition() {
    LinkedList<Thread> busyThreads = new LinkedList<Thread>();
    Iterator<Thread> iterator = this.threads.iterator();
    while (iterator.hasNext()) {
      Thread busyThread = iterator.next();
      busyThreads.add(busyThread);
    }
    int terminationAttemptNumber = 0;
    LinkedList<Thread> persistentBusyThreads = new LinkedList<Thread>();
    for (final Thread busyThread : busyThreads) {
      busyThread.setAsInterruptedByAdaptiveThreadFactory();
      try {
        busyThread.join(this.threadTerminationWaitingTimeInMilliseconds);
      } catch (InterruptedException interruptedException) {
        throw new RuntimeException(interruptedException.getMessage());
      }
      if (busyThread.isAlive()) {
        persistentBusyThreads.add(busyThread);
      } else {
        this.threadCreationHandler.ifPresent((Runnable runnable) ->
            runnable.run()
          );
      }
    }
    busyThreads = persistentBusyThreads;
    persistentBusyThreads = new LinkedList<Thread>();
    terminationAttemptNumber += 1;
    while (
      terminationAttemptNumber < this.maximalNumberTerminationAttempts &&
      busyThreads.size() > 0
    ) {
      for (final Thread busyThread : busyThreads) {
        try {
          busyThread.join(this.threadTerminationWaitingTimeInMilliseconds);
        } catch (InterruptedException interruptedException) {
          throw new RuntimeException(interruptedException.getMessage());
        }
        if (busyThread.isAlive()) {
          persistentBusyThreads.add(busyThread);
        } else {
          this.threadCreationHandler.ifPresent((Runnable runnable) ->
              runnable.run()
            );
        }
      }
      busyThreads = persistentBusyThreads;
      persistentBusyThreads = new LinkedList<Thread>();
      terminationAttemptNumber += 1;
    }
  }
  */

  private void startTransitionManager() {
    this.transitionManager =
      new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
          final ThreadType newQueryResult = queryMonitor();
          final boolean shallTransition = shallTransition(newQueryResult);
          if (shallTransition) {
            performTransition();
          }
          try {
            Thread.sleep(this.stateQueryInterval.get());
          } catch (InterruptedException interruptedException) {
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
    } catch (InterruptedException interruptedException) {
      throw new RuntimeException(interruptedException.getMessage());
    }
  }

  private void startCpuUsageSampler() {
    this.cpuUsageSampler =
      new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
          final double cpuUsageSample = this.systemCpuUsageSupplier.get();
          this.cpuUsageSamples.add(cpuUsageSample);
          if (
            this.cpuUsageSamples.size() >
            this.numberRelevantCpuUsageSamples
          ) {
            this.cpuUsageSamples.poll();
          }
          try {
            Thread.sleep(this.cpuUsageSamplingPeriod);
          } catch (InterruptedException interruptedException) {
            return;
          }
        }
      });
    this.cpuUsageSampler.setDaemon(true);
    this.cpuUsageSampler.start();
  }

  private void stopCpuUsageSampler() {
    this.cpuUsageSampler.interrupt();
    try {
      this.cpuUsageSampler.join();
    } catch (InterruptedException interruptedException) {
      throw new RuntimeException(interruptedException.getMessage());
    }
  }

  /**
   * Comment
   * @return Comment
   */
  public Thread newThread(Runnable originalTask) {
    final Runnable augmentedTask = augmentTask(originalTask);
    Thread newThread;
    if (!homogeneousExecutionModeInEffect()) {
      final ThreadType newQueryResult = queryMonitor();
      if (newQueryResult.equals(ThreadType.PLATFORM)) {
        newThread = platformThreadFactory.newThread(augmentedTask);
      } else {
        newThread = virtualThreadFactory.newThread(augmentedTask);
      }
    } else {
      if (this.currentThreadType.equals(ThreadType.PLATFORM)) {
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
      recordThreadCreation(Thread.currentThread().getAdaptiveThreadFactoryId());
      originalTask.run();
      deregisterJavaThreadAndDisassociateOSThreadFromMonitor(
        Thread.currentThread().getAdaptiveThreadFactoryId(),
        Thread.currentThread().threadId()
      );
      this.threads.remove(Thread.currentThread());
      if(Thread.currentThread().isInterruptedByAdaptiveThreadFactory()) {
        this.threadCreationHandler.ifPresent((Runnable runnable) ->
          runnable.run()
        );
      }
    };
    return augmentedTask;
  }

  private ThreadType queryMonitor() {
    Optional<ThreadType> currentFactoryThreadType;
    if (homogeneousExecutionModeInEffect()) {
      currentFactoryThreadType = Optional.of(this.currentThreadType);
    } else {
      currentFactoryThreadType = Optional.empty();
    }
    return this.discriminator.discriminate(
        getNumberParkingsInTimeWindow(),
        getNumberThreadCreationsInTimeWindow(),
        computeAverageCpuUsage(),
        getNumberThreads(),
        currentFactoryThreadType
      );
  }

  private double computeAverageCpuUsage() {
    LinkedList<Double> currentCpuUsageSamples = new LinkedList<Double>();
    Iterator<Double> iterator = this.cpuUsageSamples.iterator();
    while (iterator.hasNext()) {
      Double cpuUsageSample = iterator.next();
      currentCpuUsageSamples.add(cpuUsageSample);
    }
    final double averageCpuUsage = currentCpuUsageSamples
      .stream()
      .mapToDouble(Double::doubleValue)
      .average()
      .orElse(0.0);
    return averageCpuUsage;
  }

  /*
   * Comment
   */
  public void close() {
    stopCpuUsageSampler();
    if (homogeneousExecutionModeInEffect()) {
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
    long numberParkingsInTimeWindow = countParkingsInTimeWindow(
      this.adaptiveThreadFactoryId
    );
    return numberParkingsInTimeWindow;
  }

  /**
   * Comment
   * @return Comment
   */
  public long getNumberThreadCreationsInTimeWindow() {
    long numberThreadCreationsInTimeWindow = countThreadCreationsInTimeWindow(
      this.adaptiveThreadFactoryId
    );
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

  /**
   * Comment
   * @return Comment
   */
  public double getSystemCpuUsage() {
    return this.systemCpuUsageSupplier.get();
  }

  /**
   * Comment
   * @return Comment
   */
  public double getProcessCpuUsage() {
    return this.processCpuUsageSupplier.get();
  }

  /**
   * Comment
   *
   * @param   discriminator Comment
   */
  public void setDiscriminator(
    Discriminator discriminator
  ) {
    this.discriminator = discriminator;
  }

  /* Native methods */

  private native void addMonitor(int adaptiveThreadFactoryId);

  private native void removeMonitor(int adaptiveThreadFactoryId);

  private native void setMonitorParameters(
    int adaptiveThreadFactoryId,
    long parkingTimeWindowLength,
    long threadCreationTimeWindowLength
  );

  private static native void registerJavaThreadAndAssociateOSThreadWithMonitor(
    int adaptiveThreadFactoryId,
    long javaLevelThreadId
  ); // called by platform and virtual threads

  private static native void deregisterJavaThreadAndDisassociateOSThreadFromMonitor(
    int adaptiveThreadFactoryId,
    long javaLevelThreadId
  ); // called by platform and virtual threads

  static native void associateOSThreadWithMonitor(
    int adaptiveThreadFactoryId,
    long javaLevelThreadId
  ); // called by virtual threads only

  static native void disassociateOSThreadFromMonitor(
    int adaptiveThreadFactoryId,
    long javaLevelThreadId
  ); // called by virtual threads only

  private static native void recordThreadCreation(int adaptiveThreadFactoryId);

  /* Native methods for testing */
  static native long countParkingsInTimeWindow(int adaptiveThreadFactoryId);

  static native long countThreadCreationsInTimeWindow(
    int adaptiveThreadFactoryId
  );

  static native long countNumberThreads(int adaptiveThreadFactoryId);
}

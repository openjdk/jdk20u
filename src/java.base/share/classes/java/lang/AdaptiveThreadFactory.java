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
      long numberThreadCreationsInTimeWindow,
      long numberParkingsInTimeWindow,
      long numberThreads,
      Optional<Double> cpuUsage,
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
  private Optional<Integer> cpuUsageSamplingPeriod; // in milliseconds
  private Optional<Integer> numberRelevantCpuUsageSamples;
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

    private Optional<Long> parkingTimeWindowLength; // in milliseconds
    private Optional<Long> threadCreationTimeWindowLength; // in milliseconds
    private Optional<Discriminator> discriminator;
    private Optional<Integer> cpuUsageSamplingPeriod;
    private Optional<Integer> numberRelevantCpuUsageSamples;
    private Optional<Integer> stateQueryInterval; // in milliseconds
    private Optional<Integer> numberRecurrencesUntilTransition;
    private Optional<Runnable> threadCreationHandler;

    /**
     * Comment
     */
    public AdaptiveThreadFactoryBuilder() {
      this.parkingTimeWindowLength = Optional.empty();
      this.threadCreationTimeWindowLength = Optional.empty();
      this.discriminator = Optional.empty();
      this.cpuUsageSamplingPeriod = Optional.empty();
      this.numberRelevantCpuUsageSamples = Optional.empty();
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
      this.parkingTimeWindowLength = Optional.of(parkingTimeWindowLength);
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
      this.threadCreationTimeWindowLength =
        Optional.of(threadCreationTimeWindowLength);
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
      this.discriminator = Optional.of(discriminator);
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
      this.cpuUsageSamplingPeriod = Optional.of(cpuUsageSamplingPeriod);
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
      this.numberRelevantCpuUsageSamples =
        Optional.of(numberRelevantCpuUsageSamples);
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
        this.parkingTimeWindowLength.get(),
        this.threadCreationTimeWindowLength.get(),
        this.discriminator.get(),
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
            this.cpuUsageSamplingPeriod.isPresent() &&
            this.numberRelevantCpuUsageSamples.isPresent()
          ) ||
          (
            this.cpuUsageSamplingPeriod.isEmpty() &&
            this.numberRelevantCpuUsageSamples.isEmpty()
          )
        )
        .setMessage(
          "Either all or none of the parameters {cpuUsageSamplingPeriod, numberRelevantCpuUsageSamples} must be set."
        )
    );
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

  private boolean cpuUsageInEffect() {
    final boolean cpuUsageInEffect =
      this.cpuUsageSamplingPeriod.isPresent() &&
      this.numberRelevantCpuUsageSamples.isPresent();
    return cpuUsageInEffect;
  }

  private boolean homogeneousExecutionModeInEffect() {
    final boolean homogeneousExecutionModeInEffect =
      this.stateQueryInterval.isPresent() &&
      this.numberRecurrencesUntilTransition.isPresent();
    return homogeneousExecutionModeInEffect;
  }

  private void setDefaultValues() {
    this.maximalNumberTerminationAttempts = 10;
    this.threadTerminationWaitingTimeInMilliseconds = 50;
  }

  private void startPeriodicallyActiveThreads() {
    if (cpuUsageInEffect()) {
      startCpuUsageSampler();
    }
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
    if (cpuUsageInEffect()) {
      createCpuUsageSupplier();
      this.cpuUsageSamples = new ConcurrentLinkedQueue<Double>();
    }
    if (homogeneousExecutionModeInEffect()) {
      this.executionMode = ExecutionMode.HOMOGENEOUS;
      this.queryResults = new LinkedList<ThreadType>();
      this.currentThreadType = ThreadType.PLATFORM;
    } else {
      this.executionMode = ExecutionMode.HETEROGENEOUS;
    }
    setDefaultValues();
    startPeriodicallyActiveThreads();
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
  public AdaptiveThreadFactory(
    long parkingTimeWindowLength,
    long threadCreationTimeWindowLength,
    Discriminator discriminator,
    Optional<Integer> cpuUsageSamplingPeriod,
    Optional<Integer> numberRelevantCpuUsageSamples,
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
      200,
      200,
      (
        long numberThreadCreationsInTimeWindow,
        long numberParkingsInTimeWindow,
        long numberFactoryThreads,
        Optional<Double> cpuUsage,
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
            if (cpuUsage.get() >= 0.5) {
              return AdaptiveThreadFactory.ThreadType.PLATFORM;
            } else {
              return AdaptiveThreadFactory.ThreadType.VIRTUAL;
            }
          } else {
            if (cpuUsage.get() >= 0.425) {
              return AdaptiveThreadFactory.ThreadType.PLATFORM;
            } else {
              return AdaptiveThreadFactory.ThreadType.VIRTUAL;
            }
          }
        }
      },
      Optional.of(100),
      Optional.of(5),
      Optional.of(1500),
      Optional.of(5),
      Optional.empty()
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
            this.numberRelevantCpuUsageSamples.get()
          ) {
            this.cpuUsageSamples.poll();
          }
          try {
            Thread.sleep(this.cpuUsageSamplingPeriod.get());
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
    if (executionMode.equals(ExecutionMode.HETEROGENEOUS)) {
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
    Optional<Double> averageCpuUsage;
    if (cpuUsageInEffect()) {
      averageCpuUsage = Optional.of(computeAverageCpuUsage());
    } else {
      averageCpuUsage = Optional.empty();
    }
    Optional<ThreadType> currentFactoryThreadType;
    if (this.executionMode.equals(ExecutionMode.HOMOGENEOUS)) {
      currentFactoryThreadType = Optional.of(this.currentThreadType);
    } else {
      currentFactoryThreadType = Optional.empty();
    }
    return this.discriminator.discriminate(
        getNumberThreadCreationsInTimeWindow(),
        getNumberParkingsInTimeWindow(),
        getNumberThreads(),
        averageCpuUsage,
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
    if (cpuUsageInEffect()) {
      stopCpuUsageSampler();
    }
    if (this.executionMode.equals(ExecutionMode.HOMOGENEOUS)) {
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
    if (cpuUsageInEffect()) {
      return this.systemCpuUsageSupplier.get();
    } else {
      return -1.0;
    }
  }

  /**
   * Comment
   * @return Comment
   */
  public double getProcessCpuUsage() {
    if (cpuUsageInEffect()) {
      return this.processCpuUsageSupplier.get();
    } else {
      return -1.0;
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

  /* Native methods */

  private native void addMonitor(int adaptiveThreadFactoryId);

  private native void removeMonitor(int adaptiveThreadFactoryId);

  private native void setMonitorParameters(
    int adaptiveThreadFactoryId,
    long parkingTimeWindowLength,
    long threadCreationTimeWindowLength
  );

  static native void registerJavaThreadAndAssociateOSThreadWithMonitor(
    int adaptiveThreadFactoryId,
    long javaLevelThreadId
  ); // called by platform and virtual threads

  static native void deregisterJavaThreadAndDisassociateOSThreadFromMonitor(
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

  /* Native methods for testing */
  static native long countParkingsInTimeWindow(int adaptiveThreadFactoryId);

  static native long countThreadCreationsInTimeWindow(
    int adaptiveThreadFactoryId
  );

  static native long countNumberThreads(int adaptiveThreadFactoryId);
}

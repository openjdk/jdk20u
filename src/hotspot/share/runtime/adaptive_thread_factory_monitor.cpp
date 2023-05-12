#include "runtime/adaptive_thread_factory_utility.hpp"
#include "runtime/adaptive_thread_factory_monitor.hpp"

#include <time.h>
#include <math.h>

AdaptiveThreadFactoryMonitor::AdaptiveThreadFactoryMonitor() {}

AdaptiveThreadFactoryMonitor::AdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId) { 
    _adaptiveThreadFactoryId = adaptiveThreadFactoryId;
    _javaLevelThreadIds = new SimpleConcurrentLinkedList<long>(-1);
    _threadCreationTimes = new SimpleConcurrentLinkedList<long>(-1);
    _parkingTimes = new SimpleConcurrentLinkedList<long>(-1);
}

void AdaptiveThreadFactoryMonitor::setParameters(
    long parkingTimeWindowLength, 
    long threadCreationTimeWindowLength,
    long numberParkingsThreshold,
    long numberThreadCreationsThreshold
) {
    _parkingTimeWindowLength = parkingTimeWindowLength;
    _threadCreationTimeWindowLength = threadCreationTimeWindowLength;
    _numberParkingsThreshold = numberParkingsThreshold;
    _numberThreadCreationsThreshold = numberThreadCreationsThreshold;
}

int AdaptiveThreadFactoryMonitor::getFactoryId() const {
    return _adaptiveThreadFactoryId;
}

const long& AdaptiveThreadFactoryMonitor::addAndGetJavaLevelThreadId(long javaLevelThreadId) {
    _javaLevelThreadIds->append(javaLevelThreadId);
    AdaptiveThreadFactoryUtility::checkRequirement(
       javaLevelThreadId == _javaLevelThreadIds->get(javaLevelThreadId),
       (char*)"AdaptiveThreadFactoryMonitor::addJavaLevelThreadId: The requested ID does not exist."
    );
    return _javaLevelThreadIds->get(javaLevelThreadId);
}

const long& AdaptiveThreadFactoryMonitor::getJavaLevelThreadId(long javaLevelThreadId) {
    AdaptiveThreadFactoryUtility::checkRequirement(
       javaLevelThreadId == _javaLevelThreadIds->get(javaLevelThreadId),
       (char*)"AdaptiveThreadFactoryMonitor::addJavaLevelThreadId: The requested ID does not exist."
    );
    return _javaLevelThreadIds->get(javaLevelThreadId);
}

void AdaptiveThreadFactoryMonitor::removeJavaLevelThreadId(long javaLevelThreadId) {
    _javaLevelThreadIds->remove(javaLevelThreadId);
}

long AdaptiveThreadFactoryMonitor::getCurrentTimeInMilliseconds() {
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    long currentTimeInMilliseconds = 1000 * now.tv_sec + round((double)now.tv_nsec/1000000);
    return currentTimeInMilliseconds;
}

void AdaptiveThreadFactoryMonitor::recordThreadCreation() {
    long currentTimeInMilliseconds = getCurrentTimeInMilliseconds();
    _threadCreationTimes->append(currentTimeInMilliseconds);
}
void AdaptiveThreadFactoryMonitor::recordParking() {
    long currentTimeInMilliseconds = getCurrentTimeInMilliseconds();
    _parkingTimes->append(currentTimeInMilliseconds);
}

long AdaptiveThreadFactoryMonitor::countNumberEventsInTimeWindow(SimpleConcurrentLinkedList<long>* events, long timeWindowLength) {
    long timeLowerBound = getCurrentTimeInMilliseconds() - timeWindowLength;
    long counter = events->countRecentValuesAndRemoveOldValues(timeLowerBound);
    return counter;
}

bool AdaptiveThreadFactoryMonitor::shallCreateVirtualThread() {
    long numberParkingsInTimeWindow = countNumberEventsInTimeWindow(_parkingTimes, _parkingTimeWindowLength);
    long numberThreadCreationsInTimeWindow = countNumberEventsInTimeWindow(_threadCreationTimes, _threadCreationTimeWindowLength);
    return true;
}
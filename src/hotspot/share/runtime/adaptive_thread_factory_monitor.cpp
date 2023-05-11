#include "runtime/adaptive_thread_factory_utility.hpp"
#include "runtime/adaptive_thread_factory_monitor.hpp"

#include <time.h>
#include <math.h>

AdaptiveThreadFactoryMonitor::AdaptiveThreadFactoryMonitor() {}

AdaptiveThreadFactoryMonitor::AdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId) { 
    _adaptiveThreadFactoryId = adaptiveThreadFactoryId;
    _javaLevelThreadIds = new SimpleConcurrentLinkedList<long>(-1);
    _threadCreationTimes = new SimpleConcurrentLinkedList<long>(-1);
}

void AdaptiveThreadFactoryMonitor::setParameters(long threadCreationTimeWindowLength) {
    _threadCreationTimeWindowLength = threadCreationTimeWindowLength;
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

void AdaptiveThreadFactoryMonitor::recordThreadCreation() {
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    long currentTimeInMilliseconds = 1000 * now.tv_sec + round((double)now.tv_nsec/1000000);
    _threadCreationTimes->append(currentTimeInMilliseconds);
}
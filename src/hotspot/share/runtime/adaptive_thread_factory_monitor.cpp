#include "runtime/adaptive_thread_factory_utility.hpp"
#include "runtime/adaptive_thread_factory_monitor.hpp"

AdaptiveThreadFactoryMonitor::AdaptiveThreadFactoryMonitor() {}

AdaptiveThreadFactoryMonitor::AdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId) { 
    _adaptiveThreadFactoryId = adaptiveThreadFactoryId;
    long defaultJavaLevelThreadIdValue = -1;
    _javaLevelThreadIds = new SimpleConcurrentLinkedList<long>(defaultJavaLevelThreadIdValue);
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

//AdaptiveThreadFactoryMonitor& AdaptiveThreadFactoryMonitor::operator=(const AdaptiveThreadFactoryMonitor& adaptiveThreadFactoryMonitor) {
//    _adaptiveThreadFactoryId = adaptiveThreadFactoryMonitor._adaptiveThreadFactoryId;
//    return *this;
//}

// AdaptiveThreadFactoryMonitor::AdaptiveThreadFactoryMonitor() {
//     _numberMonitoredThreads.store(0);
// }

// AdaptiveThreadFactoryMonitor& AdaptiveThreadFactoryMonitor::operator=(const AdaptiveThreadFactoryMonitor& adaptiveThreadFactoryMonitor) {
//     this->_numberMonitoredThreads.store(adaptiveThreadFactoryMonitor._numberMonitoredThreads.load());
//     return *this;
// }

// AdaptiveThreadFactoryMonitor AdaptiveThreadFactoryMonitor::_adaptive_thread_factory_monitor;

// void AdaptiveThreadFactoryMonitor::initialiseAdaptiveThreadFactoryMonitor() {
//     AdaptiveThreadFactoryMonitor adaptiveThreadFactoryMonitor;
//     _adaptive_thread_factory_monitor = adaptiveThreadFactoryMonitor;
// }

// void adaptive_thread_factory_initialisation() {
//     AdaptiveThreadFactoryMonitor::initialiseAdaptiveThreadFactoryMonitor();
// }

// void AdaptiveThreadFactoryMonitor::incrementNumberOfMonitoredThreads() {
//     _adaptive_thread_factory_monitor._numberMonitoredThreads++;
// }

// int AdaptiveThreadFactoryMonitor::getNumberOfMonitoredThreads() {
//     return _adaptive_thread_factory_monitor._numberMonitoredThreads.load();
// }
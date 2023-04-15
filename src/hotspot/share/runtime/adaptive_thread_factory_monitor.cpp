#include "runtime/adaptive_thread_factory_monitor.hpp"

AdaptiveThreadFactoryMonitor::AdaptiveThreadFactoryMonitor() {
    _numberMonitoredThreads.store(0);
}

AdaptiveThreadFactoryMonitor& AdaptiveThreadFactoryMonitor::operator=(const AdaptiveThreadFactoryMonitor& adaptiveThreadFactoryMonitor) {
    this->_numberMonitoredThreads.store(adaptiveThreadFactoryMonitor._numberMonitoredThreads.load());
    return *this;
}

AdaptiveThreadFactoryMonitor AdaptiveThreadFactoryMonitor::_adaptive_thread_factory_monitor;

void AdaptiveThreadFactoryMonitor::initialiseAdaptiveThreadFactoryMonitor() {
    AdaptiveThreadFactoryMonitor adaptiveThreadFactoryMonitor;
    _adaptive_thread_factory_monitor = adaptiveThreadFactoryMonitor;
}

void adaptive_thread_factory_initialisation() {
    AdaptiveThreadFactoryMonitor::initialiseAdaptiveThreadFactoryMonitor();
}
#include "runtime/adaptive_thread_factory_monitor.hpp"

AdaptiveThreadFactoryMonitor::AdaptiveThreadFactoryMonitor() {
    _numberMonitoredThreads.store(0);
}
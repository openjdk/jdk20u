#ifndef SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP
#define SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP

#include "memory/allocation.hpp"

#include <atomic>

class AdaptiveThreadFactoryMonitor : public ResourceObj {
    public:
        //std::atomic<int> _numberMonitoredThreads;
        int _adaptiveThreadFactoryId;
    public:
        AdaptiveThreadFactoryMonitor();
        AdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId);
        //AdaptiveThreadFactoryMonitor();
        //AdaptiveThreadFactoryMonitor& operator=(const AdaptiveThreadFactoryMonitor& adaptiveThreadFactoryMonitor);
        //static AdaptiveThreadFactoryMonitor _adaptive_thread_factory_monitor;
        //static void initialiseAdaptiveThreadFactoryMonitor();
        //static void incrementNumberOfMonitoredThreads();
        //static int getNumberOfMonitoredThreads();
};

#endif // SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP
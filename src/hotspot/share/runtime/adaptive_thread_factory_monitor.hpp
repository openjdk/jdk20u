#ifndef SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP
#define SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP

#include <atomic>

class AdaptiveThreadFactoryMonitor {
    private:
        std::atomic<int> _numberMonitoredThreads;
    public:
        AdaptiveThreadFactoryMonitor();
};

#endif // SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP
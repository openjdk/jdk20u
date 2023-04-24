#ifndef SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP
#define SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP

#include "memory/allocation.hpp"
#include "utilities/simple_concurrent_linked_list.hpp"

//#include <atomic>

class AdaptiveThreadFactoryMonitor : public CHeapObj<mtInternal> {
    private:
        //std::atomic<int> _numberMonitoredThreads;
        int _adaptiveThreadFactoryId;
        SimpleConcurrentLinkedList<long>* _javaLevelThreadIds;
    public:
        AdaptiveThreadFactoryMonitor();
        AdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId);
        int getFactoryId() const;
        const long& addJavaLevelThreadId(long javaLevelThreadId);
        //AdaptiveThreadFactoryMonitor();
        //AdaptiveThreadFactoryMonitor& operator=(const AdaptiveThreadFactoryMonitor& adaptiveThreadFactoryMonitor);
        //static AdaptiveThreadFactoryMonitor _adaptive_thread_factory_monitor;
        //static void initialiseAdaptiveThreadFactoryMonitor();
        //static void incrementNumberOfMonitoredThreads();
        //static int getNumberOfMonitoredThreads();
};

#endif // SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP
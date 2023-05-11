#ifndef SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP
#define SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP

#include "memory/allocation.hpp"
#include "utilities/simple_concurrent_linked_list.hpp"

class AdaptiveThreadFactoryMonitor : public CHeapObj<mtInternal> {
    private:
        int _adaptiveThreadFactoryId;
        long _threadCreationTimeWindowLength;
        SimpleConcurrentLinkedList<long>* _javaLevelThreadIds;
        SimpleConcurrentLinkedList<long>* _threadCreationTimes;
        SimpleConcurrentLinkedList<long>* _parkingTimes;
        long getCurrentTimeInMilliseconds();
    public:
        AdaptiveThreadFactoryMonitor();
        AdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId);
        void setParameters(long threadCreationTimeWindowLength);
        int getFactoryId() const;
        const long& addAndGetJavaLevelThreadId(long javaLevelThreadId);
        const long& getJavaLevelThreadId(long javaLevelThreadId);
        void removeJavaLevelThreadId(long javaLevelThreadId);
        void recordThreadCreation();
        void recordParking();
};

#endif // SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP
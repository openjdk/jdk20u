#ifndef SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP
#define SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP

#include "memory/allocation.hpp"
#include "utilities/simple_concurrent_linked_list.hpp"

class AdaptiveThreadFactoryMonitor : public CHeapObj<mtInternal> {
    private:
        int _adaptiveThreadFactoryId;
        long _parkingTimeWindowLength;
        long _threadCreationTimeWindowLength;
        long _numberParkingsThreshold;
        long _numberThreadCreationsThreshold;
        SimpleConcurrentLinkedList<long>* _javaLevelThreadIds;
        SimpleConcurrentLinkedList<long>* _threadCreationTimes;
        SimpleConcurrentLinkedList<long>* _parkingTimes;
        long getCurrentTimeInMilliseconds();
        long countNumberEventsInTimeWindow(SimpleConcurrentLinkedList<long>* events, long timeWindowLength);
    public:
        AdaptiveThreadFactoryMonitor();
        AdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId);
        void setParameters(
            long parkingTimeWindowLength, 
            long threadCreationTimeWindowLength, 
            long numberParkingsThreshold,
            long numberThreadCreationsThreshold
        );
        void close();
        int getFactoryId() const;
        const long& addAndGetJavaLevelThreadId(long javaLevelThreadId);
        const long& getJavaLevelThreadId(long javaLevelThreadId);
        void removeJavaLevelThreadId(long javaLevelThreadId);
        void recordThreadCreation();
        void recordParking();
        bool shallCreateVirtualThread();
        long countParkings();
        long countThreadCreations();
        long countNumberThreads();
};

#endif // SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITOR_HPP
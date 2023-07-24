#ifndef SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITORS_HPP
#define SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITORS_HPP

#include "runtime/adaptive_thread_factory_monitor.hpp"
#include "utilities/simple_concurrent_hash_map.hpp"
#include "memory/allocation.hpp"

#include <pthread.h>

class AdaptiveThreadFactoryMonitors : AllStatic {
    private:
        static SimpleConcurrentHashMap<int, AdaptiveThreadFactoryMonitor>* _adaptiveThreadFactoryMonitors;
        static AdaptiveThreadFactoryMonitor& getMonitor(int adaptiveThreadFactoryId);
    public:
        static const pthread_key_t _monitorAccessKey;
        static const pthread_key_t _javaLevelThreadIdAccessKey;
        static void initialiseAdaptiveThreadFactoryMonitors();
        static void addAdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId);
        static void removeAdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId);
        static void setMonitorParameters(
            int adaptiveThreadFactoryId, 
            long parkingTimeWindowLength, 
            long threadCreationTimeWindowLength
        );
        /*
        static void setMonitorParameters(
            int adaptiveThreadFactoryId, 
            long parkingTimeWindowLength, 
            long threadCreationTimeWindowLength,
            long numberParkingsThreshold,
            long numberThreadCreationsThreshold
        );
        static bool answerQuery(int adaptiveThreadFactoryId);
        */
        static void registerWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId);
        static void deregisterFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId);
        static void associateWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId);
        static void disassociateFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId);
        static void recordParking(int adaptiveThreadFactoryId);
        static long countParkings(int adaptiveThreadFactoryId);
        static long countThreadCreations(int adaptiveThreadFactoryId);
        static long countNumberThreads(int adaptiveThreadFactoryId);
};

#endif // SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITORS_HPP
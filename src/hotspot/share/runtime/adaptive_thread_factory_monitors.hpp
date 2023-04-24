#ifndef SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITORS_HPP
#define SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITORS_HPP

#include "runtime/adaptive_thread_factory_monitor.hpp"
#include "utilities/simple_concurrent_hash_map.hpp"
#include "memory/allocation.hpp"

#include <pthread.h>

class AdaptiveThreadFactoryMonitors : AllStatic {
    private:
        static SimpleConcurrentHashMap<int, AdaptiveThreadFactoryMonitor>* _adaptiveThreadFactoryMonitors;
    public:
        static const pthread_key_t _monitorAccessKey;
        static const pthread_key_t _javaLevelThreadIdAccessKey;
        static void initialiseAdaptiveThreadFactoryMonitors();
        static void addAdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId);
        static bool answerQuery(int adaptiveThreadFactoryId);
        static void associateWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId);
        static void disassociateFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId);
};

#endif // SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITORS_HPP
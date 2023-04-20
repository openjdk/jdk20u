#ifndef SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITORS_HPP
#define SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITORS_HPP

#include "runtime/adaptive_thread_factory_monitor.hpp"
#include "utilities/simple_concurrent_hash_map.hpp"
#include "memory/allocation.hpp"

#include <pthread.h>

class AdaptiveThreadFactoryMonitors : AllStatic {
    private:
        // NEXT: initialise map during global initialisation by providing initialisation function
        static SimpleConcurrentHashMap<int, AdaptiveThreadFactoryMonitor>* _adaptiveThreadFactoryMonitors;
    public:
        static pthread_key_t _monitorAccessKey;
        static void initialiseAdaptiveThreadFactoryMonitors();
        static void addAdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId);
        static bool answerQuery(int adaptiveThreadFactoryId);
        static void associateWithMonitor(int adaptiveThreadFactoryId);
};

#endif // SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_MONITORS_HPP
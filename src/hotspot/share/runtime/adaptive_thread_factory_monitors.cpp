#include "runtime/adaptive_thread_factory_monitors.hpp"

#include <stdlib.h> 

SimpleConcurrentHashMap<int, AdaptiveThreadFactoryMonitor>* AdaptiveThreadFactoryMonitors::_adaptiveThreadFactoryMonitors;

pthread_key_t AdaptiveThreadFactoryMonitors::_monitorAccessKey = []{
  pthread_key_t key;
  pthread_key_create(&key, NULL);
  return key;
}();

void AdaptiveThreadFactoryMonitors::initialiseAdaptiveThreadFactoryMonitors() {
    int numberBuckets = 32;
    int(*mapKeyToInteger)(int) = [](int key){ return key; };
    _adaptiveThreadFactoryMonitors = new SimpleConcurrentHashMap<int, AdaptiveThreadFactoryMonitor>(numberBuckets, mapKeyToInteger);
}

void AdaptiveThreadFactoryMonitors::addAdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId) {

}

bool AdaptiveThreadFactoryMonitors::answerQuery(int adaptiveThreadFactoryId) {
    return true;
}

void AdaptiveThreadFactoryMonitors::associateWithMonitor(int adaptiveThreadFactoryId) {
    //pthread_setspecific(_monitorAccessKey, &);
}

void adaptive_thread_factory_monitors_initialisation() {
    AdaptiveThreadFactoryMonitors::initialiseAdaptiveThreadFactoryMonitors();
}
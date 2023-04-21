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
    AdaptiveThreadFactoryMonitor* defaultValue = new AdaptiveThreadFactoryMonitor(0);
    _adaptiveThreadFactoryMonitors = new SimpleConcurrentHashMap<int, AdaptiveThreadFactoryMonitor>(numberBuckets, mapKeyToInteger, *defaultValue);
}

void AdaptiveThreadFactoryMonitors::addAdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId) {
    AdaptiveThreadFactoryMonitor* newAdaptiveThreadFactoryMonitor = new AdaptiveThreadFactoryMonitor(adaptiveThreadFactoryId);
    _adaptiveThreadFactoryMonitors->put(adaptiveThreadFactoryId, *newAdaptiveThreadFactoryMonitor);
}

bool AdaptiveThreadFactoryMonitors::answerQuery(int adaptiveThreadFactoryId) {
    return false;
}

void AdaptiveThreadFactoryMonitors::associateWithMonitor(int adaptiveThreadFactoryId) {
    const AdaptiveThreadFactoryMonitor& associatedMonitor = _adaptiveThreadFactoryMonitors->get(adaptiveThreadFactoryId);
    // TO DO: check whether default value is returned
    pthread_setspecific(_monitorAccessKey, &associatedMonitor);
}

void adaptive_thread_factory_monitors_initialisation() {
    AdaptiveThreadFactoryMonitors::initialiseAdaptiveThreadFactoryMonitors();
}
#include "runtime/adaptive_thread_factory_utility.hpp"
#include "runtime/adaptive_thread_factory_monitors.hpp"

#include <stdlib.h> 

SimpleConcurrentHashMap<int, AdaptiveThreadFactoryMonitor>* AdaptiveThreadFactoryMonitors::_adaptiveThreadFactoryMonitors;

const pthread_key_t AdaptiveThreadFactoryMonitors::_monitorAccessKey = []{
  pthread_key_t key;
  pthread_key_create(&key, NULL);
  return key;
}();

const pthread_key_t AdaptiveThreadFactoryMonitors::_javaLevelThreadIdAccessKey = []{
  pthread_key_t key;
  pthread_key_create(&key, NULL);
  return key;
}();

void AdaptiveThreadFactoryMonitors::initialiseAdaptiveThreadFactoryMonitors() {
    // create map
    int numberBuckets = 32;
    int(*mapKeyToInteger)(int) = [](int key){ return key; };
    _adaptiveThreadFactoryMonitors = new SimpleConcurrentHashMap<int, AdaptiveThreadFactoryMonitor>(numberBuckets, mapKeyToInteger);
}

void AdaptiveThreadFactoryMonitors::addAdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId) {
    AdaptiveThreadFactoryMonitor* newAdaptiveThreadFactoryMonitor = new AdaptiveThreadFactoryMonitor(adaptiveThreadFactoryId);
    _adaptiveThreadFactoryMonitors->put(adaptiveThreadFactoryId, *newAdaptiveThreadFactoryMonitor);
}

void AdaptiveThreadFactoryMonitors::removeAdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId) {
    AdaptiveThreadFactoryMonitor& monitor = getMonitor(adaptiveThreadFactoryId);
    monitor.close();
    _adaptiveThreadFactoryMonitors->remove(adaptiveThreadFactoryId);
}

AdaptiveThreadFactoryMonitor& AdaptiveThreadFactoryMonitors::getMonitor(int adaptiveThreadFactoryId) {
    AdaptiveThreadFactoryMonitor& monitor = _adaptiveThreadFactoryMonitors->get(adaptiveThreadFactoryId);
    return monitor;
}

void AdaptiveThreadFactoryMonitors::setMonitorParameters(
    int adaptiveThreadFactoryId, 
    long parkingTimeWindowLength, 
    long threadCreationTimeWindowLength
) {
    AdaptiveThreadFactoryMonitor& associatedMonitor = getMonitor(adaptiveThreadFactoryId);
    associatedMonitor.setParameters(
        parkingTimeWindowLength, 
        threadCreationTimeWindowLength
    );
}

/*
void AdaptiveThreadFactoryMonitors::setMonitorParameters(
    int adaptiveThreadFactoryId, 
    long parkingTimeWindowLength, 
    long threadCreationTimeWindowLength, 
    long numberParkingsThreshold,
    long numberThreadCreationsThreshold
) {
    AdaptiveThreadFactoryMonitor& associatedMonitor = getMonitor(adaptiveThreadFactoryId);
    associatedMonitor.setParameters(
        parkingTimeWindowLength, 
        threadCreationTimeWindowLength,
        numberParkingsThreshold,
        numberThreadCreationsThreshold
    );
}
*/

/*
bool AdaptiveThreadFactoryMonitors::answerQuery(int adaptiveThreadFactoryId) {
    AdaptiveThreadFactoryMonitor& monitor = getMonitor(adaptiveThreadFactoryId);
    bool decision = monitor.shallCreateVirtualThread();
    return decision;
}
*/

void AdaptiveThreadFactoryMonitors::registerWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId) {
    AdaptiveThreadFactoryMonitor& monitor = getMonitor(adaptiveThreadFactoryId);
    pthread_setspecific(_monitorAccessKey, &monitor);
    const long& registeredJavaLevelThreadId = monitor.addAndGetJavaLevelThreadId(javaLevelThreadId);
    pthread_setspecific(_javaLevelThreadIdAccessKey, &registeredJavaLevelThreadId);
}

void AdaptiveThreadFactoryMonitors::deregisterFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId) {
    AdaptiveThreadFactoryMonitor& monitor = getMonitor(adaptiveThreadFactoryId);
    monitor.removeJavaLevelThreadId(javaLevelThreadId);
    pthread_setspecific(_monitorAccessKey, nullptr);
    pthread_setspecific(_javaLevelThreadIdAccessKey, nullptr);
}

void AdaptiveThreadFactoryMonitors::associateWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId) {
    AdaptiveThreadFactoryMonitor& monitor = getMonitor(adaptiveThreadFactoryId);
    pthread_setspecific(_monitorAccessKey, &monitor);
    const long& registeredJavaLevelThreadId = monitor.getJavaLevelThreadId(javaLevelThreadId);
    pthread_setspecific(_javaLevelThreadIdAccessKey, &registeredJavaLevelThreadId);
}

void AdaptiveThreadFactoryMonitors::disassociateFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId) {
    pthread_setspecific(_monitorAccessKey, nullptr);
    pthread_setspecific(_javaLevelThreadIdAccessKey, nullptr);
}

void AdaptiveThreadFactoryMonitors::recordParking(int adaptiveThreadFactoryId) {
    AdaptiveThreadFactoryMonitor& monitor = getMonitor(adaptiveThreadFactoryId);
    monitor.recordParking();
}

void AdaptiveThreadFactoryMonitors::recordThreadCreation(int adaptiveThreadFactoryId) {
    AdaptiveThreadFactoryMonitor& monitor = getMonitor(adaptiveThreadFactoryId);
    monitor.recordThreadCreation();
}

long AdaptiveThreadFactoryMonitors::countParkings(int adaptiveThreadFactoryId) {
    AdaptiveThreadFactoryMonitor& monitor = getMonitor(adaptiveThreadFactoryId);
    return monitor.countParkings();
}

long AdaptiveThreadFactoryMonitors::countThreadCreations(int adaptiveThreadFactoryId) {
    AdaptiveThreadFactoryMonitor& monitor = getMonitor(adaptiveThreadFactoryId);
    return monitor.countThreadCreations();
}

long AdaptiveThreadFactoryMonitors::countNumberThreads(int adaptiveThreadFactoryId) {
    AdaptiveThreadFactoryMonitor& monitor = getMonitor(adaptiveThreadFactoryId);
    return monitor.countNumberThreads();
}

void adaptive_thread_factory_monitors_initialisation() {
    AdaptiveThreadFactoryMonitors::initialiseAdaptiveThreadFactoryMonitors();
}
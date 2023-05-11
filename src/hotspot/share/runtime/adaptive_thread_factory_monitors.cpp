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
    AdaptiveThreadFactoryMonitor* defaultValue = new AdaptiveThreadFactoryMonitor(-1);
    _adaptiveThreadFactoryMonitors = new SimpleConcurrentHashMap<int, AdaptiveThreadFactoryMonitor>(numberBuckets, mapKeyToInteger, *defaultValue);
}

void AdaptiveThreadFactoryMonitors::addAdaptiveThreadFactoryMonitor(int adaptiveThreadFactoryId) {
    AdaptiveThreadFactoryMonitor* newAdaptiveThreadFactoryMonitor = new AdaptiveThreadFactoryMonitor(adaptiveThreadFactoryId);
    _adaptiveThreadFactoryMonitors->put(adaptiveThreadFactoryId, *newAdaptiveThreadFactoryMonitor);
}

void AdaptiveThreadFactoryMonitors::setMonitorParameters(int adaptiveThreadFactoryId, long threadCreationTimeWindowLength) {
    AdaptiveThreadFactoryMonitor& associatedMonitor = _adaptiveThreadFactoryMonitors->get(adaptiveThreadFactoryId);
    AdaptiveThreadFactoryUtility::checkRequirement(
       associatedMonitor.getFactoryId() == adaptiveThreadFactoryId,
       (char*)"AdaptiveThreadFactoryMonitors::associateWithMonitor: The provided ID does not exist."
    );
    associatedMonitor.setParameters(threadCreationTimeWindowLength);
}

bool AdaptiveThreadFactoryMonitors::answerQuery(int adaptiveThreadFactoryId) {
    // TO DO: provide implementation
    return true;
}

void AdaptiveThreadFactoryMonitors::registerWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId) {
    AdaptiveThreadFactoryMonitor& associatedMonitor = _adaptiveThreadFactoryMonitors->get(adaptiveThreadFactoryId);
    associatedMonitor.recordThreadCreation();
    AdaptiveThreadFactoryUtility::checkRequirement(
       associatedMonitor.getFactoryId() == adaptiveThreadFactoryId,
       (char*)"AdaptiveThreadFactoryMonitors::associateWithMonitor: The provided ID does not exist."
    );
    pthread_setspecific(_monitorAccessKey, &associatedMonitor);
    const long& registeredJavaLevelThreadId = associatedMonitor.addAndGetJavaLevelThreadId(javaLevelThreadId);
    pthread_setspecific(_javaLevelThreadIdAccessKey, &registeredJavaLevelThreadId);
}

void AdaptiveThreadFactoryMonitors::deregisterFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId) {
    AdaptiveThreadFactoryMonitor& associatedMonitor = _adaptiveThreadFactoryMonitors->get(adaptiveThreadFactoryId);
    AdaptiveThreadFactoryUtility::checkRequirement(
       associatedMonitor.getFactoryId() == adaptiveThreadFactoryId,
       (char*)"AdaptiveThreadFactoryMonitors::associateWithMonitor: The provided ID does not exist."
    );
    associatedMonitor.removeJavaLevelThreadId(javaLevelThreadId);
    pthread_setspecific(_monitorAccessKey, nullptr);
    pthread_setspecific(_javaLevelThreadIdAccessKey, nullptr);
}

void AdaptiveThreadFactoryMonitors::associateWithMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId) {
    AdaptiveThreadFactoryMonitor& associatedMonitor = _adaptiveThreadFactoryMonitors->get(adaptiveThreadFactoryId);
    AdaptiveThreadFactoryUtility::checkRequirement(
       associatedMonitor.getFactoryId() == adaptiveThreadFactoryId,
       (char*)"AdaptiveThreadFactoryMonitors::associateWithMonitor: The provided ID does not exist."
    );
    pthread_setspecific(_monitorAccessKey, &associatedMonitor);
    const long& registeredJavaLevelThreadId = associatedMonitor.getJavaLevelThreadId(javaLevelThreadId);
    pthread_setspecific(_javaLevelThreadIdAccessKey, &registeredJavaLevelThreadId);
}

void AdaptiveThreadFactoryMonitors::disassociateFromMonitor(int adaptiveThreadFactoryId, long javaLevelThreadId) {
    AdaptiveThreadFactoryMonitor& associatedMonitor = _adaptiveThreadFactoryMonitors->get(adaptiveThreadFactoryId);
    AdaptiveThreadFactoryUtility::checkRequirement(
       associatedMonitor.getFactoryId() == adaptiveThreadFactoryId,
       (char*)"AdaptiveThreadFactoryMonitors::associateWithMonitor: The provided ID does not exist."
    );
    pthread_setspecific(_monitorAccessKey, nullptr);
    pthread_setspecific(_javaLevelThreadIdAccessKey, nullptr);
}

void adaptive_thread_factory_monitors_initialisation() {
    AdaptiveThreadFactoryMonitors::initialiseAdaptiveThreadFactoryMonitors();
}
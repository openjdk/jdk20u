#include "runtime/adaptive_thread_factory_utility.hpp"

void AdaptiveThreadFactoryUtility::checkRequirement(bool requirement, char* message) {
    if(!requirement) {
        fprintf(stderr, "%s\n", message);
        exit(1);
    }
}
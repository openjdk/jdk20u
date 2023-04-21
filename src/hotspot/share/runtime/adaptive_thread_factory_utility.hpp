#ifndef SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_UTILITY_HPP
#define SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_UTILITY_HPP

#include "memory/allocation.hpp"

class AdaptiveThreadFactoryUtility : AllStatic {
    public:
        static void checkRequirement(bool requirement, char* message);
};

#endif // SHARE_RUNTIME_ADAPTIVE_THREAD_FACTORY_UTILITY_HPP
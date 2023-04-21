#ifndef SHARE_UTILITIES_SIMPLE_CONCURRENT_HASH_MAP_ENTRY_HPP
#define SHARE_UTILITIES_SIMPLE_CONCURRENT_HASH_MAP_ENTRY_HPP

#include "memory/allocation.hpp"

template <typename K, typename V>
class SimpleConcurrentHashMapEntry : public CHeapObj<mtInternal> {
    public:
        K key;
        V value;
        SimpleConcurrentHashMapEntry* next;
        SimpleConcurrentHashMapEntry() {}
};

#endif // SHARE_UTILITIES_SIMPLE_CONCURRENT_HASH_MAP_ENTRY_HPP
#ifndef SHARE_UTILITIES_SIMPLE_CONCURRENT_HASH_MAP_HPP
#define SHARE_UTILITIES_SIMPLE_CONCURRENT_HASH_MAP_HPP

#include "memory/allocation.hpp"
#include "utilities/simple_concurrent_hash_map_entry.hpp"

#include <stdlib.h> 
#include <pthread.h>

#include <sys/syscall.h>
#include <stdio.h>
#include <unistd.h>

template <typename K, typename V>
class SimpleConcurrentHashMap : public CHeapObj<mtInternal> {

    private:

        pthread_mutex_t _lock;
        int _numberBuckets;
        V _defaultValue;
        SimpleConcurrentHashMapEntry<K, V>** _buckets;
        int(*_mapKeyToInteger)(K);
        int hash(const K& key) {
            return _mapKeyToInteger(key) % _numberBuckets;
        }
        
    public:

        SimpleConcurrentHashMap(int numberBuckets, int(*mapKeyToInteger)(K), const V& defaultValue) {
            //fprintf(stderr, "constructor: thread: %lu\n", (unsigned long)syscall(__NR_gettid));
            if(pthread_mutex_init(&_lock, NULL) != 0) {                                                                                           
                exit(1);                                                                    
            }  
            //fprintf(stderr, "constructor: %s\n", "before acquisition");
            pthread_mutex_lock(&_lock);   
            _numberBuckets = numberBuckets;
            _mapKeyToInteger = mapKeyToInteger;
            _defaultValue = defaultValue;
            _buckets = (SimpleConcurrentHashMapEntry<K, V>**)malloc(_numberBuckets * sizeof(SimpleConcurrentHashMapEntry<K, V>*));
            for(int i = 0; i < _numberBuckets; i += 1) {
                _buckets[i] = nullptr;
            }
            //fprintf(stderr, "constructor: %s\n", "before release");
            pthread_mutex_unlock(&_lock);
            //fprintf(stderr, "constructor: %s\n", "after release");
        }

        ~SimpleConcurrentHashMap() {
            //fprintf(stderr, "destructor: thread: %lu\n", (unsigned long)syscall(__NR_gettid));
            //fprintf(stderr, "destructor: %s\n", "before acquisition");
            pthread_mutex_lock(&_lock);
            for(int i = 0; i < _numberBuckets; i += 1) {
                SimpleConcurrentHashMapEntry<K, V>* current = _buckets[i];
                while (current != nullptr) {
                    SimpleConcurrentHashMapEntry<K, V>* next = current->next;
                    delete current;
                    current = next;
                }
                _buckets[i] = nullptr;
            }
            delete _buckets;
            //fprintf(stderr, "destructor: %s\n", "before release");
            pthread_mutex_unlock(&_lock);
            //fprintf(stderr, "destructor: %s\n", "after release");
            pthread_mutex_destroy(&_lock);
        }

        void put(const K& key, const V& value) {
            //fprintf(stderr, "put: thread: %lu\n", (unsigned long)syscall(__NR_gettid));
            //fprintf(stderr, "put: %s\n", "before acquisition");
            pthread_mutex_lock(&_lock);
            int bucketIndex = hash(key);
            SimpleConcurrentHashMapEntry<K, V>* current = _buckets[bucketIndex];
            while(current != nullptr) {
                if(current->key == key) {
                    current->value = value;
                    //fprintf(stderr, "put: %s\n", "before release");
                    pthread_mutex_unlock(&_lock);
                    //fprintf(stderr, "put: %s\n", "after release");
                    return;
                }
                current = current->next;
            }
            SimpleConcurrentHashMapEntry<K, V>* newEntry = new SimpleConcurrentHashMapEntry<K, V>();
            newEntry->key = key;
            newEntry->value = value;
            newEntry->next = _buckets[bucketIndex];
            _buckets[bucketIndex] = newEntry;
            //fprintf(stderr, "put: %s\n", "before release");
            pthread_mutex_unlock(&_lock);
            //fprintf(stderr, "put: %s\n", "after release");
        }

        V& get(const K& key) {
            //fprintf(stderr, "get: thread: %lu\n", (unsigned long)syscall(__NR_gettid));
            //fprintf(stderr, "get: %s\n", "before acquisition");
            pthread_mutex_lock(&_lock);
            int bucketIndex = hash(key);
            SimpleConcurrentHashMapEntry<K, V>* current = _buckets[bucketIndex];
            while(current != nullptr) {
                if(current->key == key) {
                    V& result = current->value;
                    //fprintf(stderr, "get: %s\n", "before release");
                    pthread_mutex_unlock(&_lock);
                    //fprintf(stderr, "get: %s\n", "after release");
                    return result;
                }
                current = current->next;
            }
            //fprintf(stderr, "get: %s\n", "before release");
            pthread_mutex_unlock(&_lock);
            //fprintf(stderr, "get: %s\n", "after release");
            return _defaultValue;
        }
};

#endif // SHARE_UTILITIES_SIMPLE_CONCURRENT_HASH_MAP_HPP
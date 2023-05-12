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
        SimpleConcurrentHashMapEntry<K, V>** _buckets;
        int(*_mapKeyToInteger)(K);
        int hash(const K& key) {
            return _mapKeyToInteger(key) % _numberBuckets;
        }
        
    public:

        SimpleConcurrentHashMap(int numberBuckets, int(*mapKeyToInteger)(K)) {
            if(pthread_mutex_init(&_lock, NULL) != 0) {                                                                                           
                exit(1);                                                                    
            }  
            pthread_mutex_lock(&_lock);   
            _numberBuckets = numberBuckets;
            _mapKeyToInteger = mapKeyToInteger;
            _buckets = (SimpleConcurrentHashMapEntry<K, V>**)malloc(_numberBuckets * sizeof(SimpleConcurrentHashMapEntry<K, V>*));
            for(int i = 0; i < _numberBuckets; i += 1) {
                _buckets[i] = nullptr;
            }
            pthread_mutex_unlock(&_lock);
        }

        ~SimpleConcurrentHashMap() {
            pthread_mutex_lock(&_lock);
            for(int i = 0; i < _numberBuckets; i += 1) {
                SimpleConcurrentHashMapEntry<K, V>* current = _buckets[i];
                while (current != nullptr) {
                    SimpleConcurrentHashMapEntry<K, V>* next = current->_next;
                    delete current;
                    current = next;
                }
                _buckets[i] = nullptr;
            }
            delete _buckets;
            pthread_mutex_unlock(&_lock);
            pthread_mutex_destroy(&_lock);
        }

        void put(const K& key, const V& value) {
            pthread_mutex_lock(&_lock);
            int bucketIndex = hash(key);
            SimpleConcurrentHashMapEntry<K, V>* current = _buckets[bucketIndex];
            while(current != nullptr) {
                if(current->_key == key) {
                    current->_value = value;
                    pthread_mutex_unlock(&_lock);
                    return;
                }
                current = current->_next;
            }
            SimpleConcurrentHashMapEntry<K, V>* newEntry = new SimpleConcurrentHashMapEntry<K, V>();
            newEntry->_key = key;
            newEntry->_value = value;
            newEntry->_next = _buckets[bucketIndex];
            _buckets[bucketIndex] = newEntry;
            pthread_mutex_unlock(&_lock);
        }

        V& get(const K& key) {
            pthread_mutex_lock(&_lock);
            int bucketIndex = hash(key);
            SimpleConcurrentHashMapEntry<K, V>* current = _buckets[bucketIndex];
            while(current != nullptr) {
                if(current->_key == key) {
                    V& result = current->_value;
                    pthread_mutex_unlock(&_lock);
                    return result;
                }
                current = current->_next;
            }
            pthread_mutex_unlock(&_lock);
            fprintf(stderr, "%s\n", "SimpleConcurrentHashMap.get: The requested element is not contained in the map.");
            exit(1);
        }

        void remove(const K& key) {
            pthread_mutex_lock(&_lock);
            int bucketIndex = hash(key);
            SimpleConcurrentHashMapEntry<K, V>* current = _buckets[bucketIndex];
            SimpleConcurrentHashMapEntry<K, V>* predecessor = nullptr;
            while(current != nullptr) {
                if(current->_key == key) {
                    if(predecessor == nullptr) {
                        _buckets[bucketIndex] = current->_next;
                    } else {
                        predecessor->_next = current->_next;
                    }   
                    delete current;
                    pthread_mutex_unlock(&_lock);
                    return;
                }
                current = current->_next;
            }
            pthread_mutex_unlock(&_lock);
        }

};

#endif // SHARE_UTILITIES_SIMPLE_CONCURRENT_HASH_MAP_HPP
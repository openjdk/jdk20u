#ifndef SHARE_UTILITIES_SIMPLE_CONCURRENT_HASH_MAP_HPP
#define SHARE_UTILITIES_SIMPLE_CONCURRENT_HASH_MAP_HPP

#include "memory/allocation.hpp"
#include "utilities/simple_concurrent_hash_map_entry.hpp"

#include <stdlib.h> 
#include <pthread.h>

template <typename K, typename V>
class SimpleConcurrentHashMap : public ResourceObj {

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
            _numberBuckets = numberBuckets;
            _mapKeyToInteger = mapKeyToInteger;
            _defaultValue = _defaultValue;
            _buckets = (SimpleConcurrentHashMapEntry<K, V>**)malloc(_numberBuckets * sizeof(SimpleConcurrentHashMapEntry<K, V>*));
            for(int i = 0; i < _numberBuckets; i += 1) {
                _buckets[i] = nullptr;
            }
            pthread_mutex_init(&_lock, NULL);
        }

        ~SimpleConcurrentHashMap() {
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
        }

       /*
        SimpleConcurrentHashMap& operator=(const SimpleConcurrentHashMap& other) {
            if(this != &other) {
                _numberBuckets = other._numberBuckets;
                _mapKeyToInteger = other._mapKeyToInteger;
                _buckets = (Entry*)malloc(_numberBuckets * sizeof(Entry));
                for (int i = 0; i < _numberBuckets; i += 1) {
                    Entry* current = other._buckets[i];
                    Entry* previous = nullptr;
                    while(current != nullptr) {
                        Entry* newEntry = (Entry*)malloc(sizeof(Entry));
                        newEntry->key = current->key;
                        newEntry->value = current->value;
                        newEntry->next = nullptr;
                        if(previous == nullptr) {
                            _buckets[i] = newEntry;
                        } else {
                            previous->next = newEntry;
                        }
                        previous = newEntry;
                        current = current->next;
                    }
                }
            }
            return *this;
        }
        */

        void put(const K& key, const V& value) {
            pthread_mutex_lock(&_lock);
            int bucketIndex = hash(key);
            SimpleConcurrentHashMapEntry<K, V>* current = _buckets[bucketIndex];
            while(current != nullptr) {
                if(current->key == key) {
                    current->value = value;
                    return;
                }
                current = current->next;
            }
            SimpleConcurrentHashMapEntry<K, V>* newEntry = new SimpleConcurrentHashMapEntry<K, V>();
            newEntry->key = key;
            newEntry->value = value;
            newEntry->next = _buckets[bucketIndex];
            _buckets[bucketIndex] = newEntry;
            pthread_mutex_unlock(&_lock);
        }

        const V& get(const K& key) {
            pthread_mutex_lock(&_lock);
            int bucketIndex = hash(key);
            SimpleConcurrentHashMapEntry<K, V>* current = _buckets[bucketIndex];
            while(current != nullptr) {
                if(current->key == key) {
                    const V& result = current->value;
                    pthread_mutex_unlock(&_lock);
                    return result;
                }
                current = current->next;
            }
            return _defaultValue;
        }
};

#endif // SHARE_UTILITIES_SIMPLE_CONCURRENT_HASH_MAP_HPP
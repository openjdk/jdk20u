#ifndef SHARE_UTILITIES_SIMPLE_CONCURRENT_LINKED_LIST_HPP
#define SHARE_UTILITIES_SIMPLE_CONCURRENT_LINKED_LIST_HPP

#include "memory/allocation.hpp"
#include "utilities/simple_concurrent_linked_list_node.hpp"

#include <stdlib.h> 
#include <pthread.h>

template <typename V>
class SimpleConcurrentLinkedList : public CHeapObj<mtInternal> {
public:

    pthread_mutex_t _lock;
    SimpleConcurrentLinkedListNode<V>* _head;
    V _defaultValue;

    SimpleConcurrentLinkedList(const V& defaultValue) {
        if(pthread_mutex_init(&_lock, NULL) != 0) {                                                                                           
            exit(1);                                                                    
        }
        pthread_mutex_lock(&_lock);  
        _head = nullptr;
        _defaultValue = defaultValue;
        pthread_mutex_unlock(&_lock);
    }

    ~SimpleConcurrentLinkedList() {
        pthread_mutex_lock(&_lock);
        SimpleConcurrentLinkedListNode<V>* current = _head;
        while(current != nullptr) {
            SimpleConcurrentLinkedListNode<V>* temporary = current;
            current = current->_next;
            delete temporary;
        }
        pthread_mutex_unlock(&_lock);
    }

    void append(const V& value) {
        pthread_mutex_lock(&_lock);
        SimpleConcurrentLinkedListNode<V>* newNode = new SimpleConcurrentLinkedListNode<V>();
        newNode->_value = value;
        if(_head == nullptr) {
            _head = newNode;
        }
        else {
            SimpleConcurrentLinkedListNode<V>* current = _head;
            while(current->_next != nullptr) {
                current = current->_next;
            }
            current->_next = newNode;
        }
        pthread_mutex_unlock(&_lock);
    }

   void remove(const V& value) {
        pthread_mutex_lock(&_lock);
        if(_head == nullptr) {
            pthread_mutex_unlock(&_lock);
            return; 
        }
        if(_head->_value == value) {
            SimpleConcurrentLinkedListNode<V>* temporary = _head;
            _head = _head->_next;
            delete temporary;
            pthread_mutex_unlock(&_lock);
            return;
        }
        SimpleConcurrentLinkedListNode<V>* current = _head;
        while(current->_next != nullptr && current->_next->_value != value) {
            current = current->_next;
        }
        if(current->_next != nullptr) {
            SimpleConcurrentLinkedListNode<V>* temporary = current->_next;
            current->_next = current->_next->_next;
            delete temporary;
        }
        pthread_mutex_unlock(&_lock);
    }  

    const V& get(const V& value) {
        pthread_mutex_lock(&_lock);
        SimpleConcurrentLinkedListNode<V>* current = _head;
        while(current != nullptr) {
            if(current->_value == value) {
                pthread_mutex_unlock(&_lock);
                return current->_value;
            }
            current = current->_next;
        }
        pthread_mutex_unlock(&_lock);
        return _defaultValue;
    } 

    long countRecentValuesAndRemoveOldValues(const V& lowerBound) {
        pthread_mutex_lock(&_lock);
        long counter = 0;
        SimpleConcurrentLinkedListNode<V>* current = _head;
        if(current == nullptr) {
            pthread_mutex_unlock(&_lock);
            return 0;
        } 
        while(current != nullptr) {
            if(current->_value < lowerBound) {
                SimpleConcurrentLinkedListNode<V>* temporary = current;
                delete temporary;
            } else {
                counter += 1;
            }
            current = current->_next;
        }
        pthread_mutex_unlock(&_lock);
        return counter;
    }

    /*
    bool V& exists(const V& value) {
        SimpleConcurrentLinkedListNode<V>* current = _head;
        while(current != nullptr) {
            if (current->_value == value) {
                return true;
            }
            current = current->_next;
        }
        return false;
    }
    */
};

#endif // SHARE_UTILITIES_SIMPLE_CONCURRENT_LINKED_LIST_HPP
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

    SimpleConcurrentLinkedList() {
        if(pthread_mutex_init(&_lock, NULL) != 0) {                                                                                           
            exit(1);                                                                    
        }
        pthread_mutex_lock(&_lock);  
        _head = nullptr;
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
        newNode->_next = nullptr;
        if(_head == nullptr) {
            _head = newNode;
        } else {
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
            pthread_mutex_unlock(&_lock);
            return;
        }
        pthread_mutex_unlock(&_lock);
        fprintf(stderr, "%s\n", "SimpleConcurrentLinkedList.get: The specified element is not contained in the list.");
        exit(1);
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
        fprintf(stderr, "%s\n", "SimpleConcurrentLinkedList.get: The requested element is not contained in the list.");
        exit(1);
    } 

    long countRecentValuesAndRemoveOldValues(const V& lowerBound) {
        pthread_mutex_lock(&_lock);
        long counter = 0;
        SimpleConcurrentLinkedListNode<V>* current = _head;
        if(current == nullptr) {
            pthread_mutex_unlock(&_lock);
            return counter;
        } 
        SimpleConcurrentLinkedListNode<V>* initial = new SimpleConcurrentLinkedListNode<V>();
        initial->_next = _head;
        SimpleConcurrentLinkedListNode<V>* predecessor = initial;
        while(current != nullptr) {
            if(current->_value < lowerBound) {
                predecessor->_next = current->_next;
                delete current;
                current = predecessor->_next;
            } else {
                predecessor = current;
                current = current->_next;
                counter += 1;
            }
        }
        _head = initial->_next;
        delete initial;
        pthread_mutex_unlock(&_lock);
        return counter;
    }

    void clear() {
        pthread_mutex_lock(&_lock);
        SimpleConcurrentLinkedListNode<V>* current = _head;
        while(current != nullptr) {
            SimpleConcurrentLinkedListNode<V>* temporary = current;
            current = current->_next;
            delete temporary;
        }
        _head = nullptr;
        pthread_mutex_unlock(&_lock);
    }

    long size() {
        pthread_mutex_lock(&_lock);
        long numberElements = 0;
        SimpleConcurrentLinkedListNode<V>* current = _head;
        while(current != nullptr) {
            numberElements++;
            current = current->_next;
        }
        pthread_mutex_unlock(&_lock);
        return numberElements;
    }
    
};

#endif // SHARE_UTILITIES_SIMPLE_CONCURRENT_LINKED_LIST_HPP
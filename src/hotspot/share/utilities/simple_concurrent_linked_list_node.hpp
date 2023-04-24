#ifndef SHARE_UTILITIES_SIMPLE_CONCURRENT_LINKED_LIST_NODE_HPP
#define SHARE_UTILITIES_SIMPLE_CONCURRENT_LINKED_LIST_NODE_HPP

#include "memory/allocation.hpp"

template <typename V>
class SimpleConcurrentLinkedListNode : public CHeapObj<mtInternal> {
    public:
        V _value;
        SimpleConcurrentLinkedListNode* _next;
        SimpleConcurrentLinkedListNode() {}
};

#endif // SHARE_UTILITIES_SIMPLE_CONCURRENT_LINKED_LIST_NODE_HPP
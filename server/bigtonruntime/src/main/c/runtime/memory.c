
#include <bigton/values.h>
#include <stdlib.h>
#include <stddef.h>
#include <stdbool.h>
    
void *bigtonAllocBuff(bigton_buff_owner_t *o, size_t numBytes) {
    bigton_buff_t *buff = malloc(sizeof(bigton_buff_t) + numBytes);
    buff->owner = o;
    buff->sizeBytes = numBytes;
    buff->prev = o->last;
    buff->next = NULL;
    if (o->last == NULL) {
        o->first = buff;
        o->last = buff;
    } else {
        o->last->next = buff;
        o->last = buff;
    }
    o->totalSizeBytes += numBytes;
    return (void *) buff->data;
}

#define GET_HEADER(b) \
    (bigton_buff_t *) (((const uint8_t *) (b)) - offsetof(bigton_buff_t, data))

#define POINT_PREV_NODE_TO(o, prev, newRef) \
    if ((prev) == NULL) { (o)->first = (newRef); } \
    else { (prev)->next = (newRef); }

#define POINT_NEXT_NODE_TO(o, next, newRef) \
    if ((next) == NULL) { (o)->last = (newRef); } \
    else { (next)->prev = (newRef); }
    
void bigtonFreeBuff(const void *buffData) {
    bigton_buff_t *buff = GET_HEADER(buffData);
    bigton_buff_owner_t *o = buff->owner;
    bigton_buff_t *prev = buff->prev;
    bigton_buff_t *next = buff->next;
    POINT_PREV_NODE_TO(o, prev, next); // prev.next = next
    POINT_NEXT_NODE_TO(o, next, prev); // next.prev = prev
    o->totalSizeBytes -= buff->sizeBytes;
    free((void *) buff);
}

void *bigtonReallocBuff(const void *buffData, size_t numBytes) {
    bigton_buff_t *oldBuff = GET_HEADER(buffData);
    bigton_buff_owner_t *o = oldBuff->owner;
    bigton_buff_t *prev = oldBuff->prev;
    bigton_buff_t *next = oldBuff->next;
    size_t oldSize = oldBuff->sizeBytes;
    bigton_buff_t *newBuff = realloc(oldBuff, sizeof(bigton_buff_t) + numBytes);
    newBuff->sizeBytes = numBytes;
    POINT_PREV_NODE_TO(o, prev, newBuff); // prev.next = newBuff
    POINT_NEXT_NODE_TO(o, next, newBuff); // next.prev = newBuff
    o->totalSizeBytes = o->totalSizeBytes - oldSize + numBytes;
    return (void *) newBuff->data;
}

void bigtonFreeAll(bigton_buff_owner_t *o) {
    bigton_buff_t *current = o->first;
    while (current != NULL) {
        bigton_buff_t *after = current->next;
        free((void *) current);
        current = after;
    }
    o->totalSizeBytes = 0;
}
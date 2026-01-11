
#include "bigton.h"
#include <stdlib.h>
#include <stddef.h>
#include <stdbool.h>

void *bigtonAllocBuff(bigton_buff_owner_t *o, size_t numBytes) {
    bigton_buff_t *buff = malloc(sizeof(bigton_buff_t) + numBytes);
    buff->owner = o;
    buff->sizeBytes = numBytes;
    if (o->first == NULL) {
        buff->prev = NULL;
        buff->next = NULL;
        o->first = buff;
        o->last = buff;
    } else {
        bigton_buff_t *prev = o->last;
        buff->prev = prev;
        buff->next = NULL;
        prev->next = buff;
        o->last = buff;
    }
    o->totalSizeBytes += numBytes;
    return (void *) buff->data;
}

void bigtonFreeBuff(const void *buffData) {
    bigton_buff_t *buff
        = (bigton_buff_t *) (buffData - offsetof(bigton_buff_t, data));
    bigton_buff_owner_t *o = buff->owner;
    bigton_buff_t *prev = buff->prev;
    bigton_buff_t *next = buff->next;
    if (prev == NULL) {
        o->first = next;
    } else {
        prev->next = next;
    }
    if (next == NULL) {
        o->last = prev;
    } else {
        next->prev = prev;
    }
    o->totalSizeBytes -= buff->sizeBytes;
    return free((void *) buff);
}

void *bigtonReallocBuff(const void *buffData, size_t numBytes) {
    bigton_buff_t *oldBuff
        = (bigton_buff_t *) (buffData - offsetof(bigton_buff_t, data));
    bigton_buff_owner_t *o = oldBuff->owner;
    bigton_buff_t *prev = oldBuff->prev;
    bigton_buff_t *next = oldBuff->next;
    size_t oldSize = oldBuff->sizeBytes;
    bigton_buff_t *newBuff = realloc(oldBuff, numBytes);
    newBuff->owner = o;
    newBuff->sizeBytes = numBytes;
    newBuff->prev = prev;
    newBuff->next = next;
    if (prev == NULL) {
        o->first = newBuff;
    } else {
        prev->next = newBuff;
    }
    if (next == NULL) {
        o->last = newBuff;
    } else {
        next->prev = newBuff;
    }
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
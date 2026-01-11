
#include "bigton.h"
#include <stdlib.h>

void *bigtonAllocBuff(size_t numBytes) {
    return malloc(numBytes);
}

void bigtonFreeBuff(const void *buffer) {
    return free((void *) buffer);
}

void *bigtonReallocBuff(const void *buffer, size_t numBytes) {
    return realloc((void *) buffer, numBytes);
}

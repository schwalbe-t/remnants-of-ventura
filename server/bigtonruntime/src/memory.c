
#include "bigton.h"
#include <stdlib.h>

void *bigtonAllocBuff(size_t numBytes) {
    return malloc(numBytes);
}

void bigtonFreeBuff(const void *buffer) {
    return free((void *) buffer);
}


#ifndef BIGTON_JNI_HELPERS
#define BIGTON_JNI_HELPERS


#define NO_PARAMS() \
    (JNIEnv *env, jclass cls)
#define PARAMS(...) \
    (JNIEnv *env, jclass cls, __VA_ARGS__)


#define UNPACK(valueHandle, T, value) \
    T *value = (T *) (intptr_t) (valueHandle)
#define AS_HANDLE(value) \
    (jlong) (intptr_t) (value)


#define MALLOC_VALUE(value) \
    bigton_tagged_value_t *value = malloc(sizeof(bigton_tagged_value_t))

#endif
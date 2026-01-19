
#include "bigton/error.h"
#include "bigton/values.h"
#include <bigton/runtime.h>
#include <stdio.h>
#include <inttypes.h>
#include <stdlib.h>
#include <wchar.h>

static int readProgramFile(
    const char *path, const uint8_t **programOut, size_t *lenOut
) {
    FILE *f = fopen(path, "rb");
    if (f == NULL) {
        perror("Failed to open file");
        return 1;
    }
    fseek(f, 0, SEEK_END);
    size_t rawProgramSize = (size_t) ftell(f);
    rewind(f);
    
    uint8_t *rawProgram = malloc(sizeof(uint8_t) * rawProgramSize);
    size_t readBytes = fread(rawProgram, sizeof(uint8_t), rawProgramSize, f);
    if (readBytes != rawProgramSize) {
        perror("Failed to read file");
        return 1;
    }
    fclose(f);
    
    *programOut = rawProgram;
    *lenOut = rawProgramSize;
    return 0;
}

static bigton_string_t *valueToString(
    bigton_runtime_state_t *r, bigton_tagged_value_t value
) {
    static const size_t buffLen = 64;
    wchar_t buff[buffLen];
    switch (value.t) {
        case BIGTON_NULL:
            return bigtonAllocString(&r->b, 4, u"null");
        case BIGTON_INT: {
            size_t len = swprintf(buff, buffLen, L"%" PRIi64, value.v.i);
            return bigtonAllocString(&r->b, len, (const bigton_char_t *) buff);
        }
        case BIGTON_FLOAT: {
            size_t len = swprintf(buff, buffLen, L"%f", value.v.f);
            return bigtonAllocString(&r->b, len, (const bigton_char_t *) buff);
        }
        case BIGTON_STRING:
            return bigtonAllocString(&r->b, 8, u"<string>");
        case BIGTON_TUPLE:
            return bigtonAllocString(&r->b, 7, u"<tuple>");
        case BIGTON_OBJECT:
            return bigtonAllocString(&r->b, 8, u"<object>");
        case BIGTON_ARRAY:
            return bigtonAllocString(&r->b, 7, u"<array>");
    }
    return bigtonAllocString(&r->b, 9, u"<unknown>");
}

static void builtinPrint(bigton_runtime_state_t *r) {
    bigton_tagged_value_t v = bigtonStackPop(&r->stack, r);
    if (r->error != BIGTONE_NONE) { return; }
    bigton_string_t *disp = valueToString(r, v);
    bigtonLogLine(r, disp);
    bigtonValRcDecr(v);
    bigtonStackPush(&r->stack, BIGTON_NULL_VALUE, r);
}

static void builtinError(bigton_runtime_state_t *r) {
    builtinPrint(r);
    r->error = BIGTONE_BY_PROGRAM;
}

static void builtinString(bigton_runtime_state_t *r) {
    bigton_tagged_value_t v = bigtonStackPop(&r->stack, r);
    if (r->error != BIGTONE_NONE) { return; }
    bigton_string_t *disp = valueToString(r, v);
    bigtonStackPush(&r->stack, BIGTON_STRING_VALUE(disp), r);
    bigtonValRcDecr(v);
}

typedef void (*bigton_builtin_impl_t)(bigton_runtime_state_t *r);

const bigton_builtin_impl_t builtinImpls[] = {
    &builtinPrint,
    &builtinError,
    &builtinString
};

int main(int argc, char **argv) {
    if (argc != 2) {
        printf("Usage: bigton <file>");
        return 1;
    }
    
    const uint8_t *rawProgram;
    size_t rawProgramSize;
    int readStatus = readProgramFile(argv[1], &rawProgram, &rawProgramSize);
    if (readStatus != 0) { return readStatus; }
    
    bigton_parsed_program_t p;
    bigton_error_t parseError = BIGTONE_NONE;
    bigtonParseProgram(rawProgram, rawProgramSize, &p, &parseError);
    if (parseError != BIGTONE_NONE) {
        fprintf(stderr, "Error while parsing BIGTON program: %u\n", parseError);
        return 1;
    }
    bigtonDebugProgram(&p);
    
    bigton_runtime_settings_t settings = (bigton_runtime_settings_t) {
        .tickInstructionLimit = 9999, //UINT64_MAX,
        .memoryUsageLimit = SIZE_MAX,
        .maxCallDepth = 2048,
        .maxTupleSize = 256
    };
    
    bigton_runtime_state_t r;
    bigtonInit(&r, &settings, &p);
    
    bigtonStartTick(&r);
    for (;;) {
        bigton_exec_status_t status = bigtonExecBatch(&r);
        if (r.error != BIGTONE_NONE || status == BIGTONST_ERROR) {
            fprintf(stderr, "Execution error: %u\n", r.error);
            break;
        }
        if (status == BIGTONST_CONTINUE) {
            continue;
        }
        if (status == BIGTONST_EXEC_BUILTIN_FUN) {
            builtinImpls[r.awaitingBuiltinFun](&r);
            continue;
        }
        if (status == BIGTONST_AWAIT_TICK) {
            bigtonStartTick(&r);
            continue;
        }
        if (status == BIGTONST_COMPLETE) {
            printf("Execution completed successfully\n");
            break;
        }
    }
    
    printf("%zu line(s) logged\n", r.logsCount);
    
    bigtonFree(&r);
    free((void *) rawProgram);
    return 0;
}
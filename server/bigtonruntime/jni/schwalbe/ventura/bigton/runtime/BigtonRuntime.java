
package schwalbe.ventura.bigton.runtime;

public final class BigtonRuntime {

    public static final class Status {
        public static final int CONTINUE = 0;
        public static final int EXEC_BUILTIN_FUN = 1;
        public static final int AWAIT_TICK = 2;
        public static final int COMPLETE = 3;
        public static final int ERROR = 4;
    }
    
    /**
    * Allocates runtime settings and initializes them with the given values.
    * @param tickInstructionLimit
    * @param memoryUsageLimit
    * @param maxCallDepth
    * @param maxTupleSize
    * @return The pointer to the allocated settings
    */
    public static native long createSettings(
        long tickInstructionLimit,
        long memoryUsageLimit,
        int maxCallDepth,
        int maxTupleSize
    );
    /**
    * Deletes the given runtime settings.
    * @param settings The pointer to the settings
    */
    public static native void freeSettings(long settings);
    
    /**
    * Creates a new BIGTON runtime instance.
    * Destroy with {@link BigtonRuntime#free(long)} once complete.
    * @param settings The settings to use for the runtime.
    * These are copied and have no lifetime requirenments.
    * @param rawProgram A pointer to the start of the raw program.
    * This is NOT COPIED and REFERENCED BY THE RUNTIME, and therefore
    * must outlive this runtime instance
    * @param rawProgramLength The size of the raw program block referenced
    * by 'rawProgram'
    * @return The pointer to the allocated runtime - a value of 0 indicates
    * failure when parsing the given program
    */
    public static native long create(
        long settings, long rawProgram, long rawProgramLength
    );
    /**
    * Destroys the runtime instance pointed to by 'r',
    * ALSO FREEING ALL OBJECTS ALLOCATED BY THE INSTANCE.
    * @param r The pointer to the resource instance.
    */
    public static native void free(long r);
    
    /**
    * @param r A pointer to a runtime instance
    * @return The number of lines logged in the runtime
    */
    public static native long getLogLength(long r);
    /**
    * @param r A pointer to the runtime instance
    * @param i The index of the logged line -
    * an out of bounds index results in undefined behavior
    * @return The string value of the line
    */
    public static native String getLogString(long r, long i);
    /**
    * Adds the given string line to the runtime logs.
    * @param r A pointer to the runtime instance
    * @param v A pointer to a value (which must be of type STRING)
    * Does not take ownership of the value.
    */
    public static native void addLogLine(long r, long v);
    
    /**
    * @param r A pointer to the runtime instance
    * @return The length of the stack trace
    */
    public static native long getTraceLength(long r);
    /**
    * @param r A pointer to the runtime instance
    * @param i The index of the stack trace entry -
    * an out of bounds index results in undefined behavior
    * @return A newly allocated and populated trace object instance.
    * Ownership is transferred to the caller and the object must be freed
    * using {@link BigtonTrace#free(long)}.
    */
    public static native long getTrace(long r, long i);
    
    /**
    * @param r A pointer to the runtime instance
    * @return The number of values in the runtime value stack
    */
    public static native long getStackLength(long r);
    /**
    * Pushes the given value onto the runtime value stack.
    * @param r A pointer to the runtime instance
    * @param value A pointer to the value to push onto the stack.
    * Does not take ownership of the value.
    */
    public static native void pushStack(long r, long value);
    /**
    * @param r A pointer to the runtime instance
    * @param i The index of the value in the runtime value stack -
    * an out of bounds index results in undefined behavior
    * @return A pointer to a newly allocated value object containing the value -
    * ownership belongs to the caller and must be freed using
    * {@link BigtonValue#free(long)}
    */
    public static native long getStack(long r, long i);
    /**
    * @param r A pointer to the runtime instance
    * @return A pointer to a newly allocated value object containing the popped
    * value - may be 0 if the stack is empty - ownership belongs to the caller
    * and must be freed using {@link BigtonValue#free(long)}
    */
    public static native long popStack(long r);
    
    public static native boolean hasError(long r);
    public static native int getError(long r);
    public static native int getCurrentFile(long r);
    public static native int getCurrentLine(long r);
    public static native long getUsedMemory(long r);
    public static native int getAwaitingBuiltinFun(long r);
    
    public static native String getConstString(long r, int id);
    
    public static native void startTick(long r);
    public static native int execBatch(long r);
    
}
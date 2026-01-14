
package schwalbe.ventura.bigton.runtime;

import java.nio.ByteBuffer;
import java.nio.file.Paths;

public final class BigtonRuntime {
    
    /**
     * Mirror of definition of 'bigton_exec_status_t'
     * in 'bigtonruntime/include/bigton/runtime.h'
     */
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
     * @param settings The pointer to the settings - pointers that do not refer
     * to a valid settings object result in undefined behavior
     */
    public static native void freeSettings(long settings);
    
    /**
     * Creates a new BIGTON runtime instance.
     * Destroy with {@link BigtonRuntime#free(long)} once complete.
     * @param settings The settings to use for the runtime.
     * These are copied and have no lifetime requirenments.
     * @param rawProgram A DIRECT byte buffer containing the raw program.
     * This is NOT COPIED and REFERENCED BY THE RUNTIME, and therefore
     * must outlive this runtime instance!
     * Passing a non-direct byte buffer or a byte buffer with insufficient
     * capacity results in undefined behavior.
     * @param rawProgramStart The offset in bytes of the start of the raw
     * program inside the given byte buffer
     * @param rawProgramLength The length of the raw program in bytes
     * @return The pointer to the allocated runtime - a value of 0 indicates
     * failure during parsing of the given program
     */
    public static native long create(
        long settings, ByteBuffer rawProgram,
        int rawProgramStart, int rawProgramLength
    );
    /**
     * Destroys the runtime instance pointed to by 'r',
     * ALSO FREEING ALL OBJECTS ALLOCATED BY THE INSTANCE.
     * @param r The pointer to the runtime instance - pointers that do not refer
     * to a valid runtime instance object result in undefined behavior
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
     * {@link BigtonValue#free}
     */
    public static native long getStack(long r, long i);
    /**
     * @param r A pointer to the runtime instance
     * @return A pointer to a newly allocated value object containing the popped
     * value - may be 0 if the stack is empty - ownership belongs to the caller
     * and must be freed using {@link BigtonValue#free}
     */
    public static native long popStack(long r);
    
    /**
     * @param r A pointer to the runtime instance
     * @return Whether or not an error flag is set
     */
    public static native boolean hasError(long r);
    /**
     * @param r A pointer to the runtime instance
     * @return The current value of the error flag
     */
    public static native int getError(long r);
    /**
     * @param r A pointer to the runtime instance
     * @return The constant string ID of the current file name
     */
    public static native int getCurrentFile(long r);
    /**
     * @param r A pointer to the runtime instance
     * @return The number of the current line
     */
    public static native int getCurrentLine(long r);
    /**
     * @param r A pointer to the runtime instance
     * @return The number of used memory in bytes
     */
    public static native long getUsedMemory(long r);
    /**
     * @param r A pointer to the runtime instance
     * @return The index of th builtin function that was last requested to be
     * executed
     */
    public static native int getAwaitingBuiltinFun(long r);
    
    /**
     * @param r A pointer to the runtime instance
     * @param id The ID of a constant string in the program loaded by the given
     * runtime instance - an ID that is out of bounds results in
     * undefined behavior
     * @return The string as a JVM string instance
     */
    public static native String getConstString(long r, int id);
    
    /**
     * Indicates to the given runtime instance that a new tick has begun.
     * @param r A pointer to the runtime instance
     */
    public static native void startTick(long r);
    /**
     * Executes all instructions up to the next special case, which is
     * indicated by the return value being one of the following execution status
     * codes:
     * <ul>
     * <li>{@link Status#CONTINUE} - call this method again</li>
     * <li>{@link Status#EXEC_BUILTIN_FUN} - execute the builtin function
     * indicated by {@link #getAwaitingBuiltinFun}, then call this method
     * again</li>
     * <li>{@link Status#AWAIT_TICK} - wait until the start of the next tick,
     * then call this method again</li>
     * <li>{@link Status#COMPLETE} - program execution complete</li>
     * <li>{@link Status#ERROR} - could not continue execution due to
     * error code indicated by {@link #getError}</li>
     * </ul>
     * Note that in special cases, the runtime may set the error flag without
     * explicitly indicating this by returning {@link Status#ERROR}; the host
     * environment should check {@link #hasError} after each call to this
     * method even if the returned status does not indicate an error.
     * @param r A pointer to the runtime instance
     * @return The execution status code
     */
    public static native int execBatch(long r);
    
    
    private static String getOsLibraryPrefix(String osName) {
        if (osName.contains("win")) { return ""; }
        if (osName.contains("mac")) { return "lib"; }
        if (osName.contains("nux") || osName.contains("nix")) { return "lib"; }
        throw new IllegalStateException("Unsupported OS '" + osName + "'");
    }
    
    private static String getOsLibraryFileExt(String osName) {
        if (osName.contains("win")) { return "dll"; }
        if (osName.contains("mac")) { return "dylib"; }
        if (osName.contains("nux") || osName.contains("nix")) { return "so"; }
        throw new IllegalStateException("Unsupported OS '" + osName + "'");
    }
    
    /**
     * Attempts to load the specified BIGTON runtime binary from disk.
     * @param relDir The directory relative to the current working directory
     * containing the binary
     * @param name The name of the binary, without any platform-specific prefix
     * or file extension 
     */
    public static void loadLibrary(String relDir, String name) {
        String osName = System.getProperty("os.name").toLowerCase();
        String prefix = BigtonRuntime.getOsLibraryPrefix(osName);
        String ext = BigtonRuntime.getOsLibraryFileExt(osName);
        String fileName = prefix + name + "." + ext;
        String absPath = Paths.get(relDir, fileName)
            .toAbsolutePath().toString();
        System.load(absPath);
    }
    
}
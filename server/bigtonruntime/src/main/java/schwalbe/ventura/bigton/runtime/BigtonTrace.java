
package schwalbe.ventura.bigton.runtime;

public final class BigtonTrace {
    
    /**
     * @param t A pointer to a trace object
     * @return The ID of the constant string containing the name of the
     * function
     */
    public static native int getName(long t);
    /**
     * @param t A pointer to a trace object
     * @return The ID of the constant string containing the name of the file
     * that contains the function declaration
     */
    public static native int getDeclFile(long t);
    /** 
     * @param t A pointer to a trace object
     * @return The line number (from IR line declarations) of the function
     * declaration
     */
    public static native int getDeclLine(long t);
    /**
     * @param t A pointer to a trace object
     * @return The ID of the constant string containing the name of the file
     * that contains the call site
     */
    public static native int getFromFile(long t);
    /** 
     * @param t A pointer to a trace object
     * @return The line number (from IR line declarations) of the call site
     */
    public static native int getFromLine(long t);
    
    /**
     * Frees the object.
     * @param t A pointer to a trace object - pointers that do not refer to a
     * valid trace object result in undefined behavior
     */
    public static native void free(long t);
    
}
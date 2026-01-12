
package schwalbe.ventura.bigton.runtime;

public final class BigtonTrace {
    
    public static native int getName(long t);
    public static native int getDeclFile(long t);
    public static native int getDeclLine(long t);
    public static native int getFromFile(long t);
    public static native int getFromLine(long t);
    
    public static native void free(long t);
    
}
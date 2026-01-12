
package schwalbe.ventura.bigton.runtime;

public final class BigtonValue {
    
    public static final class Type {
        public static final int NULL = 0;
        public static final int INT = 1;
        public static final int FLOAT = 2;
        public static final int STRING = 3;
        public static final int TUPLE = 4;
        public static final int OBJECT = 5;
        public static final int ARRAY = 6;
    }
    
    public static native long createNull();
    public static native long createInt(long i);
    public static native long createFloat(double f);
    public static native long createString(String s, long r);
    public static native long createTuple(int length, long r);
    public static native long createArray(int length, long r);
    
    public static native long copy(long v);
    public static native void free(long v, long r);
    
    public static native int getType(long v);
    public static native long getInt(long v);
    public static native double getFloat(long v);
    public static native String getString(long v);
    public static native int getTupleLength(long v);
    public static native long getTupleMember(long v, int i);
    public static native void setTupleMember(long v, int i, long value);
    public static native int findObjectPropDyn(long v, String name);
    public static native int findObjectPropConst(long v, int nameId);
    public static native int getObjectSize(long v);
    public static native long getObjectMember(long v, int propId);
    public static native void setObjectMember(
        long v, int propId, long value
    );
    public static native int getArrayLength(long v);
    public static native long getArrayElement(long v, int i);
    public static native void setArrayElement(long v, int i, long value);
    
}
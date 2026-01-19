
package schwalbe.ventura.bigton.runtime;

public final class BigtonValue {
    
    /**
     * Mirror of definition of 'bigton_value_type_t'
     * in 'bigtonruntime/include/bigton/values.h'
     */
    public static final class Type {
        public static final int NULL = 0;
        public static final int INT = 1;
        public static final int FLOAT = 2;
        public static final int STRING = 3;
        public static final int TUPLE = 4;
        public static final int OBJECT = 5;
        public static final int ARRAY = 6;
    }
    
    /** 
     * @param v The pointer of a value object
     * @return A newly allocated value object containing the same value
     * as the given value object - ownership is transferred to the caller and
     * the object needs to be freed using {@link BigtonValue#free}
     */
    public static native long copy(long v);
    /**
     * Deallocates the given value object and handles destruction of the
     * value or reference contained by it. 
     * @param v The pointer to a value object to deallocate
     */
    public static native void free(long v);
    
    /**
     * @param v A pointer to a value object
     * @return One of {@link BigtonValue.Type} indicating the type of the value
     * contained by the given value object
     */
    public static native int getType(long v);
    
    /**
     * @return A newly allocated value object containing the null value -
     * ownership is transferred to the caller and the object needs to be freed
     * using {@link BigtonValue#free}
     */
    public static native long createNull();
    
    /**
     * @param i The integer value
     * @return A newly allocated value object containing the given integer
     * value - ownership is transferred to the caller and the object needs to
     * be freed using {@link BigtonValue#free}
     */
    public static native long createInt(long i);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#INT} - a value object containing another type
     * results in undefined behavior
     * @return The contained integer value
     */
    public static native long getInt(long v);
    
    /**
     * @param f The float value
     * @return A newly allocated value object containing the given float
     * value - ownership is transferred to the caller and the object needs to
     * be freed using {@link BigtonValue#free}
     */
    public static native long createFloat(double f);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#FLOAT} - a value object containing another type
     * results in undefined behavior
     * @return The contained float value
     */
    public static native double getFloat(long v);
    
    /**
     * @param s The string value
     * @param r The runtime to allocate the string with - the contained
     * string reference (but not the value object) will become automatically
     * deallocated when {@link BigtonRuntime#free} is used on this instance
     * @return A newly allocated value object containing the given string
     * reference - ownership is transferred to the caller and the object
     * needs to be freed using {@link BigtonValue#free} - may return 0 if an
     * invalid string is passed
     */
    public static native long createString(String s, long r);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#STRING} - a value object containing another type
     * results in undefined behavior
     * @return The contained string value
     */
    public static native String getString(long v);
    
    /**
     * @param length The length of the tuple
     * @param r The runtime to allocate the tuple with - the contained
     * tuple reference (but not the value object) will become automatically
     * deallocated when {@link BigtonRuntime#free} is used on this instance
     * @return A newly allocated value object containing the given tuple
     * reference - ownership is transferred to the caller and the object
     * needs to be freed using {@link BigtonValue#free} - the tuple is filled
     * with value type NULL
     */
    public static native long createTuple(int length, long r);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#TUPLE} - a value object containing another type
     * results in undefined behavior
     * @return The length of the contained tuple
     */
    public static native int getTupleLength(long v);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#TUPLE} - a value object containing another type
     * results in undefined behavior
     * @param i The index of a member in the tuple - an out of bounds index
     * results in undefined behavior
     * @return A newly allocated value object containing the value of the tuple
     * member at the given index - ownership is transferred to the caller and
     * the object needs to be freed using {@link BigtonValue#free}
     */
    public static native long getTupleMember(long v, int i);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#TUPLE} - a value object containing another type
     * results in undefined behavior
     * @param i The index of a member in the tuple - an out of bounds index
     * results in undefined behavior
     * @param value A pointer to the value to set the member of the tuple
     * at the given index to - does not take ownership of the value object
     */
    public static native void setTupleMember(long v, int i, long value);
    /**
     * Updates the tuple contained by the given value object bsaed on the
     * values contained in the tuple. Call this method after modifying a tuple
     * using {@link #setTupleMember}.
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#TUPLE} - a value object containing another type
     * results in undefined behavior
     */
    public static native void updateTupleInfo(long v);
    
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#OBJECT} - a value object containing another type
     * results in undefined behavior
     * @param name The name of a member of the object
     * @param r The pointer of the runtime instance 
     * @return The shape slot of the member with the given name - returns -1
     * if no member of the object has the given name
     */
    public static native int findObjectPropDyn(long v, String name, long r);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#OBJECT} - a value object containing another type
     * results in undefined behavior
     * @param nameId The ID of a constant string containing the object name
     * @param r The pointer of the runtime instance
     * @return The shape slot of the member with the given name - sets the
     * runtime error flag if the object does not contain the given member
     */
    public static native int findObjectPropConst(long v, int nameId, long r);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#OBJECT} - a value object containing another type
     * results in undefined behavior
     * @return The number of properties present in the object shape
     * (equal to the number of slots in the object)
     */
    public static native int getObjectSize(long v);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#OBJECT} - a value object containing another type
     * results in undefined behavior
     * @param slotId The ID of the slot to get the name of
     * @param r The pointer of the runtime instance
     * @return The const string ID of the name of the member specified by the
     * give slot (or -1 if the object or prop ID is invalid)
     */
    public static native int getObjectPropName(long v, int slotId, long r);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#OBJECT} - a value object containing another type
     * results in undefined behavior
     * @param slotId The ID of the slot to get the value of - an out of bounds
     * slot index results in undefined behavior
     * @return A newly allocated value object containing the value of the object
     * slot with the given ID - ownership is transferred to the caller and
     * the object needs to be freed using {@link BigtonValue#free}
     */
    public static native long getObjectMember(long v, int slotId);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#OBJECT} - a value object containing another type
     * results in undefined behavior
     * @param slotId The ID of the slot to get the value of - an out of bounds
     * slot index results in undefined behavior
     * @param value A pointer to the value to set the member of the object
     * slot with the given ID to - does not take ownership of the value object
     */
    public static native void setObjectMember(
        long v, int slotId, long value
    );
    
    /**
     * @param length The length of the array
     * @param r The runtime to allocate the array with - the contained
     * array reference (but not the value object) will become automatically
     * deallocated when {@link BigtonRuntime#free} is used on this instance
     * @return A newly allocated value object containing the given array
     * reference - ownership is transferred to the caller and the object
     * needs to be freed using {@link BigtonValue#free}
     */
    public static native long createArray(int length, long r);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#ARRAY} - a value object containing another type
     * results in undefined behavior
     * @return The length of the contained array
     */
    public static native int getArrayLength(long v);
    /** 
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#ARRAY} - a value object containing another type
     * results in undefined behavior
     * @param i The index of an element in the array - an out of bounds index
     * results in undefined behavior
     * @return A newly allocated value object containing the value of the array
     * element at the given index - ownership is transferred to the caller and
     * the object needs to be freed using {@link BigtonValue#free}
     */
    public static native long getArrayElement(long v, int i);
    /**
     * @param v A pointer to a value object with contained type
     * {@link BigtonValue.Type#ARRAY} - a value object containing another type
     * results in undefined behavior
     * @param i The index of an element in the array - an out of bounds index
     * results in undefined behavior
     * @param value A pointer to the value to set the member of the tuple
     * at the given index to - does not take ownership of the value object
     */
    public static native void setArrayElement(long v, int i, long value);
    
}
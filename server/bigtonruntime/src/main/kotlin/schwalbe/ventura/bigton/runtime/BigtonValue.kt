
package schwalbe.ventura.bigton.runtime

data class BigtonConstStr(val handle: Int)

data class BigtonObjectProperty(val handle: Int)

object BigtonValueN {
    /**
     * Mirror of 'bigton_value_type_t'
     * defined in 'src/main/headers/bigton/values.h'
     */
    object ValueType {
        const val NULL      = 0
        const val INT       = 1
        const val FLOAT     = 2
        const val STRING    = 3
        const val TUPLE     = 4
        const val OBJECT    = 5
        const val ARRAY     = 6
    }
    
    @JvmStatic external fun free(handle: Long)
    
    @JvmStatic external fun getType(handle: Long): Int
    
    @JvmStatic external fun createNull(): Long
    
    @JvmStatic external fun createInt(value: Long): Long
    @JvmStatic external fun getInt(handle: Long): Long
    
    @JvmStatic external fun createFloat(value: Double): Long
    @JvmStatic external fun getFloat(handle: Long): Double
    
    @JvmStatic external fun createString(
        value: String, runtimeHandle: Long
    ): Long
    @JvmStatic external fun getString(handle: Long): String
    @JvmStatic external fun getStringLength(handle: Long): Int
    @JvmStatic external fun concatStrings(
        handleA: Long, handleB: Long, runtimeHandle: Long
    ): Long
    @JvmStatic external fun sliceString(
        handle: Long, startIdx: Int, endIdx: Int, runtimeHandle: Long
    ): Long
    
    @JvmStatic external fun createTuple(
        length: Int, runtimeHandle: Long
    ): Long
    @JvmStatic external fun setTupleAt(
        handle: Long, index: Int, valueHandle: Long
    )
    @JvmStatic external fun completeTuple(handle: Long)
    @JvmStatic external fun getTupleLength(handle: Long): Int
    @JvmStatic external fun getTupleFlatLength(handle: Long): Int
    @JvmStatic external fun getTupleAt(handle: Long, index: Int): Long

    @JvmStatic external fun getObjectPropCount(handle: Long): Int
    @JvmStatic external fun findObjectProp(
        handle: Long, name: String, runtimeHandle: Long
    ): Int
    @JvmStatic external fun getObjectPropName(
        handle: Long, propHandle: Int, runtimeHandle: Long
    ): String
    @JvmStatic external fun getObjectPropValue(
        handle: Long, propHandle: Int
    ): Long
    @JvmStatic external fun setObjectPropValue(
        handle: Long, propHandle: Int, valueHandle: Long
    )

    @JvmStatic external fun createArray(
        length: Int, runtimeHandle: Long
    ): Long
    @JvmStatic external fun getArrayLength(handle: Long): Int
    @JvmStatic external fun setArrayAt(
        handle: Long, index: Int, valueHandle: Long
    )
    @JvmStatic external fun insertArrayAt(
        handle: Long, index: Int, valueHandle: Long, runtimeHandle: Long
    )
    @JvmStatic external fun removeArrayAt(handle: Long, index: Int): Long
    @JvmStatic external fun getArrayAt(handle: Long, index: Int): Long
    @JvmStatic external fun concatArrays(
        handleA: Long, handleB: Long, runtimeHandle: Long
    ): Long
    @JvmStatic external fun sliceArray(
        handle: Long, startIdx: Int, endIdx: Int, runtimeHandle: Long
    ): Long
    
    fun wrapHandle(handle: Long): BigtonValue {
        return when (BigtonValueN.getType(handle)) {
            ValueType.NULL -> BigtonNull(handle)
            ValueType.INT -> BigtonInt(handle)
            ValueType.FLOAT -> BigtonFloat(handle)
            ValueType.STRING -> BigtonString(handle)
            ValueType.TUPLE -> BigtonTuple(handle)
            ValueType.OBJECT -> BigtonObject(handle)
            ValueType.ARRAY -> BigtonArray(handle)
            else -> BigtonNull(handle) // <- THIS SHOULD NEVER HAPPEN!
        }
    }
}

sealed class BigtonValue(val handle: Long): AutoCloseable {
    
    override fun close() = BigtonValueN.free(this.handle)
    
}

val BigtonValue.isTruthy: Boolean
    get() = when (this) {
        is BigtonNull -> false
        is BigtonInt -> this.value != 0L
        is BigtonFloat -> this.value != 0.0 && !this.value.isNaN()
        is BigtonString, is BigtonTuple, is BigtonObject, is BigtonArray -> true
    }


class BigtonNull    (handle: Long) : BigtonValue(handle) { companion object }

fun BigtonNull.Companion.create()
    = BigtonNull(BigtonValueN.createNull())


class BigtonInt     (handle: Long) : BigtonValue(handle) { companion object }
    
fun BigtonInt.Companion.fromValue(value: Long)
    = BigtonInt(BigtonValueN.createInt(value))

val BigtonInt.value: Long
    get() = BigtonValueN.getInt(this.handle)

    
class BigtonFloat   (handle: Long) : BigtonValue(handle) { companion object }
    
fun BigtonFloat.Companion.fromValue(value: Double)
    = BigtonFloat(BigtonValueN.createFloat(value))

val BigtonFloat.value: Double
    get() = BigtonValueN.getFloat(this.handle)

    
class BigtonString  (handle: Long) : BigtonValue(handle) { companion object }

fun BigtonString.Companion.fromValue(value: String, runtime: BigtonRuntime)
    = BigtonString(BigtonValueN.createString(value, runtime.handle))

val BigtonString.length: Int
    get() = BigtonValueN.getStringLength(this.handle)

val BigtonString.value: String
    get() = BigtonValueN.getString(this.handle)

fun BigtonString.Companion.concat(
    a: BigtonString, b: BigtonString, runtime: BigtonRuntime
) = BigtonString(BigtonValueN.concatStrings(
    a.handle, b.handle, runtime.handle)
)

fun BigtonString.slice(
    startIdx: Int, endIdx: Int, runtime: BigtonRuntime
): BigtonString {
    val thisLength: Int = this.length
    require(startIdx in 0..thisLength)
    require(endIdx in 0..thisLength)
    require(startIdx <= endIdx)
    return BigtonString(BigtonValueN.sliceString(
        this.handle, startIdx, endIdx, runtime.handle
    ))
}


class BigtonTuple   (handle: Long) : BigtonValue(handle) { companion object }
    
fun BigtonTuple.Companion.fromElements(
    values: Iterable<BigtonValue>, runtime: BigtonRuntime
): BigtonTuple {
    val handle: Long = BigtonValueN.createTuple(values.count(), runtime.handle)
    for ((i, v) in values.withIndex()) {
        BigtonValueN.setTupleAt(handle, i, v.handle)
    }
    BigtonValueN.completeTuple(handle)
    return BigtonTuple(handle)
}

val BigtonTuple.length: Int
    get() = BigtonValueN.getTupleLength(this.handle)

val BigtonTuple.flatLength: Int
    get() = BigtonValueN.getTupleFlatLength(this.handle)

operator fun BigtonTuple.get(index: Int): BigtonValue {
    require(index >= 0 && index < this.length)
    val valueHandle: Long = BigtonValueN.getTupleAt(this.handle, index)
    return BigtonValueN.wrapHandle(valueHandle)
}

operator fun BigtonTuple.set(index: Int, value: BigtonValue) {
    require(index >= 0 && index < this.length)
    BigtonValueN.setTupleAt(this.handle, index, value.handle)
}

val BigtonTuple.values: Sequence<BigtonValue>
    get() = (0..<this.length).asSequence().map(this::get)


class BigtonObject  (handle: Long) : BigtonValue(handle) { companion object }

val BigtonObject.size: Int
    get() = BigtonValueN.getObjectPropCount(this.handle)

val BigtonObject.properties: Sequence<BigtonObjectProperty>
    get() = (0..<this.size).asSequence().map(::BigtonObjectProperty)

fun BigtonObject.find(
    propertyName: String, runtime: BigtonRuntime
): BigtonObjectProperty? {
    val propHandle: Int = BigtonValueN.findObjectProp(
        this.handle, propertyName, runtime.handle
    )
    return if (propHandle == -1) { null }
        else { BigtonObjectProperty(propHandle) }
}

fun BigtonObject.nameOf(
    property: BigtonObjectProperty, runtime: BigtonRuntime
): String {
    require(property.handle >= 0 && property.handle < this.size)
    return BigtonValueN.getObjectPropName(
        this.handle, property.handle, runtime.handle
    )
}

operator fun BigtonObject.get(property: BigtonObjectProperty): BigtonValue {
    require(property.handle >= 0 && property.handle < this.size)
    val vh: Long = BigtonValueN.getObjectPropValue(this.handle, property.handle)
    return BigtonValueN.wrapHandle(vh)
}

operator fun BigtonObject.set(
    property: BigtonObjectProperty, value: BigtonValue
) {
    require(property.handle >= 0 && property.handle < this.size)
    BigtonValueN.setObjectPropValue(this.handle, property.handle, value.handle)
}


class BigtonArray   (handle: Long) : BigtonValue(handle) { companion object }

fun BigtonArray.Companion.fromElements(
    values: Iterable<BigtonValue>, runtime: BigtonRuntime
): BigtonArray {
    val handle: Long = BigtonValueN.createArray(values.count(), runtime.handle)
    for ((i, v) in values.withIndex()) {
        BigtonValueN.setArrayAt(handle, i, v.handle)
    }
    return BigtonArray(handle)
}

val BigtonArray.length: Int
    get() = BigtonValueN.getArrayLength(this.handle)
    
operator fun BigtonArray.get(index: Int): BigtonValue {
    require(index >= 0 && index < this.length)
    val valueHandle: Long = BigtonValueN.getArrayAt(this.handle, index)
    return BigtonValueN.wrapHandle(valueHandle)
}

operator fun BigtonArray.set(index: Int, value: BigtonValue) {
    require(index >= 0 && index < this.length)
    BigtonValueN.setArrayAt(this.handle, index, value.handle)
}

val BigtonArray.values: Sequence<BigtonValue>
    get() = (0..<this.length).asSequence().map(this::get)

fun BigtonArray.Companion.concat(
    a: BigtonArray, b: BigtonArray, runtime: BigtonRuntime
) = BigtonArray(BigtonValueN.concatArrays(
    a.handle, b.handle, runtime.handle
))

fun BigtonArray.slice(
    startIdx: Int, endIdx: Int, runtime: BigtonRuntime
): BigtonArray {
    val thisLength: Int = this.length
    require(startIdx in 0..thisLength)
    require(endIdx in 0..thisLength)
    require(startIdx <= endIdx)
    return BigtonArray(BigtonValueN.sliceArray(
        this.handle, startIdx, endIdx, runtime.handle
    ))
}

fun BigtonArray.insert(index: Int, value: BigtonValue, runtime: BigtonRuntime) {
    require(index in 0..this.length)
    BigtonValueN.insertArrayAt(
        this.handle, index, value.handle, runtime.handle
    )
}

fun BigtonArray.remove(index: Int): BigtonValue {
    require(index in 0..<this.length)
    return BigtonValueN.wrapHandle(
        BigtonValueN.removeArrayAt(this.handle, index)
    )
}


inline fun <R> Iterable<BigtonValue?>.useAll(f: () -> R): R {
    try {
        return f()
    } finally {
        this.forEach { it?.close() }
    }
}

inline fun <R> Array<BigtonValue?>.useAll(f: () -> R): R {
    try {
        return f()
    } finally {
        this.forEach { it?.close() }
    }
}
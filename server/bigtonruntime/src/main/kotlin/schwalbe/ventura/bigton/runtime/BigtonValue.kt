
package schwalbe.ventura.bigton.runtime

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
    
    @JvmStatic external fun createTuple(
        length: Int, runtimeHandle: Long
    ): Long
    @JvmStatic external fun setTupleAt(
        handle: Long, index: Int, valueHandle: Long
    )
    @JvmStatic external fun completeTuple(handle: Long)
    @JvmStatic external fun getTupleLength(handle: Long): Int
    @JvmStatic external fun getTupleAt(handle: Long, index: Int): Long
    
    @JvmStatic external fun createArray(
        length: Int, runtimeHandle: Long
    ): Long
    @JvmStatic external fun getArrayLength(handle: Long): Int
    @JvmStatic external fun setArrayAt(
        handle: Long, index: Int, valueHandle: Long
    )
    @JvmStatic external fun addArrayAt(
        handle: Long, index: Int, valueHandle: Long, runtimeHandle: Long
    )
    @JvmStatic external fun getArrayAt(handle: Long, index: Int): Long
    
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

val BigtonString.value: String
    get() = BigtonValueN.getString(this.handle)


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

operator fun BigtonTuple.get(index: Int): BigtonValue {
    require(index >= 0 && index < this.length)
    val valueHandle: Long = BigtonValueN.getTupleAt(this.handle, index)
    return BigtonValueN.wrapHandle(valueHandle)
}

operator fun BigtonTuple.set(index: Int, value: BigtonValue) {
    require(index >= 0 && index < this.length)
    BigtonValueN.setTupleAt(this.handle, index, value.handle)
}


class BigtonObject  (handle: Long) : BigtonValue(handle) { companion object }

// TODO!


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

fun BigtonArray.add(value: BigtonValue, runtime: BigtonRuntime)
    = this.add(this.length, value, runtime)

fun BigtonArray.add(index: Int, value: BigtonValue, runtime: BigtonRuntime) {
    require(index >= 0 && index <= this.length)
    BigtonValueN.addArrayAt(this.handle, index, value.handle, runtime.handle)
}
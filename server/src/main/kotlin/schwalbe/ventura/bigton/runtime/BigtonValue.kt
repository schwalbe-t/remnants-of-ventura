
package schwalbe.ventura.bigton.runtime

import schwalbe.ventura.bigton.*

sealed interface BigtonValue

object BigtonNull
    : BigtonValue
data class BigtonInt(val v: Long)
    : BigtonValue
data class BigtonFloat(val v: Double)
    : BigtonValue
data class BigtonString(val v: String)
    : BigtonValue
data class BigtonTuple(val elements: List<BigtonValue>, val flatLen: Int)
    : BigtonValue
data class BigtonObject(val members: MutableMap<String, BigtonValue>)
    : BigtonValue

fun BigtonValue.display(): String = when (this) {
    is BigtonNull -> "null"
    is BigtonInt -> this.v.toString()
    is BigtonFloat -> this.v.toString()
    is BigtonString -> "\"${this.v}\""
    is BigtonTuple -> {
        val inner: String = this.elements
            .joinToString(", ", transform = BigtonValue::display)
        "($inner)"
    }
    is BigtonObject -> if (this.members.isEmpty()) { "{}" }
    else {
        val inner: String = this.members
            .map { (m, v) -> "$m = ${v.display()}" }
            .joinToString(", ")
        "{ $inner }"
    }
}

fun BigtonValue.typeString(): String = when (this) {
    is BigtonNull -> "null"
    is BigtonInt -> "int"
    is BigtonFloat -> "float"
    is BigtonString -> "string"
    is BigtonTuple -> "tuple"
    is BigtonObject -> "object"
}
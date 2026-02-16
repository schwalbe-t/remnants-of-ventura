
package schwalbe.ventura.bigton

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

// The purpose of this class is to write binary data into a buffer while
// following C struct property alignment. Consider the following struct:
//
// struct Test {
//     int32_t a;
//     int64_t b;
//     int32_t c;
// };
//
// It would not take up 4 + 8 + 4 (= 16) bytes, but instead 24 due to alignment.
// Integers that are 4 bytes need to sit at memory addresses that are a multiple
// of 4, while 8 byte integers need to be aligned to a multiple of 8.
//
// If we were to imagine an instance of 'Test' to be sitting at memory address
// 0, the memory address of 'b' would be 4, which is not allowed. To fix this,
// padding of 4 bytes is inserted between 'a' and 'b'.
//
// Another issue is that if we were to create an array of instances of 'Test'
// at memory address 0, the second instance (index 1) would be sitting at
// memory address 4 ('a') + 4 (padding) + 8 ('b') + 4 ('c') = 20, meaning 'b'
// would be at memory address 20 + 8 = 28, which is not a multiple of 8!
// To fix this, we also need to make the size of 'Test' a multiple of 8
// (or generally the size of the struct needs to be a multiple of the maximum
// of any of the struct member alignments), which we achieve by simply inserting
// another 4 bytes of padding at the end of the struct.
//
// In the end, our struct will actually look like this in memory:
//
// struct Test {
//     int32_t a;
//     int32_t PADDING_0;
//     int64_t b;
//     int32_t c;
//     int32_t PADDING_1;
// };
//
// All of this logic is implemented by 'BinaryWriter', making sure that
// the output buffer can be safely passed to C code and cast to the equivalent
// C structes. It also makes sure that the written data follows the native
// (or given) byte order.
// Our 'Test' struct could be written using this class like so:
//
// val bw = BinaryWriter()
// bw.putStruct { it
//     .putInt(1)     // writes: (int32_t) 1 [value]
//     .putLong(2)    // writes: (int32_t) 0 [padding], (int64_t) 2 [value]
//     .putInt(3)     // writes: (int32_t) 3 [value]
// }                  // writes: (int32_t) 0 [padding]
//
class BinaryWriter(byteOrder: ByteOrder = ByteOrder.nativeOrder()) {
    
    private val temp = ByteBuffer.allocate(8).order(byteOrder)
    
    val output = ByteArrayOutputStream()
        
    var offset: Int = 0
        private set

    private val structAlignments: MutableList<Int> = mutableListOf()

    fun beginStruct() {
        this.structAlignments.add(1)
    }
    
    private fun updateStructAlignment(alignment: Int) {
        if (this.structAlignments.isEmpty()) { return }
        val lastI: Int = this.structAlignments.size - 1
        this.structAlignments[lastI] = maxOf(
            this.structAlignments[lastI], alignment
        )
    }
    
    fun endStruct() {
        val structAlignment: Int = this.structAlignments.removeLast()
        this.alignTo(structAlignment)
    }

    inline fun putStruct(crossinline f: (BinaryWriter) -> Unit): BinaryWriter {
        this.beginStruct()
        f(this)
        this.endStruct()
        return this
    }
    
    fun alignTo(alignment: Int) {
        this.updateStructAlignment(alignment)
        val afterAlign: Int = this.offset % alignment
        if (afterAlign == 0) { return }
        val padding: Int = alignment - afterAlign
        for (i in 0..<padding) {
            this.output.write(0)
        }
        this.offset += padding
    }
    
    private fun writeTemp() {
        this.temp.flip()
        for (i in 0..<this.temp.remaining()) {
            this.output.write(this.temp.get(i).toInt())
        }
        this.offset += this.temp.remaining()
        this.temp.clear()
    }
    
    fun putByte(b: Byte): BinaryWriter {
        this.output.write(b.toInt())
        this.offset += 1
        return this
    }
    
    fun putShort(s: Short): BinaryWriter {
        this.alignTo(2)
        this.temp.putShort(s)
        this.writeTemp()
        return this
    }
    
    fun putInt(i: Int): BinaryWriter {
        this.alignTo(4)
        this.temp.putInt(i)
        this.writeTemp()
        return this
    }
    
    fun putLong(l: Long): BinaryWriter {
        this.alignTo(8)
        this.temp.putLong(l)
        this.writeTemp()
        return this
    }
    
    fun putFloat(f: Float): BinaryWriter {
        this.alignTo(4)
        this.temp.putFloat(f)
        this.writeTemp()
        return this
    }
    
    fun putDouble(d: Double): BinaryWriter {
        this.alignTo(8)
        this.temp.putDouble(d)
        this.writeTemp()
        return this
    }
    
    fun putWideStringNoTerm(s: String): BinaryWriter {
        for (c in s) {
            this.putShort(c.code.toShort())
        }
        return this
    }
    
    fun putBytesNoTerm(b: ByteArray): BinaryWriter {
        this.output.write(b)
        this.offset += b.size
        return this
    }
    
}
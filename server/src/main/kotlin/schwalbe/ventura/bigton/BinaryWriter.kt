
package schwalbe.ventura.bigton

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryWriter {
    
    private val temp = ByteBuffer.allocate(8)
        .order(ByteOrder.nativeOrder())
    
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
    
    fun putByte(b: Byte) {
        this.output.write(b.toInt())
        this.offset += 1
    }
    
    fun putShort(s: Short) {
        this.alignTo(2)
        this.temp.putShort(s)
        this.writeTemp()
    }
    
    fun putInt(i: Int) {
        this.alignTo(4)
        this.temp.putInt(i)
        this.writeTemp()
    }
    
    fun putLong(l: Long) {
        this.alignTo(8)
        this.temp.putLong(l)
        this.writeTemp()
    }
    
    fun putFloat(f: Float) {
        this.alignTo(4)
        this.temp.putFloat(f)
        this.writeTemp()
    }
    
    fun putDouble(d: Double) {
        this.alignTo(8)
        this.temp.putDouble(d)
        this.writeTemp()
    }
    
    fun putString(s: String) {
        for (c in s) {
            this.putShort(c.code.toShort())
        }
    }
    
    fun putBytes(b: ByteArray) {
        this.output.write(b)
        this.offset += b.size
    }
    
}
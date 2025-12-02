
import java.net.Socket
import java.util.Scanner

fun main() {
    val connection = Socket("localhost", 9999)
    connection.tcpNoDelay = true
    val incoming = Scanner(connection.inputStream)
    connection.outputStream.write("Hello, Ventura!\n".toByteArray())
    connection.outputStream.flush()
    while (incoming.hasNextLine()) {
        println(incoming.nextLine())
    }
    connection.close()
}
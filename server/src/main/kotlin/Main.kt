
import java.net.ServerSocket
import java.net.Socket
import java.util.Scanner
import org.joml.Matrix4f

fun main() {
    val server = ServerSocket(9999)
    val connection: Socket = server.accept()
    connection.tcpNoDelay = true
    val incoming = Scanner(connection.inputStream)
    while (incoming.hasNextLine()) {
        var line: String = incoming.nextLine()
        println(line)
        connection.outputStream.write(line.toByteArray())
        connection.outputStream.write("\n".toByteArray())
    }
    connection.close()
}
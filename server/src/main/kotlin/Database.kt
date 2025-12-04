
package schwalbe.ventura.server

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


const val DEFAULT_DB_URL: String = "jdbc:postgresql://localhost:5432/ventura"
const val DEFAULT_DB_USER: String = "ventura"

fun getDbUrl(): String = System.getenv("VENTURA_DB_URL")
    ?: DEFAULT_DB_URL

fun getDbUser(): String = System.getenv("VENTURA_DB_USER")
    ?: DEFAULT_DB_USER

fun getDbPass(): String = System.getenv("VENTURA_DB_PASS")
    ?: throw IllegalArgumentException(
        "The 'VENTURA_DB_PASS' environment variable was not specified"
    )

fun connectDatabase() {
    Database.connect(
        url = getDbUrl(),
        driver = "org.postgresql.Driver",
        user = getDbUser(),
        password = getDbPass()
    )
}


object Accounts : Table("accounts") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 32).uniqueIndex()
    override val primaryKey = PrimaryKey(id)
}

fun initDatabase() {
    transaction {
        SchemaUtils.create(Accounts)
    }
}
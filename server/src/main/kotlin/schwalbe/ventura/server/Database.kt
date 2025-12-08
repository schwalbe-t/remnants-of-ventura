
package schwalbe.ventura.server.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.kotlin.datetime.datetime


fun initDatabase() {
    connect()
    initTables()
}


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

private fun connect() {
    val url: String = getDbUrl()
    val user: String = getDbUser()
    Database.connect(
        url = url,
        driver = "org.postgresql.Driver",
        user = user,
        password = getDbPass()
    )
    println("Connected to PostgreSQL database at URL '$url' as user '$user'")
}


const val ACCOUNT_NAME_MAX_LEN: Int = 32
const val ACCOUNT_SALT_MAX_LEN: Int = 16
const val ACCOUNT_HASH_MAX_LEN: Int = 32

object AccountsTable : Table("accounts") {
    val username = varchar("username", ACCOUNT_NAME_MAX_LEN)
    val salt = binary("salt", ACCOUNT_SALT_MAX_LEN)
    val hash = binary("hash", ACCOUNT_HASH_MAX_LEN)
    val userdata = blob("userdata")
    val isOnline = bool("is_online")
    override val primaryKey = PrimaryKey(username)
}

object SessionsTable: Table("sessions") {
    val token = uuid("token")
    val username = varchar("username", ACCOUNT_NAME_MAX_LEN)
        .index(isUnique = false)
    val expiration = datetime("expiration")
    override val primaryKey = PrimaryKey(token)
}

private fun initTables() {
    transaction {
        SchemaUtils.create(AccountsTable)
        SchemaUtils.create(SessionsTable)
    }
}
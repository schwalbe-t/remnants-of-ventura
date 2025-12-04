
package schwalbe.ventura.server.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID


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
    println("Connected to PostgreSQL database at $url as $user")
}


object Accounts : Table("accounts") {
    val username = varchar("username", 32)
    val salt = binary("salt", 16)
    val hash = binary("hash", 32)
    override val primaryKey = PrimaryKey(username)
}

private fun initTables() {
    transaction {
        SchemaUtils.create(Accounts)
    }
}


data class AccountInfo(
    val username: String,
    val salt: ByteArray,
    val hash: ByteArray
)

fun insertNewAccount(info: AccountInfo)
    = transaction {
        Accounts.insert {
            it[Accounts.username] = info.username
            it[Accounts.salt] = info.salt
            it[Accounts.hash] = info.hash
        }
    }

private fun ResultRow.toAccountInfo() = AccountInfo(
    username = this[Accounts.username],
    salt = this[Accounts.salt],
    hash = this[Accounts.hash]
)

fun findAccountInfo(searchName: String): AccountInfo?
    = transaction { Accounts
        .selectAll()
        .where({ Accounts.username eq searchName })
        .singleOrNull()
        ?.toAccountInfo()
    }

fun updateAccountPassword(searchName: String, salt: ByteArray, hash: ByteArray)
    = transaction {
        Accounts.update({ Accounts.username eq searchName }) {
            it[Accounts.salt] = salt
            it[Accounts.hash] = hash
        }
    }

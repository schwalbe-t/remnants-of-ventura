
package schwalbe.ventura.server.persistence

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.*
import java.util.UUID

object ServerNetwork {
    val thisServerId: UUID = UUID.randomUUID()

    val SERVER_REPORT_INTERVAL: Duration = 10.seconds
    val SERVER_EXPIRATION_DELAY = DateTimePeriod(seconds = 20)
}

private fun ServerNetwork.getNewExpiration(): LocalDateTime
    = Clock.System.now()
    .plus(SERVER_EXPIRATION_DELAY, TimeZone.UTC)
    .toLocalDateTime(TimeZone.UTC)

fun ServerNetwork.registerServer() {
    transaction {
        ServersTable.insert {
            it[ServersTable.id] = ServerNetwork.thisServerId
            it[ServersTable.expiration] = ServerNetwork.getNewExpiration()
        }
    }
}

fun ServerNetwork.reportServerOnline(): Boolean {
    val now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val updates: Int = transaction {
        ServersTable.update({
            (ServersTable.id eq ServerNetwork.thisServerId) and
            (ServersTable.expiration greater now)
        }) {
            it[ServersTable.expiration] = ServerNetwork.getNewExpiration()
        }
    }
    return updates >= 1
}

fun ServerNetwork.deleteAllExpired() {
    val now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    transaction {
        ServersTable.deleteWhere { ServersTable.expiration lessEq now }
    }
}

fun ServerNetwork.tryAcquireAccount(username: String): Boolean {
    val sql = """
        UPDATE accounts
        SET owning_server = ?
        WHERE accounts.username = ?
        AND (
            accounts.owning_server IS NULL
            OR NOT EXISTS (
                SELECT 1 FROM servers
                WHERE servers.id = accounts.owning_server
            )
            OR EXISTS (
                SELECT 1 FROM servers
                WHERE servers.id = accounts.owning_server
                AND servers.expiration < (NOW() AT TIME ZONE 'UTC')
            )
        )
    """.trimIndent()
    val numAffectedRows: Int = transaction {
        val conn = TransactionManager.current().connection
        val stmt = conn.prepareStatement(sql, returnKeys = false)
        stmt.fillParameters(listOf(
            Pair(UUIDColumnType(), ServerNetwork.thisServerId),
            Pair(VarCharColumnType(), username)
        ))
        stmt.executeUpdate()
    }
    return numAffectedRows >= 1
}

fun ServerNetwork.releaseAccount(username: String) {
    transaction {
        AccountsTable.update({ AccountsTable.username eq username }) {
            it[AccountsTable.owningServer] = null
        }
    }
}
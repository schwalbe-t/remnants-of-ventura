
package schwalbe.ventura.server.persistence

import schwalbe.ventura.server.database.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.*
import kotlinx.datetime.*
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

class Session {
    companion object {
        val EXPIRATION_DELAY = DateTimePeriod(days = 30)
    }
}

fun Session.Companion.create(username: String): Uuid? {
    val token = Uuid.random()
    val expiration: LocalDateTime = Clock.System.now()
        .plus(Session.EXPIRATION_DELAY, TimeZone.UTC)
        .toLocalDateTime(TimeZone.UTC)
    try {
        transaction { SessionsTable.insert {
            it[SessionsTable.token] = token.toJavaUuid()
            it[SessionsTable.username] = username
            it[SessionsTable.expiration] = expiration
        } }
    } catch (e: ExposedSQLException) {
        return null
    }
    return token
}

fun Session.Companion.getSessionUser(token: Uuid): String? {
    val now: LocalDateTime = Clock.System.now()
        .toLocalDateTime(TimeZone.UTC)
    val jToken = token.toJavaUuid()
    return transaction { SessionsTable
        .select(SessionsTable.username)
        .where {
            (SessionsTable.token eq jToken) and
            (SessionsTable.expiration greater now)
        }
        .firstOrNull()
        ?.let { it[SessionsTable.username] }
    }
}

fun Session.Companion.deleteAllForUser(username: String) {
    transaction {
        SessionsTable.deleteWhere { SessionsTable.username eq username }
    }
}

fun Session.Companion.deleteAllExpired() {
    val now: LocalDateTime = Clock.System.now()
        .toLocalDateTime(TimeZone.UTC)
    transaction {
        SessionsTable.deleteWhere { SessionsTable.expiration lessEq now }
    }
}
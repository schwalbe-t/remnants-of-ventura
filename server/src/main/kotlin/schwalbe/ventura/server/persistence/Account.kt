
package schwalbe.ventura.server.persistence

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import kotlinx.datetime.*
import schwalbe.ventura.ACCOUNT_NAME_MAX_LEN
import schwalbe.ventura.ACCOUNT_NAME_MIN_LEN
import schwalbe.ventura.ACCOUNT_PASSWORD_MAX_LEN
import schwalbe.ventura.ACCOUNT_PASSWORD_MIN_LEN
import java.security.SecureRandom

object Account {
    val SESSION_CREATION_COOLDOWN = DateTimePeriod(minutes = 1)
}

private fun generateAccountSalt(): ByteArray {
    val salt = ByteArray(ACCOUNT_SALT_MAX_LEN)
    SecureRandom().nextBytes(salt)
    return salt
}

private fun generatePasswordHash(password: String, salt: ByteArray): ByteArray {
    val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
        .withSalt(salt)
        .withParallelism(1)
        .withMemoryAsKB(1024 * 64) // 64 MB
        .withIterations(3)
        .build()
    val generator = Argon2BytesGenerator()
    generator.init(params)
    val passwordBytes: ByteArray = password.toByteArray(Charsets.UTF_8)
    val hash = ByteArray(ACCOUNT_HASH_MAX_LEN)
    generator.generateBytes(passwordBytes, hash)
    passwordBytes.fill(0)
    return hash
}

private fun assertCredentialsLength(
    username: String, password: String
): Boolean =
    username.length in ACCOUNT_NAME_MIN_LEN..ACCOUNT_NAME_MAX_LEN &&
    password.length in ACCOUNT_PASSWORD_MIN_LEN..ACCOUNT_PASSWORD_MAX_LEN

fun Account.create(
    username: String, password: String, playerData: ByteArray
): Boolean {
    if (!assertCredentialsLength(username, password)) { return false }
    val salt: ByteArray = generateAccountSalt()
    val hash: ByteArray = generatePasswordHash(password, salt)
    val now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    try {
        transaction { AccountsTable.insert {
            it[AccountsTable.username] = username
            it[AccountsTable.salt] = salt
            it[AccountsTable.hash] = hash
            it[AccountsTable.sessionCooldownUntil] = now
            it[AccountsTable.userdata] = ExposedBlob(playerData)
            it[AccountsTable.owningServer] = null
        } }
    } catch (e: ExposedSQLException) {
        return false
    }
    return true
}

private fun hashesEqual(a: ByteArray, b: ByteArray): Boolean {
    if (a.size != b.size) { return false }
    var e: Int = 0
    for (i in a.indices) {
        e = e or (a[i].toInt() xor b[i].toInt())
    }
    return e == 0
}

fun Account.hasMatchingPassword(
    username: String, password: String
): Boolean {
    if (!assertCredentialsLength(username, password)) { return false }
    val (salt, reqHash) = transaction { AccountsTable
        .select(AccountsTable.salt, AccountsTable.hash)
        .where { AccountsTable.username eq username }
        .firstOrNull()
        ?.let { Pair(it[AccountsTable.salt], it[AccountsTable.hash]) }
    } ?: return false
    val gotHash: ByteArray = generatePasswordHash(password, salt)
    val matches: Boolean = hashesEqual(gotHash, reqHash)
    reqHash.fill(0)
    gotHash.fill(0)
    return matches
}

fun Account.fetchPlayerData(username: String): ByteArray?
    = transaction { AccountsTable
        .select(AccountsTable.userdata)
        .where { AccountsTable.username eq username }
        .firstOrNull()
        ?.let { it[AccountsTable.userdata].bytes }
    }

fun Account.writePlayerData(username: String, playerData: ByteArray) {
    transaction {
        AccountsTable.update({ AccountsTable.username eq username }) {
            it[AccountsTable.userdata] = ExposedBlob(playerData)
        }
    }
}

fun Account.tryApplyLoginCooldown(username: String): Boolean {
    val now: LocalDateTime = Clock.System.now()
        .toLocalDateTime(TimeZone.UTC)
    val mayLogin: Boolean = transaction { AccountsTable
        .select(AccountsTable.sessionCooldownUntil)
        .where { AccountsTable.username eq username }
        .firstOrNull()
        ?.let { it[AccountsTable.sessionCooldownUntil] <= now }
        ?: false
    }
    if (!mayLogin) { return false }
    val newCooldown: LocalDateTime = Clock.System.now()
        .plus(Account.SESSION_CREATION_COOLDOWN, TimeZone.UTC)
        .toLocalDateTime(TimeZone.UTC)
    transaction {
        AccountsTable.update({ AccountsTable.username eq username }) {
            it[AccountsTable.sessionCooldownUntil] = newCooldown
        }
    }
    return true
}
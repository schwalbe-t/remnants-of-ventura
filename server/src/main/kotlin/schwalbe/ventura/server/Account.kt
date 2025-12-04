
package schwalbe.ventura.server

import schwalbe.ventura.server.database.*
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import java.security.SecureRandom

class Account {
    companion object {}
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

fun Account.Companion.create(username: String, password: String): Boolean {
    val salt: ByteArray = generateAccountSalt()
    val hash: ByteArray = generatePasswordHash(password, salt)
    try {
        transaction { AccountsTable.insert {
            it[AccountsTable.username] = username
            it[AccountsTable.salt] = salt
            it[AccountsTable.hash] = hash
        } }
    } catch (e: ExposedSQLException) {
        return false
    }
    hash.fill(0)
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

fun Account.Companion.hasMatchingPassword(
    username: String, password: String
): Boolean {
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

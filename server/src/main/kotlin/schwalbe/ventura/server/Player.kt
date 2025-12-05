
package schwalbe.ventura.server

import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.joml.Vector3f

@Serializable
data class PlayerData(
    val worlds: ArrayDeque<PlayerData.WorldEntry>
) {
    
    @Serializable
    data class WorldEntry(
        val worldId: Int
    )

}

val PLAYER_MIN_SAVE_COOLDOWN = DateTimePeriod(minutes = 3)

class Player(val username: String, val data: PlayerData) {

    private var nextSaveTime: Instant = Clock.System.now()

    init {
        this.saveLater()
    }

    fun saveAsap() {
        synchronized(this) {
            nextSaveTime = Clock.System.now()
        }
    }

    fun saveLater() {
        synchronized(this) {
            nextSaveTime = Clock.System.now()
                .plus(PLAYER_MIN_SAVE_COOLDOWN, TimeZone.UTC)
        }
    }

    fun savingRequired(now: Instant): Boolean {
        synchronized(this) {
            return now >= this.nextSaveTime
        }
    }

}
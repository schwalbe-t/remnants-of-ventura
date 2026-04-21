
package schwalbe.ventura.client

import schwalbe.ventura.data.TrackList
import schwalbe.ventura.engine.ResourceLoader
import schwalbe.ventura.engine.audio.Audio
import schwalbe.ventura.engine.audio.loadOgg
import schwalbe.ventura.engine.audio.tracklistOf

object Soundtrack {

    val buriedSignals = Audio.loadOgg("res/ost/buried_signals.ogg")
    val hauntedDecay = Audio.loadOgg("res/ost/haunted_decay.ogg")
    val uncertainFuture = Audio.loadOgg("res/ost/uncertain_future.ogg")

    fun submitResources(loader: ResourceLoader): Unit = loader.submitAll(
        buriedSignals, hauntedDecay, uncertainFuture
    )

    val TRACK_LISTS: Map<TrackList, List<() -> Audio>> = mapOf(
        TrackList.MAIN_MENU to tracklistOf(
            uncertainFuture
        ),
        TrackList.SURFACE to tracklistOf(
            uncertainFuture, hauntedDecay
        ),
        TrackList.RUINS to tracklistOf(
            buriedSignals, hauntedDecay
        )
    )

    operator fun get(list: TrackList): List<() -> Audio>
        = TRACK_LISTS[list] ?: listOf()

}


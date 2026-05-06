
package schwalbe.ventura.data

import kotlinx.serialization.Serializable

@Serializable
data class RemoteLocalization(
    val dialogue: Map<String, List<Dialogue>>
) {
    @Serializable
    data class Dialogue(
        val name: String,
        val lines: List<String>
    )
}

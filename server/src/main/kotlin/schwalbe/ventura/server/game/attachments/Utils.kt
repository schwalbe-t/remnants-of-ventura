
package schwalbe.ventura.server.game.attachments

import schwalbe.ventura.bigton.runtime.*

fun BigtonRuntime.reportDynError(reason: String) {
    this.logLine("ERROR: $reason")
    this.error = BigtonRuntimeError.BY_PROGRAM
}

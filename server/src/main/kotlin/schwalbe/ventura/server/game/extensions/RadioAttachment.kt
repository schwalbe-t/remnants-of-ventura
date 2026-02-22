
package schwalbe.ventura.server.game.extensions

import kotlinx.serialization.Serializable
import schwalbe.ventura.bigton.BigtonModule
import schwalbe.ventura.bigton.runtime.*

@Serializable
data class RadioAttachmentState(
    var channel: Long = 0,
    val messages: MutableList<String> = mutableListOf()
) : GameAttachment {
    companion object {
        val TYPE = AttachmentStates.register(::RadioAttachmentState)
        const val MAX_BUFFER_SIZE: Int = 32
    }
}

val RADIO_ATTACHMENT_MODULE = BigtonModule(BIGTON_MODULES.functions)
    .withCtxFunction("radioSetChannel", cost = 1, argc = 1) { r, ctx ->
        val state = ctx.robot.attachmentStates[RadioAttachmentState.TYPE]
        state.channel = r.popStack()
            ?.use { when (it) {
                is BigtonInt -> it.value
                else -> return@withCtxFunction r.reportDynError(
                    "The given channel number was not an integer"
                )
            } }
            ?: 0
        BigtonNull.create().use(r::pushStack)
    }
    .withCtxFunction("radioGetChannel", cost = 1, argc = 0) { r, ctx ->
        val state = ctx.robot.attachmentStates[RadioAttachmentState.TYPE]
        BigtonInt.fromValue(state.channel).use(r::pushStack)
    }
    .withCtxFunction("radioSend", cost = 1, argc = 1) { r, ctx ->
        val state = ctx.robot.attachmentStates[RadioAttachmentState.TYPE]
        val message: String = r.popStack()
            ?.use { when (it) {
                is BigtonString -> it.value
                else -> return@withCtxFunction r.reportDynError(
                    "The given message was not an integer"
                )
            } }
            ?: ""
        val maxNumMessages: Int = RadioAttachmentState.MAX_BUFFER_SIZE
        for (recRobot in ctx.player.data.deployedRobots.values) {
            val recState = recRobot.attachmentStates[RadioAttachmentState.TYPE]
            if (recRobot.id == ctx.robot.id) { continue }
            if (recState.channel != state.channel) { continue }
            val messages: MutableList<String> = recState.messages
            messages.add(message)
            if (messages.size > maxNumMessages) {
                messages.subList(0, messages.size - maxNumMessages).clear()
            }
        }
        BigtonNull.create().use(r::pushStack)
    }
    .withCtxFunction("radioReceive", cost = 1, argc = 0) { r, ctx ->
        val state = ctx.robot.attachmentStates[RadioAttachmentState.TYPE]
        val received: String? = state.messages.removeFirstOrNull()
        if (received == null) {
            BigtonNull.create().use(r::pushStack)
        } else {
            BigtonString.fromValue(received, r).use(r::pushStack)
        }
    }
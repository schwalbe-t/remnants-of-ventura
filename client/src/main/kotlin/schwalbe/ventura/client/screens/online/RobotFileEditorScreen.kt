
package schwalbe.ventura.client.screens.online

import org.joml.Vector3f
import schwalbe.ventura.client.*
import schwalbe.ventura.client.game.*
import schwalbe.ventura.client.screens.GameScreen
import schwalbe.ventura.engine.input.*
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.net.Packet
import schwalbe.ventura.net.PacketType
import schwalbe.ventura.net.SharedRobotInfo
import schwalbe.ventura.utils.toVector3f
import java.nio.file.Path
import kotlin.uuid.Uuid

private const val MAX_CROUCH_DISTANCE: Float = 2f

fun robotFileEditorScreen(
    client: Client, robotId: Uuid
): () -> GameScreen = {
    val world: World? = client.world
    val sharedRobotInfo: SharedRobotInfo?
        = world?.state?.lastReceived?.allRobots[robotId]
    fun playerToRobot(): Vector3f? {
        if (world == null || sharedRobotInfo == null) { return null }
        return sharedRobotInfo.position.toVector3f()
            .sub(world.player.position)
    }
    val autoRestart: Boolean = client.config.settings.autoRestartEnabled
    val playerDist: Float = playerToRobot()?.length() ?: Float.MAX_VALUE
    val playerSquat: Boolean = playerDist <= MAX_CROUCH_DISTANCE
    val file: Path = SourceFiles.getRobotSourceFile(robotId)
    val editorTitle: String = sharedRobotInfo?.name ?: robotId.toString()
    val editor: CodeEditor? = CodeEditor.openFile(file.toFile(), editorTitle)
    val screen = PausedScreen(
        client,
        camMode = { w -> CameraModes.playerInRightThird(w.player) },
        onClose = {
            editor?.save()
            SourceFiles.uploadFileContents(client, file)
            if (autoRestart) {
                client.network.outPackets?.send(Packet.serialize(
                    PacketType.RESTART_ROBOT, robotId
                ))
            }
        },
        closeIf = { Key.ESCAPE.wasPressed || Key.C.wasPressed },
        playerFollowCursor = !playerSquat,
        playerAnim = if (playerSquat) PlayerAnim.squat else PlayerAnim.thinking,
        render = {
            val playerToRobot: Vector3f? = playerToRobot()
            if (playerSquat && playerToRobot != null) {
                world?.player?.rotateAlong(playerToRobot)
            }
            editor?.update()
        }
    )
    screen.screen.add(layer = 0, element = Axis.row()
        .add(66.6.vw, Stack()
            .add(screen.background)
            .add(FlatBackground().withColor(CodeEditor.BACKGROUND_COLOR))
            .add(editor?.root ?: Space())
        )
    )
    screen.screen
}

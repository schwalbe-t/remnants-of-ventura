
package schwalbe.ventura.client.screens.online

import schwalbe.ventura.ACCOUNT_NAME_MAX_LEN
import schwalbe.ventura.ACCOUNT_NAME_MIN_LEN
import schwalbe.ventura.ACCOUNT_PASSWORD_MAX_LEN
import schwalbe.ventura.ACCOUNT_PASSWORD_MIN_LEN
import schwalbe.ventura.engine.ui.*
import schwalbe.ventura.client.*
import schwalbe.ventura.client.LocalKeys.*
import schwalbe.ventura.client.game.addErrorLogging
import schwalbe.ventura.client.screens.*
import schwalbe.ventura.client.screens.offline.serverConnectionFailedScreen
import schwalbe.ventura.net.*

fun serverAuthenticationScreen(
    name: String, client: Client
): () -> GameScreen = {
    val l = localized()
    val statusText = Text()
    var username: String = ""
    var password: String = ""
    val packets = PacketHandler.receiveDownPackets<Unit>()
        .addErrorLogging()
        .onPacket(PacketType.TAGGED_ERROR) { p: TaggedErrorPacket, _ ->
            when (p) {
                TaggedErrorPacket.INVALID_ACCOUNT_PARAMS -> {
                    statusText.withText(l[ERROR_INVALID_ACCOUNT_PARAMS])
                }
                TaggedErrorPacket.INVALID_ACCOUNT_CREDS -> {
                    statusText.withText(l[ERROR_INVALID_ACCOUNT_CREDS])
                }
                TaggedErrorPacket.SESSION_CREATION_COOLDOWN -> {
                    statusText.withText(l[ERROR_SESSION_CREATION_COOLDOWN])
                }
                TaggedErrorPacket.INVALID_SESSION_CREDS -> {
                    statusText.withText(l[ERROR_INVALID_SESSION_CREDS])
                }
                TaggedErrorPacket.ACCOUNT_ALREADY_ONLINE -> {
                    statusText.withText(l[ERROR_ACCOUNT_ALREADY_ONLINE])
                }
                else -> {
                    println("[error] ${p.name}")
                    return@onPacket
                }
            }
            client.config.sessions.remove(name)
            client.config.write()
        }
        .onPacket(PacketType.CREATE_ACCOUNT_SUCCESS) { _, _ ->
            println("Created account with username '$username'")
            println("Logging in as user '$username'")
            client.network.outPackets?.send(Packet.serialize(
                PacketType.CREATE_SESSION,
                AccountCredPacket(username, password)
            ))
        }
        .onPacket(PacketType.CREATE_SESSION_SUCCESS) { token, _ ->
            println("Created session for user '$username'")
            client.config.sessions[name] = Config.Session(
                username, token = token.token
            )
            client.config.write()
            client.network.outPackets?.send(Packet.serialize(
                PacketType.LOGIN_SESSION,
                SessionCredPacket(username, token.token)
            ))
        }
        .onPacket(PacketType.LOGIN_SESSION_SUCCESS) { _, _ ->
            println("Logged in as user '$username'")
            client.username = username
            client.nav.replace(controllingPlayerScreen(client))
        }
    val screen = GameScreen(
        render = renderGridBackground(client),
        networkState = keepNetworkConnectionAlive(client, onFail = { reason ->
            client.nav.replace(serverConnectionFailedScreen(reason, client))
            client.network.clearError()
        }),
        packets = packets,
        client.nav
    )
    val savedSession: Config.Session? = client.config.sessions[name]
    if (savedSession != null) {
        username = savedSession.username
        println("Using saved session for user '${savedSession.username}'")
        client.network.outPackets?.send(Packet.serialize(
            PacketType.LOGIN_SESSION,
            SessionCredPacket(savedSession.username, savedSession.token)
        ))
    }
    val passwordDisp: (String) -> String = { "●".repeat(it.length) }
    val login = Axis.column()
    login.add(7.ph, Text()
        .withFont(googleSansSb())
        .withText(localized()[TITLE_LOGIN])
        .withSize(2.5.vmin)
    )
    val loginUsername = addLabelledInput(
        login, l[LABEL_USERNAME], l[PLACEHOLDER_USERNAME],
        "", googleSansR(), maxLength = ACCOUNT_NAME_MAX_LEN
    )
    val loginPassword = addLabelledInput(
        login, l[LABEL_PASSWORD], l[PLACEHOLDER_PASSWORD],
        "", googleSansR(), passwordDisp, maxLength = ACCOUNT_PASSWORD_MAX_LEN
    )
    login.add(6.vmin,
        createTextButton(
            content = l[BUTTON_LOG_IN],
            handler = {
                username = loginUsername.valueString.trim()
                password = loginPassword.valueString
                println("Logging in as user '$username'")
                client.network.outPackets?.send(Packet.serialize(
                    PacketType.CREATE_SESSION,
                    AccountCredPacket(username, password)
                ))
            }
        ).pad(top = 2.vmin, left = 30.pw, right = 30.pw)
    )
    val signUp = Axis.column()
    signUp.add(7.ph, Text()
        .withFont(googleSansSb())
        .withText(localized()[TITLE_SIGN_UP])
        .withSize(2.5.vmin)
    )
    val signUpUsername = addLabelledInput(
        signUp, l[LABEL_USERNAME], l[PLACEHOLDER_USERNAME],
        "", googleSansR(), maxLength = ACCOUNT_NAME_MAX_LEN
    )
    val signUpPassword = addLabelledInput(
        signUp, l[LABEL_PASSWORD], l[PLACEHOLDER_PASSWORD],
        "", googleSansR(), passwordDisp, maxLength = ACCOUNT_PASSWORD_MAX_LEN
    )
    val signUpPasswordRepeat = addLabelledInput(
        signUp, l[LABEL_REPEAT_PASSWORD], l[PLACEHOLDER_REPEAT_PASSWORD],
        "", googleSansR(), passwordDisp, maxLength = ACCOUNT_PASSWORD_MAX_LEN
    )
    signUp.add(6.vmin,
        createTextButton(
            content = l[BUTTON_SIGN_UP],
            handler = signUp@{
                username = signUpUsername.valueString.trim()
                password = signUpPassword.valueString
                if (username.length < ACCOUNT_NAME_MIN_LEN) {
                    statusText.withText(l[ERROR_USERNAME_TOO_SHORT])
                    return@signUp
                }
                if (password.length < ACCOUNT_PASSWORD_MIN_LEN) {
                    statusText.withText(l[ERROR_PASSWORD_TOO_SHORT])
                    return@signUp
                }
                if (signUpPasswordRepeat.valueString != password) {
                    statusText.withText(l[ERROR_PASSWORDS_DONT_MATCH])
                    return@signUp
                }
                println("Creating account with username '$username'")
                client.network.outPackets?.send(Packet.serialize(
                    PacketType.CREATE_ACCOUNT,
                    AccountCredPacket(username, password)
                ))
            }
        ).pad(top = 2.vmin, left = 30.pw, right = 30.pw)
    )
    screen.add(layer = 0, element = Axis.column()
        .add(100.ph - 8.vmin, Axis.row()
            .add(50.pw - 2.5.vmin, login)
            .add(5.vmin, Space())
            .add(50.pw - 2.5.vmin, signUp)
        )
        .add(3.vmin, statusText
            .withFont(googleSansSb())
            .withSize(2.vmin)
            .alignCenter()
        )
        .add(5.vmin, createTextButton(
            content = localized()[BUTTON_DISCONNECT],
            handler = {
                client.nav.pop()
            }
        ).pad(top = 1.vmin, right = 100.pw - 30.vmin))
        .pad(5.vmin)
    )
    screen
}

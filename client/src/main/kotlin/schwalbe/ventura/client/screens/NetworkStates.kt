
package schwalbe.ventura.client.screens

import schwalbe.ventura.client.*

fun noNetworkConnections(client: Client): () -> Unit = {
    when (val s = client.network.state) {
        is NetworkClient.Idle -> {}
        is NetworkClient.Connecting -> {}
        is NetworkClient.Connected -> {
            client.network.disconnect()
        }
        is NetworkClient.ExceptionError -> {
            // IGNORE
        }
    }
}

fun establishNetworkConnection(
    client: Client,
    onSuccess: () -> Unit,
    onFail: (message: String) -> Unit
): () -> Unit = {
    when (val s = client.network.state) {
        is NetworkClient.Idle -> {
            onFail("Unknown Error")
        }
        is NetworkClient.Connecting -> {}
        is NetworkClient.Connected -> onSuccess()
        is NetworkClient.ExceptionError -> {
            onFail(s.e.toString())
        }
    }
}

fun keepNetworkConnectionAlive(
    client: Client,
    onFail: (message: String) -> Unit
): () -> Unit = {
    when (val s = client.network.state) {
        is NetworkClient.Idle -> {
            onFail("Unknown Error")
        }
        is NetworkClient.Connecting -> {}
        is NetworkClient.Connected -> {}
        is NetworkClient.ExceptionError -> {
            onFail(s.e.toString())
        }
    }
}
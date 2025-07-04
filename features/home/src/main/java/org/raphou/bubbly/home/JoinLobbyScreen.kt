package org.raphou.bubbly.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun JoinLobbyScreen(navController: NavHostController, code: String) {
    val viewModel: JoinLobbyScreenViewModel = viewModel()
    val lobby = viewModel.currentSession.collectAsState().value
    val players = viewModel.players.collectAsState().value

    LaunchedEffect(code) {
        viewModel.joinLobby(code)
    }

    LaunchedEffect(true) {
        viewModel.navigateToGameChannel.collect { event ->
            when (event) {
                is JoinLobbyNavigationEvent.NavigateToGame -> {
                    navController.navigate("game/${event.lobbyId}/theme/${event.themeId}") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    LobbyContent(lobby = lobby, onBack = { navController.popBackStack() }, players)
}

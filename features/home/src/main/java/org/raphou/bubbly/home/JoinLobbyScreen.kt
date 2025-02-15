package org.raphou.bubbly.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun JoinLobbyScreen(navController: NavHostController, code: String) {
    val viewModel: LobbyScreenViewModel = viewModel()
    val lobby = viewModel.currentSession.collectAsState().value
    val players = viewModel.players.collectAsState().value
    val isGameStarted = viewModel.isGameStarted.collectAsState().value

    LaunchedEffect(code) {
        viewModel.joinLobby(code)
    }

    LaunchedEffect(isGameStarted) {
        if (isGameStarted) {
            navController.navigate("game/${lobby?.id}/false")
        }
    }

    LobbyContent(lobby = lobby, onBack = { navController.popBackStack() }, players)
}



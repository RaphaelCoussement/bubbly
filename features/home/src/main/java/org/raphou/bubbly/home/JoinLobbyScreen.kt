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

    LaunchedEffect(code) {
        viewModel.joinLobby(code)
    }

    LobbyContent(lobby = lobby, onBack = { navController.popBackStack() })
}

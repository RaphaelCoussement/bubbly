package org.raphou.bubbly.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.SharedFlow
import org.raphou.bubbly.ui.R


@Composable
fun CreateLobbyScreen(navController: NavHostController, themeId: String?) {
    val viewModel: CreateLobbyScreenViewModel = viewModel()
    val lobby = viewModel.currentSession.collectAsState().value
    val players by viewModel.players.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is CreateLobbyNavigationEvent.NavigateToGame -> {
                    val finalThemeId = event.themeId ?: "default"
                    navController.navigate("game/${event.lobbyId}/theme/$finalThemeId") {
                        popUpTo(0) { inclusive = true }
                    }
                }

                is CreateLobbyNavigationEvent.NavigateToHome -> {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.createLobby(themeId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LobbyContent(lobby = lobby, onBack = { navController.popBackStack() }, players)

        FloatingActionButton(
            onClick = { viewModel.startGame() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .size(72.dp),
            containerColor = colorResource(id = R.color.orange_primary),
            shape = MaterialTheme.shapes.medium,
            elevation = FloatingActionButtonDefaults.elevation(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start",
                tint = Color.White
            )
        }
    }
}




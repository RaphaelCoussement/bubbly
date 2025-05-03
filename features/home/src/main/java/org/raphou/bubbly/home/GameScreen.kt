package org.raphou.bubbly.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.raphou.bubbly.game.FirstPlayerScreen
import org.raphou.bubbly.game.OtherPlayerScreen
import org.raphou.bubbly.ui.R

@Composable
fun GameScreen(navController: NavController, lobbyId: String, themeId: String?) {
    val viewModel: GameScreenViewModel = viewModel()
    val screenState = viewModel.screenState.collectAsState()

    LaunchedEffect(lobbyId) {
        viewModel.init(lobbyId)
    }

    when (val state = screenState.value) {
        is GameScreenState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(id = R.color.orange_primary))
            }
        }
        is GameScreenState.FirstPlayer -> {
            FirstPlayerScreen(navController = navController, lobbyId = lobbyId, themeId = themeId)
        }
        is GameScreenState.OtherPlayer -> {
            OtherPlayerScreen(navController = navController, lobbyId = lobbyId, themeId = themeId)
        }
        is GameScreenState.Finish -> {
            // Redirection vers le classement final
            LaunchedEffect(Unit) {
                navController.navigate("game/$lobbyId/best-story") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(id = R.color.orange_primary))
            }
        }
    }
}






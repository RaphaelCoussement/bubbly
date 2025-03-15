package org.raphou.bubbly.home

import androidx.compose.material3.Text
import androidx.compose.runtime.*

@Composable
fun GameScreen(navController: androidx.navigation.NavController, lobbyId: String, isFirstPlayer: Boolean) {
    Text(
        text = if (isFirstPlayer) "Tu es le premier joueur" else "Tu n'es pas le premier joueur"
    )
}



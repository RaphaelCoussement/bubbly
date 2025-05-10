package org.raphou.bubbly.game

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.raphou.bubbly.domain.lobby.Player
import org.raphou.bubbly.ui.R.color.beige_background
import org.raphou.bubbly.ui.R.color.orange_primary

@SuppressLint("ResourceAsColor")
@Composable
fun PlayersVoteScreen(
    lobbyId: String,
    navController: NavController,
) {
    val viewModel: PlayersVoteScreenViewModel = viewModel()
    val players by viewModel.players.collectAsState()
    val votes by viewModel.votes.collectAsState()

    var winners by remember { mutableStateOf<List<Player>>(emptyList()) }
    var showWinners by remember { mutableStateOf(false) }
    var hasVoted by remember { mutableStateOf(false) }
    var votedPlayerId by remember { mutableStateOf<String?>(null) }
    val allVotesReceived by viewModel.allVotesReceived.collectAsState()


    LaunchedEffect(lobbyId) {
        viewModel.listenToLobbyPlayers(lobbyId)
        viewModel.listenToVotes(lobbyId)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is PlayersVoteUiEvent.NavigateToFinalRanking -> {
                    // Logique pour naviguer vers le ranking final
                    val maxVotes = votes.values.maxOrNull() ?: 0
                    val winnerIds = votes.filterValues { it == maxVotes }.keys.toList()
                    winners = players.filter { it.id in winnerIds }

                    viewModel.addPointsToWinners(lobbyId, winnerIds)

                    showWinners = true

                    // Délai pour montrer les gagnants avant de naviguer
                    delay(3000L)
                    navController.navigate("game/$lobbyId/final-ranking") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                else -> {}
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = beige_background)),
        color = Color.Transparent
    ) {
        if (showWinners) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (winners.size > 1) stringResource(R.string.meilleures_histoires) else stringResource(
                        R.string.meilleure_histoire
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFE53A0C)
                )
                Spacer(modifier = Modifier.height(16.dp))
                winners.forEach { player ->
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.votez_pour_un_joueur),
                    style = MaterialTheme.typography.titleLarge,
                    color = colorResource(id = orange_primary)
                )

                Spacer(modifier = Modifier.height(24.dp))

                players.forEach { player ->
                    val isVotedPlayer = votedPlayerId == player.id

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isVotedPlayer -> colorResource(id = orange_primary)
                                else -> Color.White
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(4.dp)
                            .then(
                                if (!hasVoted) Modifier.clickable {
                                    viewModel.voteForPlayer(lobbyId, player.id)
                                    hasVoted = true
                                    votedPlayerId = player.id
                                } else Modifier
                            )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = if (isVotedPlayer) Color.White else Color(0xFF4A4A4A)
                            )
                        }
                    }
                }
            }
        }
    }
}



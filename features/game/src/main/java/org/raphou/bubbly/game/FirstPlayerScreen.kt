package org.raphou.bubbly.game

import android.os.CountDownTimer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.raphou.bubbly.domain.word.Word
import org.raphou.bubbly.ui.R
import org.raphou.bubbly.ui.R.string.*

@Composable
fun FirstPlayerScreen(navController: NavController, lobbyId: String, themeId: String?) {
    val viewModel: FirstPlayerScreenViewModel = viewModel()
    val words by viewModel.words
    val foundWords by viewModel.foundWords.collectAsState()
    val score by viewModel.score.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(lobbyId) {
        viewModel.fetchWords(lobbyId, themeId)
    }

    LaunchedEffect(lobbyId) {
        while (true) {
            viewModel.checkWordStatus(lobbyId)
            kotlinx.coroutines.delay(2000)
        }
    }

    // Timer
    var timeLeft by remember { mutableStateOf(35) }
    var showFloatingButton by remember { mutableStateOf(false) }
    var isTimerStarted by remember { mutableStateOf(false) }

    // Start the timer only when words are loaded
    LaunchedEffect(words) {
        if (words.isNotEmpty()) {
            viewModel.isTimeStarted(lobbyId)
            object : CountDownTimer(35_000, 1_000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeLeft = (millisUntilFinished / 1_000).toInt()
                }

                override fun onFinish() {
                    timeLeft = 0
                    showFloatingButton = true
                    Log.d("FirstPlayerScreen", "Timer finished, showFloatingButton = $showFloatingButton")

                    coroutineScope.launch {
                        viewModel.fetchFinalScore(lobbyId = lobbyId)
                    }
                }
            }.start()
            isTimerStarted = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.beige_background))
    ) {
        Text(
            text = stringResource(voici_les_mots_faire_deviner),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally),
            color = colorResource(id = R.color.orange_primary),
        )

        // Display timer
        Text(
            text = if (timeLeft > 0) {
                stringResource(temps_restant_secondes, timeLeft)
            } else {
                stringResource(temps_coul)
            },
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 24.sp
            ),
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally),
            color = colorResource(id = R.color.orange_primary)
        )

        if (words.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorResource(id = R.color.orange_primary))
            }
        } else {
            LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(id = R.color.orange_primary)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (timeLeft == 0) {
                        Text(
                            text = stringResource(
                                nombre_de_gorg_es_distribuer,
                                score
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(), // indispensable pour que textAlign fonctionne bien
                            textAlign = TextAlign.Center,
                            color = colorResource(id = R.color.white)
                        )
                    }
                }
            }

            // Liste des mots
            items(words.size) { index ->
                val isFound = foundWords[words[index].name] ?: false
                WordCard(word = words[index], index = index, isFound = isFound)
            }
        }
        }

        if (showFloatingButton) {
            FloatingActionButton(
                onClick = {
                    viewModel.setIsTimeFinished(lobbyId)
                    viewModel.onPlayerTurnFinished(lobbyId)
                    navController.navigate("game/$lobbyId/ranking/${themeId ?: "default"}") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
                    .size(72.dp),
                containerColor = colorResource(id = R.color.orange_primary),
                shape = MaterialTheme.shapes.medium,
                elevation = FloatingActionButtonDefaults.elevation(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Go to Ranking",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun WordCard(word: Word, index: Int, isFound: Boolean) {
    val backgroundColor = if (isFound) Color.Gray.copy(alpha = 0.5f) else {
        if (index % 2 == 0) colorResource(id = R.color.orange_primary) else Color.White
    }
    val textColor = if (isFound) Color.DarkGray else if (index % 2 == 0) Color.White else Color(0xFF4A4A4A)

    val difficulties = listOf("FACILE", "MOYEN", "DIFFICILE", "EXTREME")
    val difficulty = difficulties.getOrNull(index) ?: "INCONNUE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = word.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = textColor
            )
            Text(
                text = difficulty,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}



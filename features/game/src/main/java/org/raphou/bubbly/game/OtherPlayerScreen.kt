package org.raphou.bubbly.game

import OtherPlayerScreenViewModel
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import org.raphou.bubbly.game.R.string
import org.raphou.bubbly.ui.R

@Composable
fun OtherPlayerScreen(navController: NavController, lobbyId: String) {
    val viewModel: OtherPlayerScreenViewModel = viewModel()
    val suggestions by viewModel.suggestions
    val wordsToFind by viewModel.wordsToFind
    val isTimeUp by viewModel.isTimeUp
    val score by viewModel.score
    val suggestionInput = remember { mutableStateOf(TextFieldValue()) }

    val totalTime = 30
    var timeLeft by remember { mutableStateOf(totalTime) }
    var isNavigation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        viewModel.setTimeUp()
    }

    LaunchedEffect(isTimeUp) {
        if (isTimeUp) {
            viewModel.resetGame(lobbyId)
        }
    }

    LaunchedEffect(lobbyId) {
        while (!isNavigation) {
            delay(2000)
            isNavigation = viewModel.isTimeFinished(lobbyId)
        }

        if (isNavigation) {
            navController.navigate("game/$lobbyId/ranking")
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.beige_background))
    ) {
        Text(
            text = stringResource(string.suggestions_de_mots),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
            color = colorResource(id = R.color.orange_primary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (!isTimeUp) stringResource(string.temps_restant_s, timeLeft) else stringResource(string.temps_coul),
            color = Color.Red,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isTimeUp) {
            OutlinedTextField(
                value = suggestionInput.value,
                onValueChange = { suggestionInput.value = it },
                label = { Text(stringResource(string.sugg_rer_un_mot)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = suggestions.size < 10,
                isError = suggestionInput.value.text.isEmpty() && suggestions.size < 10,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (suggestionInput.value.text.isNotEmpty() && suggestions.size < 10) {
                        viewModel.addPlayerSuggestion(lobbyId, suggestionInput.value.text.trim())
                        suggestionInput.value = TextFieldValue("")
                    } else {
                        Toast.makeText(
                            navController.context,
                            "Vous ne pouvez plus suggérer ou le mot est vide.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = suggestions.size < 10,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.orange_primary)
                )
            ) {
                Text(stringResource(string.soumettre_la_suggestion))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Affichage des mots suggérés
        if (!isTimeUp) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(suggestions) { index, word ->
                    SuggestedWordCard(word = word, index = index)
                }
            }
        }

        // Affichage des mots à trouver et du score une fois le temps écoulé
        if (isTimeUp || wordsToFind.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorResource(id = R.color.beige_background)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Score
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
                    Text(
                        text = stringResource(string.votre_score, score),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                }

                Text(
                    text = stringResource(string.mots_trouver),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp),
                    color = colorResource(id = R.color.orange_primary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(wordsToFind) { index, word ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (index % 2 == 0) colorResource(id = R.color.orange_primary) else Color.White
                            )
                        ) {
                            Text(
                                text = word,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp),
                                color = if (index % 2 == 0) Color.White else Color(0xFF4A4A4A)
                            )
                        }
                    }
                }
            }
        }


    }
}

@Composable
fun SuggestedWordCard(word: String, index: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (index % 2 == 0) colorResource(id = R.color.orange_primary) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = if (index % 2 == 0) Color.White else Color(0xFF4A4A4A)
            )
        }
    }
}



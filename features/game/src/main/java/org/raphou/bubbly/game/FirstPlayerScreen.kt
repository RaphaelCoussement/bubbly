package org.raphou.bubbly.game

import android.os.CountDownTimer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.raphou.bubbly.domain.word.Word
import org.raphou.bubbly.game.R.string.temps_coul
import org.raphou.bubbly.game.R.string.temps_restant_secondes
import org.raphou.bubbly.game.R.string.voici_les_mots_faire_deviner
import org.raphou.bubbly.ui.R

@Composable
fun FirstPlayerScreen(navController: NavController, lobbyId: String) {
    val viewModel: FirstPlayerScreenViewModel = viewModel()
    val words by viewModel.words

    // Timer
    var timeLeft by remember { mutableStateOf(30) }

    LaunchedEffect(Unit) {
        object : CountDownTimer(30_000, 1_000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = (millisUntilFinished / 1_000).toInt()
            }

            override fun onFinish() {
                timeLeft = 0
            }
        }.start()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.beige_background))
    ) {
        Text(
            text = stringResource(id = voici_les_mots_faire_deviner),
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

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(words.size) { index ->
                WordCard(word = words[index], index = index)
            }
        }
    }
}

@Composable
fun WordCard(word: Word, index: Int) {
    val isEven = index % 2 == 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEven) colorResource(id = R.color.orange_primary) else Color.White // Alternating colors
        )
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
                color = if (isEven) Color.White else Color(0xFF4A4A4A) // White text on orange, dark text on white
            )
            Text(
                text = word.difficulty.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isEven) Color.White else Color(0xFF4A4A4A) // White text on orange, dark text on white
            )
        }
    }
}

@Composable
fun getDifficultyColor(difficulty: org.raphou.bubbly.domain.word.Difficulty): Color {
    return when (difficulty) {
        org.raphou.bubbly.domain.word.Difficulty.FACILE -> colorResource(id = R.color.orange_primary)
        org.raphou.bubbly.domain.word.Difficulty.MOYEN -> colorResource(id = R.color.beige_background)
        org.raphou.bubbly.domain.word.Difficulty.DIFFICILE -> colorResource(id = R.color.orange_primary)
        org.raphou.bubbly.domain.word.Difficulty.EXTREME -> colorResource(id = R.color.beige_background)
    }
}

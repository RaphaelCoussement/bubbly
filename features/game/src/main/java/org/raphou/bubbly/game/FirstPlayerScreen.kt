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
    val foundWords by viewModel.foundWords.collectAsState()

    LaunchedEffect(foundWords) {
        println("Mots trouvés mis à jour : $foundWords")
    }

    LaunchedEffect(lobbyId) {
        viewModel.fetchWords(lobbyId)
    }

    LaunchedEffect(lobbyId) {
        while (true) {
            viewModel.checkWordStatus(lobbyId)
            kotlinx.coroutines.delay(2000) // Vérifie toutes les 2 secondes
        }
    }

    // Timer
    var timeLeft by remember { mutableStateOf(30) }

    LaunchedEffect(Unit) {
        object : CountDownTimer(50_000, 1_000) {
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
                val isFound = foundWords[words[index].name] ?: false
                WordCard(word = words[index], index = index, isFound = isFound)
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
                text = word.difficulty.name,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}



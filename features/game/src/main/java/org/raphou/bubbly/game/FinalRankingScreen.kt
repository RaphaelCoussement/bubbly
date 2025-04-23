package org.raphou.bubbly.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.raphou.bubbly.ui.R.color.beige_background
import org.raphou.bubbly.ui.R.color.orange_primary
import org.raphou.bubbly.ui.R.string.*

@Composable
fun FinalRankingScreen(navController: NavController, lobbyId: String) {
    val viewModel: FinalRankingScreenViewModel = viewModel()
    val ranking = viewModel.ranking.value

    LaunchedEffect(Unit) {
        viewModel.fetchRanking(lobbyId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = beige_background))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(classement_final),
            style = MaterialTheme.typography.headlineLarge,
            color = colorResource(id = orange_primary),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn {
            itemsIndexed(ranking) { index, item ->
                val (name, points) = item

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (index == 0) colorResource(id = orange_primary) else Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = name,
                            color = if (index == 0) Color.White else Color.Black,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$points",
                            color = if (index == 0) Color.White else Color.Black,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}



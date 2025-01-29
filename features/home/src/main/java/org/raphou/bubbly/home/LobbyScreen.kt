package org.raphou.bubbly.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import org.raphou.bubbly.home.R.*
import org.raphou.bubbly.ui.R

@Composable
fun LobbyScreen(
    navController: NavHostController,
    isCreator: Boolean,
    code: String,
) {
    val viewModel: LobbyScreenViewModel = viewModel()
    val session by viewModel.currentSession.collectAsState()

    LaunchedEffect(isCreator) {
        if (isCreator) {
            viewModel.createLobby()
        } else {
            viewModel.joinLobby(code)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.beige_background))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = colorResource(id = R.color.orange_primary)
                )
            }

            Text(
                text = stringResource(
                    string.code_de_session,
                    session?.code ?: stringResource(string.chargement)
                ),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = colorResource(id = R.color.orange_primary),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Text(
                text = stringResource(string.participants),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (session?.players.isNullOrEmpty()) {
                    item {
                        Text(
                            text = stringResource(string.aucun_participant),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    items(session?.players.orEmpty()) { player ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.white)),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isCreator) {
            FloatingActionButton(
                onClick = { /* Action pour démarrer la session */ },
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
}


package org.raphou.bubbly.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun RulesScreen(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = org.raphou.bubbly.ui.R.color.beige_background))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.retour),
                        tint = colorResource(id = org.raphou.bubbly.ui.R.color.orange_primary)
                    )
                }
                Text(
                    text = stringResource(R.string.regles_du_jeu),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contenu des règles
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔹 Comment jouer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "- Créez une session et partagez le code.\n" +
                                "- Chaque manche, un joueur raconte une histoire avec 4 mots imposés.\n" +
                                "- Les autres devinent les mots et gagnent des points (1 à 4 selon la difficulté).",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )

                    Text(
                        text = "🔹 Points et défis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "- Les mots trouvés rapportent des points et des gorgées à distribuer.\n" +
                                "- Si aucun mot n'est deviné, le joueur actif fait un gage.\n" +
                                "- Entre les manches, des défis permettent de gagner des points bonus.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )

                    Text(
                        text = "🔹 Fin de la partie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "- Après 1 tour complet, le joueur avec le plus de points gagne.\n" +
                                "- Une élection désigne la meilleure histoire (+2 points).",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )
                }
            }
        }
    }
}


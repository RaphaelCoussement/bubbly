package org.raphou.bubbly.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.raphou.bubbly.ui.R

@Composable
fun SplashScreen(navController: NavHostController) {
    val viewModel: SplashScreenViewModel = viewModel()
    val scope = rememberCoroutineScope()

    LaunchedEffect(true) {
        scope.launch {
            viewModel.loadPseudo()
            val pseudo = viewModel.pseudo.value

            if (pseudo.isNullOrEmpty()) {
                navController.navigate("choosePseudo") {
                    popUpTo("splash") { inclusive = true }
                }
            } else {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.beige_background)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Bubbly",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFFE53A0C)
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = colorResource(id = R.color.orange_primary))
        }
    }
}


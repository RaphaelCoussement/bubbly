package org.raphou.bubbly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.android.ext.android.inject
import org.raphou.bubbly.home.HomeScreen
import org.raphou.bubbly.home.HomeScreenViewModel
import org.raphou.bubbly.home.RulesScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val homeScreenViewModel: HomeScreenViewModel by inject()

        setContent {
            Content(homeScreenViewModel)
        }
    }
}

@Composable
fun Content(homeScreenViewModel: HomeScreenViewModel) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(viewModel = homeScreenViewModel, navController = navController)
        }
        composable("rules") {
            RulesScreen(navController = navController)
        }
    }
}

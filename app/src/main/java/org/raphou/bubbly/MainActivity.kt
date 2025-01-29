package org.raphou.bubbly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.koin.android.ext.android.inject
import org.raphou.bubbly.home.CreateLobbyScreen
import org.raphou.bubbly.home.HomeScreen
import org.raphou.bubbly.home.HomeScreenViewModel
import org.raphou.bubbly.home.JoinLobbyScreen
import org.raphou.bubbly.home.LobbyScreen
import org.raphou.bubbly.home.RulesScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Content()
        }
    }
}

@Composable
fun Content() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("rules") {
            RulesScreen(navController = navController)
        }
        composable(
            "joinLobby/{code}",
            arguments = listOf(navArgument("code") { defaultValue = "" })
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code").orEmpty()
            JoinLobbyScreen(navController = navController, code = code)
        }

        composable("createLobby") {
            CreateLobbyScreen(navController = navController)
        }
    }
}

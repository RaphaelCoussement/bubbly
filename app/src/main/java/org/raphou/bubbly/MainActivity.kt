package org.raphou.bubbly

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.raphou.bubbly.domain.home.LanguageManager
import org.raphou.bubbly.game.FinalRankingScreen
import org.raphou.bubbly.game.FirstPlayerScreen
import org.raphou.bubbly.game.OtherPlayerScreen
import org.raphou.bubbly.game.PlayersVoteScreen
import org.raphou.bubbly.game.RankingScreen
import org.raphou.bubbly.home.ChoosePseudoScreen
import org.raphou.bubbly.home.CreateLobbyScreen
import org.raphou.bubbly.home.GameScreen
import org.raphou.bubbly.home.HomeScreen
import org.raphou.bubbly.home.JoinLobbyScreen
import org.raphou.bubbly.home.RulesScreen
import org.raphou.bubbly.home.SettingsScreen
import org.raphou.bubbly.home.SplashScreen
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = LanguageManager.getSavedLanguage(newBase)
        val context = if (lang != "auto") {
            LanguageManager.applyLanguage(newBase, lang)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val LocalAppLocale = compositionLocalOf { Locale.getDefault() }
            val lang = LanguageManager.getSavedLanguage(this)
            val locale = Locale(lang)

            CompositionLocalProvider(LocalAppLocale provides locale) {
                Content()
            }
        }
    }
}

@Composable
fun Content() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(navController)
        }
        composable("choosePseudo") {
            ChoosePseudoScreen(navController)
        }
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("rules") {
            RulesScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable(
            "joinLobby/{code}",
            arguments = listOf(navArgument("code") { defaultValue = "" })
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code").orEmpty()
            JoinLobbyScreen(navController = navController, code = code)
        }

        composable(
            "createLobby/{themeId}",
            arguments = listOf(navArgument("themeId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val themeId = backStackEntry.arguments?.getString("themeId")
            CreateLobbyScreen(navController = navController, themeId = themeId)
        }

        composable(
            "game/{lobbyId}/theme/{themeId}",
            arguments = listOf(
                navArgument("lobbyId") { type = NavType.StringType },
                navArgument("themeId") { type = NavType.StringType; nullable = true }  // Permet themeId d’être nullable
            )
        ) { backStackEntry ->
            val lobbyId = backStackEntry.arguments?.getString("lobbyId").orEmpty()
            val themeId = backStackEntry.arguments?.getString("themeId") // themeId peut être null

            GameScreen(navController = navController, lobbyId = lobbyId, themeId = themeId)
        }

        composable(
            "game/{lobbyId}/ranking/{themeId}",
            arguments = listOf(
                navArgument("lobbyId") { type = NavType.StringType },
                navArgument("themeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lobbyId = backStackEntry.arguments?.getString("lobbyId").orEmpty()
            val themeId = backStackEntry.arguments?.getString("themeId").orEmpty()  // Récupère themeId de la route

            RankingScreen(navController = navController, lobbyId = lobbyId, themeId = themeId)
        }

        composable(
            "game/{lobbyId}/final-ranking",
            arguments = listOf(navArgument("lobbyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val lobbyId = backStackEntry.arguments?.getString("lobbyId").orEmpty()
            FinalRankingScreen(navController = navController, lobbyId = lobbyId)
        }

        composable(
            "game/{lobbyId}/best-story",
            arguments = listOf(navArgument("lobbyId") { type = NavType.StringType })
        ) { backStackEntry ->
            val lobbyId = backStackEntry.arguments?.getString("lobbyId").orEmpty()
            PlayersVoteScreen(navController = navController, lobbyId = lobbyId)
        }

    }
}

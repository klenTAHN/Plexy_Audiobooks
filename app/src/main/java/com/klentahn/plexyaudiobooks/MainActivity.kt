package com.klentahn.plexyaudiobooks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.klentahn.plexyaudiobooks.ui.navigation.Screen
import com.klentahn.plexyaudiobooks.ui.screens.auth.LibrarySelectScreen
import com.klentahn.plexyaudiobooks.ui.screens.auth.PlexLinkScreen
import com.klentahn.plexyaudiobooks.ui.screens.auth.ServerSelectScreen
import com.klentahn.plexyaudiobooks.ui.screens.library.AuthorsScreen
import com.klentahn.plexyaudiobooks.ui.screens.library.AuthorScreen
import com.klentahn.plexyaudiobooks.ui.screens.library.LibraryScreen
import com.klentahn.plexyaudiobooks.ui.screens.player.PlayerScreen
import com.klentahn.plexyaudiobooks.ui.screens.player.PlayerViewModel
import com.klentahn.plexyaudiobooks.ui.theme.PlexyAudiobooksTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val settingsManager = (application as PlexyAudiobooksApplication).container.settingsManager
        
        // Determine start destination
        val startDestination = runBlocking {
            val token = settingsManager.authToken.first()
            val serverUri = settingsManager.serverUri.first()
            val libraryKey = settingsManager.libraryKey.first()
            
            when {
                token == null -> Screen.PlexLink.route
                serverUri == null -> Screen.ServerSelect.route
                libraryKey == null -> Screen.LibrarySelect.route
                else -> Screen.MainLibrary.route
            }
        }

        setContent {
            PlexyAudiobooksTheme {
                PlexyAudiobooksApp(startDestination)
            }
        }
    }
}

@Composable
fun PlexyAudiobooksApp(startDestination: String) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsManager = (context.applicationContext as PlexyAudiobooksApplication).container.settingsManager
    
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.PlexLink.route) {
            PlexLinkScreen(
                onSuccess = {
                    navController.navigate(Screen.ServerSelect.route) {
                        popUpTo(Screen.PlexLink.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.ServerSelect.route) {
            ServerSelectScreen(
                onServerSelected = {
                    navController.navigate(Screen.LibrarySelect.route)
                }
            )
        }
        composable(Screen.LibrarySelect.route) {
            LibrarySelectScreen(
                onLibrarySelected = {
                    navController.navigate(Screen.MainLibrary.route) {
                        popUpTo(Screen.PlexLink.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.MainLibrary.route) {
            val serverUri by settingsManager.serverUri.collectAsState(initial = null)
            val token by settingsManager.authToken.collectAsState(initial = null)
            
            LibraryScreen(
                onBookClick = { ratingKey ->
                    navController.navigate(Screen.Player.createRoute(ratingKey))
                },
                onAuthorClick = { author ->
                    navController.navigate(Screen.AuthorBooks.createRoute(author))
                },
                onNavigateToAuthors = {
                    navController.navigate(Screen.Authors.route)
                },
                serverUri = serverUri,
                token = token
            )
        }
        composable(Screen.Authors.route) {
            AuthorsScreen(
                onAuthorClick = { author ->
                    navController.navigate(Screen.AuthorBooks.createRoute(author))
                },
                onNavigateToBooks = {
                    navController.navigate(Screen.MainLibrary.route) {
                        popUpTo(Screen.MainLibrary.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.AuthorBooks.route,
            arguments = listOf(
                androidx.navigation.navArgument("author") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val author = backStackEntry.arguments?.getString("author") ?: ""
            val serverUri by settingsManager.serverUri.collectAsState(initial = null)
            val token by settingsManager.authToken.collectAsState(initial = null)
            
            AuthorScreen(
                author = author,
                onBookClick = { ratingKey ->
                    navController.navigate(Screen.Player.createRoute(ratingKey))
                },
                onNavigateBack = { navController.popBackStack() },
                serverUri = serverUri,
                token = token
            )
        }
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                androidx.navigation.navArgument("ratingKey") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val ratingKey = backStackEntry.arguments?.getString("ratingKey") ?: ""
            val appContainer = (LocalContext.current.applicationContext as PlexyAudiobooksApplication).container
            val playerViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                PlayerViewModel(
                    plexRepository = appContainer.plexRepository,
                    settingsManager = appContainer.settingsManager,
                    metadataMaster = appContainer.metadataMaster
                )
            }
            
            PlayerScreen(
                ratingKey = ratingKey,
                viewModel = playerViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

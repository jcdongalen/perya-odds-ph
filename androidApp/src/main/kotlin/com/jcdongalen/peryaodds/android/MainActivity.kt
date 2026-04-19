package com.jcdongalen.peryaodds.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jcdongalen.peryaodds.android.ui.screens.AnalyticsDashboard
import com.jcdongalen.peryaodds.android.ui.screens.GameSelectionScreen
import com.jcdongalen.peryaodds.android.ui.screens.ObservationScreen
import com.jcdongalen.peryaodds.android.ui.screens.StrategyScreen
import com.jcdongalen.peryaodds.android.ui.theme.PeryaOddsTheme
import com.jcdongalen.peryaodds.shared.presentation.GameSessionViewModel
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PeryaOddsTheme {
                PeryaOddsApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeryaOddsApp() {
    val viewModel: GameSessionViewModel = koinInject()
    val activeGameType by viewModel.activeGameType.collectAsState()
    val error by viewModel.error.collectAsState()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle storage errors with Snackbar
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = "Storage Error: $errorMessage",
                duration = SnackbarDuration.Long,
                actionLabel = "Dismiss"
            )
            viewModel.clearError()
        }
    }
    
    // If no game is selected, show game selection screen
    if (activeGameType == null) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            GameSelectionScreen(
                viewModel = viewModel,
                onGameSelected = { gameType ->
                    viewModel.selectGame(gameType)
                },
                modifier = Modifier.padding(paddingValues)
            )
        }
    } else {
        // Show main navigation with bottom bar
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "observation",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("observation") {
                    ObservationScreen(viewModel = viewModel)
                }
                composable("analytics") {
                    AnalyticsDashboard(viewModel = viewModel)
                }
                composable("strategy") {
                    StrategyScreen(viewModel = viewModel)
                }
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem("observation", Icons.Default.Edit, "Observe"),
    BottomNavItem("analytics", Icons.Default.BarChart, "Analytics"),
    BottomNavItem("strategy", Icons.Default.Lightbulb, "Strategy")
)

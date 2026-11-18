package com.example.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.AnimeSeries
import com.example.model.FriendRequest
import com.example.model.GameRoom
import com.example.model.HabitItem
import com.example.model.PublicProfile
import com.example.model.QuoteHistoryEntry
import com.example.model.UiState
import com.example.model.UserSettings
import com.example.model.VisualizerProfile
import com.example.ui.components.SoftCyberParticleCanvas
import com.example.ui.screens.ArchiveSearchScreen
import com.example.ui.screens.HomeHabitsScreen
import com.example.ui.screens.MinigamesHubScreen
import com.example.ui.screens.ProfileSocialScreen
import com.example.viewmodel.VicdaruMainViewModel

sealed class VicdaruDestination(
    val route: String,
    val baseRoute: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    data object Home : VicdaruDestination(
        route = "home",
        baseRoute = "home",
        label = "Pulso",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    )

    data object Archive : VicdaruDestination(
        route = "archive",
        baseRoute = "archive",
        label = "Archivo",
        selectedIcon = Icons.Filled.Bookmarks,
        unselectedIcon = Icons.Outlined.Bookmarks,
        testTag = "nav_archive"
    )

    data object Arcade : VicdaruDestination(
        route = "arcade?tab={tab}",
        baseRoute = "arcade",
        label = "Juegos",
        selectedIcon = Icons.Filled.SportsEsports,
        unselectedIcon = Icons.Outlined.SportsEsports,
        testTag = "nav_arcade"
    ) {
        fun buildRoute(tab: Int = 0): String = "arcade?tab=$tab"
    }

    data object Profile : VicdaruDestination(
        route = "profile",
        baseRoute = "profile",
        label = "Perfil",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_profile"
    )

    companion object {
        val primaryTabs: List<VicdaruDestination> = listOf(Home, Archive, Arcade, Profile)
    }
}

@Composable
fun VicdaruMainScaffold(
    viewModel: VicdaruMainViewModel,
    currentUserId: String,
    activeAnime: AnimeSeries,
    darkMode: Boolean,
    settings: UserSettings?,
    profile: PublicProfile?,
    quotesState: UiState<List<QuoteHistoryEntry>>,
    habitsState: UiState<List<HabitItem>>,
    visualizerProfilesState: UiState<List<VisualizerProfile>>,
    friendProfiles: List<PublicProfile>,
    incomingRequests: List<FriendRequest>,
    invitedRooms: List<GameRoom>,
    activeRoom: GameRoom?,
    isFetchingQuote: Boolean,
    isOffline: Boolean = false,
    pendingCacheSyncCount: Int = 0,
    snackbarHostState: SnackbarHostState,
    navController: NavHostController = rememberNavController(),
    onReplayOnboarding: () -> Unit = {},
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigateToTab(destination: VicdaruDestination, arcadeSubTab: Int = 0) {
        viewModel.haptics.navTabSwitch()
        val targetRoute = if (destination is VicdaruDestination.Arcade) {
            VicdaruDestination.Arcade.buildRoute(arcadeSubTab)
        } else {
            destination.route
        }
        navController.navigate(targetRoute) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = destination !is VicdaruDestination.Arcade
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar {
                        VicdaruDestination.primaryTabs.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route?.startsWith(dest.baseRoute) == true
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        VicdaruDestination.primaryTabs.forEach { dest ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route?.startsWith(dest.baseRoute) == true
                            } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    SoftCyberParticleCanvas()

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Subtle top status strip showing active anime palette & quick Dark Mode switch
                        ActiveThemeStatusBar(
                            activeAnime = activeAnime,
                            darkMode = darkMode,
                            onToggleDarkMode = { viewModel.toggleDarkMode(!darkMode) }
                        )

                        NavHost(
                            navController = navController,
                            startDestination = VicdaruDestination.Home.route,
                            enterTransition = { fadeIn(animationSpec = tween(260)) },
                            exitTransition = { fadeOut(animationSpec = tween(220)) },
                            popEnterTransition = { fadeIn(animationSpec = tween(260)) },
                            popExitTransition = { fadeOut(animationSpec = tween(220)) },
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(route = VicdaruDestination.Home.route) {
                                HomeHabitsScreen(
                                    viewModel = viewModel,
                                    quotesState = quotesState,
                                    habitsState = habitsState,
                                    isFetchingQuote = isFetchingQuote,
                                    isOffline = isOffline,
                                    pendingCacheSyncCount = pendingCacheSyncCount
                                )
                            }

                            composable(route = VicdaruDestination.Archive.route) {
                                ArchiveSearchScreen(
                                    viewModel = viewModel,
                                    quotesState = quotesState,
                                    isOffline = isOffline,
                                    pendingCacheSyncCount = pendingCacheSyncCount
                                )
                            }

                            composable(
                                route = VicdaruDestination.Arcade.route,
                                arguments = listOf(
                                    navArgument("tab") {
                                        type = NavType.IntType
                                        defaultValue = 0
                                    }
                                )
                            ) { backStackEntry ->
                                val subTab = backStackEntry.arguments?.getInt("tab") ?: 0
                                MinigamesHubScreen(
                                    viewModel = viewModel,
                                    profile = profile,
                                    settings = settings,
                                    visualizerProfilesState = visualizerProfilesState,
                                    friendProfiles = friendProfiles,
                                    invitedRooms = invitedRooms,
                                    activeRoom = activeRoom,
                                    currentUserId = currentUserId,
                                    initialTab = subTab
                                )
                            }

                            composable(route = VicdaruDestination.Profile.route) {
                                ProfileSocialScreen(
                                    viewModel = viewModel,
                                    profile = profile,
                                    settings = settings,
                                    friendProfiles = friendProfiles,
                                    incomingRequests = incomingRequests,
                                    onInviteToGame = { gameType, friendUid ->
                                        val goal = if (gameType == "CLIMBER") 300L else 100L
                                        val subTab = if (gameType == "MUSIC_FOCUS") 1 else 0
                                        viewModel.createMultiplayerRoom(gameType, goal, friendUid)
                                        navigateToTab(VicdaruDestination.Arcade, arcadeSubTab = subTab)
                                    },
                                    onReplayOnboarding = onReplayOnboarding
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveThemeStatusBar(
    activeAnime: AnimeSeries,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Paleta: ${activeAnime.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        IconButton(
            onClick = onToggleDarkMode,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("quick_dark_mode_toggle")
        ) {
            Icon(
                imageVector = if (darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = if (darkMode) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

package com.hawatraders.manager.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hawatraders.manager.AppContainer
import com.hawatraders.manager.ui.components.LocalContainer
import com.hawatraders.manager.ui.screens.BackupScreen
import com.hawatraders.manager.ui.screens.ChangePasswordScreen
import com.hawatraders.manager.ui.screens.ProductEditScreen
import com.hawatraders.manager.ui.screens.ProductManageScreen
import com.hawatraders.manager.ui.screens.SearchScreen
import com.hawatraders.manager.ui.screens.SettingsScreen
import com.hawatraders.manager.util.Strings

object Routes {
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PRODUCTS = "products"
    const val EDIT = "edit"
    const val EDIT_PATTERN = "edit?productId={productId}"
    const val CHANGE_PASSWORD = "change_password"
    const val BACKUP = "backup"

    /** Everything that belongs to the Settings tab; leaving this set locks the owner area. */
    val settingsFlow = setOf(SETTINGS, PRODUCTS, EDIT_PATTERN, CHANGE_PASSWORD, BACKUP)

    /** Screens that must never be visible while locked. */
    val protectedScreens = setOf(PRODUCTS, EDIT_PATTERN, CHANGE_PASSWORD, BACKUP)
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun HawaTradersApp(container: AppContainer) {
    CompositionLocalProvider(LocalContainer provides container) {
        AppScaffold(container)
    }
}

@Composable
private fun AppScaffold(container: AppContainer) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val session = container.session
    val unlocked by session.unlocked.collectAsStateWithLifecycle()

    // 1) Auto-lock when the user leaves the Settings tab.
    LaunchedEffect(route) {
        if (route != null && route !in Routes.settingsFlow) session.lock()
    }
    // 2) Never show a protected screen while locked (e.g. back stack restored after process death,
    //    or the lock button / background lock fired while an owner screen was open).
    LaunchedEffect(route, unlocked) {
        if (!unlocked && route in Routes.protectedScreens) {
            navController.popBackStack(Routes.SETTINGS, inclusive = false)
        }
    }
    // 3) Auto-lock when the app goes to the background, except when we ourselves launched the
    //    camera / picker / share sheet.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (!session.externalLaunch) session.lock()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { session.externalLaunch = false }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { session.externalLaunch = false }

    val tabs = listOf(
        Tab(Routes.SEARCH, Strings.tabSearch, Icons.Filled.Search),
        Tab(Routes.SETTINGS, Strings.tabSettings, Icons.Filled.Settings),
    )
    val showBar = route == Routes.SEARCH || route == Routes.SETTINGS

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                if (route != tab.route) {
                                    if (tab.route == Routes.SEARCH) {
                                        navController.popBackStack(Routes.SEARCH, inclusive = false)
                                    } else {
                                        navController.navigate(tab.route) { launchSingleTop = true }
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SEARCH,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Routes.SEARCH) { SearchScreen() }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onAddProduct = { navController.navigate(Routes.EDIT) },
                    onManageProducts = { navController.navigate(Routes.PRODUCTS) },
                    onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                    onBackup = { navController.navigate(Routes.BACKUP) },
                )
            }
            composable(Routes.PRODUCTS) {
                ProductManageScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Routes.EDIT) },
                    onEdit = { id -> navController.navigate("${Routes.EDIT}?productId=$id") },
                )
            }
            composable(
                route = Routes.EDIT_PATTERN,
                arguments = listOf(
                    navArgument("productId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                ),
            ) {
                ProductEditScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CHANGE_PASSWORD) {
                ChangePasswordScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.BACKUP) {
                BackupScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

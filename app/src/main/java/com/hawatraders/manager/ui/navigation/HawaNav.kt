package com.hawatraders.manager.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hawatraders.manager.ui.components.ScanMenuSheet
import com.hawatraders.manager.ui.components.TextScanDialogs
import com.hawatraders.manager.viewmodel.AppViewModelProvider
import com.hawatraders.manager.viewmodel.TextScanViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hawatraders.manager.ui.components.rememberCameraGate
import com.hawatraders.manager.ui.screens.ScanOutcome
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
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
import com.hawatraders.manager.ui.screens.BulkPriceScreen
import com.hawatraders.manager.ui.screens.ProductImportScreen
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
    const val BULK = "bulk_price"
    const val IMPORT_PRODUCTS = "import_products"

    /** Everything that belongs to the Settings tab; leaving this set locks the owner area. */
    val settingsFlow = setOf(SETTINGS, PRODUCTS, EDIT_PATTERN, CHANGE_PASSWORD, BACKUP, BULK, IMPORT_PRODUCTS)

    /** Screens that must never be visible while locked. */
    val protectedScreens = setOf(PRODUCTS, EDIT_PATTERN, CHANGE_PASSWORD, BACKUP, BULK, IMPORT_PRODUCTS)
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

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

    // ---- Barcode scan (centre button) ----
    val scope = rememberCoroutineScope()
    var scanOutcome by remember { mutableStateOf<ScanOutcome?>(null) }
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        session.externalLaunch = false
        val code = result.contents?.trim().orEmpty()
        if (code.isNotEmpty()) {
            scope.launch {
                val product = try {
                    container.productRepository.findByBarcode(code)
                } catch (e: Exception) {
                    null
                }
                // The Search screen is always the root of the back stack.
                if (route != Routes.SEARCH) navController.popBackStack(Routes.SEARCH, inclusive = false)
                scanOutcome = if (product != null) ScanOutcome.Found(product.id) else ScanOutcome.NotFound
            }
        }
    }
    val scanGate = rememberCameraGate {
        session.externalLaunch = true
        try {
            scanLauncher.launch(
                ScanOptions()
                    .setPrompt(Strings.scanPrompt)
                    .setBeepEnabled(false)
                    .setOrientationLocked(false)
            )
        } catch (e: Exception) {
            session.externalLaunch = false
        }
    }

    // ---- Label-text scan (photo -> OCR -> matching products) ----
    val textVm: TextScanViewModel = viewModel(factory = AppViewModelProvider.Factory)
    var showScanMenu by remember { mutableStateOf(false) }
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        session.externalLaunch = false
        val uri = cameraUri
        cameraUri = null
        if (ok && uri != null) textVm.process(uri) else container.imageStore.clearCameraTemp()
    }
    val photoGate = rememberCameraGate {
        val uri = container.imageStore.newCameraUri()
        cameraUri = uri
        session.externalLaunch = true
        try {
            photoLauncher.launch(uri)
        } catch (e: Exception) {
            session.externalLaunch = false
        }
    }
    fun finishTextScan(outcome: ScanOutcome?) {
        textVm.dismiss()
        container.imageStore.clearCameraTemp()
        if (outcome != null) {
            if (route != Routes.SEARCH) navController.popBackStack(Routes.SEARCH, inclusive = false)
            scanOutcome = outcome
        }
    }

    val tabs = listOf(
        Tab(Routes.SEARCH, Strings.tabSearch, Icons.Outlined.Search, Icons.Filled.Search),
        Tab(Routes.SETTINGS, Strings.tabSettings, Icons.Outlined.Settings, Icons.Filled.Settings),
    )
    val showBar = route == Routes.SEARCH || route == Routes.SETTINGS

    if (showScanMenu) {
        ScanMenuSheet(
            onBarcode = { showScanMenu = false; scanGate() },
            onText = { showScanMenu = false; photoGate() },
            onDismiss = { showScanMenu = false },
        )
    }
    TextScanDialogs(
        state = textVm.state,
        onPick = { id -> finishTextScan(ScanOutcome.Found(id)) },
        onSearchText = { text -> finishTextScan(ScanOutcome.Query(text)) },
        onDismiss = { finishTextScan(null) },
    )

    Scaffold(
        floatingActionButton = {
            if (showBar) {
                // Centre scan button: a ring in the page colour cuts a notch into the bar.
                Box(
                    modifier = Modifier
                        .offset(y = 40.dp)
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    FloatingActionButton(
                        onClick = { showScanMenu = true },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        modifier = Modifier.size(68.dp),
                    ) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = Strings.scanLabel,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            AnimatedVisibility(
                visible = showBar,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        if (index == 1) {
                            // Empty slot under the centre scan button.
                            NavigationBarItem(
                                selected = false,
                                onClick = {},
                                enabled = false,
                                icon = { Box(Modifier.size(24.dp)) },
                                label = { Text(Strings.scanLabel) },
                                colors = NavigationBarItemDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
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
                            icon = {
                                Icon(
                                    if (route == tab.route) tab.selectedIcon else tab.icon,
                                    contentDescription = null,
                                )
                            },
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
            composable(Routes.SEARCH) {
                SearchScreen(scanOutcome = scanOutcome, onScanHandled = { scanOutcome = null })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onAddProduct = { navController.navigate(Routes.EDIT) },
                    onManageProducts = { navController.navigate(Routes.PRODUCTS) },
                    onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                    onBackup = { navController.navigate(Routes.BACKUP) },
                    onBulkPrice = { navController.navigate(Routes.BULK) },
                    onImportProducts = { navController.navigate(Routes.IMPORT_PRODUCTS) },
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
            composable(Routes.BULK) {
                BulkPriceScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.IMPORT_PRODUCTS) {
                ProductImportScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

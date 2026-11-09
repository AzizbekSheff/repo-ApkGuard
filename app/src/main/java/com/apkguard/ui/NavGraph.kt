package com.apkguard.ui

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.compose.ui.res.stringResource
import com.apkguard.model.*
import com.apkguard.ui.screens.*
import com.apkguard.ui.theme.DangerRed
import com.apkguard.viewmodel.AuthState
import com.apkguard.viewmodel.AuthViewModel
import com.apkguard.viewmodel.ScanViewModel
import com.apkguard.viewmodel.UserProfile
import kotlinx.coroutines.launch
import java.io.File

sealed class Screen(val route: String) {
    object Auth    : Screen("auth")
    object Home    : Screen("home")
    object ScanSelection : Screen("scan_selection")
    object Scan    : Screen("scan")
    object Result  : Screen("result")
    object History : Screen("history")
    object InstalledApps : Screen("installed_apps")
    object Profile : Screen("profile")
    object Subscription : Screen("subscription")
    object Admin : Screen("admin")
    object Detail  : Screen("detail/{id}") {
        fun createRoute(id: Long) = "detail/$id"
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: ScanViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val scanState by viewModel.scanState.collectAsState()
    val history by viewModel.history.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val detail by viewModel.detailResult.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showBottomBar = currentRoute in listOf(Screen.Home.route, Screen.Profile.route, Screen.ScanSelection.route)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = Color(0xFF1A1A2E)
                    )
                    NavigationBar(
                        containerColor = Color(0xFF0D0D1A),
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text(stringResource(com.apkguard.R.string.nav_home)) },
                            selected = currentRoute == Screen.Home.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF4A9EF5),
                                selectedTextColor = Color(0xFF4A9EF5),
                                unselectedIconColor = Color(0xFF8A8A9A),
                                unselectedTextColor = Color(0xFF8A8A9A),
                                indicatorColor = Color.Transparent
                            ),
                            onClick = {
                                if (currentRoute != Screen.Home.route) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route) { inclusive = true }
                                    }
                                }
                            }
                        )

                        // Middle Scan Button
                        NavigationBarItem(
                            icon = {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = if (currentRoute == Screen.ScanSelection.route) Color(0xFF4A9EF5) else Color(0xFF1A1A2E)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.QrCodeScanner,
                                            null,
                                            tint = if (currentRoute == Screen.ScanSelection.route) Color.White else Color(0xFF8A8A9A)
                                        )
                                    }
                                }
                            },
                            label = { Text(stringResource(com.apkguard.R.string.nav_scan)) },
                            selected = currentRoute == Screen.ScanSelection.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF4A9EF5),
                                selectedTextColor = Color(0xFF4A9EF5),
                                unselectedIconColor = Color(0xFF8A8A9A),
                                unselectedTextColor = Color(0xFF8A8A9A),
                                indicatorColor = Color.Transparent
                            ),
                            onClick = {
                                if (currentRoute != Screen.ScanSelection.route) {
                                    navController.navigate(Screen.ScanSelection.route) {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                    }
                                }
                            }
                        )

                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            label = { Text(stringResource(com.apkguard.R.string.profile_title)) },
                            selected = currentRoute == Screen.Profile.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF4A9EF5),
                                selectedTextColor = Color(0xFF4A9EF5),
                                unselectedIconColor = Color(0xFF8A8A9A),
                                unselectedTextColor = Color(0xFF8A8A9A),
                                indicatorColor = Color.Transparent
                            ),
                            onClick = {
                                if (currentRoute != Screen.Profile.route) {
                                    navController.navigate(Screen.Profile.route) {
                                        popUpTo(Screen.Home.route) { inclusive = false }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (authState is AuthState.Authenticated) Screen.Home.route else Screen.Auth.route,
            modifier = Modifier.padding(padding),
            enterTransition = { slideInHorizontally { it } + fadeIn() },
            exitTransition = { slideOutHorizontally { -it } + fadeOut() },
            popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it } + fadeOut() }
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthenticated = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = authViewModel,
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToProfile = {
                        navController.navigate(Screen.Profile.route)
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    onNavigateToSubscription = {
                        navController.navigate(Screen.Subscription.route)
                    },
                    onNavigateToAdmin = {
                        navController.navigate(Screen.Admin.route)
                    }
                )
            }

            composable(Screen.ScanSelection.route) {
                ScanSelectionScreen(
                    viewModel = viewModel,
                    onApkSelected = { uri ->
                        viewModel.startScan(
                            uri = uri,
                            userProfile = userProfile,
                            onPermissionRequired = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = authViewModel.getString(com.apkguard.R.string.error_permissions_required),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onLimitExceeded = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = authViewModel.getString(com.apkguard.R.string.limit_reached_msg),
                                        actionLabel = authViewModel.getString(com.apkguard.R.string.btn_subscribe),
                                        duration = SnackbarDuration.Long
                                    ).let { result ->
                                        if (result == SnackbarResult.ActionPerformed) {
                                            navController.navigate(Screen.Subscription.route)
                                        }
                                    }
                                }
                            },
                            onScanStarted = {
                                authViewModel.incrementScanCount()
                                navController.navigate(Screen.Scan.route)
                            }
                        )
                    },
                    onNavigateToInstalledApps = {
                        viewModel.loadInstalledApps()
                        navController.navigate(Screen.InstalledApps.route)
                    },
                    onEntryClick = { id ->
                        viewModel.loadDetailResult(id)
                        navController.navigate(Screen.Detail.createRoute(id))
                    }
                )
            }

            composable(Screen.Scan.route) {
                when (val state = scanState) {
                    is ScanState.Scanning -> ScanProgressScreen(
                        state = state,
                        onCancel = {
                            viewModel.resetScan()
                            navController.popBackStack()
                        }
                    )
                    is ScanState.Done -> {
                        LaunchedEffect(Unit) {
                            navController.navigate(Screen.Result.route) {
                                popUpTo(Screen.Scan.route) { inclusive = true }
                            }
                        }
                    }
                    is ScanState.Error -> {
                        ScanErrorScreen(
                            message = state.message,
                            onBack = {
                                viewModel.resetScan()
                                navController.popBackStack()
                            }
                        )
                    }
                    else -> {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
            }

            composable(Screen.Result.route) {
                val state = scanState
                if (state is ScanState.Done) {
                    ResultScreen(
                        result = state.result,
                        onApprove = {
                            viewModel.saveDecision(state.result, UserDecision.APPROVED)
                            navController.popBackStack(Screen.Home.route, false)
                        },
                        onReject = {
                            viewModel.saveDecision(state.result, UserDecision.REJECTED)
                            navController.popBackStack(Screen.Home.route, false)
                        },
                        onBack = {
                            viewModel.resetScan()
                            navController.popBackStack(Screen.Home.route, false)
                        }
                    )
                }
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    history = history,
                    onBack = { navController.popBackStack() },
                    onEntryClick = { id ->
                        viewModel.loadDetailResult(id)
                        navController.navigate(Screen.Detail.createRoute(id))
                    },
                    onDelete = { viewModel.deleteHistory(it) },
                    onClearAll = { viewModel.clearAllHistory() }
                )
            }

            composable(Screen.InstalledApps.route) {
                InstalledAppsScreen(
                    apps = installedApps,
                    onBack = { navController.popBackStack() },
                    onAppClick = { app ->
                        val uri = Uri.fromFile(File(app.sourceDir))
                        viewModel.startScan(
                            uri = uri,
                            userProfile = userProfile,
                            onPermissionRequired = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = authViewModel.getString(com.apkguard.R.string.error_permissions_required),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onLimitExceeded = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = authViewModel.getString(com.apkguard.R.string.limit_reached_msg),
                                        actionLabel = authViewModel.getString(com.apkguard.R.string.btn_subscribe),
                                        duration = SnackbarDuration.Long
                                    ).let { result ->
                                        if (result == SnackbarResult.ActionPerformed) {
                                            navController.navigate(Screen.Subscription.route)
                                        }
                                    }
                                }
                            },
                            onScanStarted = {
                                authViewModel.incrementScanCount()
                                navController.navigate(Screen.Scan.route)
                            }
                        )
                    }
                )
            }

            composable(Screen.Subscription.route) {
                SubscriptionScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Admin.route) {
                AdminScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.Detail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStack ->
                val id = backStack.arguments?.getLong("id") ?: return@composable
                LaunchedEffect(id) { viewModel.loadDetailResult(id) }

                detail?.let { result ->
                    ResultScreen(
                        result = result,
                        onApprove = {
                            viewModel.updateDecision(id, UserDecision.APPROVED)
                            navController.popBackStack()
                        },
                        onReject = {
                            viewModel.updateDecision(id, UserDecision.REJECTED)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() },
                        readOnly = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanErrorScreen(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = DangerRed
        )
        Spacer(Modifier.height(16.dp))
        Text(
            androidx.compose.ui.res.stringResource(com.apkguard.R.string.error_scan_failed),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onBack) { Text(androidx.compose.ui.res.stringResource(com.apkguard.R.string.error_go_back)) }
    }
}

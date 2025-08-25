package com.blesense.app

import android.app.Activity
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ble_jetpackcompose.OpticalSensorScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    // Initialize AuthViewModel
    val authViewModel: AuthViewModel = viewModel()
    // Observe authentication state
    val authState by authViewModel.authState.collectAsState()
    // Get current context
    val context = LocalContext.current
    // Get application context
    val application = context.applicationContext as Application
    // Get activity context
    val activity = context as ComponentActivity
    // Initialize BluetoothScanViewModel with factory
    val bluetoothViewModel: BluetoothScanViewModel<Any?> by activity.viewModels { BluetoothScanViewModelFactory(application) }

    // Initialize ThemeManager with system theme on app start
//    LaunchedEffect(Unit) {
//        ThemeManager.run { initializeWithSystemTheme(isSystemInDarkTheme()) }
//    }

    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                // Navigate to intermediate screen on successful authentication
                navController.navigate("intermediate_screen") {
                    popUpTo("first_screen") { inclusive = true }
                    popUpTo("splash_screen") { inclusive = true }
                }
            }
            is AuthState.Error -> {
                // Handle error if needed (e.g., show a toast)
            }
            is AuthState.Idle -> {
                // Navigate to first screen if not on splash or first screen
                if (navController.currentDestination?.route !in listOf("first_screen", "splash_screen")) {
                    navController.navigate("first_screen") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    // Define navigation graph
    NavHost(
        navController = navController,
        startDestination = if (authViewModel.isUserAuthenticated()) "intermediate_screen" else "splash_screen"
    ) {
        // Splash screen route
        composable("splash_screen") {
            SplashScreen(
                onNavigateToLogin = {
                    if (!authViewModel.isUserAuthenticated()) {
                        navController.navigate("first_screen") {
                            popUpTo("splash_screen") { inclusive = true }
                        }
                    }
                }
            )
        }

        // First screen route
        composable("first_screen") {
            AnimatedFirstScreen(
                onNavigateToLogin = {
                    navController.navigate("login")
                },
                onNavigateToSignup = {
                    navController.navigate("register")
                },
                onGuestSignIn = {
                    authViewModel.signInAsGuest()
                }
            )
        }

        // Login screen route
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate("register")
                },
                onNavigateToHome = {
                    navController.navigate("intermediate_screen") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Register screen route
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("intermediate_screen") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Intermediate screen route
        composable("intermediate_screen") {
            IntermediateScreen(
                navController = navController,
                isDarkMode = ThemeManager.isDarkMode.collectAsState().value
            )
        }

        // Home screen route
        composable("home_screen") {
            MainScreen(
                navController = navController,
                bluetoothViewModel = bluetoothViewModel
            )
        }

        // Chat screen route
        composable("chat_screen") {
            ChatScreen(navController = navController)
        }

        // Game activity screen route
        composable("game_loading") {
            BLEGamesScreen(navController = navController)
        }

        composable("game_screen") {
            val activity = LocalContext.current as Activity
            GameActivityScreen(
                activity = activity,
                onBackToHome = {
                    navController.navigate("intermediate_screen") {
                        popUpTo("game_screen") { inclusive = true }
                    }
                }
            )
        }

        // Robot control screen route
        composable("robot_screen") {
            val activity = LocalContext.current as Activity
            RobotControlScreen(
                onBackPressed = { activity.finish() }
            )
        }

        composable("optical_sensor_screen") {
            OpticalSensorScreen(
                navController = navController,
                viewModel = bluetoothViewModel as BluetoothScanViewModel<Any>
            )
        }

        // Settings screen route
        composable("settings_screen") {
            ModernSettingsScreen(
                viewModel = authViewModel,
                onSignOut = {
                    navController.navigate("first_screen") {
                        popUpTo("intermediate_screen") { inclusive = true }
                    }
                },
                navController = navController
            )
        }

        // Advertising data screen route
        composable(
            route = "advertising/{deviceName}/{deviceAddress}/{sensorType}/{deviceId}",
            arguments = listOf(
                navArgument("deviceName") { type = NavType.StringType },
                navArgument("deviceAddress") { type = NavType.StringType },
                navArgument("sensorType") { type = NavType.StringType },
                navArgument("deviceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deviceName = backStackEntry.arguments?.getString("deviceName") ?: ""
            val deviceAddress = backStackEntry.arguments?.getString("deviceAddress") ?: ""
            val sensorType = backStackEntry.arguments?.getString("sensorType") ?: ""
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""

            AdvertisingDataScreen(
                deviceAddress = deviceAddress,
                deviceName = deviceName,
                navController = navController,
                deviceId = deviceId,
                viewModel = bluetoothViewModel
            )
        }

        // Chart screen route
        composable(
            route = "chart_screen/{deviceAddress}",
            arguments = listOf(
                navArgument("deviceAddress") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ChartScreen(
                navController = navController,
                deviceAddress = backStackEntry.arguments?.getString("deviceAddress")
            )
        }

        // Second chart screen route
        composable(
            route = "chart_screen_2/{title}/{value}",
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("value") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title")
            val value = backStackEntry.arguments?.getString("value")

            ChartScreen2(navController = navController, title = title, value = value)
        }

        // Water quality screen route
        composable("water_quality_screen") {
            WaterQualityScreen(
                navController = navController,
                viewModel = bluetoothViewModel as BluetoothScanViewModel<Any>
            )
        }
    }
}
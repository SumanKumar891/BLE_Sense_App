package com.example.ble_jetpackcompose

// Import necessary Android and Compose libraries for navigation, view models, and context
import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

// Main navigation composable for the app
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

    // Check if system is in dark mode
    val systemDarkMode = isSystemInDarkTheme()

//    val intent = Intent(context, RobotControlCompose::class.java)
//    context.startActivity(intent)

    // Initialize ThemeManager with system theme on app start
    LaunchedEffect(Unit) {
        ThemeManager.initializeWithSystemTheme(systemDarkMode)
    }

    // Check authentication state when the app starts
    LaunchedEffect(Unit) {
        if (authViewModel.isUserAuthenticated()) {
            // Navigate to home screen if user is authenticated
            navController.navigate("home_screen") {
                popUpTo("splash_screen") { inclusive = true }
            }
        }
    }

    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                // Navigate to home screen on successful authentication
                navController.navigate("home_screen") {
                    popUpTo("first_screen") { inclusive = true }
                }
            }
            is AuthState.Error -> {
                // Handle error if needed
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
        // Set start destination based on authentication status
        startDestination = if (authViewModel.isUserAuthenticated()) "home_screen" else "splash_screen"
    ) {
        // Splash screen route
        composable("splash_screen") {
            SplashScreen(
                onNavigateToLogin = {
                    if (!authViewModel.isUserAuthenticated()) {
                        // Navigate to first screen if not authenticated
                        navController.navigate("first_screen") {
                            popUpTo("splash_screen") { inclusive = true }
                        }
                    }
                }
            )
        }

        // First screen route (likely onboarding or welcome screen)
        composable("first_screen") {
            AnimatedFirstScreen(
                onNavigateToLogin = {
                    // Navigate to login screen
                    navController.navigate("login")
                },
                onNavigateToSignup = {
                    // Navigate to register screen
                    navController.navigate("register")
                },
                onGuestSignIn = {
                    // Sign in as guest
                    authViewModel.signInAsGuest()
                }
            )
        }

        // Login screen route
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    // Navigate to register screen
                    navController.navigate("register")
                },
                onNavigateToHome = {
                    // Navigate to home screen, clearing back stack
                    navController.navigate("home_screen") {
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
                    // Navigate to login screen, clearing register screen
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    // Navigate to home screen, clearing back stack
                    navController.navigate("home_screen") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Game loading screen route
        composable("game_loading") {
            BLEGamesScreen(navController = navController)
        }

        // Game activity screen route
        composable("game_screen") {
            val activity = LocalContext.current as Activity
            GameActivityScreen(activity = activity)
        }

//        composable("landscape_screen") {
//            RobotControlScreen()
//        }

        // Robot control screen route
        composable("robot_screen") {
            val activity = LocalContext.current as Activity
            RobotControlScreen(
                onBackPressed = { activity.finish() } // Finish activity on back press
            )
        }

        // Settings screen route
        composable("settings_screen") {
            ModernSettingsScreen(
                viewModel = authViewModel,
                onSignOut = {
                    // Navigate to first screen on sign out, clearing home screen
                    navController.navigate("first_screen") {
                        popUpTo("home_screen") { inclusive = true }
                    }
                },
                navController = navController
            )
        }

        // Advertising data screen route with arguments
        composable(
            route = "advertising/{deviceName}/{deviceAddress}/{sensorType}/{deviceId}",
            arguments = listOf(
                navArgument("deviceName") { type = NavType.StringType },
                navArgument("deviceAddress") { type = NavType.StringType },
                navArgument("sensorType") { type = NavType.StringType },
                navArgument("deviceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Extract navigation arguments
            val deviceName = backStackEntry.arguments?.getString("deviceName") ?: ""
            val deviceAddress = backStackEntry.arguments?.getString("deviceAddress") ?: ""
            backStackEntry.arguments?.getString("sensorType") ?: ""
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            // Initialize BluetoothScanViewModel for this screen
            val viewModel: BluetoothScanViewModel<Any?> = viewModel(factory = BluetoothScanViewModelFactory(application))
            // Observe devices
            val devices by viewModel.devices.collectAsState()
            // Find device by address
            devices.find { it.address == deviceAddress }

            AdvertisingDataScreen(
                deviceAddress = deviceAddress,
                deviceName = deviceName,
                navController = navController,
                deviceId = deviceId
            )
        }

        // Chart screen route with device address argument
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

        // Second chart screen route with title and value arguments
        composable("chart_screen_2/{title}/{value}") { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title")
            val value = backStackEntry.arguments?.getString("value")

            ChartScreen2(navController = navController, title = title, value = value)
        }

        // Home screen route
        composable("home_screen") {
            MainScreen(
                navController = navController,
                bluetoothViewModel = bluetoothViewModel
            )
        }
    }
}
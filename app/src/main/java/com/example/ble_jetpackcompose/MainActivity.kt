package com.example.ble_jetpackcompose

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.initialize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Prevent activity recreation on orientation changes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        super.onCreate(savedInstanceState)
        installSplashScreen()
        Firebase.initialize(this)
        setContent {
            val navController = rememberNavController()
            AppNavigation(navController)
        }
    }
}
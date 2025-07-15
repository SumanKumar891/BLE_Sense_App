package com.example.ble_jetpackcompose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Data class to hold translatable text for the splash screen
data class TranslatedSplashScreenText(
    val appName: String = "BLE Sense", // Default app name
    val developedBy: String = "Developed by AWaDH, IIT Ropar" // Default developer credit
)

// Composable for the splash screen
@Composable
fun SplashScreen(onNavigateToLogin: () -> Unit) {
    LocalContext.current // Access the current context (not used but retained for compatibility)

    // Observe theme and language state
    val isDarkMode by ThemeManager.isDarkMode.collectAsState() // Dark mode state
    val currentLanguage by LanguageManager.currentLanguage.collectAsState() // Current language state

    // State for translated text with fallback values
    var translatedText by remember {
        mutableStateOf(
            TranslatedSplashScreenText(
                appName = TranslationCache.get("BLE Sense-$currentLanguage") ?: "BLE Sense",
                developedBy = TranslationCache.get("Developed by AWaDH, IIT Ropar-$currentLanguage") ?: "Developed by AWaDH, IIT Ropar"
            )
        )
    }

    // Preload translations when the language changes
    LaunchedEffect(currentLanguage) {
        val translator = GoogleTranslationService() // Initialize translation service
        val textsToTranslate = listOf("BLE Sense", "Developed by AWaDH, IIT Ropar") // Texts to translate
        val translatedList = translator.translateBatch(textsToTranslate, currentLanguage) // Perform batch translation
        translatedText = TranslatedSplashScreenText(
            appName = translatedList[0], // Update app name
            developedBy = translatedList[1] // Update developer credit
        )
    }

    // Define theme-based colors
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color.White // Background color
    val textColor = if (isDarkMode) Color.White else Color.Black // Primary text color
    val secondaryTextColor = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f) // Secondary text color with opacity

    // Navigate to login screen after a 2-second delay
    LaunchedEffect(key1 = true) {
        delay(2000L) // Wait for 2 seconds
        onNavigateToLogin() // Trigger navigation to login screen
    }

    // Set up infinite animation transition
    val infiniteTransition = rememberInfiniteTransition()

    // Slow zoom animation for the background image
    val imageScale by infiniteTransition.animateFloat(
        initialValue = 1f, // Initial scale
        targetValue = 1.05f, // Target scale
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing), // 6-second linear animation
            repeatMode = RepeatMode.Reverse // Reverse animation direction
        )
    )

    // Slow zoom animation for the app name text
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f, // Initial scale
        targetValue = 1.2f, // Target scale
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearOutSlowInEasing), // 1-second easing animation
            repeatMode = RepeatMode.Reverse // Reverse animation direction
        )
    )

    // Slow zoom animation for the developer credit text
    val footerScale by infiniteTransition.animateFloat(
        initialValue = 1f, // Initial scale
        targetValue = 1.1f, // Target scale
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing), // 3-second linear animation
            repeatMode = RepeatMode.Reverse // Reverse animation direction
        )
    )

    // Main layout for the splash screen
    Box(
        modifier = Modifier
            .fillMaxSize() // Fill entire screen
            .background(backgroundColor), // Apply background color
        contentAlignment = Alignment.Center // Center content
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // Fill entire screen
                .padding(16.dp), // Apply padding
            verticalArrangement = Arrangement.Center, // Center vertically
            horizontalAlignment = Alignment.CenterHorizontally // Center horizontally
        ) {
            // Background image with zoom animation
            Image(
                painter = painterResource(id = R.drawable.bg_remove_2), // Load background image
                contentDescription = null, // No content description for decorative image
                modifier = Modifier
                    .fillMaxWidth() // Fill available width
                    .height(300.dp) // Fixed height
                    .graphicsLayer(scaleX = imageScale, scaleY = imageScale), // Apply zoom animation
                contentScale = ContentScale.Crop // Crop image to fit
            )

            Spacer(modifier = Modifier.height(40.dp)) // Space between image and text

            // App name text with zoom animation
            BasicText(
                text = translatedText.appName, // Translated app name
                style = TextStyle(
                    fontSize = 40.sp, // Large font size
                    color = textColor, // Theme-based text color
                    fontWeight = FontWeight.Bold, // Bold text
                    textAlign = TextAlign.Center, // Center text
                    fontFamily = helveticaFont // Custom font
                ),
                modifier = Modifier
                    .graphicsLayer(scaleX = titleScale, scaleY = titleScale) // Apply zoom animation
            )

            Spacer(modifier = Modifier.height(20.dp)) // Space between texts

            // Developer credit text with zoom animation
            BasicText(
                text = translatedText.developedBy, // Translated developer credit
                style = TextStyle(
                    fontSize = 18.sp, // Smaller font size
                    color = secondaryTextColor, // Theme-based secondary color
                    fontWeight = FontWeight.SemiBold, // Semi-bold text
                    textAlign = TextAlign.Center, // Center text
                    fontFamily = helveticaFont // Custom font
                ),
                modifier = Modifier
                    .graphicsLayer(scaleX = footerScale, scaleY = footerScale) // Apply zoom animation
            )
        }
    }
}

// Preview composable for the splash screen
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(onNavigateToLogin = {}) // Empty callback for preview
}
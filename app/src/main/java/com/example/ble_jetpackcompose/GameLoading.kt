package com.example.ble_jetpackcompose

// Import necessary Compose libraries for animations, UI components, state management, and navigation
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay

// Composable for the loading screen of the BLE Games app
@Composable
fun BLEGamesScreen(navController: NavHostController) {
    // Observe theme and language state from managers
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()

    // State to hold translated text, initialized with cached or default value
    var translatedText by remember {
        mutableStateOf(
            TranslatedBLEGamesScreenText(
                bleGames = TranslationCache.get("BLE Games-$currentLanguage") ?: "BLE Games"
            )
        )
    }

    // Preload translations when language changes
    LaunchedEffect(currentLanguage) {
        val translator = GoogleTranslationService()
        val textsToTranslate = listOf("BLE Games") // Text to translate
        val translatedList = translator.translateBatch(textsToTranslate, currentLanguage) // Translate to current language
        // Update translated text state
        translatedText = TranslatedBLEGamesScreenText(
            bleGames = translatedList[0]
        )
    }

    // Define theme-based colors
    val backgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFF4D426D) // Dark gray or original purple
    val textColor = if (isDarkMode) Color.White else Color.White // White in both modes for contrast
    val barBackgroundColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray // Lighter gray in dark mode
    val loadingBarColor = if (isDarkMode) Color(0xFFBB86FC) else Color.Red // Purple in dark mode, red in light

    // Animate the width of the loading bar
    val lineWidth = remember { Animatable(0f) } // Red bar width animation

    // Trigger loading animation and navigation
    LaunchedEffect(Unit) {
        // Animate loading bar width to 198dp over 4 seconds
        lineWidth.animateTo(
            targetValue = 198f,
            animationSpec = tween(durationMillis = 4000, easing = FastOutSlowInEasing)
        )
        delay(500) // Wait briefly after animation
        // Navigate to game screen and remove loading screen from back stack
        navController.navigate("game_screen") {
            popUpTo("game_loading") { inclusive = true }
        }
    }

    // Calculate progress for animations
    val progress = lineWidth.value / 198f
    val alpha = progress // Fade-in effect based on progress
    val offset = -45f + (progress * 50f) // Vertical offset for text animation

    // Main screen layout
    Box(
        modifier = Modifier
            .fillMaxSize() // Fill the entire screen
            .padding(WindowInsets.systemBars.asPaddingValues()) // Respect system bars
            .background(backgroundColor) // Apply theme-based background
    ) {
        // Center content vertically and horizontally
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center), // Center in the Box
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Display title text
            Text(
                text = translatedText.bleGames,
                fontFamily = helveticaFont, // Custom font
                style = MaterialTheme.typography.h4.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 39.sp
                ),
                color = textColor,
                modifier = Modifier
                    .alpha(alpha) // Apply animated opacity
                    .offset(y = offset.dp) // Apply animated vertical offset
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(32.dp)) // Space between title and loading bar

            // Loading bar container
            Box(
                modifier = Modifier
                    .width(198.dp)
                    .height(5.dp)
                    .background(barBackgroundColor, shape = RoundedCornerShape(2.dp)) // Background of the bar
            ) {
                // Animated loading bar
                Box(
                    modifier = Modifier
                        .width(lineWidth.value.dp) // Animated width
                        .fillMaxHeight()
                        .background(loadingBarColor, shape = RoundedCornerShape(2.dp)) // Animated bar color
                )
            }
        }
    }
}

// Data class for translatable text
data class TranslatedBLEGamesScreenText(
    val bleGames: String // Translated title text
)
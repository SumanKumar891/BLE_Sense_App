package com.blesense.app

// Import necessary Compose libraries for animations, UI components, and icons
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Data class for translatable text in LoadingDialog
data class TranslatedLoadingDialogText(
    val waitMessage: String = "Wait.It's Sensing..." // Default loading message
)

// Composable for displaying a loading dialog with animated arcs
@Composable
fun LoadingDialog(onDismissRequest: () -> Unit) {
    // Observe theme and language state
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()

    // Initialize translated text with cached value or default
    var translatedText by remember {
        mutableStateOf(
            TranslatedLoadingDialogText(
                waitMessage = TranslationCache.get("Wait.It's Sensing...-$currentLanguage") ?: "Wait.It's Sensing..."
            )
        )
    }

    // Preload translations when language changes
    LaunchedEffect(currentLanguage) {
        val translator = GoogleTranslationService()
        val textsToTranslate = listOf("Wait.It's Sensing...")
        val translatedList = translator.translateBatch(textsToTranslate, currentLanguage)
        // Update translated text state
        translatedText = TranslatedLoadingDialogText(
            waitMessage = translatedList[0]
        )
    }

    // Define theme-based colors
    val dialogBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White // Dialog background
    val textColor = if (isDarkMode) Color.White else Color.Black // Text color
    val primaryColor = if (isDarkMode) Color(0xFFBB86FC) else MaterialTheme.colorScheme.primary // Primary color for icon and arcs

    // Display dialog with custom properties
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true, // Allow dismissal on back press
            dismissOnClickOutside = false, // Prevent dismissal on outside click
            usePlatformDefaultWidth = false // Use custom width
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .shadow(elevation = 8.dp, shape = MaterialTheme.shapes.medium) // Add shadow
                .background(
                    color = dialogBackgroundColor,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(24.dp) // Internal padding
        ) {
            // Container for animated arcs and Bluetooth icon
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Animated arcs component
                OptimizedAnimatedArcs(
                    primaryColor = primaryColor,
                    modifier = Modifier.matchParentSize()
                )
                // Bluetooth icon component
                BluetoothIcon(primaryColor = primaryColor)
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Display loading message
            Text(
                text = translatedText.waitMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}

// Composable for displaying the Bluetooth icon
@Composable
fun BluetoothIcon(primaryColor: Color) {
    Icon(
        imageVector = Icons.Default.Bluetooth,
        contentDescription = "Bluetooth Icon",
        modifier = Modifier.size(60.dp),
        tint = primaryColor // Apply theme-based color
    )
}

// Composable for drawing animated arcs
@Composable
fun OptimizedAnimatedArcs(
    primaryColor: Color, // Color for the arcs
    modifier: Modifier = Modifier
) {
    // Create infinite transition for animation
    val transition = rememberInfiniteTransition(label = "Arc Animation")

    // Animate a single value for all arcs to optimize performance
    val animationValue by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "Combined Animation"
    )

    // Draw arcs on canvas
    Canvas(modifier = modifier) {
        val baseRadius = size.minDimension / 4 // Base radius for arcs
        val strokeWidth = 4.dp.toPx() // Stroke width for arcs

        // Calculate scale based on animation value
        val scale = 1f + (animationValue * 0.3f)

        // Draw three concentric arcs with varying alpha and radius
        for (i in 0..2) {
            val alphaMultiplier = (3 - i) / 3f // Decrease alpha for outer arcs
            val alpha = (animationValue * alphaMultiplier).coerceIn(0f, 0.7f) // Animate alpha
            val scaledRadius = baseRadius * scale + (i * 20) // Scale radius for each arc

            drawArc(
                color = primaryColor.copy(alpha = alpha),
                startAngle = -45f, // Start angle for arc
                sweepAngle = 90f, // Sweep angle for arc
                useCenter = false,
                style = Stroke(width = strokeWidth),
                size = Size(scaledRadius * 1.6f, scaledRadius * 2.0f), // Arc size
                topLeft = Offset(
                    center.x - (scaledRadius * 0.8f),
                    center.y - scaledRadius
                ) // Position arc
            )
        }
    }
}
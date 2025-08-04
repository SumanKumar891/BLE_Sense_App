package com.blesense.app

// Import necessary Compose libraries for animations, UI components, and state management
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import com.example.ble_jetpackcompose.LanguageManager
//import com.example.ble_jetpackcompose.ThemeManager

// Data class to hold translatable text for the AnimatedFirstScreen
data class TranslatedFirstScreenText(
    val appName: String = "BLE Sense", // Default app name
    val login: String = "Login", // Default login text
    val signUp: String = "Sign Up", // Default sign-up text
    val or: String = "OR", // Default divider text
    val continueAsGuest: String = "Continue as Guest" // Default guest sign-in text
)

// Composable function for the initial animated screen with login, sign-up, and guest options
@Composable
fun AnimatedFirstScreen(
    onNavigateToLogin: () -> Unit, // Callback for navigating to login screen
    onNavigateToSignup: () -> Unit, // Callback for navigating to sign-up screen
    onGuestSignIn: () -> Unit // Callback for guest sign-in
) {
    // Observe theme and language state from managers
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()

    // State to hold translated text, initialized with cached or default values
    var translatedText by remember {
        mutableStateOf(
            TranslatedFirstScreenText(
                appName = TranslationCache.get("BLE Sense-$currentLanguage") ?: "BLE Sense",
                login = TranslationCache.get("Login-$currentLanguage") ?: "Login",
                signUp = TranslationCache.get("Sign Up-$currentLanguage") ?: "Sign Up",
                or = TranslationCache.get("OR-$currentLanguage") ?: "OR",
                continueAsGuest = TranslationCache.get("Continue as Guest-$currentLanguage") ?: "Continue as Guest"
            )
        )
    }

    // Preload translations when language changes
    LaunchedEffect(currentLanguage) {
        val translator = GoogleTranslationService()
        // List of texts to translate
        val textsToTranslate = listOf("BLE Sense", "Login", "Sign Up", "OR", "Continue as Guest")
        // Translate texts to the current language
        val translatedList = translator.translateBatch(textsToTranslate, currentLanguage)
        // Update translated text state
        translatedText = TranslatedFirstScreenText(
            appName = translatedList[0],
            login = translatedList[1],
            signUp = translatedList[2],
            or = translatedList[3],
            continueAsGuest = translatedList[4]
        )
    }

    // Define theme-based colors
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color.White // Background color
    val shapeBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFD9EFFF) // Shape background
    val textColor = if (isDarkMode) Color.White else Color.Black // Text color
    val dividerColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray // Divider color
    val buttonBackgroundColor = if (isDarkMode) Color(0xFFBB86FC) else colorResource(R.color.btnColor) // Button background
    val buttonTextColor = if (isDarkMode) Color.Black else Color.White // Button text color
    val loadingIndicatorColor = if (isDarkMode) Color(0xFFBB86FC) else colorResource(R.color.btnColor) // Loading indicator color

    // Animation states for background, icon, text, and buttons
    val backgroundScale = remember { Animatable(0f) } // Scale for background shape
    val iconAlpha = remember { Animatable(0f) } // Alpha for icon
    val textAlpha = remember { Animatable(0f) } // Alpha for text
    val buttonAlpha = remember { Animatable(0f) } // Alpha for buttons

    // Trigger animations on composition
    LaunchedEffect(Unit) {
        // Animate background scale
        backgroundScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        // Animate icon alpha
        iconAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = LinearEasing)
        )
        // Animate text alpha
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = LinearEasing)
        )
        // Animate button alpha
        buttonAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = LinearEasing)
        )
    }

    // State to track loading status
    var isLoading by remember { mutableStateOf(false) }

    // Main container for the screen
    Box(
        modifier = Modifier
            .fillMaxSize() // Fill the entire screen
            .background(backgroundColor) // Apply theme-based background color
    ) {
        // Animated Background Shape
        Box(
            modifier = Modifier
                .fillMaxSize() // Fill the entire screen
                .graphicsLayer(
                    scaleX = backgroundScale.value,
                    scaleY = backgroundScale.value,
                    transformOrigin = TransformOrigin(0f, 1f) // Scale from bottom-left
                )
                .clip(GenericShape { size, _ ->
                    // Define a custom shape using a quadratic Bezier curve
                    val path = Path().apply {
                        moveTo(0f, size.height * 0.9f)
                        quadraticBezierTo(
                            size.width * 0.1f, size.height * 0.62f,
                            size.width * 0.55f, size.height * 0.55f
                        )
                        quadraticBezierTo(
                            size.width * 1f, size.height * 0.47f,
                            size.width, size.height * 0.4f
                        )
                        lineTo(size.width, 0f)
                        lineTo(0f, 0f)
                        close()
                    }
                    addPath(path)
                })
                .background(shapeBackgroundColor) // Apply theme-based shape background
        )

        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize() // Fill the entire screen
                .padding(16.dp), // Apply padding
            horizontalAlignment = Alignment.CenterHorizontally, // Center horizontally
            verticalArrangement = Arrangement.Center // Center vertically
        ) {
            // App icon
            Image(
                painter = painterResource(id = R.drawable.bg_remove_ble),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(200.dp) // Fixed size for icon
                    .alpha(iconAlpha.value) // Apply animated alpha
            )

            // App name text
            Text(
                text = translatedText.appName,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = helveticaFont, // Custom Helvetica font
                color = textColor, // Theme-based text color
                modifier = Modifier
                    .alpha(textAlpha.value) // Apply animated alpha
                    .padding(bottom = 140.dp) // Bottom padding
            )

            // Spacer for vertical spacing
            Spacer(modifier = Modifier.height(80.dp))

            // Login Button
            Button(
                onClick = { onNavigateToLogin() }, // Navigate to login screen
                modifier = Modifier
                    .fillMaxWidth(0.6f) // 60% of screen width
                    .alpha(buttonAlpha.value) // Apply animated alpha
                    .padding(vertical = 8.dp) // Vertical padding
                    .height(50.dp), // Fixed height
                shape = RoundedCornerShape(12.dp), // Rounded corners
                colors = ButtonDefaults.buttonColors(backgroundColor = buttonBackgroundColor), // Theme-based background
                elevation = ButtonDefaults.elevation(defaultElevation = 8.dp) // Button elevation
            ) {
                Text(
                    text = translatedText.login,
                    fontSize = 18.sp,
                    color = buttonTextColor, // Theme-based text color
                    fontFamily = helveticaFont, // Custom Helvetica font
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Spacer for vertical spacing
            Spacer(modifier = Modifier.height(10.dp))

            // Sign Up Button
            Button(
                onClick = { onNavigateToSignup() }, // Navigate to sign-up screen
                modifier = Modifier
                    .fillMaxWidth(0.6f) // 60% of screen width
                    .alpha(buttonAlpha.value) // Apply animated alpha
                    .padding(vertical = 8.dp) // Vertical padding
                    .height(50.dp), // Fixed height
                shape = RoundedCornerShape(12.dp), // Rounded corners
                colors = ButtonDefaults.buttonColors(backgroundColor = buttonBackgroundColor), // Theme-based background
                elevation = ButtonDefaults.elevation(defaultElevation = 8.dp) // Button elevation
            ) {
                Text(
                    text = translatedText.signUp,
                    fontSize = 18.sp,
                    color = buttonTextColor, // Theme-based text color
                    fontFamily = helveticaFont, // Custom Helvetica font
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Spacer for vertical spacing
            Spacer(modifier = Modifier.height(24.dp))

            // OR Divider
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.6f) // 60% of screen width
                    .alpha(buttonAlpha.value), // Apply animated alpha
                horizontalArrangement = Arrangement.Center, // Center horizontally
                verticalAlignment = Alignment.CenterVertically // Center vertically
            ) {
                // Left divider line
                Divider(
                    modifier = Modifier
                        .weight(1f) // Equal weight for left divider
                        .alpha(0.5f), // Semi-transparent
                    color = dividerColor, // Theme-based color
                    thickness = 1.dp // Line thickness
                )
                // OR text
                Text(
                    text = "  ${translatedText.or}  ",
                    color = dividerColor, // Theme-based color
                    fontSize = 14.sp,
                    fontFamily = helveticaFont, // Custom Helvetica font
                    modifier = Modifier.padding(horizontal = 8.dp) // Horizontal padding
                )
                // Right divider line
                Divider(
                    modifier = Modifier
                        .weight(1f) // Equal weight for right divider
                        .alpha(0.5f), // Semi-transparent
                    color = dividerColor, // Theme-based color
                    thickness = 1.dp // Line thickness
                )
            }

            // Spacer for vertical spacing
            Spacer(modifier = Modifier.height(24.dp))

            // Conditional display of loading animation or guest sign-in text
            if (isLoading) {
                LoadingAnimation(
                    modifier = Modifier
                        .size(48.dp) // Fixed size for loading animation
                        .alpha(buttonAlpha.value), // Apply animated alpha
                    color = loadingIndicatorColor // Theme-based color
                )
            } else {
                // Guest sign-in text
                Text(
                    text = translatedText.continueAsGuest,
                    fontSize = 16.sp,
                    color = textColor, // Theme-based color
                    fontFamily = helveticaFont, // Custom Helvetica font
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .alpha(buttonAlpha.value) // Apply animated alpha
                        .clickable(
                            indication = null, // No ripple effect
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            isLoading = true // Show loading animation
                            onGuestSignIn() // Trigger guest sign-in
                        }
                        .padding(vertical = 8.dp) // Vertical padding
                )
            }
        }
    }
}

// Composable function for a loading animation
@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    color: Color // Color for the loading indicator
) {
    // Create an infinite transition for animations
    val infiniteTransition = rememberInfiniteTransition()
    // Animate rotation from 0 to 360 degrees
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Animate scale between 0.6 and 1.0
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Container for the loading animation
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center // Center the content
    ) {
        // Circular progress indicator with animated scale and rotation
        CircularProgressIndicator(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotationAngle
                }
                .size(32.dp), // Fixed size
            color = color, // Theme-based color
            strokeWidth = 3.dp // Stroke width
        )
    }
}
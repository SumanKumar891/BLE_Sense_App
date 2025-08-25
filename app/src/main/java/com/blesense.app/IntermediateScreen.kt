package com.blesense.app

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ble_jetpackcompose.OpticalSensorScreen

@Composable
fun IntermediateScreen(
    navController: NavHostController,
    isDarkMode: Boolean
) {
    // Define theme-based colors
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val selectedColor = if (isDarkMode) Color(0xFF0D47A1) else Color(0xFF21CBF3)
    val gradientStart = if (isDarkMode) Color(0xFF0D47A1) else Color(0xFF21CBF3)
    val gradientEnd = if (isDarkMode) Color(0xFF0D47A1) else Color(0xFF21CBF3)

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { /* Handle result if needed */ }
    )

    Scaffold(
        backgroundColor = backgroundColor,
        topBar = {
            TopAppBar(
                backgroundColor = cardBackgroundColor,
                elevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BLE Sense",
                        fontFamily = helveticaFont,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        style = MaterialTheme.typography.h5,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("chat_screen") },
                backgroundColor = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF007AFF),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Filled.ChatBubble,
                    contentDescription = "Chatbot"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(
                    text = "Choose a Destination",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            // First row: Bluetooth and Gameplay
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NavigationIconBox(
                        iconResId = R.drawable.bluetooth,
                        label = "Bluetooth",
                        textColor = textColor,
                        gradientStart = gradientStart,
                        gradientEnd = gradientEnd,
                        backgroundColor = cardBackgroundColor,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("home_screen") }
                    )

                    NavigationIconBox(
                        iconResId = R.drawable.gamepad,
                        label = "Gameplay",
                        textColor = textColor,
                        gradientStart = gradientStart,
                        gradientEnd = gradientEnd,
                        backgroundColor = cardBackgroundColor,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("game_loading") }
                    )
                }
            }

            // Second row: Robot Control and Optical Sensor
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NavigationIconBox(
                        iconResId = R.drawable.robo_car_icon,
                        label = "Robot Control",
                        textColor = textColor,
                        gradientStart = gradientStart,
                        gradientEnd = gradientEnd,
                        backgroundColor = cardBackgroundColor,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(context, RobotControlCompose::class.java)
                            launcher.launch(intent)
                        }
                    )

                    NavigationIconBox(
                        iconResId = R.drawable.icons_leaf,
                        label = "Optical Sensor",
                        textColor = textColor,
                        gradientStart = gradientStart,
                        gradientEnd = gradientEnd,
                        backgroundColor = cardBackgroundColor,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("optical_sensor_screen") }
                    )
                }
            }

            // Third row: Water Quality and Settings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NavigationIconBox(
                        iconResId = R.drawable.waterquality,
                        label = "Water Quality",
                        textColor = textColor,
                        gradientStart = gradientStart,
                        gradientEnd = gradientEnd,
                        backgroundColor = cardBackgroundColor,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("water_quality_screen") }
                    )

                    NavigationIconBox(
                        iconResId = R.drawable.settings,
                        label = "Settings",
                        textColor = textColor,
                        gradientStart = gradientStart,
                        gradientEnd = gradientEnd,
                        backgroundColor = cardBackgroundColor,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("settings_screen") }
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationIconBox(
    iconResId: Int,
    label: String,
    textColor: Color,
    gradientStart: Color,
    gradientEnd: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    // Enhanced animations
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "elevation"
    )

    // Color animation for pressed state
    val cardColor by animateColorAsState(
        targetValue = if (isPressed) backgroundColor.copy(alpha = 0.8f) else backgroundColor,
        animationSpec = tween(durationMillis = 150),
        label = "cardColor"
    )

    // Icon scale animation
    val iconScale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "iconScale"
    )

    Card(
        modifier = modifier
            .aspectRatio(1.1f)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val success = tryAwaitRelease()
                        isPressed = false
                        if (success) {
                            onClick()
                        }
                    }
                )
            },
        elevation = elevation,
        shape = RoundedCornerShape(20.dp),
        backgroundColor = cardColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                // Enhanced icon container with gradient background and scale animation
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .scale(iconScale)
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isPressed) {
                                    listOf(
                                        gradientStart.copy(alpha = 0.9f),
                                        gradientEnd.copy(alpha = 0.9f)
                                    )
                                } else {
                                    listOf(gradientStart, gradientEnd)
                                },
                                start = Offset(0f, 0f),
                                end = Offset(1f, 1f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = label,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.body1,
                    color = if (isPressed) textColor.copy(alpha = 0.7f) else textColor,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
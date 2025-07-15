package com.example.ble_jetpackcompose

// Import necessary Compose libraries for animations, UI components, and navigation
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Data class for translatable text in ChartScreen2
data class TranslatedChart2Text(
    val unknownTitle: String = "Unknown Title", // Default title for unknown cases
    val sensorTitlePrefix: String = "Bluetooth Sensor", // Prefix for sensor titles
    val advertisingType: String = "Advertising type", // Label for advertising type
    val dataStatus: String = "Data status", // Label for data status
    val primaryPhy: String = "Primary PHY", // Label for primary PHY
    val bluetooth5: String = "Bluetooth 5", // Bluetooth 5 version label
    val bluetooth42: String = "Bluetooth 4.2", // Bluetooth 4.2 version label
    val complete: String = "Complete", // Label for complete data status
    val partial: String = "Partial", // Label for partial data status
    val le1m: String = "LE 1M", // Label for LE 1M PHY
    val leCoded: String = "LE Coded" // Label for LE Coded PHY
)

// Composable for displaying a chart screen with sensor data
@Composable
fun ChartScreen2(navController: NavController, title: String?, value: String?) {
    // Use default title if none provided
    val actualTitle = title ?: "Unknown Title"
    // Default value if none provided
    value ?: "Unknown Value"

    // Observe theme and language state
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()

    // Initialize translated text with cached values or defaults
    var translatedText by remember {
        mutableStateOf(
            TranslatedChart2Text(
                unknownTitle = TranslationCache.get("Unknown Title-$currentLanguage") ?: "Unknown Title",
                sensorTitlePrefix = TranslationCache.get("Bluetooth Sensor-$currentLanguage") ?: "Bluetooth Sensor",
                advertisingType = TranslationCache.get("Advertising type-$currentLanguage") ?: "Advertising type",
                dataStatus = TranslationCache.get("Data status-$currentLanguage") ?: "Data status",
                primaryPhy = TranslationCache.get("Primary PHY-$currentLanguage") ?: "Primary PHY",
                bluetooth5 = TranslationCache.get("Bluetooth 5-$currentLanguage") ?: "Bluetooth 5",
                bluetooth42 = TranslationCache.get("Bluetooth 4.2-$currentLanguage") ?: "Bluetooth 4.2",
                complete = TranslationCache.get("Complete-$currentLanguage") ?: "Complete",
                partial = TranslationCache.get("Partial-$currentLanguage") ?: "Partial",
                le1m = TranslationCache.get("LE 1M-$currentLanguage") ?: "LE 1M",
                leCoded = TranslationCache.get("LE Coded-$currentLanguage") ?: "LE Coded"
            )
        )
    }

    // Preload translations when language changes
    LaunchedEffect(currentLanguage) {
        val translator = GoogleTranslationService()
        val textsToTranslate = listOf(
            "Unknown Title", "Bluetooth Sensor", "Advertising type", "Data status", "Primary PHY",
            "Bluetooth 5", "Bluetooth 4.2", "Complete", "Partial", "LE 1M", "LE Coded"
        )
        val translatedList = translator.translateBatch(textsToTranslate, currentLanguage)
        // Update translated text state
        translatedText = TranslatedChart2Text(
            unknownTitle = translatedList[0],
            sensorTitlePrefix = translatedList[1],
            advertisingType = translatedList[2],
            dataStatus = translatedList[3],
            primaryPhy = translatedList[4],
            bluetooth5 = translatedList[5],
            bluetooth42 = translatedList[6],
            complete = translatedList[7],
            partial = translatedList[8],
            le1m = translatedList[9],
            leCoded = translatedList[10]
        )
    }

    // Mock sensor data with translated strings
    val sensorData = listOf(
        "${translatedText.sensorTitlePrefix} 1" to "${translatedText.advertisingType}: ${translatedText.bluetooth5}\n${translatedText.dataStatus}: ${translatedText.complete}\n${translatedText.primaryPhy}: ${translatedText.le1m}",
        "${translatedText.sensorTitlePrefix} 2" to "${translatedText.advertisingType}: ${translatedText.bluetooth5}\n${translatedText.dataStatus}: ${translatedText.complete}\n${translatedText.primaryPhy}: ${translatedText.le1m}",
        "${translatedText.sensorTitlePrefix} 3" to "${translatedText.advertisingType}: ${translatedText.bluetooth42}\n${translatedText.dataStatus}: ${translatedText.partial}\n${translatedText.primaryPhy}: ${translatedText.le1m}",
        "${translatedText.sensorTitlePrefix} 4" to "${translatedText.advertisingType}: ${translatedText.bluetooth5}\n${translatedText.dataStatus}: ${translatedText.complete}\n${translatedText.primaryPhy}: ${translatedText.leCoded}",
        "${translatedText.sensorTitlePrefix} 1" to "${translatedText.advertisingType}: ${translatedText.bluetooth5}\n${translatedText.dataStatus}: ${translatedText.complete}\n${translatedText.primaryPhy}: ${translatedText.le1m}",
        "${translatedText.sensorTitlePrefix} 2" to "${translatedText.advertisingType}: ${translatedText.bluetooth5}\n${translatedText.dataStatus}: ${translatedText.complete}\n${translatedText.primaryPhy}: ${translatedText.le1m}",
        "${translatedText.sensorTitlePrefix} 3" to "${translatedText.advertisingType}: ${translatedText.bluetooth42}\n${translatedText.dataStatus}: ${translatedText.partial}\n${translatedText.primaryPhy}: ${translatedText.le1m}"
    )

    // Define theme-based colors
    val backgroundGradient = if (isDarkMode) {
        Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF424242))) // Dark mode gradient
    } else {
        Brush.verticalGradient(listOf(Color.White, Color.LightGray)) // Light mode gradient
    }
    val appBarBackground = if (isDarkMode) Color(0xFF121212) else Color.White // App bar background
    val cardBackground = if (isDarkMode) Color(0xFF1E1E1E) else Color.White // Card background
    val chartBackground = if (isDarkMode) Color(0xFF1E1E1E) else Color.White // Chart background
    val textColor = if (isDarkMode) Color.White else Color.Black // Primary text color
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray // Secondary text color
    val chartLineColor = if (isDarkMode) Color(0xFFBB86FC) else Color.Blue // Chart line color

    // Manage LazyColumn state
    val listState = rememberLazyListState()
    // Track chart size state
    var isSmallSize by remember { mutableStateOf(false) }
    // Animate chart scale
    val scale by animateFloatAsState(
        targetValue = if (isSmallSize) 0.5f else 1f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ), label = ""
    )

    // Adjust chart scale based on scroll offset
    LaunchedEffect(remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }) {
        isSmallSize = listState.firstVisibleItemScrollOffset > 0 // Shrink chart when scrolling
    }

    // Main UI structure
    Scaffold(
        topBar = {
            // Top app bar with navigation
            TopAppBar(
                title = {
                    Text(
                        if (title == null) translatedText.unknownTitle else actualTitle,
                        fontSize = 20.sp,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                backgroundColor = appBarBackground,
                elevation = 4.dp
            )
        },
        backgroundColor = Color.Transparent // Transparent scaffold background
    ) { paddingValues ->
        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(paddingValues)
        ) {
            // Chart container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((200 * scale).dp) // Adjust height based on scale
                    .scale(scale) // Apply animated scale
                    .background(chartBackground),
                contentAlignment = Alignment.Center
            ) {
                // Display dynamic chart
                DynamicChart(
                    dataPoints = listOf(10f, 15f, 20f, 25f, 18f, 12f, 5f), // Mock data points
                    modifier = Modifier.fillMaxSize(),
                    lineColor = chartLineColor
                )
            }

            // List of sensor cards
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(sensorData) { _, (itemTitle, itemValue) ->
                    RoundedSensorCard(
                        title = itemTitle,
                        value = itemValue,
                        cardBackground = cardBackground,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )
                }
            }
        }
    }
}

// Composable for individual sensor card
@Composable
fun RoundedSensorCard(
    title: String, // Card title
    value: String, // Card value
    cardBackground: Color, // Card background color
    textColor: Color, // Primary text color
    secondaryTextColor: Color // Secondary text color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp,
        backgroundColor = cardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                fontSize = 14.sp,
                color = secondaryTextColor
            )
        }
    }
}

// Composable for drawing a dynamic line chart
@Composable
fun DynamicChart(
    dataPoints: List<Float>, // Data points for the chart
    modifier: Modifier = Modifier,
    lineColor: Color // Line color
) {
    Canvas(modifier = modifier) {
        val stepX = size.width / (dataPoints.size - 1) // X-axis step
        val maxY = dataPoints.maxOrNull() ?: 1f // Maximum Y value
        val minY = dataPoints.minOrNull() ?: (maxY - 1f) // Minimum Y value
        val rangeY = if (maxY - minY == 0f) 1f else maxY - minY // Y-axis range

        // Draw lines connecting data points
        for (i in 0 until dataPoints.size - 1) {
            val x1 = i * stepX
            val y1 = size.height - ((dataPoints[i] - minY) / rangeY) * size.height
            val x2 = (i + 1) * stepX
            val y2 = size.height - ((dataPoints[i + 1] - minY) / rangeY) * size.height

            drawLine(
                color = lineColor,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 4f,
                pathEffect = PathEffect.cornerPathEffect(10f)
            )
        }
    }
}
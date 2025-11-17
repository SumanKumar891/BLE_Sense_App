package com.blesense.app

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

@Composable
fun ChartScreen2(navController: NavController, title: String?, value: String?) {
    val actualTitle = title ?: "Unknown Title"
    value ?: "Unknown Value"

    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    // Hardcoded English strings
    val unknownTitle = "Unknown Title"
    val sensorTitlePrefix = "Bluetooth Sensor"
    val advertisingType = "Advertising type"
    val dataStatus = "Data status"
    val primaryPhy = "Primary PHY"
    val bluetooth5 = "Bluetooth 5"
    val bluetooth42 = "Bluetooth 4.2"
    val complete = "Complete"
    val partial = "Partial"
    val le1m = "LE 1M"
    val leCoded = "LE Coded"

    // Mock sensor data with hardcoded strings
    val sensorData = listOf(
        "$sensorTitlePrefix 1" to "$advertisingType: $bluetooth5\n$dataStatus: $complete\n$primaryPhy: $le1m",
        "$sensorTitlePrefix 2" to "$advertisingType: $bluetooth5\n$dataStatus: $complete\n$primaryPhy: $le1m",
        "$sensorTitlePrefix 3" to "$advertisingType: $bluetooth42\n$dataStatus: $partial\n$primaryPhy: $le1m",
        "$sensorTitlePrefix 4" to "$advertisingType: $bluetooth5\n$dataStatus: $complete\n$primaryPhy: $leCoded",
        "$sensorTitlePrefix 1" to "$advertisingType: $bluetooth5\n$dataStatus: $complete\n$primaryPhy: $le1m",
        "$sensorTitlePrefix 2" to "$advertisingType: $bluetooth5\n$dataStatus: $complete\n$primaryPhy: $le1m",
        "$sensorTitlePrefix 3" to "$advertisingType: $bluetooth42\n$dataStatus: $partial\n$primaryPhy: $le1m"
    )

    val backgroundGradient = if (isDarkMode) {
        Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF424242)))
    } else {
        Brush.verticalGradient(listOf(Color.White, Color.LightGray))
    }
    val appBarBackground = if (isDarkMode) Color(0xFF121212) else Color.White
    val cardBackground = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val chartBackground = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray
    val chartLineColor = if (isDarkMode) Color(0xFFBB86FC) else Color.Blue

    val listState = rememberLazyListState()
    var isSmallSize by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isSmallSize) 0.5f else 1f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = ""
    )

    LaunchedEffect(remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }) {
        isSmallSize = listState.firstVisibleItemScrollOffset > 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (title == null) unknownTitle else actualTitle,
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
        backgroundColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((200 * scale).dp)
                    .scale(scale)
                    .background(chartBackground),
                contentAlignment = Alignment.Center
            ) {
                DynamicChart(
                    dataPoints = listOf(10f, 15f, 20f, 25f, 18f, 12f, 5f),
                    modifier = Modifier.fillMaxSize(),
                    lineColor = chartLineColor
                )
            }

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

@Composable
fun RoundedSensorCard(
    title: String,
    value: String,
    cardBackground: Color,
    textColor: Color,
    secondaryTextColor: Color
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

@Composable
fun DynamicChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color
) {
    Canvas(modifier = modifier) {
        val stepX = size.width / (dataPoints.size - 1)
        val maxY = dataPoints.maxOrNull() ?: 1f
        val minY = dataPoints.minOrNull() ?: (maxY - 1f)
        val rangeY = if (maxY - minY == 0f) 1f else maxY - minY

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
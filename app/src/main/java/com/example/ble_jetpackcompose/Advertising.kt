package com.example.ble_jetpackcompose

import android.R.attr.radius
import android.R.attr.text
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.fontscaling.MathUtils.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files.size
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.io.path.Path
import kotlin.io.path.moveTo
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Update TranslatedAdvertisingText to include ammonia
data class TranslatedAdvertisingText(
    val advertisingDataTitle: String = "Advertising Data",
    val deviceNameLabel: String = "Device Name",
    val nodeIdLabel: String = "Node ID",
    val downloadData: String = "DOWNLOAD DATA",
    val exportingData: String = "EXPORTING DATA...",
    val temperature: String = "Temperature",
    val humidity: String = "Humidity",
    val xAxis: String = "X-Axis",
    val yAxis: String = "Y-Axis",
    val zAxis: String = "Z-Axis",
    val nitrogen: String = "Nitrogen",
    val phosphorus: String = "Phosphorus",
    val potassium: String = "Potassium",
    val moisture: String = "Moisture",
    val electricConductivity: String = "Electric Conductivity",
    val pH: String = "pH",
    val lightIntensity: String = "Light Intensity",
    val speed: String = "Speed",
    val distance: String = "Distance",
    val objectDetected: String = "Object Detected",
    val steps: String = "Steps",
    val ammonia: String = "Ammonia", // New field
    val resetSteps: String = "RESET STEPS",
    val warningTitle: String = "Warning",
    val warningMessage: String = "The %s has exceeded the threshold of %s!",
    val dismissButton: String = "Dismiss"

)

@Composable
fun AdvertisingDataScreen(
    deviceAddress: String,
    deviceName: String,
    navController: NavController,
    deviceId: String,
) {
    val context: Context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf(MediaPlayer.create(context, R.raw.nuclear_alarm)) }
    val viewModel: BluetoothScanViewModel<Any?> =
        viewModel(factory = BluetoothScanViewModelFactory(context))
    val activity = context as Activity

    // Theme and Language state
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()

    // Collect device list and current device
    val devices by viewModel.devices.collectAsState()
    val currentDevice by remember(devices, deviceAddress) {
        derivedStateOf { devices.find { it.address == deviceAddress } }
    }



    // Threshold and alarm state
    var thresholdValue by remember { mutableStateOf("") }
    var isAlarmActive by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var parameterType by remember { mutableStateOf("Temperature") }
    var isThresholdSet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

//    // MediaPlayer for alarm
//    val context = LocalContext.current
//    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Initialize MediaPlayer
    LaunchedEffect(Unit) {
        mediaPlayer = MediaPlayer.create(context, R.raw.nuclear_alarm).apply {
            isLooping = true
        }
    }
   // Clean up MediaPlayer
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }




    // Blinking animation state
    val isBlinking by remember(isAlarmActive) { derivedStateOf { isAlarmActive } }
    val blinkAlpha by animateFloatAsState(
        targetValue = if (isBlinking) 0.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    // Use derivedStateOf to minimize recompositions
    val ammoniaValue by remember(currentDevice?.sensorData) {
        derivedStateOf {
            when (val sensorData = currentDevice?.sensorData) {
                is BluetoothScanViewModel.SensorData.AmmoniaSensorData -> {
                    sensorData.ammonia.replace(" ppm", "").toFloatOrNull() ?: 0f
                }

                else -> 0f
            }
        }
    }

// Then use it in your UI
    AmmoniaRingAnimation(ammoniaValue = ammoniaValue)

    var displayedAmmoniaValue by remember { mutableStateOf(0f) }
    val debouncedAmmoniaValue by remember(ammoniaValue) {
        derivedStateOf { ammoniaValue }
    }

    LaunchedEffect(debouncedAmmoniaValue) {
        displayedAmmoniaValue = debouncedAmmoniaValue
    }

    AmmoniaRingAnimation(ammoniaValue = displayedAmmoniaValue)




    // Threshold check for SHT40Data and AmmoniaSensorData
    // Inside AdvertisingDataScreen composable, modify the LaunchedEffect for threshold checks:

    // Replace the current LaunchedEffect for threshold checks with this:
    LaunchedEffect(currentDevice, thresholdValue, parameterType, isThresholdSet) {
        delay(500L) // Debounce
        if (isThresholdSet) {
            val threshold = thresholdValue.toFloatOrNull()
            if (threshold != null) {
                when (val sensorData = currentDevice?.sensorData) {
                    is BluetoothScanViewModel.SensorData.SHT40Data -> {
                        val valueToCheck = when (parameterType) {
                            "Temperature" -> sensorData.temperature.toFloatOrNull()
                            "Humidity" -> sensorData.humidity.toFloatOrNull()
                            else -> null
                        }
                        if (valueToCheck != null) {
                            isAlarmActive = valueToCheck > threshold
                        } else {
                            isAlarmActive = false
                        }
                    }
                    is BluetoothScanViewModel.SensorData.AmmoniaSensorData -> {
                        if (parameterType == "Ammonia") {
                            val ammoniaValue = sensorData.ammonia.replace(" ppm", "").toFloatOrNull() ?: 0f
                            isAlarmActive = ammoniaValue != null && ammoniaValue > threshold
                        } else {
                            isAlarmActive = false
                        }
                    }
                    else -> {
                        isAlarmActive = false
                    }
                }
                if (isAlarmActive) {
                    showAlertDialog = true
                    if (!mediaPlayer.isPlaying) {
                        try {
                            mediaPlayer.isLooping = true
                            mediaPlayer.start()
                        } catch (e: IllegalStateException) {
                            mediaPlayer.reset()
                            MediaPlayer.create(context, R.raw.nuclear_alarm)?.let {
                                mediaPlayer.release()
                                mediaPlayer = it
                                mediaPlayer.isLooping = true
                                mediaPlayer.start()
                            }
                        }
                    }
                } else {
                    try {
                        mediaPlayer.stop()
                        mediaPlayer.prepare()
                    } catch (e: IllegalStateException) {
                        mediaPlayer.reset()
                        MediaPlayer.create(context, R.raw.nuclear_alarm)?.let {
                            mediaPlayer.release()
                            mediaPlayer = it
                        }
                    }
                    showAlertDialog = false
                }
            } else {
                isAlarmActive = false
                showAlertDialog = false
                try {
                    mediaPlayer.stop()
                    mediaPlayer.prepare()
                } catch (e: IllegalStateException) {
                    mediaPlayer.reset()
                    MediaPlayer.create(context, R.raw.nuclear_alarm)?.let {
                        mediaPlayer.release()
                        mediaPlayer = it
                    }
                }
            }
        } else {
            isAlarmActive = false
            showAlertDialog = false
            try {
                mediaPlayer.stop()
                mediaPlayer.prepare()
            } catch (e: IllegalStateException) {
                mediaPlayer.reset()
                MediaPlayer.create(context, R.raw.nuclear_alarm)?.let {
                    mediaPlayer.release()
                    mediaPlayer = it
                }
            }
        }
    }

    // Clean up MediaPlayer on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }


    // Translated text state
    var translatedText by remember {
        mutableStateOf(
            TranslatedAdvertisingText(
                advertisingDataTitle = TranslationCache.get("Advertising Data-$currentLanguage")
                    ?: "Advertising Data",
                deviceNameLabel = TranslationCache.get("Device Name-$currentLanguage")
                    ?: "Device Name",
                nodeIdLabel = TranslationCache.get("Node ID-$currentLanguage") ?: "Node ID",
                downloadData = TranslationCache.get("DOWNLOAD DATA-$currentLanguage")
                    ?: "DOWNLOAD DATA",
                exportingData = TranslationCache.get("EXPORTING DATA...-$currentLanguage")
                    ?: "EXPORTING DATA...",
                temperature = TranslationCache.get("Temperature-$currentLanguage") ?: "Temperature",
                humidity = TranslationCache.get("Humidity-$currentLanguage") ?: "Humidity",
                xAxis = TranslationCache.get("X-Axis-$currentLanguage") ?: "X-Axis",
                yAxis = TranslationCache.get("Y-Axis-$currentLanguage") ?: "Y-Axis",
                zAxis = TranslationCache.get("Z-Axis-$currentLanguage") ?: "Z-Axis",
                nitrogen = TranslationCache.get("Nitrogen-$currentLanguage") ?: "Nitrogen",
                phosphorus = TranslationCache.get("Phosphorus-$currentLanguage") ?: "Phosphorus",
                potassium = TranslationCache.get("Potassium-$currentLanguage") ?: "Potassium",
                moisture = TranslationCache.get("Moisture-$currentLanguage") ?: "Moisture",
                electricConductivity = TranslationCache.get("Electric Conductivity-$currentLanguage")
                    ?: "Electric Conductivity",
                pH = TranslationCache.get("pH-$currentLanguage") ?: "pH",
                lightIntensity = TranslationCache.get("Light Intensity-$currentLanguage")
                    ?: "Light Intensity",
                speed = TranslationCache.get("Speed-$currentLanguage") ?: "Speed",
                distance = TranslationCache.get("Distance-$currentLanguage") ?: "Distance",
                objectDetected = TranslationCache.get("Object Detected-$currentLanguage")
                    ?: "Object Detected",
                steps = TranslationCache.get("Steps-$currentLanguage") ?: "Steps",
                ammonia = TranslationCache.get("Ammonia-$currentLanguage") ?: "Ammonia",
                resetSteps = TranslationCache.get("RESET STEPS-$currentLanguage") ?: "RESET STEPS",
                warningTitle = TranslationCache.get("Warning-$currentLanguage") ?: "Warning",
                warningMessage = TranslationCache.get("Threshold Exceeded-$currentLanguage")
                    ?: "The %s has exceeded the threshold of %s!",
                dismissButton = TranslationCache.get("Dismiss-$currentLanguage") ?: "Dismiss"
            )
        )
    }

    // Preload translations
    LaunchedEffect(currentLanguage) {
        val translator = GoogleTranslationService()
        val textsToTranslate = listOf(
            "Advertising Data",
            "Device Name",
            "Node ID",
            "DOWNLOAD DATA",
            "EXPORTING DATA...",
            "Temperature",
            "Humidity",
            "X-Axis",
            "Y-Axis",
            "Z-Axis",
            "Nitrogen",
            "Phosphorus",
            "Potassium",
            "Moisture",
            "Electric Conductivity",
            "pH",
            "Light Intensity",
            "Speed",
            "Distance",
            "Object Detected",
            "Steps",
            "Ammonia",
            "RESET STEPS",
            "Warning",
            "Threshold Exceeded",
            "Dismiss"
        )
        val translatedList = translator.translateBatch(textsToTranslate, currentLanguage)
        translatedText = TranslatedAdvertisingText(
            advertisingDataTitle = translatedList[0],
            deviceNameLabel = translatedList[1],
            nodeIdLabel = translatedList[2],
            downloadData = translatedList[3],
            exportingData = translatedList[4],
            temperature = translatedList[5],
            humidity = translatedList[6],
            xAxis = translatedList[7],
            yAxis = translatedList[8],
            zAxis = translatedList[9],
            nitrogen = translatedList[10],
            phosphorus = translatedList[11],
            potassium = translatedList[12],
            moisture = translatedList[13],
            electricConductivity = translatedList[14],
            pH = translatedList[15],
            lightIntensity = translatedList[16],
            speed = translatedList[17],
            distance = translatedList[18],
            objectDetected = translatedList[19],
            steps = translatedList[20],
            ammonia = translatedList[21],
            resetSteps = translatedList[22],
            warningTitle = translatedList[23],
            warningMessage = translatedList[24],
            dismissButton = translatedList[25]
        )
    }

    // Display data for all sensor types
    val displayData by remember(currentDevice?.sensorData, translatedText) {
        derivedStateOf {
            when (val sensorData = currentDevice?.sensorData) {
                is BluetoothScanViewModel.SensorData.SHT40Data -> listOf(
                    translatedText.temperature to "${sensorData.temperature.takeIf { it.isNotEmpty() } ?: "0"}°C",
                    translatedText.humidity to "${sensorData.humidity.takeIf { it.isNotEmpty() } ?: "0"}%"
                )

                is BluetoothScanViewModel.SensorData.LuxData -> listOf(
                    translatedText.lightIntensity to "${sensorData.calculatedLux} LUX"
                )

                is BluetoothScanViewModel.SensorData.LIS2DHData -> listOf(
                    translatedText.xAxis to "${sensorData.x.takeIf { it.isNotEmpty() } ?: "0"} m/s²",
                    translatedText.yAxis to "${sensorData.y.takeIf { it.isNotEmpty() } ?: "0"} m/s²",
                    translatedText.zAxis to "${sensorData.z.takeIf { it.isNotEmpty() } ?: "0"} m/s²"
                )

                is BluetoothScanViewModel.SensorData.SoilSensorData -> listOf(
                    translatedText.nitrogen to "${sensorData.nitrogen.takeIf { it.isNotEmpty() } ?: "0"} mg/kg",
                    translatedText.phosphorus to "${sensorData.phosphorus.takeIf { it.isNotEmpty() } ?: "0"} mg/kg",
                    translatedText.potassium to "${sensorData.potassium.takeIf { it.isNotEmpty() } ?: "0"} mg/kg",
                    translatedText.moisture to "${sensorData.moisture.takeIf { it.isNotEmpty() } ?: "0"}%",
                    translatedText.temperature to "${sensorData.temperature.takeIf { it.isNotEmpty() } ?: "0"}°C",
                    translatedText.electricConductivity to "${sensorData.ec.takeIf { it.isNotEmpty() } ?: "0"} mS/cm",
                    translatedText.pH to "${sensorData.pH.takeIf { it.isNotEmpty() } ?: "0"}"
                )

                is BluetoothScanViewModel.SensorData.SDTData -> listOf(
                    translatedText.speed to "${sensorData.speed.takeIf { it.isNotEmpty() } ?: "0"} m/s",
                    translatedText.distance to "${sensorData.distance.takeIf { it.isNotEmpty() } ?: "0"} m"
                )

                is BluetoothScanViewModel.SensorData.ObjectDetectorData -> listOf(
                    translatedText.objectDetected to if (sensorData.detection) "Yes" else "No"
                )

                is BluetoothScanViewModel.SensorData.StepCounterData -> listOf(
                    translatedText.steps to "${sensorData.steps.takeIf { it.isNotEmpty() } ?: "0"}"
                )

                is BluetoothScanViewModel.SensorData.AmmoniaSensorData -> {
                    val ammoniaValue = sensorData.ammonia.replace(" ppm", "").toFloatOrNull() ?: 0f
                    listOf(
                        translatedText.ammonia to "${sensorData.ammonia}",
                        "Raw Data" to sensorData.rawData
                    )
                }

                else -> emptyList()
            }
        }
    }

    // Theme-based colors
    val backgroundGradient = if (isDarkMode) {
        Brush.verticalGradient(listOf(Color(0xFF1E1E1E), Color(0xFF424242)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF0A74DA), Color(0xFFADD8E6)))
    }
    val cardBackground = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFF2A9EE5)
    val cardGradient = if (isDarkMode) {
        Brush.verticalGradient(listOf(Color(0xFF424242), Color(0xFF212121)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF2A9EE5), Color(0xFF076FB8)))
    }
    val textColor = if (isDarkMode) Color.White else Color.White
    val buttonColor = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF0A74DA)

    // Start scanning
    LaunchedEffect(Unit) {
        viewModel.startScan(activity)
    }

    // Clean up scanning
    DisposableEffect(navController) {
        onDispose {
            viewModel.stopScan()
            viewModel.clearDevices()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(WindowInsets.systemBars.asPaddingValues()),
        contentAlignment = Alignment.Center
    ) {
        // Blinking red overlay
        if (isAlarmActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = blinkAlpha))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())  // <-- Add this for scrolling
                .padding(WindowInsets.systemBars.asPaddingValues()),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderSection(
                navController = navController,
                viewModel = viewModel,
                deviceAddress = deviceAddress,
                translatedText = translatedText,
                textColor = textColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            DeviceInfoSection(
                deviceName = deviceName,
                deviceAddress = deviceAddress,
                deviceId = deviceId,
                translatedText = translatedText,
                cardBackground = cardBackground,
                cardGradient = cardGradient,
                textColor = textColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            ResponsiveDataCards(
                data = displayData,
                cardBackground = cardBackground,
                cardGradient = cardGradient,
                textColor = textColor
            )

            Spacer(modifier = Modifier.height(32.dp))


            // In the AdvertisingDataScreen's main Column, update the ThresholdInputSection usage:
            if (currentDevice?.sensorData is BluetoothScanViewModel.SensorData.SHT40Data ||
                currentDevice?.sensorData is BluetoothScanViewModel.SensorData.AmmoniaSensorData) {
                ThresholdInputSection(
                    thresholdValue = thresholdValue,
                    onThresholdChange = { thresholdValue = it },
                    parameterType = parameterType,
                    onParameterChange = { parameterType = it },
                    isDarkMode = isDarkMode,
                    sensorData = currentDevice?.sensorData,
                    onConfirmThreshold = {
                        if (thresholdValue.toFloatOrNull() != null) {
                            isThresholdSet = true
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Reset Steps button for StepCounterData
            if (currentDevice?.sensorData is BluetoothScanViewModel.SensorData.StepCounterData) {
                ResetStepsButton(
                    viewModel = viewModel,
                    deviceAddress = deviceAddress,
                    translatedText = translatedText
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            DownloadButton(
                viewModel = viewModel,
                deviceAddress = deviceAddress,
                deviceName = deviceName,
                deviceId = deviceId,
                translatedText = translatedText
            )

            // Update the AlertDialog section
            // Alert Dialog for threshold
            if (showAlertDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showAlertDialog = false
                        isAlarmActive = false
                        isThresholdSet = false
                        try {
                            mediaPlayer.stop()
                            mediaPlayer.prepare()
                        } catch (e: IllegalStateException) {
                            mediaPlayer.reset()
                            MediaPlayer.create(context, R.raw.nuclear_alarm)?.let {
                                mediaPlayer.release()
                                mediaPlayer = it
                            }
                        }
                    },
                    title = { Text(translatedText.warningTitle) },
                    text = {
                        Text(
                            text = translatedText.warningMessage.format(
                                parameterType,
                                thresholdValue
                            )
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showAlertDialog = false
                                isAlarmActive = false
                                isThresholdSet = false
                                try {
                                    mediaPlayer.stop()
                                    mediaPlayer.prepare()
                                } catch (e: IllegalStateException) {
                                    mediaPlayer.reset()
                                    MediaPlayer.create(context, R.raw.nuclear_alarm)?.let {
                                        mediaPlayer.release()
                                        mediaPlayer = it
                                    }
                                }
                            }
                        ) {
                            Text(translatedText.dismissButton)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AmmoniaSensorDisplay(
    ammoniaValue: Float,  // Directly use the incoming value
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Simplified value handling - no debounce needed
        AmmoniaRingAnimation(ammoniaValue = ammoniaValue)

        // Current value display
        Text(
            text = "%.1f ppm".format(ammoniaValue),
            style = MaterialTheme.typography.displayMedium,
            color = when {
                ammoniaValue > 50 -> Color.Red
                ammoniaValue > 25 -> Color.Yellow
                else -> Color.Green
            }
        )
    }
}

@Composable
fun AmmoniaRingAnimation(
    ammoniaValue: Float,
    modifier: Modifier = Modifier
) {
    // Animate the fill percentage
    val animatedFill by animateFloatAsState(
        targetValue = (ammoniaValue / 100f).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 100f
        ),
        label = "ammoniaFill"
    )

    // Color based on ammonia level
    val liquidColor by animateColorAsState(
        targetValue = when {
            ammoniaValue <= 25 -> Color(0xFF4CAF50)  // Green - safe
            ammoniaValue <= 50 -> Color(0xFFFFC107)  // Yellow - caution
            else -> Color(0xFFF44336)               // Red - danger
        },
        animationSpec = tween(durationMillis = 300),
        label = "liquidColor"
    )

    Box(
        modifier = modifier
            .size(220.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension * 0.4f
            val ringWidth = size.minDimension * 0.1f

            // Background ring
            drawArc(
                color = Color(0xFF333333).copy(alpha = 0.3f),
                startAngle = 270f,
                sweepAngle = 360f,
                useCenter = false,
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius),
                style = Stroke(width = ringWidth)
            )

            // Filled part
            drawArc(
                color = liquidColor.copy(alpha = 0.7f),
                startAngle = 270f,
                sweepAngle = -360f * animatedFill,  // Negative for counter-clockwise
                useCenter = false,
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius),
                style = Stroke(width = ringWidth, cap = StrokeCap.Round)
            )
        }

        // Current value display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.1f".format(ammoniaValue),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "ppm",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ThresholdInputSection(
    thresholdValue: String,
    onThresholdChange: (String) -> Unit,
    parameterType: String,
    onParameterChange: (String) -> Unit,
    isDarkMode: Boolean,
    sensorData: BluetoothScanViewModel.SensorData?,
    onConfirmThreshold: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Parameter type toggle based on sensor data
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val parameters = when (sensorData) {
                is BluetoothScanViewModel.SensorData.SHT40Data -> listOf("Temperature", "Humidity")
                is BluetoothScanViewModel.SensorData.AmmoniaSensorData -> listOf("Ammonia")
                else -> emptyList()
            }
            parameters.forEach { type ->
                Button(
                    onClick = { onParameterChange(type) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (parameterType == type) {
                            if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF0A74DA)
                        } else {
                            if (isDarkMode) Color(0xFF424242) else Color(0xFFADD8E6)
                        }
                    )
                ) {
                    Text(type)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Threshold input field
        TextField(
            value = thresholdValue,
            onValueChange = { onThresholdChange(it) },
            label = { Text("Enter $parameterType Threshold") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = thresholdValue.isNotEmpty() && thresholdValue.toFloatOrNull() == null,
            supportingText = {
                if (thresholdValue.isNotEmpty() && thresholdValue.toFloatOrNull() == null) {
                    Text("Please enter a valid number")
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color.White,
                unfocusedContainerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color.White,
                focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
                errorContainerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color.White
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Confirm button
        Button(
            onClick = onConfirmThreshold,
            enabled = thresholdValue.isNotEmpty() && thresholdValue.toFloatOrNull() != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF0A74DA)
            )
        ) {
            Text("Confirm Threshold")
        }
    }
}

@Composable
private fun ResetStepsButton(
    viewModel: BluetoothScanViewModel<Any?>,
    deviceAddress: String,
    translatedText: TranslatedAdvertisingText
) {
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val buttonBackgroundColor = if (isDarkMode) Color(0xFFFF5252) else Color(0xFFE53935)
    val buttonTextColor = Color.White

    Button(
        onClick = {
            viewModel.resetStepCounter(deviceAddress)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonBackgroundColor)
    ) {
        Text(
            text = translatedText.resetSteps,
            color = buttonTextColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HeaderSection(
    navController: NavController,
    viewModel: BluetoothScanViewModel<Any?>,
    deviceAddress: String,
    translatedText: TranslatedAdvertisingText,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                viewModel.stopScan()
                navController.popBackStack()
            }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = textColor
            )
        }

        Text(
            text = translatedText.advertisingDataTitle,
            fontFamily = helveticaFont,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        IconButton(
            onClick = {
                navController.navigate("chart_screen/$deviceAddress")
            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.graph),
                contentDescription = "Graph Icon",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun DeviceInfoSection(
    deviceName: String,
    deviceAddress: String,
    deviceId: String,
    translatedText: TranslatedAdvertisingText,
    cardBackground: Color,
    cardGradient: Brush,
    textColor: Color
) {
    InfoCard(
        text = "${translatedText.deviceNameLabel}: $deviceName ($deviceAddress)",
        cardBackground = cardBackground,
        cardGradient = cardGradient,
        textColor = textColor
    )
    Spacer(modifier = Modifier.height(8.dp))
    InfoCard(
        text = "${translatedText.nodeIdLabel}: $deviceId",
        cardBackground = cardBackground,
        cardGradient = cardGradient,
        textColor = textColor
    )
}

@Composable
private fun InfoCard(
    text: String,
    cardBackground: Color,
    cardGradient: Brush,
    textColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardBackground
    ) {
        Box(
            modifier = Modifier
                .background(cardGradient)
                .padding(16.dp)
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
private fun DownloadButton(
    viewModel: BluetoothScanViewModel<Any?>,
    deviceAddress: String,
    deviceName: String,
    deviceId: String,
    translatedText: TranslatedAdvertisingText,
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            exportDataToCSV(context, uri, viewModel, deviceAddress, deviceName, deviceId) {
                isExporting = false
            }
        }
    }

    val buttonBackgroundColor = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF0A74DA)
    val buttonTextColor = if (isDarkMode) Color.Black else Color.White

    Button(
        onClick = {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "sensor_data_${deviceId}_$timestamp.csv"
            createDocumentLauncher.launch(filename)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = !isExporting,
        colors = ButtonDefaults.buttonColors(containerColor = buttonBackgroundColor)
    ) {
        Text(
            text = if (isExporting) translatedText.exportingData else translatedText.downloadData,
            color = buttonTextColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun exportDataToCSV(
    context: Context,
    uri: Uri,
    viewModel: BluetoothScanViewModel<Any?>,
    deviceAddress: String,
    deviceName: String,
    deviceId: String,
    onComplete: () -> Unit
) {
    MainScope().launch {
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val historicalData = viewModel.getHistoricalDataForDevice(deviceAddress)
                    if (historicalData.isEmpty()) {
                        val devices = viewModel.devices.value
                        val currentDevice = devices.find { it.address == deviceAddress }
                        currentDevice?.sensorData?.let { sensorData ->
                            val currentTime = System.currentTimeMillis()
                            val entry = BluetoothScanViewModel.HistoricalDataEntry(
                                timestamp = currentTime,
                                sensorData = sensorData
                            )
                            historicalData.toMutableList().add(entry)
                        }
                    }

                    val headerBuilder = StringBuilder()
                    headerBuilder.append("Timestamp,Device Name,Device Address,Node ID,")
                    val sensorType = if (historicalData.isNotEmpty()) historicalData[0].sensorData else null
                    when (sensorType) {
                        is BluetoothScanViewModel.SensorData.SHT40Data -> headerBuilder.append("Temperature (°C),Humidity (%)")
                        is BluetoothScanViewModel.SensorData.LIS2DHData -> headerBuilder.append("X-Axis (m/s²),Y-Axis (m/s²),Z-Axis (m/s²)")
                        is BluetoothScanViewModel.SensorData.SoilSensorData -> headerBuilder.append("Nitrogen (mg/kg),Phosphorus (mg/kg),Potassium (mg/kg),Moisture (%),Temperature (°C),Electric Conductivity (mS/cm),pH")
                        is BluetoothScanViewModel.SensorData.LuxData -> headerBuilder.append("Light Intensity (LUX)")
                        is BluetoothScanViewModel.SensorData.SDTData -> headerBuilder.append("Speed (m/s),Distance (m)")
                        is BluetoothScanViewModel.SensorData.ObjectDetectorData -> headerBuilder.append("Object Detected")
                        is BluetoothScanViewModel.SensorData.StepCounterData -> headerBuilder.append("Steps")
                        is BluetoothScanViewModel.SensorData.AmmoniaSensorData -> headerBuilder.append("Ammonia (ppm)")
                        else -> {}
                    }
                    headerBuilder.append("\n")
                    outputStream.write(headerBuilder.toString().toByteArray())

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                    historicalData.forEach { entry ->
                        val dataBuilder = StringBuilder()
                        dataBuilder.append("${dateFormat.format(Date(entry.timestamp))},$deviceName,$deviceAddress,$deviceId,")
                        when (val sensorData = entry.sensorData) {
                            is BluetoothScanViewModel.SensorData.SHT40Data -> dataBuilder.append("${sensorData.temperature},${sensorData.humidity}")
                            is BluetoothScanViewModel.SensorData.LIS2DHData -> dataBuilder.append("${sensorData.x},${sensorData.y},${sensorData.z}")
                            is BluetoothScanViewModel.SensorData.SoilSensorData -> dataBuilder.append("${sensorData.nitrogen},${sensorData.phosphorus},${sensorData.potassium},${sensorData.moisture},${sensorData.temperature},${sensorData.ec},${sensorData.pH}")
                            is BluetoothScanViewModel.SensorData.LuxData -> dataBuilder.append("${sensorData.calculatedLux}")
                            is BluetoothScanViewModel.SensorData.SDTData -> dataBuilder.append("${sensorData.speed},${sensorData.distance}")
                            is BluetoothScanViewModel.SensorData.ObjectDetectorData -> dataBuilder.append("${sensorData.detection}")
                            is BluetoothScanViewModel.SensorData.StepCounterData -> dataBuilder.append("${sensorData.steps}")
                            is BluetoothScanViewModel.SensorData.AmmoniaSensorData -> dataBuilder.append("${sensorData.ammonia}")
                            null -> {}
                        }
                        dataBuilder.append("\n")
                        outputStream.write(dataBuilder.toString().toByteArray())
                        if (historicalData.indexOf(entry) % 100 == 0) outputStream.flush()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}

@Composable
private fun LuxAnimationSection(
    luxData: BluetoothScanViewModel.SensorData.LuxData,
    isDarkMode: Boolean
) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        SunWithRayAnimation(
            lux = luxData.calculatedLux,
            isDarkMode = isDarkMode
        )
    }
}

@Composable
fun SunWithRayAnimation(
    lux: Float,
    isDarkMode: Boolean,
    rayThickness: Dp = 2.dp,
    rayCount: Int = 12,
    maxLux: Float = 255f
) {
    val luxThresholds = listOf(200f, 700f, 1500f)
    val rayStage = when {
        lux >= luxThresholds[2] -> 3
        lux >= luxThresholds[1] -> 2
        lux >= luxThresholds[0] -> 1
        else -> 0
    }
    val rayLengths = listOf(10.dp, 100.dp, 150.dp, 200.dp)
    val currentRayLength = rayLengths[rayStage]
    val normalizedLux = lux.coerceIn(0f, maxLux) / maxLux
    val rayOpacity = normalizedLux.coerceIn(0.1f, 1f)
    val rayColor = if (isDarkMode) {
        Color(
            red = 1f,
            green = (0.7f + 0.3f * normalizedLux).coerceIn(0.7f, 1f),
            blue = (0.5f * normalizedLux).coerceIn(0f, 0.5f)
        )
    } else {
        Color(
            red = 1f,
            green = (0.7f + 0.3f * normalizedLux).coerceIn(0.7f, 1f),
            blue = (0.2f * normalizedLux).coerceIn(0f, 0.2f)
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        for (i in 0 until rayCount) {
            Box(
                modifier = Modifier
                    .width(rayThickness)
                    .height(currentRayLength)
                    .graphicsLayer {
                        rotationZ = (i * (360f / rayCount))
                        alpha = rayOpacity
                    }
                    .background(rayColor)
            )
        }
        Image(
            painter = painterResource(id = R.drawable.sun),
            contentDescription = "Sun",
            modifier = Modifier.size(100.dp)
        )
    }
}

@Composable
private fun ResponsiveDataCards(
    data: List<Pair<String, String>>,
    cardBackground: Color,
    cardGradient: Brush,
    textColor: Color
) {
    // Check if we have ammonia data
    val ammoniaData = data.find { it.first.contains("Ammonia", ignoreCase = true) }
    val otherData = data.filterNot { it.first.contains("Ammonia", ignoreCase = true) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Show ammonia animation if available
        ammoniaData?.let { (label, value) ->
            val ammoniaValue = value.replace(" ppm", "").toFloatOrNull() ?: 0f
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    fontSize = 18.sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                AmmoniaRingAnimation(ammoniaValue = ammoniaValue)
            }
        }

        // Show other data in cards
        when (otherData.size) {
            0 -> {
                Box(modifier = Modifier.height(100.dp))
            }
            1 -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DataCard(
                        label = otherData[0].first,
                        value = otherData[0].second,
                        cardBackground = cardBackground,
                        cardGradient = cardGradient,
                        textColor = textColor
                    )
                }
            }
            2 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    otherData.forEach { (label, value) ->
                        DataCard(
                            label = label,
                            value = value,
                            cardBackground = cardBackground,
                            cardGradient = cardGradient,
                            textColor = textColor
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    otherData.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowItems.forEach { (label, value) ->
                                DataCard(
                                    label = label,
                                    value = value,
                                    cardBackground = cardBackground,
                                    cardGradient = cardGradient,
                                    textColor = textColor
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.width(141.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DataCard(
    label: String,
    value: String,
    cardBackground: Color,
    cardGradient: Brush,
    textColor: Color
) {
    Surface(
        modifier = Modifier
            .height(95.dp)
            .width(141.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardBackground,
        tonalElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(cardGradient, RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 18.sp,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

}
package com.example.ble_jetpackcompose

import android.R.attr.radius
import android.R.attr.text
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color.*
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext


import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import java.nio.file.Files.copy
import java.nio.file.Files.size
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
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
    val dismissButton: String = "Dismiss",
    val reflectanceValues: String = "Reflectance Values",
    val rawData: String = "Raw Data",
    val doTemperature: String = "DO Temp",
    val doPercentage: String = "DO %",
    val doValue: String = "DO Value"

)


@Composable
fun AdvertisingDataScreen(
    deviceAddress: String,
    deviceName: String,
    navController: NavController,
    deviceId: String,
) {

    val context = LocalContext.current
    val activity = context as? Activity



    // Initialize ViewModel with explicit type parameters
    val viewModel: BluetoothScanViewModel<Any> = viewModel(
        factory = remember { BluetoothScanViewModelFactory(context) }
    )

    LaunchedEffect(deviceAddress) {
        viewModel.loadPersistedData(deviceAddress)
    }
    LaunchedEffect(activity) {
        activity?.let {
            viewModel.startScan(it)
        }
    }
    var mediaPlayer by remember { mutableStateOf(MediaPlayer.create(context, R.raw.nuclear_alarm)) }

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
    // lux  new data
    val luxValue by remember(currentDevice?.sensorData) {
        derivedStateOf {
            when (val sensorData = currentDevice?.sensorData) {
                is BluetoothScanViewModel.SensorData.LuxSensorData -> {
                    sensorData.lux.toFloatOrNull() ?: 0f
                }
                else -> 0f
            }
        }
    }

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
            "Dismiss",
            "DO Temp",
            "DO %",
            "DO Value"
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
            dismissButton = translatedList[25],
            doTemperature = translatedList[26],
            doPercentage = translatedList[27],
            doValue = translatedList[28]

        )
    }
    var nutrientPredictions by remember { mutableStateOf<SoilNutrientModelHelper.NutrientPrediction?>(null) }

// Display data for all sensor types
    val displayData by remember(currentDevice?.sensorData, translatedText, nutrientPredictions) {
        derivedStateOf {
            when (val sensorData = currentDevice?.sensorData) {

                is BluetoothScanViewModel.SensorData.SHT40Data -> listOf(
                    translatedText.temperature to "${sensorData.temperature.takeIf { it.isNotEmpty() } ?: "0"}°C",
                    translatedText.humidity to "${sensorData.humidity.takeIf { it.isNotEmpty() } ?: "0"}%"
                )

                is BluetoothScanViewModel.SensorData.DOSensorData -> listOf(
                    translatedText.doTemperature to "${sensorData.temperature}",
                    translatedText.doPercentage to sensorData.doPercentage,
                    translatedText.doValue to sensorData.doValue
                )

                is BluetoothScanViewModel.SensorData.SDTData -> listOf(
                    translatedText.speed to "${sensorData.speed.takeIf { it.isNotEmpty() } ?: "0"} m/s",
                    translatedText.distance to "${sensorData.distance.takeIf { it.isNotEmpty() } ?: "0"} m"
                )

                is BluetoothScanViewModel.SensorData.LIS2DHData -> listOf(
                    translatedText.xAxis to "${sensorData.x.takeIf { it.isNotEmpty() } ?: "0"} m/s²",
                    translatedText.yAxis to "${sensorData.y.takeIf { it.isNotEmpty() } ?: "0"} m/s²",
                    translatedText.zAxis to "${sensorData.z.takeIf { it.isNotEmpty() } ?: "0"} m/s²"
                )

                is BluetoothScanViewModel.SensorData.SDTData -> listOf(
                    translatedText.speed to "${sensorData.speed.takeIf { it.isNotEmpty() } ?: "0"} m/s",
                    translatedText.distance to "${sensorData.distance.takeIf { it.isNotEmpty() } ?: "0"} m"
                )

                is BluetoothScanViewModel.SensorData.ObjectDetectorData -> listOf(
                    translatedText.objectDetected to if (sensorData.detection) "Yes" else "No"
                )

                // soil senosr
                is BluetoothScanViewModel.SensorData.SoilSensorData -> listOf(
                    translatedText.nitrogen to "${sensorData.nitrogen.takeIf { it.isNotEmpty() } ?: "0"} mg/kg",
                    translatedText.phosphorus to "${sensorData.phosphorus.takeIf { it.isNotEmpty() } ?: "0"} mg/kg",
                    translatedText.potassium to "${sensorData.potassium.takeIf { it.isNotEmpty() } ?: "0"} mg/kg",
                    translatedText.moisture to "${sensorData.moisture.takeIf { it.isNotEmpty() } ?: "0"}%",
                    translatedText.temperature to "${sensorData.temperature.takeIf { it.isNotEmpty() } ?: "0"}°C",
                    translatedText.electricConductivity to "${sensorData.ec.takeIf { it.isNotEmpty() } ?: "0"} mS/cm",
                    translatedText.pH to "${sensorData.pH.takeIf { it.isNotEmpty() } ?: "0"}"
                )
                is BluetoothScanViewModel.SensorData.StepCounterData -> listOf(
                    translatedText.steps to "${sensorData.steps.takeIf { it.isNotEmpty() } ?: "0"}"
                )

                is BluetoothScanViewModel.SensorData.AmmoniaSensorData -> listOf(
                    translatedText.ammonia to sensorData.ammonia,
                    translatedText.rawData to sensorData.rawData
                )

                is BluetoothScanViewModel.SensorData.OpticalSensorData -> {
                    val formattedValues = sensorData.reflectanceValues.mapIndexed { index, value ->
                        "Channel ${index + 1}: ${"%.6f".format(value)}"
                    }

                    val baseData = listOf(
                        translatedText.reflectanceValues to formattedValues.joinToString("\n"),
                        translatedText.rawData to sensorData.rawData
                    )

                    nutrientPredictions?.let { predictions ->
                        baseData + listOf(
                            "Nutrient Predictions" to
                                    "N: ${predictions.nitrogen} m/kg, " +
                                    "P: ${predictions.phosphorus} mg/kg, " +
                                    "K: ${predictions.potassium} mg/kg, " +
                                    "OC: ${predictions.organicCarbon} %"
                        )
                    } ?: baseData
                }
                else -> emptyList()
            }
        }
    }


// Trigger prediction when reflectance data arrives

    LaunchedEffect(currentDevice?.sensorData) {
        val device = currentDevice
        if (device?.sensorData is BluetoothScanViewModel.SensorData.OpticalSensorData) {
            val rawReflectance = device.sensorData.reflectanceValues
            Log.d("NutrientUI", "Raw reflectance from BLE: $rawReflectance")

            val safeReflectance = rawReflectance.mapIndexed { idx, v ->
                if (v <= 0f) {
                    Log.d("NutrientUI", "Reflectance[$idx]=$v → replaced with 0.01f")
                    0.01f
                } else v
            }
            Log.d("NutrientUI", "Prepared reflectance to model: $safeReflectance")

            if (safeReflectance.size == 18) {
                try {
                    val helper = SoilNutrientModelHelper(context)
                    val predictions = withContext(Dispatchers.Default) {
                        helper.predictNutrients(safeReflectance)
                    }
                    helper.close()
                    Log.d("NutrientUI", "✅ Predicted nutrients: $predictions")
                    nutrientPredictions = predictions
                } catch (e: Exception) {
                    Log.e("NutrientUI", "Prediction failed: ${e.message}", e)
                    nutrientPredictions = null
                }
            } else {
                Log.w("NutrientUI", "Invalid reflectance data size: ${safeReflectance.size}")
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
                    .background(Red.copy(alpha = blinkAlpha))
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

            // Display Lux sensor data
            if (currentDevice?.sensorData is BluetoothScanViewModel.SensorData.LuxSensorData) {
                LuxSensorDisplay(
                    luxValue = luxValue,
                    rawData = (currentDevice!!.sensorData as BluetoothScanViewModel.SensorData.LuxSensorData).rawData,
                    modifier = Modifier.fillMaxWidth()
                )

            }

          if  ( currentDevice?.sensorData is BluetoothScanViewModel.SensorData.DOSensorData) {
              val doData =
                  currentDevice!!.sensorData as BluetoothScanViewModel.SensorData.DOSensorData

              DOSensorDisplay(
                  temperature = doData.temperature.replace(" °C", "").toFloatOrNull() ?: 0f,
                  doPercentage = doData.doPercentage.replace("%", "").toFloatOrNull() ?: 0f,
                  doValue = doData.doValue.replace(" mg/L", "").toFloatOrNull() ?: 0f,
                  modifier = Modifier.fillMaxWidth()
              )
          }


            if (currentDevice?.sensorData is BluetoothScanViewModel.SensorData.OpticalSensorData) {
                ResponsiveDataCards(
                    data = displayData,
                    cardBackground = Color(0xFF2A2A2A),
                    cardGradient = Brush.verticalGradient(listOf(Color(0xFF424242), Color(0xFF212121))),
                    textColor = Color.White,
                    nutrientPredictions = nutrientPredictions
                )
            }

            Spacer(modifier = Modifier.height(32.dp))




            // In the AdvertisingDataScreen's main Column, update the ThresholdInputSection usage:
            if (currentDevice?.sensorData is BluetoothScanViewModel.SensorData.SHT40Data ||
                currentDevice?.sensorData is BluetoothScanViewModel.SensorData.AmmoniaSensorData ||
                currentDevice?.sensorData is BluetoothScanViewModel.SensorData.LuxSensorData ||
                currentDevice?.sensorData is BluetoothScanViewModel.SensorData.DOSensorData) {
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


          // SHT40 sensor path → show threshold input AND temperature/humidity cards
            if (currentDevice?.sensorData is BluetoothScanViewModel.SensorData.SHT40Data) {

                // Show SHT40 temperature & humidity cards
                SHT40DataCards(
                    data = displayData, // list with temp & humidity pairs
                    cardBackground = Color(0xFF3E78B2),
                    cardGradient = Brush.verticalGradient(listOf(Color(0xFF5FA8D3), Color(0xFF3E78B2))),
                    textColor = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Show threshold input section
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



// For Ammonia sensor: show only threshold input
            if (currentDevice?.sensorData is BluetoothScanViewModel.SensorData.AmmoniaSensorData) {
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
                ammoniaValue > 50 -> Red
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
fun LuxSensorDisplay(
    luxValue: Float,
    rawData: String, // Add rawData parameter
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Existing Lux visualization
        LuxRingAnimation(luxValue = luxValue)

        Text(
            text = "%.0f LUX".format(luxValue),
            style = MaterialTheme.typography.displayMedium,
            color = when {
                luxValue > 10000 -> Red
                luxValue > 5000 -> Color.Yellow
                else -> Color.Green
            }
        )

        // Add Raw Data Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Raw Sensor Data",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = rawData,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
fun LuxRingAnimation(
    luxValue: Float,
    modifier: Modifier = Modifier
) {
    // Animate the fill percentage
    val animatedFill by animateFloatAsState(
        targetValue = (luxValue / 20000f).coerceIn(0f, 1f), // Assuming 20,000 LUX max
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 100f
        ),
        label = "luxFill"
    )

    // Color based on lux level
    val lightColor by animateColorAsState(
        targetValue = when {
            luxValue > 10000 -> Color(0xFFF44336) // Red - very bright
            luxValue > 5000 -> Color(0xFFFFC107)  // Yellow - bright
            else -> Color(0xFF4CAF50)            // Green - normal
        },
        animationSpec = tween(durationMillis = 300),
        label = "lightColor"
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
                color = lightColor.copy(alpha = 0.7f),
                startAngle = 270f,
                sweepAngle = -360f * animatedFill,
                useCenter = false,
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius),
                style = Stroke(width = ringWidth, cap = StrokeCap.Round)
            )
        }

        // Current value display
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.0f".format(luxValue),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "LUX",
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
                is BluetoothScanViewModel.SensorData.LuxSensorData -> listOf("Light Intensity")
                is BluetoothScanViewModel.SensorData.DOSensorData -> listOf("DO Temperature", "DO Percentage", "DO Value")
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
    viewModel: BluetoothScanViewModel<Any>,
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
    viewModel: BluetoothScanViewModel<Any>,
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
    viewModel: BluetoothScanViewModel<Any>,
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
    viewModel: BluetoothScanViewModel<Any>,
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
                        is BluetoothScanViewModel.SensorData.LuxSensorData -> headerBuilder.append("Light Intensity (LUX)")
                        is BluetoothScanViewModel.SensorData.SDTData -> headerBuilder.append("Speed (m/s),Distance (m)")
                        is BluetoothScanViewModel.SensorData.ObjectDetectorData -> headerBuilder.append("Object Detected")
                        is BluetoothScanViewModel.SensorData.StepCounterData -> headerBuilder.append("Steps")
                        is BluetoothScanViewModel.SensorData.AmmoniaSensorData -> headerBuilder.append("Ammonia (ppm)")
                        is BluetoothScanViewModel.SensorData.OpticalSensorData -> {
                            // Add wavelength-specific headers
                            val wavelengths = listOf(410, 435, 460, 485, 510, 535, 560, 585, 610, 645, 680, 705, 730, 760, 810, 860, 900, 960)
                            wavelengths.forEach { wavelength ->
                                headerBuilder.append("${wavelength}nm,")
                            }
                            headerBuilder.append("Raw Data")
                        }
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
                            is BluetoothScanViewModel.SensorData.LuxSensorData -> {
                                val parsedLux = sensorData.lux.replace("Lux=", "").trim().toFloatOrNull() ?: 0f
                                dataBuilder.append("$parsedLux")
                            }
                            is BluetoothScanViewModel.SensorData.SDTData -> dataBuilder.append("${sensorData.speed},${sensorData.distance}")
                            is BluetoothScanViewModel.SensorData.ObjectDetectorData -> dataBuilder.append("${sensorData.detection}")
                            is BluetoothScanViewModel.SensorData.StepCounterData -> dataBuilder.append("${sensorData.steps}")
                            is BluetoothScanViewModel.SensorData.AmmoniaSensorData -> dataBuilder.append("${sensorData.ammonia}")
                            is BluetoothScanViewModel.SensorData.DOSensorData -> {
                                dataBuilder.append("${sensorData.temperature.replace(" °C", "")}," +
                                        "${sensorData.doPercentage.replace("%", "")}," +
                                        "${sensorData.doValue.replace(" mg/L", "")}")
                            }
                            is BluetoothScanViewModel.SensorData.OpticalSensorData -> {
                                sensorData.reflectanceValues.forEach { value ->
                                    dataBuilder.append("$value,")
                                }
                                dataBuilder.append("\"${sensorData.rawData}\"")
                            }
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
fun DOSensorDisplay(
    temperature: Float,
    doPercentage: Float,
    doValue: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Temperature gauge
        CircularProgressIndicator(
            progress = (temperature / 50f).coerceIn(0f, 1f), // Assuming max 50°C
            modifier = Modifier.size(100.dp),
            color = when {
                temperature > 30 -> Red
                temperature > 20 -> Color.Yellow
                else -> Color.Green
            },
            strokeWidth = 8.dp
        )
        Text("$temperature °C", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // DO percentage gauge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    progress = doPercentage / 100f,
                    modifier = Modifier.size(80.dp),
                    color = when {
                        doPercentage < 30 -> Red
                        doPercentage < 60 -> Color.Yellow
                        else -> Color.Green
                    }
                )
                Text("$doPercentage%", style = MaterialTheme.typography.bodyLarge)
                Text("Saturation", style = MaterialTheme.typography.labelSmall)
            }

            // DO value gauge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    progress = (doValue / 20f).coerceIn(0f, 1f), // Assuming max 20 mg/L
                    modifier = Modifier.size(80.dp),
                    color = when {
                        doValue < 2 -> Red
                        doValue < 5 -> Color.Yellow
                        else -> Color.Green
                    }
                )
                Text("$doValue mg/L", style = MaterialTheme.typography.bodyLarge)
                Text("Concentration", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun OpticalSensorVisualization(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    // Define the 18 wavelengths
    val wavelengths = listOf(
        410, 435, 460, 485, 510, 535, 560, 585, 610,
        645, 680, 705, 730, 760, 810, 860, 900, 940
    )

    if (values.size != 18) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Invalid data format. Expected 18 values, got ${values.size}",
                color = Red
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorScheme.primaryContainer,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                )
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Wavelength (nm)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Reflectance",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }

        // Data rows with alternating background
        values.forEachIndexed { index, value ->
            val backgroundColor = if (index % 2 == 0) {
                colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                colorScheme.surface.copy(alpha = 0.7f)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = backgroundColor,
                        shape = if (index == values.size - 1) {
                            RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                        } else {
                            RectangleShape
                        }
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${wavelengths[index]} nm",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "%.6f".format(value),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }

        // Visualization chart
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Reflectance Spectrum",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Chart implementation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(8.dp)
                .background(
                    color = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxValue = values.maxOrNull() ?: 1f
                val minValue = values.minOrNull() ?: 0f
                val valueRange = maxValue - minValue

                val padding = 40f
                val graphWidth = size.width - padding * 2
                val graphHeight = size.height - padding * 2

                // Draw grid lines
                val paint = Paint().apply {
                    color = darkColorScheme().onSurface.copy(alpha = 0.2f)
                    strokeWidth = 1f
                }

                // Horizontal grid lines
                for (i in 0..4) {
                    val y = padding + graphHeight * (1 - i / 4f)
                    drawLine(
                        paint = paint,
                        start = Offset(padding, y),
                        end = Offset(size.width - padding, y)
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        "%.2f".format(minValue + valueRange * i / 4f),
                        padding - 35f,
                        y + 5f,
                        android.graphics.Paint().apply {
                            // this.color = Red
                            textSize = 24f
                        }
                    )
                }

                // Vertical grid lines (wavelengths)
                wavelengths.forEachIndexed { index, wavelength ->
                    val x = padding + graphWidth * index / (wavelengths.size - 1f)
                    drawLine(
                        paint = paint,
                        start = Offset(x, padding),
                        end = Offset(x, size.height - padding)
                    )

                    if (index % 2 == 0) {
                        drawContext.canvas.nativeCanvas.drawText(
                            wavelength.toString(),
                            x - 15f,
                            size.height - padding + 30f,
                            android.graphics.Paint().apply {
                                color = darkColorScheme().onSurface.toArgb()
                                textSize = 24f
                            }
                        )
                    }
                }

                // Draw the spectrum line
                val linePaint = Paint().apply {
                    color = Color.Blue
                    strokeWidth = 3f
                    strokeCap = StrokeCap.Round
                }

                val path = Path().apply {
                    values.forEachIndexed { index, value ->
                        val x = padding + graphWidth * index / (values.size - 1f)
                        val y = padding + graphHeight * (1 - (value - minValue) / valueRange)

                        if (index == 0) {
                            moveTo(x, y)
                        } else {
                            lineTo(x, y)
                        }
                    }
                }

                drawPath(
                    path = path,
                    color = darkColorScheme().primary,
                    style = Stroke(width = 3f)
                )

                // Draw data points
                values.forEachIndexed { index, value ->
                    val x = padding + graphWidth * index / (values.size - 1f)
                    val y = padding + graphHeight * (1 - (value - minValue) / valueRange)

                    drawCircle(
                        color = darkColorScheme().primary,
                        radius = 5f,
                        center = Offset(x, y)
                    )
                }
            }
        }


        // Legend
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(color = Red.copy(alpha = 0.7f), label = "Very High", range = "> 80%")
            LegendItem(color = Color.Blue.copy(alpha = 0.7f), label = "High", range = "60-80%")
            LegendItem(color = Color.Yellow.copy(alpha = 0.7f), label = "Medium", range = "40-60%")
            LegendItem(color = Color.Green.copy(alpha = 0.7f), label = "Low", range = "20-40%")
            LegendItem(color = Color.Blue.copy(alpha = 0.7f), label = "Very Low", range = "< 20%")
        }
    }
}





fun drawLine(paint: Paint, start: Offset, end: Offset) {

}

@Composable
fun LegendItem(color: Color, label: String, range: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color)
        )
        Text(text = label, fontSize = 10.sp)
        Text(text = range, fontSize = 8.sp)
    }
}

@Composable
fun NutrientCard(
    name: String,
    value: Float,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingAnimation"
    )

    Card(
        modifier = modifier
            .padding(4.dp)
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.8f),
                            color.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = alpha)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = "%.1f".format(value),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponsiveDataCards(
    data: List<Pair<String, String>>,
    cardBackground: Color,

    cardGradient: Brush,
    textColor: Color,
    nutrientPredictions: SoilNutrientModelHelper.NutrientPrediction? = null
) {
    // Separate the data types
    val ammoniaData = data.find { it.first.contains("Ammonia", ignoreCase = true) }
    val reflectanceData = data.find { it.first.contains("Reflectance", ignoreCase = true) }
    val rawData = data.find { it.first.contains("Raw Data", ignoreCase = true) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Always show nutrient predictions section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = " Nutrient Predictions ",
                style = MaterialTheme.typography.titleLarge,
                color = textColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutrientCard(
                    name = "Nitrogen",
                    value = nutrientPredictions?.nitrogen ?: 0f,
                    unit = "mg/kg",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                    isLoading = nutrientPredictions == null
                )

                // Spacer(modifier = Modifier.width(8.dp))

                // NutrientCard(
                //     name = "Phosphorus",
                //     value = nutrientPredictions?.phosphorus ?: 0f,
                //     unit = "mg/kg",
                //     color = Color(0xFF2196F3),
                //     modifier = Modifier.weight(1f),
                //     isLoading = nutrientPredictions == null
                // )

                // Spacer(modifier = Modifier.width(8.dp))

                // NutrientCard(
                //     name = "Potassium",
                //     value = nutrientPredictions?.potassium ?: 0f,
                //     unit = "mg/kg",
                //     color = Color(0xFFFFC107),
                //     modifier = Modifier.weight(1f),
                //     isLoading = nutrientPredictions == null
                // )

                // Spacer(modifier = Modifier.width(8.dp))

                // NutrientCard(
                //     name = "Organic Carbon",
                //     value = nutrientPredictions?.organicCarbon ?: 0f,
                //     unit = "%",
                //     color = Color(0xFF795548),
                //     modifier = Modifier.weight(1f),
                //     isLoading = nutrientPredictions == null
                // )
            }

        }

        // 2. Show reflectance data visualization if available
        reflectanceData?.let { (label, value) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Optical Sensor Data",
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                val reflectanceValues = value.split("\n")
                    .mapNotNull { line ->
                        val regex = """Channel \d+: ([\d.]+)""".toRegex()
                        regex.find(line)?.groupValues?.get(1)?.toFloatOrNull()
                    }

                if (reflectanceValues.size == 18) {
                    OpticalSensorVisualization(
                        values = reflectanceValues,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Waiting for reflectance data...",
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // 3. Raw data display
        rawData?.let { (label, value) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Raw Sensor Data",
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = cardBackground.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }



    }
}

@Composable
fun LIS2DHDataCards(
    data: List<Pair<String, String>>,
    cardBackground: Color,
    cardGradient: Brush,
    textColor: Color
) {
    if (data.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "LIS2DH Live Data",
                style = MaterialTheme.typography.titleLarge,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(data, key = { it.first }) { pair ->
                    val label = pair.first
                    val value = pair.second
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
    }
}

@Composable
fun SHT40DataCards(
    data: List<Pair<String, String>>,
    cardBackground: Color,
    cardGradient: Brush,
    textColor: Color
) {
    if (data.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SHT40 Live Data",
                style = MaterialTheme.typography.titleLarge,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(data, key = { it.first }) { pair ->
                    val label = pair.first
                    val value = pair.second
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
package com.example.ble_jetpackcompose

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.blesense.app.BluetoothScanViewModel
import com.blesense.app.DownloadButton
import com.blesense.app.NutrientCard
import com.blesense.app.OpticalSensorVisualization
import com.blesense.app.R
import com.blesense.app.SoilNutrientModelHelper
import com.blesense.app.ThemeManager
import com.blesense.app.TranslatedAdvertisingText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OpticalSensorScreen(
    navController: NavController,
    viewModel: BluetoothScanViewModel<Any>,
    context: Context = LocalContext.current
) {
    val activity = LocalContext.current as? android.app.Activity
    val coroutineScope = rememberCoroutineScope()
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    // Theme-based colors matching WaterQualityScreen.kt
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF757575)

    var connectedDevice by remember { mutableStateOf<BluetoothScanViewModel.BluetoothDevice?>(null) }
    var nutrientPredictions by remember { mutableStateOf<SoilNutrientModelHelper.NutrientPrediction?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Start scan on first load
    LaunchedEffect(Unit) {
        activity?.let { viewModel.startScan(it) }
    }

    // Stop scan when composable leaves composition
    DisposableEffect(Unit) {
        onDispose { viewModel.stopScan() }
    }

    // Collect devices
    val deviceList by viewModel.devices.collectAsState()

    // Auto connect
    LaunchedEffect(deviceList) {
        val opticalDevice = deviceList.find { it.name.contains("Optical", ignoreCase = true) }
        if (opticalDevice != null && connectedDevice?.address != opticalDevice.address) {
            connectedDevice = opticalDevice
            Log.d("OpticalScreen", "Connected to optical sensor: ${opticalDevice.address}")
        }
    }

    // Get reflectance values
    val reflectanceValues = connectedDevice?.sensorData.let { data ->
        if (data is BluetoothScanViewModel.SensorData.OpticalSensorData) data.reflectanceValues else emptyList()
    }

    // Predict nutrients when reflectance data updates
    LaunchedEffect(reflectanceValues) {
        if (reflectanceValues.size == 18) {
            try {
                val helper = SoilNutrientModelHelper(context)
                val preds = withContext(Dispatchers.Default) {
                    helper.predictNutrients(reflectanceValues)
                }
                helper.close()
                nutrientPredictions = preds
            } catch (e: Exception) {
                Log.e("OpticalScreen", "Prediction failed: ${e.message}")
                nutrientPredictions = null
            }
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with back button and title
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
                Text(
                    text = "Optical Sensor",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Image(
                    painter = painterResource(id = R.drawable.refresh),
                    contentDescription = "Refresh",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            isRefreshing = true
                            activity?.let { viewModel.startScan(it) }
                            coroutineScope.launch {
                                delay(2000)
                                isRefreshing = false
                            }
                        }
                )
            }

            if (isRefreshing) {
                Text(
                    text = "Refreshing...",
                    color = secondaryTextColor,
                    fontSize = 16.sp
                )
            }

            // Show nutrient prediction
            nutrientPredictions?.let { preds ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NutrientCard(
                        name = "Predicted Nitrogen",
                        value = preds.nitrogen,
                        unit = "g/kg",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    // You can uncomment more cards if needed
                    // NutrientCard(name = "Phosphorus", value = preds.phosphorus, unit = "mg/kg", color = Color(0xFF2196F3), modifier = Modifier.weight(1f))
                    // NutrientCard(name = "Potassium", value = preds.potassium, unit = "mg/kg", color = Color(0xFFFFC107), modifier = Modifier.weight(1f))
                    // NutrientCard(name = "Organic Carbon", value = preds.organicCarbon, unit = "%", color = Color(0xFF795548), modifier = Modifier.weight(1f))
                }
            } ?: Text(
                text = "Waiting for nutrient prediction...",
                color = secondaryTextColor,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show reflectance graph
            if (reflectanceValues.isNotEmpty()) {
                OpticalSensorVisualization(
                    values = reflectanceValues,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "Waiting for optical sensor data...",
                    color = secondaryTextColor,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download CSV button
            connectedDevice?.let { device ->
                val translatedText = TranslatedAdvertisingText(
                    downloadData = "Download Data",
                    exportingData = "Exporting..."
                )
                DownloadButton(
                    viewModel = viewModel,
                    deviceAddress = device.address,
                    deviceName = device.name,
                    deviceId = device.address,
                    translatedText = translatedText
                )
            }
        }
    }
}
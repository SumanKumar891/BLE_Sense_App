package com.blesense.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun WaterQualityScreen(
    navController: NavController,
    viewModel: BluetoothScanViewModel<Any>
) {
    val devices by viewModel.devices.collectAsState()
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()

    // Theme-based colors matching MainScreen.kt
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF757575)

    // Dropdown menu state
    var expanded by remember { mutableStateOf(false) }
    val sensorTypes = listOf("DO", "COD", "BOD", "TDS", "TSS", "Turbidity", "Conductivity")
    var selectedSensor by remember { mutableStateOf(sensorTypes[0]) }

    // Filter devices for water quality sensors (currently only DO is implemented)
    val waterQualityDevices = devices.filter { device ->
        when (selectedSensor) {
            "DO" -> device.sensorData is BluetoothScanViewModel.SensorData.DOSensorData
            else -> false // Other sensors not yet implemented
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with back button and dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    text = "Water Quality Management",
                    fontFamily = helveticaFont,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Select Sensor",
                            tint = textColor
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(cardBackgroundColor)
                    ) {
                        sensorTypes.forEach { sensor ->
                            DropdownMenuItem(
                                text = { Text(sensor, color = textColor) },
                                onClick = {
                                    selectedSensor = sensor
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // List of water quality sensor devices
            if (waterQualityDevices.isEmpty()) {
                Text(
                    text = when (selectedSensor) {
                        "DO" -> "No DO sensors detected"
                        else -> "$selectedSensor data not available"
                    },
                    color = secondaryTextColor,
                    fontSize = 16.sp
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(waterQualityDevices, key = { it.address }) { device ->
                        WaterQualityDeviceCard(
                            device = device,
                            selectedSensor = selectedSensor,
                            textColor = textColor,
                            secondaryTextColor = secondaryTextColor,
                            cardBackgroundColor = cardBackgroundColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterQualityDeviceCard(
    device: BluetoothScanViewModel.BluetoothDevice,
    selectedSensor: String,
    textColor: Color,
    secondaryTextColor: Color,
    cardBackgroundColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardBackgroundColor
    ) {
        Box(
            modifier = Modifier
                .background(cardBackgroundColor)
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Device: ${device.name} (${device.address})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Node ID: ${device.deviceId}",
                    fontSize = 14.sp,
                    color = secondaryTextColor
                )

                when (val sensorData = device.sensorData) {
                    is BluetoothScanViewModel.SensorData.DOSensorData -> {
                        if (selectedSensor == "DO") {
                            DOSensorDisplay(
                                temperature = sensorData.temperatureFloat,
                                doPercentage = sensorData.doPercentageFloat,
                                doValue = sensorData.doValueFloat,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "$selectedSensor data not available",
                            color = secondaryTextColor
                        )
                    }
                }
            }
        }
    }
}
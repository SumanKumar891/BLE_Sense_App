//homescreen with large data values

package com.blesense.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.blesense.app.BluetoothScanViewModel.SensorData.DataLoggerData


// Data class for translatable text in MainScreen
data class TranslatedMainScreenText(
    val appTitle: String = "BLE Sense",
    val nearbyDevices: String = "Nearby Devices",
    val gameDevices: String = "Game Devices",
    val bluetoothPermissionsRequired: String = "Bluetooth permissions required",
    val scanningForDevices: String = "Scanning for devices...",
    val noDevicesFound: String = "No devices found",
    val scanningForGameDevices: String = "Scanning for game devices...",
    val noGameDevicesFound: String = "No game devices found",
    val showMore: String = "Show More",
    val showLess: String = "Show Less"
)

// Main composable for the app's main screen
@SuppressLint("MissingPermission")
@Composable
fun MainScreen(navController: NavHostController, bluetoothViewModel: BluetoothScanViewModel<Any?>) {
    // Check if device is in landscape orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // State to toggle robot controls visibility
    var showRobotControls by remember { mutableStateOf(false) }

    // Observe Bluetooth devices and scanning state
    val bluetoothDevices by bluetoothViewModel.devices.collectAsState()
    val isScanning by bluetoothViewModel.isScanning.collectAsState()
    // Get current context
    val context = LocalContext.current
    // Get activity context
    val activity = context as ComponentActivity

    // Track Bluetooth permission status
    val isPermissionGranted = remember { mutableStateOf(checkBluetoothPermissions(context)) }

    // State for dropdown menu and sensor selection
    var expanded by remember { mutableStateOf(false) }
    // List of supported sensor types
    val sensorTypes = listOf(
        "SHT40", "LIS2DH", "Weather", "Lux Sensor","Soil Sensor",
        "Speed Distance", "Metal Detector", "Step Counter",
        "Ammonia Sensor", "DataLogger"
    )

    // Track selected sensor type
    var selectedSensor by remember { mutableStateOf(sensorTypes[0]) }
    // Toggle to show all devices or a limited number
    var showAllDevices by remember { mutableStateOf(false) }

    // Observe theme and language state
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()

    // Initialize translated text with cached values or defaults
    var translatedText by remember {
        mutableStateOf(
            TranslatedMainScreenText(
                appTitle = TranslationCache.get("BLE Sense-$currentLanguage") ?: "BLE Sense",
                nearbyDevices = TranslationCache.get("Nearby Devices-$currentLanguage") ?: "Nearby Devices",
                gameDevices = TranslationCache.get("Game Devices-$currentLanguage") ?: "Game Devices",
                bluetoothPermissionsRequired = TranslationCache.get("Bluetooth permissions required-$currentLanguage") ?: "Bluetooth permissions required",
                scanningForDevices = TranslationCache.get("Scanning for devices...-$currentLanguage") ?: "Scanning for devices...",
                noDevicesFound = TranslationCache.get("No devices found-$currentLanguage") ?: "No devices found",
                scanningForGameDevices = TranslationCache.get("Scanning for game devices...-$currentLanguage") ?: "Scanning for game devices...",
                noGameDevicesFound = TranslationCache.get("No game devices found-$currentLanguage") ?: "No game devices found",
                showMore = TranslationCache.get("Show More-$currentLanguage") ?: "Show More",
                showLess = TranslationCache.get("Show Less-$currentLanguage") ?: "Show Less"
            )
        )
    }

    // Preload translations when language changes
    LaunchedEffect(currentLanguage) {
        val translator = GoogleTranslationService()
        val textsToTranslate = listOf(
            "BLE Sense", "Nearby Devices", "Game Devices", "Bluetooth permissions required",
            "Scanning for devices...", "No devices found", "Scanning for game devices...",
            "No game devices found", "Show More", "Show Less"
        )
        val translatedList = translator.translateBatch(textsToTranslate, currentLanguage)
        translatedText = TranslatedMainScreenText(
            appTitle = translatedList[0],
            nearbyDevices = translatedList[1],
            gameDevices = translatedList[2],
            bluetoothPermissionsRequired = translatedList[3],
            scanningForDevices = translatedList[4],
            noDevicesFound = translatedList[5],
            scanningForGameDevices = translatedList[6],
            noGameDevicesFound = translatedList[7],
            showMore = translatedList[8],
            showLess = translatedList[9]
        )
    }

    // Define theme-based colors
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF757575)
    val dividerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFE0E0E0)

    // Get Bluetooth adapter
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }

    // Check permissions on initial composition
    LaunchedEffect(Unit) {
        isPermissionGranted.value = checkBluetoothPermissions(context)
    }

    // Handle Bluetooth permission requests
    BluetoothPermissionHandler(
        onPermissionsGranted = {
            isPermissionGranted.value = true
        }
    )

    // Monitor Bluetooth adapter state changes
    val bluetoothStateReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        if (state == BluetoothAdapter.STATE_ON && isPermissionGranted.value && !isScanning) {
                            bluetoothViewModel.startPeriodicScan(activity)
                        }
                    }
                }
            }
        }
    }

    // Register and unregister the BroadcastReceiver
    DisposableEffect(Unit) {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)
        onDispose {
            context.unregisterReceiver(bluetoothStateReceiver)
        }
    }

    // Start periodic Bluetooth scanning when permissions are granted and Bluetooth is enabled
    DisposableEffect(isPermissionGranted.value, bluetoothAdapter?.isEnabled) {
        if (isPermissionGranted.value && bluetoothAdapter?.isEnabled == true && !isScanning) {
            bluetoothViewModel.startPeriodicScan(activity)
        }
        onDispose {
            bluetoothViewModel.stopScan()
        }
    }

    // Main UI container
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
                    // Back button
                    IconButton(
                        onClick = { navController.navigate("intermediate_screen") },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                    Text(
                        text = translatedText.appTitle,
                        fontFamily = helveticaFont,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        style = MaterialTheme.typography.h6,
                        textAlign = TextAlign.Center
                    )
                    // Theme toggle button
                    IconButton(
                        onClick = { ThemeManager.toggleDarkMode(!isDarkMode) },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF5D4037)
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Main content list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Nearby Devices section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = cardBackgroundColor
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${translatedText.nearbyDevices} (${bluetoothDevices.size})",
                                    style = MaterialTheme.typography.h6,
                                    color = textColor
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Refresh scan button
                                    IconButton(
                                        onClick = @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT) {
                                            if (isPermissionGranted.value && !isScanning) {
                                                if (bluetoothAdapter?.isEnabled == true) {
                                                    bluetoothViewModel.startPeriodicScan(activity)
                                                } else {
                                                    // Prompt to enable Bluetooth
                                                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                                    context.startActivity(enableBtIntent)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh",
                                            tint = textColor
                                        )
                                    }
                                    // Sensor type dropdown menu
                                    Box {
                                        IconButton(onClick = { expanded = true }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "More Options",
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
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Handle permission and device states
                            if (!isPermissionGranted.value) {
                                Text(
                                    text = translatedText.bluetoothPermissionsRequired,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = textColor
                                )
                            } else if (bluetoothAdapter?.isEnabled != true) {
                                Text(
                                    text = "Please enable Bluetooth",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = textColor
                                )
                            } else if (bluetoothDevices.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(
                                            color = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF2196F3)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(translatedText.scanningForDevices, color = textColor)
                                    } else {
                                        Text(translatedText.noDevicesFound, color = textColor)
                                    }
                                }
                            } else {
                                // Display devices
                                val devicesToShow = if (showAllDevices) bluetoothDevices else bluetoothDevices.take(4)
                                devicesToShow.forEach { device ->
                                    BluetoothDeviceItem(
                                        device = device,
                                        navController = navController,
                                        selectedSensor = selectedSensor,
                                        isDarkMode = isDarkMode
                                    )
                                    Divider(color = dividerColor)
                                }
                                if (bluetoothDevices.size > 4) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clickable { showAllDevices = !showAllDevices }
                                            .background(
                                                if (isDarkMode) Color(0xFF2A2A2A) else Color.LightGray,
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (showAllDevices) translatedText.showLess else translatedText.showMore,
                                            modifier = Modifier.padding(12.dp),
                                            textAlign = TextAlign.Center,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Game Devices section
                item {
                    val allowedGameDevices = listOf(
                        "Scarlet Witch", "Black Widow", "Captain Marvel", "Wasp", "Hela",
                        "Hulk", "Thor", "Iron_Man", "Spider Man", "Captain America"
                    )
                    val gameDevices = bluetoothDevices.filter { it.name in allowedGameDevices }
                    val devicesToShow = if (showAllDevices) gameDevices else gameDevices.take(4)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = cardBackgroundColor
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${translatedText.gameDevices} (${gameDevices.size})",
                                    style = MaterialTheme.typography.h6,
                                    color = textColor
                                )
                                IconButton(
                                    onClick = @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT) {
                                        if (isPermissionGranted.value && !isScanning) {
                                            if (bluetoothAdapter?.isEnabled == true) {
                                                bluetoothViewModel.startPeriodicScan(activity)
                                            } else {
                                                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                                context.startActivity(enableBtIntent)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = textColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            if (!isPermissionGranted.value) {
                                Text(
                                    text = translatedText.bluetoothPermissionsRequired,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = textColor
                                )
                            } else if (bluetoothAdapter?.isEnabled != true) {
                                Text(
                                    text = "Please enable Bluetooth",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = textColor
                                )
                            } else if (gameDevices.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(
                                            color = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF2196F3)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(translatedText.scanningForGameDevices, color = textColor)
                                    } else {
                                        Text(translatedText.noGameDevicesFound, color = textColor)
                                    }
                                }
                            } else {
                                devicesToShow.forEach { device ->
                                    BluetoothDeviceItem(
                                        device = device,
                                        navController = navController,
                                        selectedSensor = selectedSensor,
                                        isDarkMode = isDarkMode
                                    )
                                    Divider(color = dividerColor)
                                }
                                if (gameDevices.size > 4) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .clickable { showAllDevices = !showAllDevices }
                                            .background(
                                                if (isDarkMode) Color(0xFF2A2A2A) else Color.LightGray,
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (showAllDevices) translatedText.showLess else translatedText.showMore,
                                            modifier = Modifier.padding(12.dp),
                                            textAlign = TextAlign.Center,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to check Bluetooth permissions based on Android version
private fun checkBluetoothPermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // For Android 12 and above, check new Bluetooth permissions
        checkPermission(context, Manifest.permission.BLUETOOTH_SCAN) &&
                checkPermission(context, Manifest.permission.BLUETOOTH_CONNECT) &&
                checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        // For older Android versions, check legacy Bluetooth permissions
        checkPermission(context, Manifest.permission.BLUETOOTH) &&
                checkPermission(context, Manifest.permission.BLUETOOTH_ADMIN) &&
                checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

// Helper function to check a single permission
private fun checkPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

// Composable for individual Bluetooth device item
@Composable
fun BluetoothDeviceItem(
    device: BluetoothScanViewModel.BluetoothDevice,
    navController: NavHostController,
    selectedSensor: String,
    isDarkMode: Boolean,
    showPreview: Boolean = false
) {
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF757575)
    val iconBackgroundColor = if (isDarkMode) Color(0xFF0D47A1) else Color(0xFFE3F2FD)
    val iconTintColor = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF2196F3)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                navController.navigate(
                    "advertising/${device.name.replace("/", "-")}/${device.address}/${selectedSensor.ifEmpty { "SHT40" }}/${device.deviceId}"
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(iconBackgroundColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.bluetooth),
                contentDescription = "Bluetooth Device",
                modifier = Modifier.size(24.dp),
                tint = iconTintColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = device.name,
                style = MaterialTheme.typography.subtitle1,
                color = textColor
            )
            Text(
                text = "Address: ${device.address}",
                style = MaterialTheme.typography.caption,
                color = secondaryTextColor
            )
            Text(
                text = "Signal Strength: ${device.rssi} dBm",
                style = MaterialTheme.typography.caption,
                color = secondaryTextColor
            )

            device.sensorData?.let { sensorData ->
                if (showPreview) {
                    when (sensorData) {
                        is BluetoothScanViewModel.SensorData.DOSensorData -> {
                            DOSensorPreview(
                                temperature = sensorData.temperature,
                                doPercentage = sensorData.doPercentage,
                                doValue = sensorData.doValue,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                        is BluetoothScanViewModel.SensorData.DataLoggerData -> {
                            DataLoggerPreview(
                                rawData = "Packet ID: ${sensorData.packetId}, Packets: ${sensorData.payloadPackets.size}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                        is BluetoothScanViewModel.SensorData.StepCounterData -> {
                            val rawDataString = sensorData.rawData?.joinToString(" ") { "%02X".format(it) } ?: "N/A"
                            StepCounterPreview(
                                steps = sensorData.steps,
                                rawData = rawDataString,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                        else -> {} // No preview for other sensor types
                    }
                }

                Text(
                    text = when {
                        selectedSensor.isEmpty() -> when (sensorData) {
                            is BluetoothScanViewModel.SensorData.SHT40Data ->
                                "Temp: ${sensorData.temperature}°C, Humidity: ${sensorData.humidity}%"
                            is BluetoothScanViewModel.SensorData.LIS2DHData ->
                                "X: ${sensorData.x}, Y: ${sensorData.y}, Z: ${sensorData.z}"
                            is BluetoothScanViewModel.SensorData.ObjectDetectorData ->
                                "Metal Detected: ${if (sensorData.detection) "Yes" else "No"}"
                            is BluetoothScanViewModel.SensorData.SDTData ->
                                "Speed: ${sensorData.speed}m/s, Distance: ${sensorData.distance}m"
                            is BluetoothScanViewModel.SensorData.AmmoniaSensorData ->
                                "Ammonia: ${sensorData.ammonia}"
                            is BluetoothScanViewModel.SensorData.DOSensorData ->
                                "Temp: ${sensorData.temperature}, DO: ${sensorData.doPercentage}, ${sensorData.doValue}"
                            is BluetoothScanViewModel.SensorData.LuxSensorData ->
                                "Device ID: ${sensorData.deviceId}, Raw: ${sensorData.rawData}"
                            is BluetoothScanViewModel.SensorData.SoilSensorData ->
                                "Temp: ${sensorData.temperature}°C, Moisture: ${sensorData.moisture}%"
                            is BluetoothScanViewModel.SensorData.StepCounterData -> {
                                val rawDataString = sensorData.rawData?.joinToString(" ") { "%02X".format(it) } ?: "N/A"
                                "Steps: ${sensorData.steps}, Raw: $rawDataString"
                            }
                            is BluetoothScanViewModel.SensorData.DataLoggerData ->
                                "Packet ID: ${sensorData.packetId}, Packets: ${sensorData.payloadPackets.size}"
                        }
                        selectedSensor == "SHT40" && sensorData is BluetoothScanViewModel.SensorData.SHT40Data ->
                            "Temp: ${sensorData.temperature}°C, Humidity: ${sensorData.humidity}%"
                        selectedSensor == "Metal Detector" && sensorData is BluetoothScanViewModel.SensorData.ObjectDetectorData ->
                            "Metal Detected: ${if (sensorData.detection) "Yes" else "No"}"
                        selectedSensor == "LIS2DH" && sensorData is BluetoothScanViewModel.SensorData.LIS2DHData ->
                            "X: ${sensorData.x}, Y: ${sensorData.y}, Z: ${sensorData.z}"
                        selectedSensor == "Speed Distance" && sensorData is BluetoothScanViewModel.SensorData.SDTData ->
                            "Speed: ${sensorData.speed}m/s, Distance: ${sensorData.distance}m"
                        selectedSensor == "Ammonia Sensor" && sensorData is BluetoothScanViewModel.SensorData.AmmoniaSensorData ->
                            "Ammonia: ${sensorData.ammonia}"
                        selectedSensor == "DO Sensor" && sensorData is BluetoothScanViewModel.SensorData.DOSensorData ->
                            "Temp: ${sensorData.temperature}, DO: ${sensorData.doPercentage}, ${sensorData.doValue}"
                        selectedSensor == "Lux Sensor" && sensorData is BluetoothScanViewModel.SensorData.LuxSensorData ->
                            "Device ID: ${sensorData.deviceId}, Raw: ${sensorData.rawData}"
                        selectedSensor == "Soil Sensor" && sensorData is BluetoothScanViewModel.SensorData.SoilSensorData ->
                            "N: ${sensorData.nitrogen}, P: ${sensorData.phosphorus}, K: ${sensorData.potassium}\n" +
                                    "Moisture: ${sensorData.moisture}%, Temp: ${sensorData.temperature}°C\n" +
                                    "EC: ${sensorData.ec}, pH: ${sensorData.pH}"
                        selectedSensor == "Step Counter" && sensorData is BluetoothScanViewModel.SensorData.StepCounterData -> {
                            val rawDataString = sensorData.rawData?.joinToString(" ") { "%02X".format(it) } ?: "N/A"
                            "Steps: ${sensorData.steps}, Raw: $rawDataString"
                        }
                        selectedSensor == "DataLogger" && sensorData is BluetoothScanViewModel.SensorData.DataLoggerData ->
                            "Packet ID: ${sensorData.packetId}, Packets: ${sensorData.payloadPackets.size}"
                        else -> "Incompatible sensor type"
                    },
                    style = MaterialTheme.typography.caption,
                    color = if (isDarkMode) Color(0xFF64B5F6) else MaterialTheme.colors.primary,
                    maxLines = 2
                )
            } ?: Text(
                text = "No sensor data available",
                style = MaterialTheme.typography.caption,
                color = if (isDarkMode) Color(0xFF64B5F6) else MaterialTheme.colors.primary
            )
        }
    }
}

// Composable for DO Sensor data preview
@Composable
fun DOSensorPreview(
    temperature: String, // Temperature value
    doPercentage: String, // Dissolved oxygen percentage
    doValue: String, // Dissolved oxygen value
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(60.dp)
            .fillMaxWidth()
            .background(
                if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color.White,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Temperature display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = temperature,
                    style = MaterialTheme.typography.caption,
                    color = if (isSystemInDarkTheme()) Color(0xFF64B5F6) else Color(0xFF2196F3)
                )
                Text("Temp", style = MaterialTheme.typography.overline)
            }
            // DO percentage display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = doPercentage,
                    style = MaterialTheme.typography.caption,
                    color = if (isSystemInDarkTheme()) Color(0xFF64B5F6) else Color(0xFF2196F3)
                )
                Text("DO %", style = MaterialTheme.typography.overline)
            }
            // DO value display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = doValue,
                    style = MaterialTheme.typography.caption,
                    color = if (isSystemInDarkTheme()) Color(0xFF64B5F6) else Color(0xFF2196F3)
                )
                Text("DO mg/L", style = MaterialTheme.typography.overline)
            }
        }
    }
}

// Composable for Lux Sensor data preview
@Composable
fun LuxSensorPreview(
    value: String, // Raw sensor data
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(60.dp)
            .fillMaxWidth()
            .background(
                if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color.White,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Raw Data: $value",
            style = MaterialTheme.typography.caption,
            color = if (isSystemInDarkTheme()) Color(0xFF64B5F6) else Color(0xFF2196F3),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun DataLoggerPreview(
    rawData: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(80.dp) // Increased height to show more data
            .fillMaxWidth()
            .background(
                if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color.White,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rawData,
            style = MaterialTheme.typography.caption,
            color = if (isSystemInDarkTheme()) Color(0xFF64B5F6) else Color(0xFF2196F3),
            textAlign = TextAlign.Center,
            maxLines = 3, // Allow more lines for raw data
            overflow = TextOverflow.Ellipsis
        )
    }
}
@Composable
fun StepCounterPreview(
    steps: String,
    rawData: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(60.dp)
            .fillMaxWidth()
            .background(
                if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color.White,
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Steps: $steps",
                style = MaterialTheme.typography.caption,
                color = if (isSystemInDarkTheme()) Color(0xFF64B5F6) else Color(0xFF2196F3),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Raw: $rawData",
                style = MaterialTheme.typography.caption,
                color = if (isSystemInDarkTheme()) Color(0xFF64B5F6) else Color(0xFF2196F3),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
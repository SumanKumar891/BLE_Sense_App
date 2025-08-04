package com.blesense.app
// Import necessary Android and Compose libraries for UI, permissions, and navigation
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState


// Data class for translatable text in MainScreen
data class TranslatedMainScreenText(
    val appTitle: String = "BLE Sense", // Default app title
    val nearbyDevices: String = "Nearby Devices", // Label for nearby devices section
    val gameDevices: String = "Game Devices", // Label for game devices section
    val bluetoothPermissionsRequired: String = "Bluetooth permissions required", // Message for missing permissions
    val scanningForDevices: String = "Scanning for devices...", // Message during device scanning
    val noDevicesFound: String = "No devices found", // Message when no devices are found
    val scanningForGameDevices: String = "Scanning for game devices...", // Message during game device scanning
    val noGameDevicesFound: String = "No game devices found", // Message when no game devices are found
    val showMore: String = "Show More", // Label for showing more devices
    val showLess: String = "Show Less" // Label for showing fewer devices
)

// Main composable for the app's main screen
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
        "SHT40", "LIS2DH", "Weather", "Lux Sensor",
        "Speed Distance", "Metal Detector", "Step Counter",
        "Ammonia Sensor", "Optical Sensor", "DO Sensor"
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
        // Update translated text state
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
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5) // Background color
    val cardBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White // Card background color
    val textColor = if (isDarkMode) Color.White else Color.Black // Primary text color
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF757575) // Secondary text color
    val dividerColor = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFE0E0E0) // Divider color

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

    // Start periodic Bluetooth scanning when permissions are granted, stop on dispose
    DisposableEffect(isPermissionGranted.value) {
        if (isPermissionGranted.value) {
            bluetoothViewModel.startPeriodicScan(activity)
        }
        onDispose {
            bluetoothViewModel.stopScan()
        }
    }

    // Main UI container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            // Apply system bars padding to avoid overlap with status and navigation bars
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top app bar
            androidx.compose.material.TopAppBar(
                backgroundColor = cardBackgroundColor,
                elevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
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
                            tint = if (isDarkMode) Color.Yellow else Color(0xFF5D4037)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Main content list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
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
                                        onClick = {
                                            if (isPermissionGranted.value && !isScanning) {
                                                bluetoothViewModel.startPeriodicScan(activity)
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
                                // Display devices (limited or all based on showAllDevices)
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
                                // Show more/less toggle if more than 4 devices
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
                    // List of allowed game device names
                    val allowedGameDevices = listOf(
                        "Scarlet Witch", "Black Widow", "Captain Marvel", "Wasp", "Hela",
                        "Hulk", "Thor", "Iron_Man", "Spider Man", "Captain America"
                    )
                    // Filter devices to show only game devices
                    val gameDevices = bluetoothDevices.filter { it.name in allowedGameDevices }
                    val devicesToShow = if (showAllDevices) gameDevices else gameDevices.take(4)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp), // Match Nearby Devices padding
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
                                // Refresh scan button for game devices
                                IconButton(
                                    onClick = {
                                        if (isPermissionGranted.value && !isScanning) {
                                            bluetoothViewModel.startPeriodicScan(activity)
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

                            // Handle permission and game device states
                            if (!isPermissionGranted.value) {
                                Text(
                                    text = translatedText.bluetoothPermissionsRequired,
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
                                // Display game devices (limited or all based on showAllDevices)
                                devicesToShow.forEach { device ->
                                    BluetoothDeviceItem(
                                        device = device,
                                        navController = navController,
                                        selectedSensor = selectedSensor,
                                        isDarkMode = isDarkMode
                                    )
                                    Divider(color = dividerColor)
                                }
                                // Show more/less toggle if more than 4 game devices
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

//            // Bottom navigation bar
            Box {
                CustomBottomNavigation(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    navController = navController,
                    isDarkMode = isDarkMode
                )
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
    device: BluetoothScanViewModel.BluetoothDevice, // Device data
    navController: NavHostController, // Navigation controller
    selectedSensor: String, // Selected sensor type
    isDarkMode: Boolean, // Dark mode state
    showPreview: Boolean = false // Whether to show sensor preview
) {
    // Define theme-based colors
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF757575)
    val iconBackgroundColor = if (isDarkMode) Color(0xFF0D47A1) else Color(0xFFE3F2FD)
    val iconTintColor = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF2196F3)

    // Device item row, clickable to navigate to details
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
        // Device icon
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
            // Device name
            Text(
                text = device.name,
                style = MaterialTheme.typography.subtitle1,
                color = textColor
            )
            // Device address
            Text(
                text = "Address: ${device.address}",
                style = MaterialTheme.typography.caption,
                color = secondaryTextColor
            )
            // Signal strength
            Text(
                text = "Signal Strength: ${device.rssi} dBm",
                style = MaterialTheme.typography.caption,
                color = secondaryTextColor
            )

            // Display sensor data based on selected sensor type
            device.sensorData?.let { sensorData ->
                // Show preview for DO Sensor if enabled
                if (showPreview && sensorData is BluetoothScanViewModel.SensorData.DOSensorData) {
                    DOSensorPreview(
                        temperature = sensorData.temperature,
                        doPercentage = sensorData.doPercentage,
                        doValue = sensorData.doValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
                // Show preview for Optical Sensor if enabled
                if (showPreview && sensorData is BluetoothScanViewModel.SensorData.OpticalSensorData) {
                    OpticalSensorPreview(
                        values = sensorData.reflectanceValues,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }

                // Display sensor data text based on sensor type
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
                            is BluetoothScanViewModel.SensorData.OpticalSensorData ->
                                "Reflectance: ${sensorData.reflectanceValues.take(3).joinToString(", ") { "%.2f".format(it) }}" +
                                        (if (sensorData.reflectanceValues.size > 3) " +${sensorData.reflectanceValues.size - 3} more" else "")
                            is BluetoothScanViewModel.SensorData.LuxSensorData ->
                                "Device ID: ${sensorData.deviceId}, Raw: ${sensorData.rawData}"
                            else -> "No data available"
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
                        selectedSensor == "Optical Sensor" && sensorData is BluetoothScanViewModel.SensorData.OpticalSensorData ->
                            "Reflectance: ${sensorData.reflectanceValues.take(3).joinToString(", ") { "%.2f".format(it) }}" +
                                    (if (sensorData.reflectanceValues.size > 3) " +${sensorData.reflectanceValues.size - 3} more" else "")
                        selectedSensor == "Lux Sensor" && sensorData is BluetoothScanViewModel.SensorData.LuxSensorData ->
                            "Device ID: ${sensorData.deviceId}, Raw: ${sensorData.rawData}"
                        else -> "Incompatible sensor type"
                    },
                    style = MaterialTheme.typography.caption,
                    color = if (isDarkMode) Color(0xFF64B5F6) else MaterialTheme.colors.primary,
                    maxLines = 2
                )
            } ?: Text(
                text = "No sensor data",
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

// Composable for Optical Sensor data preview
@Composable
fun OpticalSensorPreview(
    values: List<Float>, // Reflectance values
    modifier: Modifier = Modifier
) {
    // Exit if insufficient data
    if (values.size < 18) return // Need all 18 values

    Box(
        modifier = modifier
            .height(60.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Draw bar chart for reflectance values
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = size.width / 20f
            val maxValue = values.maxOrNull() ?: 1f

            // Draw simple bar chart preview
            for (i in 0 until 18) {
                val value = values[i]
                val normalizedValue = (value / maxValue).coerceIn(0f, 1f)
                val barHeight = size.height * normalizedValue

                val x = i * (size.width / 18) + 2.dp.toPx()

                drawRect(
                    color = Color(
                        red = 0.2f,
                        green = 0.7f * normalizedValue + 0.3f,
                        blue = 0.2f,
                        alpha = 0.8f
                    ),
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth - 4.dp.toPx(), barHeight)
                )
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

// Composable for custom bottom navigation bar

@Composable
fun CustomBottomNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    isDarkMode: Boolean = false
) {
    val backgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val contentColor = if (isDarkMode) Color.White else Color.Black
    val selectedColor = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF2196F3)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { /* Handle result if needed */ }
    )

    BottomNavigation(
        modifier = modifier,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = 8.dp
    ) {
        BottomNavigationItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.bluetooth),
                    contentDescription = "Bluetooth",
                    modifier = Modifier.size(24.dp),
                    tint = selectedColor
                )
            },
            selected = true,
            onClick = { /* Navigate to Bluetooth */ }
        )

        BottomNavigationItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.gamepad),
                    contentDescription = "Gameplay",
                    modifier = Modifier.size(24.dp),
                    tint = if (currentRoute == "game_loading") selectedColor else contentColor
                )
            },
            selected = currentRoute == "game_loading",
            onClick = {
                navController.navigate("game_loading")
            }
        )





        BottomNavigationItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.robo_car_icon),
                    contentDescription = "Robot Control",
                    modifier = Modifier.size(32.dp),
                    tint = if (currentRoute == "robot_control") selectedColor else contentColor
                )
            },
            selected = currentRoute == "robot_control",
            onClick = {
                val intent = Intent(context, RobotControlCompose::class.java)
                launcher.launch(intent)
            }
        )

        BottomNavigationItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.icons_leaf),
                    contentDescription = "Optical Sensor",
                    modifier = Modifier.size(24.dp),
                    tint = if (currentRoute == "optical_sensor_screen") selectedColor else contentColor
                )
            },
            selected = currentRoute == "optical_sensor_screen",
            onClick = {
                if (currentRoute != "optical_sensor_screen") {
                    navController.navigate("optical_sensor_screen") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            alwaysShowLabel = false
        )

        BottomNavigationItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.waterquality),
                    contentDescription = "Water Quality",
                    modifier = Modifier.size(24.dp),
                    tint = if (currentRoute == "water_quality_screen") selectedColor else contentColor
                )
            },
            selected = currentRoute == "water_quality_screen",
            onClick = {
                if (currentRoute != "water_quality_screen") {
                    navController.navigate("water_quality_screen") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            alwaysShowLabel = false
        )

        BottomNavigationItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.settings),
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp),
                    tint = if (currentRoute == "settings_screen") selectedColor else contentColor
                )
            },
            selected = currentRoute == "settings_screen",
            onClick = {
                if (currentRoute != "settings_screen") {
                    navController.navigate("settings_screen") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }
}
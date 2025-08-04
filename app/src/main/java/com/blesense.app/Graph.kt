package com.blesense.app

// Import necessary libraries for Compose, Android, and navigation
import android.app.Activity
import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Data class for 3D point coordinates with rotation functions
data class Point3D(val x: Float, val y: Float, val z: Float) {
    // Rotate point around X-axis
    fun rotateX(angle: Double): Point3D = Point3D(
        x,
        (y * cos(angle) - z * sin(angle)).toFloat(),
        (y * sin(angle) + z * cos(angle)).toFloat()
    )

    // Rotate point around Y-axis
    fun rotateY(angle: Double): Point3D = Point3D(
        (x * cos(angle) + z * sin(angle)).toFloat(),
        y,
        (-x * sin(angle) + z * cos(angle)).toFloat()
    )

    // Rotate point around Z-axis
    fun rotateZ(angle: Double): Point3D = Point3D(
        (x * cos(angle) - y * sin(angle)).toFloat(),
        (x * sin(angle) + y * cos(angle)).toFloat(),
        z
    )
}

// Draw a 3D cube with specified rotations
private fun DrawScope.draw3DCube(rotX: Float, rotY: Float, rotZ: Float, canvasSize: Size) {
    val centerX = canvasSize.width / 2 // Center X of canvas
    val centerY = canvasSize.height / 2 // Center Y of canvas
    val cubeSize = 80f // Cube size

    // Define all 8 vertices of the cube
    val vertices = listOf(
        Point3D(-cubeSize/2, -cubeSize/2, -cubeSize/2), // 0
        Point3D(cubeSize/2, -cubeSize/2, -cubeSize/2),  // 1
        Point3D(cubeSize/2, cubeSize/2, -cubeSize/2),   // 2
        Point3D(-cubeSize/2, cubeSize/2, -cubeSize/2),  // 3
        Point3D(-cubeSize/2, -cubeSize/2, cubeSize/2),  // 4
        Point3D(cubeSize/2, -cubeSize/2, cubeSize/2),   // 5
        Point3D(cubeSize/2, cubeSize/2, cubeSize/2),    // 6
        Point3D(-cubeSize/2, cubeSize/2, cubeSize/2)    // 7
    )

    // Apply rotations and project to 2D
    val projectedVertices = vertices.map { vertex ->
        val rotated = vertex
            .rotateX(Math.toRadians(rotX.toDouble())) // Rotate around X
            .rotateY(Math.toRadians(rotY.toDouble())) // Rotate around Y
            .rotateZ(Math.toRadians(rotZ.toDouble())) // Rotate around Z

        // Simple perspective projection
        val scale = 200f / (200f + rotated.z)
        Offset(
            centerX + rotated.x * scale,
            centerY + rotated.y * scale
        )
    }

    // Draw cube edges
    drawCubeEdges(projectedVertices)
}

// Draw edges and vertices of the cube
private fun DrawScope.drawCubeEdges(vertices: List<Offset>) {
    // Define edges by connecting vertices (12 edges in a cube)
    val edges = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 0, // Front face
        4 to 5, 5 to 6, 6 to 7, 7 to 4, // Back face
        0 to 4, 1 to 5, 2 to 6, 3 to 7  // Connecting edges
    )

    // Draw each edge
    edges.forEach { (startIdx, endIdx) ->
        if (startIdx < vertices.size && endIdx < vertices.size) {
            drawLine(
                color = Color.White,
                start = vertices[startIdx],
                end = vertices[endIdx],
                strokeWidth = 3f
            )
        }
    }

    // Draw small circles at each vertex
    vertices.forEach { vertex ->
        drawCircle(
            color = Color.Red,
            radius = 5f,
            center = vertex
        )
    }
}

// Data class for translatable text in ChartScreen
data class TranslatedChartText(
    val graphsTitle: String = "Graphs",
    val temperatureLabel: String = "Temperature (°C)",
    val humidityLabel: String = "Humidity (%)",
    val speedLabel: String = "Speed (m/s)",
    val distanceLabel: String = "Distance (m)",
    val xAxisLabel: String = "X Axis (g)",
    val yAxisLabel: String = "Y Axis (g)",
    val zAxisLabel: String = "Z Axis (g)",
    val soilSensorDataTitle: String = "Soil Sensor Data (Click for detailed view)",
    val soilMoistureLabel: String = "Soil Moisture (%)",
    val soilTemperatureLabel: String = "Soil Temperature (°C)",
    val soilNitrogenLabel: String = "Soil Nitrogen (ppm)",
    val soilPhosphorusLabel: String = "Soil Phosphorus (ppm)",
    val soilPotassiumLabel: String = "Soil Potassium (ppm)",
    val soilEcLabel: String = "Soil EC (µS/cm)",
    val soilPhLabel: String = "Soil pH",
    val clickToViewAll: String = "Click to view all soil parameters and table view",
    val waitingForData: String = "Waiting for data...",
    val waitingForSensorData: String = "Waiting for sensor data...",
    val ensureDeviceConnected: String = "Make sure the device is connected and sending data",
    val currentLabel: String = "Current",
    val naLabel: String = "N/A",
    val graphsTab: String = "Graphs",
    val soilDataTableTab: String = "Soil Data Table",
    val backToAllSensors: String = "Back to All Sensors"
)

// Composable for displaying sensor data charts and tables
@Composable
fun ChartScreen(
    navController: NavController, // Navigation controller
    deviceAddress: String? = null // Optional device address
) {
    // Get application context
    val context = LocalContext.current
    val application = context.applicationContext as Application
    // Create ViewModel factory
    val factory = remember { BluetoothScanViewModelFactory(application) }
    // Initialize ViewModel
    val viewModel: BluetoothScanViewModel<Any?> = viewModel(factory = factory)

    // Observe theme and language state
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()

    // Collect sensor data for the specified device
    val sensorData by remember(deviceAddress) {
        viewModel.devices
            .map { devices -> devices.find { it.address == deviceAddress }?.sensorData }
    }.collectAsState(initial = null)

    // Extract sensor data values
    val temperatureData = (sensorData as? BluetoothScanViewModel.SensorData.SHT40Data)?.temperature?.toFloatOrNull()
    val humidityData = (sensorData as? BluetoothScanViewModel.SensorData.SHT40Data)?.humidity?.toFloatOrNull()
    val speedData = (sensorData as? BluetoothScanViewModel.SensorData.SDTData)?.speed?.toFloatOrNull()
    val distanceData = (sensorData as? BluetoothScanViewModel.SensorData.SDTData)?.distance?.toFloatOrNull()
    val xAxisData = (sensorData as? BluetoothScanViewModel.SensorData.LIS2DHData)?.x?.toFloatOrNull()
    val yAxisData = (sensorData as? BluetoothScanViewModel.SensorData.LIS2DHData)?.y?.toFloatOrNull()
    val zAxisData = (sensorData as? BluetoothScanViewModel.SensorData.LIS2DHData)?.z?.toFloatOrNull()
    val soilMoistureData = (sensorData as? BluetoothScanViewModel.SensorData.SoilSensorData)?.moisture?.toFloatOrNull()
    val soilTemperatureData = (sensorData as? BluetoothScanViewModel.SensorData.SoilSensorData)?.temperature?.toFloatOrNull()
    val soilNitrogenData = (sensorData as? BluetoothScanViewModel.SensorData.SoilSensorData)?.nitrogen?.toFloatOrNull()
    val soilPhosphorusData = (sensorData as? BluetoothScanViewModel.SensorData.SoilSensorData)?.phosphorus?.toFloatOrNull()
    val soilPotassiumData = (sensorData as? BluetoothScanViewModel.SensorData.SoilSensorData)?.potassium?.toFloatOrNull()
    val soilEcData = (sensorData as? BluetoothScanViewModel.SensorData.SoilSensorData)?.ec?.toFloatOrNull()
    val soilPhData = (sensorData as? BluetoothScanViewModel.SensorData.SoilSensorData)?.pH?.toFloatOrNull()

    // Historical data lists to store sensor values
    val temperatureHistory = remember { mutableStateListOf<Float>() }
    val humidityHistory = remember { mutableStateListOf<Float>() }
    val speedHistory = remember { mutableStateListOf<Float>() }
    val distanceHistory = remember { mutableStateListOf<Float>() }
    val xAxisHistory = remember { mutableStateListOf<Float>() }
    val yAxisHistory = remember { mutableStateListOf<Float>() }
    val zAxisHistory = remember { mutableStateListOf<Float>() }
    val soilMoistureHistory = remember { mutableStateListOf<Float>() }
    val soilTemperatureHistory = remember { mutableStateListOf<Float>() }
    val soilNitrogenHistory = remember { mutableStateListOf<Float>() }
    val soilPhosphorusHistory = remember { mutableStateListOf<Float>() }
    val soilPotassiumHistory = remember { mutableStateListOf<Float>() }
    val soilEcHistory = remember { mutableStateListOf<Float>() }
    val soilPhHistory = remember { mutableStateListOf<Float>() }
    val soilDataTimestamps = remember { mutableStateListOf<String>() }
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // Initialize translated text with cached values or defaults
    var translatedText by remember {
        mutableStateOf(
            TranslatedChartText(
                graphsTitle = TranslationCache.get("Graphs-$currentLanguage") ?: "Graphs",
                temperatureLabel = TranslationCache.get("Temperature (°C)-$currentLanguage") ?: "Temperature (°C)",
                humidityLabel = TranslationCache.get("Humidity (%)-$currentLanguage") ?: "Humidity (%)",
                speedLabel = TranslationCache.get("Speed (m/s)-$currentLanguage") ?: "Speed (m/s)",
                distanceLabel = TranslationCache.get("Distance (m)-$currentLanguage") ?: "Distance (m)",
                xAxisLabel = TranslationCache.get("X Axis (g)-$currentLanguage") ?: "X Axis (g)",
                yAxisLabel = TranslationCache.get("Y Axis (g)-$currentLanguage") ?: "Y Axis (g)",
                zAxisLabel = TranslationCache.get("Z Axis (g)-$currentLanguage") ?: "Z Axis (g)",
                soilSensorDataTitle = TranslationCache.get("Soil Sensor Data (Click for detailed view)-$currentLanguage") ?: "Soil Sensor Data (Click for detailed view)",
                soilMoistureLabel = TranslationCache.get("Soil Moisture (%)-$currentLanguage") ?: "Soil Moisture (%)",
                soilTemperatureLabel = TranslationCache.get("Soil Temperature (°C)-$currentLanguage") ?: "Soil Temperature (°C)",
                soilNitrogenLabel = TranslationCache.get("Soil Nitrogen (ppm)-$currentLanguage") ?: "Soil Nitrogen (ppm)",
                soilPhosphorusLabel = TranslationCache.get("Soil Phosphorus (ppm)-$currentLanguage") ?: "Soil Phosphorus (ppm)",
                soilPotassiumLabel = TranslationCache.get("Soil Potassium (ppm)-$currentLanguage") ?: "Soil Potassium (ppm)",
                soilEcLabel = TranslationCache.get("Soil EC (µS/cm)-$currentLanguage") ?: "Soil EC (µS/cm)",
                soilPhLabel = TranslationCache.get("Soil pH-$currentLanguage") ?: "Soil pH",
                clickToViewAll = TranslationCache.get("Click to view all soil parameters and table view-$currentLanguage") ?: "Click to view all soil parameters and table view",
                waitingForData = TranslationCache.get("Waiting for data...-$currentLanguage") ?: "Waiting for data...",
                waitingForSensorData = TranslationCache.get("Waiting for sensor data...-$currentLanguage") ?: "Waiting for sensor data...",
                ensureDeviceConnected = TranslationCache.get("Make sure the device is connected and sending data-$currentLanguage") ?: "Make sure the device is connected and sending data",
                currentLabel = TranslationCache.get("Current-$currentLanguage") ?: "Current",
                naLabel = TranslationCache.get("N/A-$currentLanguage") ?: "N/A",
                graphsTab = TranslationCache.get("Graphs-$currentLanguage") ?: "Graphs",
                soilDataTableTab = TranslationCache.get("Soil Data Table-$currentLanguage") ?: "Soil Data Table",
                backToAllSensors = TranslationCache.get("Back to All Sensors-$currentLanguage") ?: "Back to All Sensors"
            )
        )
    }

    // Preload translations when language changes
    LaunchedEffect(currentLanguage) {
        val translator = GoogleTranslationService()
        val textsToTranslate = listOf(
            "Graphs", "Temperature (°C)", "Humidity (%)", "Speed (m/s)", "Distance (m)",
            "X Axis (g)", "Y Axis (g)", "Z Axis (g)", "Soil Sensor Data (Click for detailed view)",
            "Soil Moisture (%)", "Soil Temperature (°C)", "Soil Nitrogen (ppm)", "Soil Phosphorus (ppm)",
            "Soil Potassium (ppm)", "Soil EC (µS/cm)", "Soil pH",
            "Click to view all soil parameters and table view", "Waiting for data...",
            "Waiting for sensor data...", "Make sure the device is connected and sending data",
            "Current", "N/A", "Graphs", "Soil Data Table", "Back to All Sensors"
        )
        val translatedList = translator.translateBatch(textsToTranslate, currentLanguage)
        // Update translated text state
        translatedText = TranslatedChartText(
            graphsTitle = translatedList[0],
            temperatureLabel = translatedList[1],
            humidityLabel = translatedList[2],
            speedLabel = translatedList[3],
            distanceLabel = translatedList[4],
            xAxisLabel = translatedList[5],
            yAxisLabel = translatedList[6],
            zAxisLabel = translatedList[7],
            soilSensorDataTitle = translatedList[8],
            soilMoistureLabel = translatedList[9],
            soilTemperatureLabel = translatedList[10],
            soilNitrogenLabel = translatedList[11],
            soilPhosphorusLabel = translatedList[12],
            soilPotassiumLabel = translatedList[13],
            soilEcLabel = translatedList[14],
            soilPhLabel = translatedList[15],
            clickToViewAll = translatedList[16],
            waitingForData = translatedList[17],
            waitingForSensorData = translatedList[18],
            ensureDeviceConnected = translatedList[19],
            currentLabel = translatedList[20],
            naLabel = translatedList[21],
            graphsTab = translatedList[22],
            soilDataTableTab = translatedList[23],
            backToAllSensors = translatedList[24]
        )
    }

    // Define theme-based colors
    val backgroundGradient = if (isDarkMode) {
        Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF424242))) // Dark mode gradient
    } else {
        Brush.verticalGradient(listOf(Color.White, Color.LightGray)) // Light mode gradient
    }
    val cardBackground = if (isDarkMode) Color(0xFF1E1E1E) else Color.White // Card background
    val textColor = if (isDarkMode) Color.White else Color.Black // Primary text color
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray // Secondary text color
    val accentColor = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF0A74DA) // Accent color
    val tabBackground = if (isDarkMode) Color(0xFF2A2A2A) else Color.Transparent // Tab background
    val appBarBackground = if (isDarkMode) Color(0xFF121212) else Color.Transparent // App bar background

    // State variables for UI control
    val isReceivingData = remember { mutableStateOf(false) } // Track if data is being received
    val hasSoilSensorData = remember { mutableStateOf(false) } // Track if soil sensor data exists
    var isSoilSensorClicked by remember { mutableStateOf(false) } // Track soil sensor card click
    var selectedTabIndex by remember { mutableStateOf(0) } // Track selected tab
    val tabTitles = listOf(translatedText.graphsTab, translatedText.soilDataTableTab) // Tab titles

    // Start Bluetooth scanning
    LaunchedEffect(Unit) {
        viewModel.startScan(context as Activity)
    }

    // Update data receiving state
    LaunchedEffect(sensorData) {
        if (temperatureData != null || humidityData != null || speedData != null ||
            distanceData != null || xAxisData != null || yAxisData != null || zAxisData != null ||
            soilMoistureData != null || soilTemperatureData != null || soilNitrogenData != null ||
            soilPhosphorusData != null || soilPotassiumData != null || soilEcData != null || soilPhData != null) {
            isReceivingData.value = true
        }
        if (soilMoistureData != null || soilTemperatureData != null || soilNitrogenData != null ||
            soilPhosphorusData != null || soilPotassiumData != null || soilEcData != null || soilPhData != null) {
            hasSoilSensorData.value = true
        }
    }

    // Update history lists with new sensor data
    LaunchedEffect(temperatureData, humidityData, speedData, distanceData, xAxisData, yAxisData, zAxisData,
        soilMoistureData, soilTemperatureData, soilNitrogenData, soilPhosphorusData, soilPotassiumData, soilEcData, soilPhData) {
        temperatureData?.let { updateHistory(temperatureHistory, it) }
        humidityData?.let { updateHistory(humidityHistory, it) }
        speedData?.let { updateHistory(speedHistory, it) }
        distanceData?.let { updateHistory(distanceHistory, it) }
        xAxisData?.let { updateHistory(xAxisHistory, it) }
        yAxisData?.let { updateHistory(yAxisHistory, it) }
        zAxisData?.let { updateHistory(zAxisHistory, it) }
        val shouldAddTimestamp = soilMoistureData != null || soilTemperatureData != null ||
                soilNitrogenData != null || soilPhosphorusData != null || soilPotassiumData != null ||
                soilEcData != null || soilPhData != null
        if (shouldAddTimestamp) {
            if (soilDataTimestamps.size >= 20) soilDataTimestamps.removeAt(0) // Limit to 20 entries
            soilDataTimestamps.add(dateFormat.format(Date())) // Add timestamp
        }
        soilMoistureData?.let { updateHistory(soilMoistureHistory, it) }
        soilTemperatureData?.let { updateHistory(soilTemperatureHistory, it) }
        soilNitrogenData?.let { updateHistory(soilNitrogenHistory, it) }
        soilPhosphorusData?.let { updateHistory(soilPhosphorusHistory, it) }
        soilPotassiumData?.let { updateHistory(soilPotassiumHistory, it) }
        soilEcData?.let { updateHistory(soilEcHistory, it) }
        soilPhData?.let { updateHistory(soilPhHistory, it) }
    }

    // Main UI structure
    Scaffold(
        topBar = {
            // Top app bar with navigation and actions
            TopAppBar(
                title = {
                    Text(
                        translatedText.graphsTitle,
                        fontFamily = helveticaFont,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Handle Export */ }) {
                        Icon(Icons.Default.TableChart, contentDescription = "Export", tint = textColor)
                    }
                    IconButton(onClick = { /* Handle Options */ }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Options", tint = textColor)
                    }
                },
                backgroundColor = appBarBackground, // Theme-based app bar color
                elevation = 0.dp
            )
        },
        backgroundColor = Color.Transparent // Transparent scaffold background
    ) { paddingValues ->
        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient) // Apply gradient background
                .padding(paddingValues)
        ) {
            Column {
                // Show tabs for soil sensor data if clicked
                if (isSoilSensorClicked && hasSoilSensorData.value) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        backgroundColor = tabBackground,
                        contentColor = accentColor
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                text = { Text(title, color = textColor) },
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index }
                            )
                        }
                    }
                }

                // Show graphs or table based on state
                if (!isSoilSensorClicked || (isSoilSensorClicked && selectedTabIndex == 0)) {
                    // Display sensor graphs
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // SHT40 sensor data (temperature and humidity)
                        if (sensorData is BluetoothScanViewModel.SensorData.SHT40Data) {
                            item { SensorGraphCard(translatedText.temperatureLabel, temperatureData, temperatureHistory, Color(0xFFE53935), cardBackground, textColor, secondaryTextColor, translatedText) }
                            item { SensorGraphCard(translatedText.humidityLabel, humidityData, humidityHistory, Color(0xFF1976D2), cardBackground, textColor, secondaryTextColor, translatedText) }
                        }
                        // SDT sensor data (speed and distance)
                        if (sensorData is BluetoothScanViewModel.SensorData.SDTData) {
                            item { SensorGraphCard(translatedText.speedLabel, speedData, speedHistory, Color(0xFF43A047), cardBackground, textColor, secondaryTextColor, translatedText) }
                            item { SensorGraphCard(translatedText.distanceLabel, distanceData, distanceHistory, Color(0xFFFFB300), cardBackground, textColor, secondaryTextColor, translatedText) }
                        }
                        // LIS2DH sensor data (accelerometer)
                        if (sensorData is BluetoothScanViewModel.SensorData.LIS2DHData) {
                            item { SensorGraphCard(translatedText.xAxisLabel, xAxisData, xAxisHistory, Color(0xFFE91E63), cardBackground, textColor, secondaryTextColor, translatedText) }
                            item { SensorGraphCard(translatedText.yAxisLabel, yAxisData, yAxisHistory, Color(0xFF9C27B0), cardBackground, textColor, secondaryTextColor, translatedText) }
                            item { SensorGraphCard(translatedText.zAxisLabel, zAxisData, zAxisHistory, Color(0xFF009688), cardBackground, textColor, secondaryTextColor, translatedText) }

                            // 3D accelerometer visualization
                            item {
                                Accelerometer3DVisualization(
                                    xAxis = xAxisData,
                                    yAxis = yAxisData,
                                    zAxis = zAxisData,
                                    cardBackground = cardBackground,
                                    textColor = textColor,
                                    isDarkMode = isDarkMode
                                )
                            }
                        }
                        // Soil sensor data
                        if (sensorData is BluetoothScanViewModel.SensorData.SoilSensorData) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isSoilSensorClicked = true }, // Toggle detailed view
                                    elevation = 2.dp,
                                    backgroundColor = if (isSoilSensorClicked) accentColor.copy(alpha = 0.1f) else cardBackground
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            translatedText.soilSensorDataTitle,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        SensorGraphCard(translatedText.soilMoistureLabel, soilMoistureData, soilMoistureHistory, Color(0xFF6200EA), cardBackground, textColor, secondaryTextColor, translatedText)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        SensorGraphCard(translatedText.soilTemperatureLabel, soilTemperatureData, soilTemperatureHistory, Color(0xFFFF6D00), cardBackground, textColor, secondaryTextColor, translatedText)
                                        if (!isSoilSensorClicked) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 16.dp),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(translatedText.clickToViewAll, color = accentColor)
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            SensorGraphCard(translatedText.soilNitrogenLabel, soilNitrogenData, soilNitrogenHistory, Color(0xFF00897B), cardBackground, textColor, secondaryTextColor, translatedText)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            SensorGraphCard(translatedText.soilPhosphorusLabel, soilPhosphorusData, soilPhosphorusHistory, Color(0xFFC2185B), cardBackground, textColor, secondaryTextColor, translatedText)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            SensorGraphCard(translatedText.soilPotassiumLabel, soilPotassiumData, soilPotassiumHistory, Color(0xFF7B1FA2), cardBackground, textColor, secondaryTextColor, translatedText)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            SensorGraphCard(translatedText.soilEcLabel, soilEcData, soilEcHistory, Color(0xFFF57C00), cardBackground, textColor, secondaryTextColor, translatedText)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            SensorGraphCard(translatedText.soilPhLabel, soilPhData, soilPhHistory, Color(0xFFD32F2F), cardBackground, textColor, secondaryTextColor, translatedText)
                                        }
                                    }
                                }
                            }
                        }
                        // Show message if no data is received
                        if (!isReceivingData.value) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        translatedText.waitingForSensorData,
                                        modifier = Modifier.padding(vertical = 32.dp),
                                        color = textColor
                                    )
                                    Text(
                                        translatedText.ensureDeviceConnected,
                                        fontSize = 14.sp,
                                        color = secondaryTextColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (isSoilSensorClicked && selectedTabIndex == 1) {
                    // Display soil sensor data table
                    SoilSensorDataTable(
                        soilMoistureHistory = soilMoistureHistory,
                        soilTemperatureHistory = soilTemperatureHistory,
                        soilNitrogenHistory = soilNitrogenHistory,
                        soilPhosphorusHistory = soilPhosphorusHistory,
                        soilPotassiumHistory = soilPotassiumHistory,
                        soilEcHistory = soilEcHistory,
                        soilPhHistory = soilPhHistory,
                        timestamps = soilDataTimestamps,
                        isReceivingData = isReceivingData.value && (soilMoistureHistory.isNotEmpty() || soilTemperatureHistory.isNotEmpty() ||
                                soilNitrogenHistory.isNotEmpty() || soilPhosphorusHistory.isNotEmpty() || soilPotassiumHistory.isNotEmpty() ||
                                soilEcHistory.isNotEmpty() || soilPhHistory.isNotEmpty()),
                        translatedText = translatedText,
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        cardBackground = cardBackground
                    )
                }

                // Button to return to all sensors
                if (isSoilSensorClicked) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = accentColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { isSoilSensorClicked = false } // Reset to main view
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                translatedText.backToAllSensors,
                                color = if (isDarkMode) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Composable for displaying a single sensor graph
@Composable
fun SensorGraphCard(
    title: String, // Graph title
    currentValue: Float?, // Current sensor value
    history: List<Float>, // Historical data
    color: Color, // Graph color
    cardBackground: Color, // Card background color
    textColor: Color, // Primary text color
    secondaryTextColor: Color, // Secondary text color
    translatedText: TranslatedChartText // Translated text
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${translatedText.currentLabel}: ${currentValue?.toString() ?: translatedText.naLabel}",
                fontSize = 16.sp,
                color = secondaryTextColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (history.isNotEmpty()) {
                // Draw graph
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    val points = history.toList()
                    if (points.isNotEmpty()) {
                        val maxValue = points.maxOrNull() ?: 0f // Maximum value
                        val minValue = (points.minOrNull() ?: 0f).coerceAtMost(maxValue - 1f) // Minimum value
                        val range = (maxValue - minValue).coerceAtLeast(1f) // Value range
                        val stepX = size.width / (points.size.coerceAtLeast(2) - 1) // X-axis step
                        val heightPadding = size.height * 0.1f // Padding for graph

                        // Draw baseline
                        drawLine(
                            color = secondaryTextColor,
                            start = Offset(0f, size.height - heightPadding),
                            end = Offset(size.width, size.height - heightPadding),
                            strokeWidth = 1f
                        )

                        // Draw data points and lines
                        for (i in 0 until points.size - 1) {
                            val x1 = i * stepX
                            val x2 = (i + 1) * stepX
                            val y1 = heightPadding + (size.height - 2 * heightPadding) * (1 - (points[i] - minValue) / range)
                            val y2 = heightPadding + (size.height - 2 * heightPadding) * (1 - (points[i + 1] - minValue) / range)

                            drawLine(
                                color = color,
                                start = Offset(x1, y1),
                                end = Offset(x2, y2),
                                strokeWidth = 4f,
                                pathEffect = PathEffect.cornerPathEffect(10f)
                            )
                            drawCircle(
                                color = color,
                                radius = 6f,
                                center = Offset(x1, y1)
                            )
                        }
                        // Draw last point
                        val lastX = (points.size - 1) * stepX
                        val lastY = heightPadding + (size.height - 2 * heightPadding) * (1 - (points.last() - minValue) / range)
                        drawCircle(
                            color = color,
                            radius = 6f,
                            center = Offset(lastX, lastY)
                        )
                    }
                }
            } else {
                // Show waiting message if no data
                Text(
                    translatedText.waitingForData,
                    modifier = Modifier.padding(vertical = 32.dp),
                    color = textColor
                )
            }
        }
    }
}

// Helper function to update history lists
private fun updateHistory(history: MutableList<Float>, value: Float) {
    if (history.size >= 20) history.removeAt(0) // Limit to 20 entries
    history.add(value) // Add new value
}

// Composable for displaying soil sensor data in a table
@Composable
fun SoilSensorDataTable(
    soilMoistureHistory: List<Float>,
    soilTemperatureHistory: List<Float>,
    soilNitrogenHistory: List<Float>,
    soilPhosphorusHistory: List<Float>,
    soilPotassiumHistory: List<Float>,
    soilEcHistory: List<Float>,
    soilPhHistory: List<Float>,
    timestamps: List<String>,
    isReceivingData: Boolean,
    translatedText: TranslatedChartText,
    textColor: Color,
    secondaryTextColor: Color,
    cardBackground: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = 4.dp,
        backgroundColor = cardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isReceivingData) {
                // Display table of soil sensor data
                LazyColumn {
                    items(timestamps.size) { index ->
                        Column {
                            Text(
                                "Timestamp: ${timestamps.getOrNull(index) ?: "-"}",
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text("${translatedText.soilMoistureLabel}: ${soilMoistureHistory.getOrNull(index) ?: "-"}", color = secondaryTextColor)
                            Text("${translatedText.soilTemperatureLabel}: ${soilTemperatureHistory.getOrNull(index) ?: "-"}", color = secondaryTextColor)
                            Text("${translatedText.soilNitrogenLabel}: ${soilNitrogenHistory.getOrNull(index) ?: "-"}", color = secondaryTextColor)
                            Text("${translatedText.soilPhosphorusLabel}: ${soilPhosphorusHistory.getOrNull(index) ?: "-"}", color = secondaryTextColor)
                            Text("${translatedText.soilPotassiumLabel}: ${soilPotassiumHistory.getOrNull(index) ?: "-"}", color = secondaryTextColor)
                            Text("${translatedText.soilEcLabel}: ${soilEcHistory.getOrNull(index) ?: "-"}", color = secondaryTextColor)
                            Text("${translatedText.soilPhLabel}: ${soilPhHistory.getOrNull(index) ?: "-"}", color = secondaryTextColor)
                            Divider(color = secondaryTextColor.copy(alpha = 0.2f), thickness = 1.dp)
                        }
                    }
                }
            } else {
                // Show waiting message if no data
                Text(
                    translatedText.waitingForSensorData,
                    modifier = Modifier.padding(vertical = 32.dp),
                    color = textColor
                )
            }
        }
    }
}

// Composable for 3D accelerometer visualization
@Composable
fun Accelerometer3DVisualization(
    xAxis: Float?, // X-axis accelerometer data
    yAxis: Float?, // Y-axis accelerometer data
    zAxis: Float?, // Z-axis accelerometer data
    cardBackground: Color, // Card background color
    textColor: Color, // Text color
    isDarkMode: Boolean // Dark mode state
) {
    // Default to 0 if null
    val x = xAxis ?: 0f
    val y = yAxis ?: 0f
    val z = zAxis ?: 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp),
        elevation = 4.dp,
        backgroundColor = cardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("3D Orientation Visualizer", color = textColor, fontSize = 18.sp)

            // Canvas for 3D visualization
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(8.dp)
            ) {
                val centerX = size.width / 2f // Canvas center X
                val centerY = size.height / 2f // Canvas center Y
                val scale = minOf(size.width, size.height) * 0.25f // Scale factor
                val axisLength = 300f // Axis length
                val diagonalLength = axisLength / sqrt(2f) // Diagonal for Z-axis

                val edgeColor = if (isDarkMode) Color.White else Color.Black // Edge color based on theme

                // Define cube vertices
                val cubeVertices = arrayOf(
                    floatArrayOf(-1f, -1f, -1f), floatArrayOf(1f, -1f, -1f),
                    floatArrayOf(1f, 1f, -1f), floatArrayOf(-1f, 1f, -1f),
                    floatArrayOf(-1f, -1f, 1f), floatArrayOf(1f, -1f, 1f),
                    floatArrayOf(1f, 1f, 1f), floatArrayOf(-1f, 1f, 1f)
                )

                // Define cube edges
                val cubeEdges = arrayOf(
                    intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 0),
                    intArrayOf(4, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 4),
                    intArrayOf(0, 4), intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7)
                )

                // Static rotation for better 3D appearance
                val rotationX = 20f
                val rotationY = 25f
                val rotationZ = 5f

                // Rotate 3D point
                fun rotate3D(x: Float, y: Float, z: Float): FloatArray {
                    val radX = Math.toRadians(rotationX.toDouble()).toFloat()
                    val radY = Math.toRadians(rotationY.toDouble()).toFloat()
                    val radZ = Math.toRadians(rotationZ.toDouble()).toFloat()

                    var ry = y * cos(radX) - z * sin(radX)
                    var rz = y * sin(radX) + z * cos(radX)
                    var rx = x

                    val rz2 = rz * cos(radY) - rx * sin(radY)
                    val rx2 = rz * sin(radY) + rx * cos(radY)

                    val rx3 = rx2 * cos(radZ) - ry * sin(radZ)
                    val ry3 = rx2 * sin(radZ) + ry * cos(radZ)

                    return floatArrayOf(rx3, ry3, rz2)
                }

                // Project 3D point to 2D
                fun project3D(x: Float, y: Float, z: Float): Offset {
                    val distance = 5f
                    val perspective = distance / (distance - z)
                    return Offset(
                        centerX + x * scale * perspective,
                        centerY - y * scale * perspective
                    )
                }

                // Project cube vertices
                val projected = cubeVertices.map {
                    val rotated = rotate3D(it[0], it[1], it[2])
                    project3D(rotated[0], rotated[1], rotated[2])
                }

                // Draw cube edges
                cubeEdges.forEach { (startIdx, endIdx) ->
                    drawLine(
                        color = edgeColor,
                        start = projected[startIdx],
                        end = projected[endIdx],
                        strokeWidth = 3f
                    )
                }

                // Draw vertices
                projected.forEach {
                    drawCircle(edgeColor, 4f, it)
                }

                // Define axis endpoints
                val xEnd = Offset(centerX + axisLength, centerY)
                val yEnd = Offset(centerX, centerY - axisLength)
                val zEnd = Offset(centerX - diagonalLength, centerY + diagonalLength)

                // Draw X, Y, Z axes
                drawLine(Color.Red, Offset(centerX, centerY), xEnd, 3f) // X
                drawLine(Color.Green, Offset(centerX, centerY), yEnd, 3f) // Y
                drawLine(Color.Blue, Offset(centerX, centerY), zEnd, 3f) // Z

                // Draw axis labels using native canvas
                drawContext.canvas.nativeCanvas.apply {
                    val labelPaint = android.graphics.Paint().apply {
                        textSize = 36f
                        isAntiAlias = true
                    }

                    labelPaint.color = android.graphics.Color.RED
                    drawText("X", xEnd.x + 10f, xEnd.y, labelPaint)

                    labelPaint.color = android.graphics.Color.GREEN
                    drawText("Y", yEnd.x + 10f, yEnd.y, labelPaint)

                    labelPaint.color = android.graphics.Color.BLUE
                    drawText("Z", zEnd.x + 20f, zEnd.y + 30f, labelPaint)
                }

                // Draw sensor point
                val maxInputRange = 10f
                val sensorX = (x / maxInputRange).coerceIn(-1f, 1f)
                val sensorY = (y / maxInputRange).coerceIn(-1f, 1f)
                val sensorZ = (z / maxInputRange).coerceIn(-1f, 1f)

                val rotatedSensor = rotate3D(sensorX, sensorY, sensorZ)
                val sensorDot = project3D(rotatedSensor[0], rotatedSensor[1], rotatedSensor[2])
                drawCircle(Color.Magenta, radius = 10f, center = sensorDot)
            }

            // Display accelerometer values
            Text(
                text = "X: %.2f, Y: %.2f, Z: %.2f".format(x, y, z),
                color = textColor,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
package com.blesense.app

import android.app.Activity
import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Utility function to convert dp to pixels for precise positioning
fun dpToPx(dp: Dp): Float {
    return dp.value
}

// Composable function for the main game screen, handling Bluetooth-based hero detection games
@Composable
fun GameActivityScreen(
    activity: Activity?, // Current Activity context, nullable for safety
    bluetoothViewModel: BluetoothScanViewModel<Any> = viewModel(
        factory = BluetoothScanViewModelFactory(LocalContext.current.applicationContext as Application)
    ), // ViewModel for Bluetooth scanning and game mode management
    onBackToHome: () -> Unit // Callback to navigate back to the home screen
) {
    // Observe dark mode and language settings for dynamic theming and localization
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()

    // State for translated text, initialized with cached or default values
    var translatedText by remember {
        mutableStateOf(
            TranslatedGameScreenText(
                huntTheHeroes = TranslationCache.get("Hunt the Heroes-$currentLanguage")
                    ?: "Hunt the Heroes",
                guessTheCharacter = TranslationCache.get("Guess the Character-$currentLanguage")
                    ?: "Guess the Character",
                whichHeroNearby = TranslationCache.get("Which hero do you think is nearby?-$currentLanguage")
                    ?: "Which hero do you think is nearby?",
                correctGuess = TranslationCache.get("Correct!-$currentLanguage") ?: "Correct!",
                youFound = TranslationCache.get("You found-$currentLanguage") ?: "You found",
                wrongGuess = TranslationCache.get("Wrong Guess!-$currentLanguage")
                    ?: "Wrong Guess!",
                tryAgain = TranslationCache.get("Try again...-$currentLanguage") ?: "Try again...",
                heroesDetected = TranslationCache.get("Heroes Detected:-$currentLanguage")
                    ?: "Heroes Detected:",
                heroesCollection = TranslationCache.get("Heroes Collection-$currentLanguage")
                    ?: "Heroes Collection",
                collected = TranslationCache.get("Collected:-$currentLanguage") ?: "Collected:"
            )
        )
    }

    // Update translations when language changes
    LaunchedEffect(currentLanguage) {
        val translator = GoogleTranslationService() // Service for translating text
        val textsToTranslate = listOf(
            "Hunt the Heroes", "Guess the Character", "Which hero do you think is nearby?",
            "Correct!", "You found", "Wrong Guess!", "Try again...", "Heroes Detected:",
            "Heroes Collection", "Collected:"
        )
        // Fetch translations for the current language
        val translatedList = translator.translateBatch(textsToTranslate, currentLanguage)
        translatedText = TranslatedGameScreenText(
            huntTheHeroes = translatedList[0],
            guessTheCharacter = translatedList[1],
            whichHeroNearby = translatedList[2],
            correctGuess = translatedList[3],
            youFound = translatedList[4],
            wrongGuess = translatedList[5],
            tryAgain = translatedList[6],
            heroesDetected = translatedList[7],
            heroesCollection = translatedList[8],
            collected = translatedList[9]
        )
    }

    // Define colors based on dark mode for consistent theming
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF8E8E93)
    val dialogBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val overlayColor = if (isDarkMode) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f)

    // Data class to manage game state
    data class GameState(
        val showScratchCard: Boolean = false, // Whether to show the scratch card
        val scratchCardHeroName: String? = null, // Hero name for the scratch card
        val expandedImage: Int? = null, // Resource ID of the expanded game mode image
        val screenWidth: Float = 0f, // Screen width in pixels
        val screenHeight: Float = 0f, // Screen height in pixels
        val isSoundOn: Boolean = true, // Whether background music is enabled
        val showTTHButton: Boolean = false, // Whether to show the "Tap to Hunt" button
        val showSearchImage: Boolean = true, // Whether to show the search animation
        val showPopup: Boolean = false, // Whether to show a popup
        val showHeroSelectionDialog: Boolean = false, // Whether to show the hero selection dialog
        val currentHeroToGuess: String? = null, // Current hero to guess in "Guess the Character"
        val showGuessResult: Boolean? = null, // Result of the guess (true for correct, false for wrong)
        val guessAnimationRunning: Boolean = false, // Whether the guess result animation is active
        val detectedHeroRssi: Int? = null, // RSSI value of the detected hero
        val lastCorrectHero: String? = null // Last correctly guessed hero
    )

    // State to manage the game state
    var gameState by remember { mutableStateOf(GameState()) }
    // Key to trigger dismissal of guess result
    val guessResultDismissKey = remember { mutableStateOf(0) }

    // List of allowed hero names for the game
    val allowedHeroes = remember {
        listOf(
            "Scarlet Witch", "Black Widow", "Captain Marvel", "Wasp", "Hela",
            "Hulk", "Thor", "Iron_Man", "Spider Man", "Captain America"
        )
    }

    // Observe Bluetooth devices from the ViewModel
    val bluetoothDevices by bluetoothViewModel.devices.collectAsState(initial = emptyList())
    // Track found characters and their counts
    var foundCharacters by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    // Get the current context and lifecycle owner
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Initialize MediaPlayer for background music
    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.bgmusic).apply {
            isLooping = true // Loop the background music
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build()
            )
            setVolume(0.5f, 0.5f) // Set volume to 50%
        }
    }

    // Initialize MediaPlayer for sound effects
    val soundPlayer = remember {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build()
            )
            setVolume(1.0f, 1.0f) // Full volume for sound effects
            isLooping = false // Do not loop sound effects
            setOnPreparedListener { start() } // Start playing when prepared
            setOnCompletionListener { reset() } // Reset after completion
        }
    }

    // Function to play a sound effect from a resource
    fun playSound(resourceId: Int) {
        soundPlayer.reset() // Reset the player
        soundPlayer.setDataSource(context, android.net.Uri.parse("android.resource://${context.packageName}/$resourceId"))
        soundPlayer.prepareAsync() // Prepare asynchronously
    }

    // Manage media players based on lifecycle events
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Pause media players when the app is paused
                    if (mediaPlayer.isPlaying) mediaPlayer.pause()
                    if (soundPlayer.isPlaying) soundPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Resume background music if sound is enabled
                    if (gameState.isSoundOn && !mediaPlayer.isPlaying) mediaPlayer.start()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    // Clean up resources when the activity is destroyed
                    mediaPlayer.release()
                    soundPlayer.release()
                    bluetoothViewModel.stopScan()
                }
                else -> {} // No action for other events
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            // Clean up when the composable is disposed
            lifecycle.removeObserver(observer)
            bluetoothViewModel.stopScan()
            if (mediaPlayer.isPlaying) mediaPlayer.stop()
            mediaPlayer.release()
            if (soundPlayer.isPlaying) soundPlayer.stop()
            soundPlayer.release()
        }
    }

    // Control background music based on sound toggle
    LaunchedEffect(gameState.isSoundOn) {
        if (gameState.isSoundOn) {
            if (!mediaPlayer.isPlaying) mediaPlayer.start()
        } else {
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
        }
    }

    // Animation for rotating search button
    val infiniteTransition = rememberInfiniteTransition(label = "searchButtonAnimation")
    val angle = if (gameState.showSearchImage) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "searchButtonRotation"
        ).value
    } else {
        0f // No rotation when search image is hidden
    }

    // Animation for blinking "Tap to Hunt" button
    val blinkAlpha = if (gameState.showTTHButton) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blinkAnimation"
        ).value
    } else {
        1.0f // Full opacity when not blinking
    }
    val radius = 100f // Radius for circular motion of search button
    val searchX = remember(angle) { radius * cos(Math.toRadians(angle.toDouble())).toFloat() }
    val searchY = remember(angle) { radius * sin(Math.toRadians(angle.toDouble())).toFloat() }

    // State for hero animation during transitions
    val animateHero by remember { mutableStateOf<String?>(null) }
    val transitionProgress by animateFloatAsState(
        targetValue = if (animateHero != null) 2f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "heroTransition"
    )
    // State to track whether the game box (heroes collection) is open
    var isGameBoxOpen by remember { mutableStateOf(false) }

    // List of particles for visual effects on correct guesses
    val particles = remember { mutableStateListOf<Triple<Float, Float, Double>>() }
    val centerX = gameState.screenWidth / 2
    val centerY = gameState.screenHeight / 2

    // Generate particles for visual effect when a hero is correctly guessed
    LaunchedEffect(gameState.lastCorrectHero) {
        if (gameState.lastCorrectHero != null) {
            particles.clear()
            repeat(4) {
                val angle = Random.nextDouble(0.0, 360.0)
                val distance = Random.nextFloat() * 25
                val startX = (centerX + cos(Math.toRadians(angle)) * distance).toFloat()
                val startY = (centerY + sin(Math.toRadians(angle)) * distance).toFloat()
                particles.add(Triple(startX, startY, angle))
            }

            // Animate particles moving outward
            repeat(2) {
                delay(100)
                particles.forEachIndexed { index, (x, y, angle) ->
                    val speedX = (cos(Math.toRadians(angle)) * 8).toFloat()
                    val speedY = (sin(Math.toRadians(angle)) * 8).toFloat()
                    particles[index] = Triple(x + speedX, y + speedY, angle)
                }
            }
            particles.clear() // Clear particles after animation
        }
    }

    // Draw particles on the canvas
    if (particles.isNotEmpty()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { (x, y, _) ->
                drawCircle(color = Color.Red, radius = 2f, center = Offset(x, y))
            }
        }
    }

    // Handle back button presses
    BackHandler(enabled = true) {
        if (gameState.expandedImage != null || gameState.showScratchCard || gameState.showHeroSelectionDialog || gameState.showGuessResult != null) {
            // Reset game state and stop sound if in a sub-screen
            if (soundPlayer.isPlaying) {
                soundPlayer.pause()
                soundPlayer.seekTo(0)
            }
            bluetoothViewModel.stopScan()
            gameState = gameState.copy(
                expandedImage = null,
                showPopup = false,
                showSearchImage = true,
                showTTHButton = false,
                showHeroSelectionDialog = false,
                showGuessResult = null,
                guessAnimationRunning = false,
                showScratchCard = false,
                scratchCardHeroName = null,
                lastCorrectHero = null
            )
            guessResultDismissKey.value += 1
        } else {
            onBackToHome() // Navigate back to home screen
        }
    }

    // Get density for pixel conversions
    val density = LocalDensity.current

    // Main UI container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor) // Apply theme-based background color
            .padding(WindowInsets.systemBars.asPaddingValues()) // Adjust for system bars
            .onGloballyPositioned { coordinates ->
                // Update screen dimensions
                gameState = gameState.copy(
                    screenWidth = coordinates.size.width.toFloat(),
                    screenHeight = coordinates.size.height.toFloat()
                )
            }
    ) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Back button
        val backButtonArrowTint = if (isDarkMode) Color.White else Color.Black
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .size(40.dp)
                .background(Color.Transparent, RoundedCornerShape(8.dp))
                .clickable { onBackToHome() }
                .zIndex(2f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.arrow_back),
                contentDescription = "Back to Home",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(backButtonArrowTint)
            )
        }

        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.1.dp)
                .zIndex(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Game title image
            Image(
                painter = painterResource(id = R.drawable.ble_games),
                contentDescription = "BLE Games Title",
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .fillMaxWidth()
                    .height(150.dp)
            )

            // Game mode buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // "Hunt the Heroes" button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .offset(y = (-30).dp)
                        .zIndex(if (gameState.expandedImage == R.drawable.hunt_the_heroes) 2f else 1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    var buttonPosition by remember { mutableStateOf(IntOffset.Zero) }
                    AnimatedImageButton(
                        imageResId = R.drawable.hunt_the_heroes,
                        contentDescription = translatedText.huntTheHeroes,
                        isExpanded = gameState.expandedImage == R.drawable.hunt_the_heroes,
                        expandedImageResId = R.drawable.hth,
                        screenWidth = gameState.screenWidth,
                        screenHeight = gameState.screenHeight,
                        currentPosition = buttonPosition,
                        onPositioned = { buttonPosition = it },
                        onClick = {
                            // Handle button click for "Hunt the Heroes"
                            if (soundPlayer.isPlaying) {
                                soundPlayer.seekTo(0)
                                soundPlayer.pause()
                            }
                            gameState = gameState.copy(
                                showGuessResult = null,
                                guessAnimationRunning = false
                            )
                            guessResultDismissKey.value += 1
                            gameState = gameState.copy(
                                expandedImage = if (gameState.expandedImage == R.drawable.hunt_the_heroes) null else R.drawable.hunt_the_heroes,
                                showPopup = !gameState.showPopup,
                                showSearchImage = true,
                                showTTHButton = false
                            )
                        }
                    )
                }
                // "Guess the Character" button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp)
                        .offset(y = (-30).dp)
                        .zIndex(if (gameState.expandedImage == R.drawable.guess_the_character) 2f else 1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    var buttonPosition by remember { mutableStateOf(IntOffset.Zero) }
                    AnimatedImageButton(
                        imageResId = R.drawable.guess_the_character,
                        contentDescription = translatedText.guessTheCharacter,
                        isExpanded = gameState.expandedImage == R.drawable.guess_the_character,
                        expandedImageResId = R.drawable.gth,
                        screenWidth = gameState.screenWidth,
                        screenHeight = gameState.screenHeight,
                        currentPosition = buttonPosition,
                        onPositioned = { buttonPosition = it },
                        onClick = {
                            // Handle button click for "Guess the Character"
                            if (soundPlayer.isPlaying) {
                                soundPlayer.pause()
                            }
                            gameState = gameState.copy(
                                showGuessResult = null,
                                guessAnimationRunning = false
                            )
                            guessResultDismissKey.value += 1
                            gameState = gameState.copy(
                                expandedImage = if (gameState.expandedImage == R.drawable.guess_the_character) null else R.drawable.guess_the_character,
                                showPopup = !gameState.showPopup,
                                showSearchImage = true,
                                showTTHButton = false
                            )
                        }
                    )
                }
            }
        }

        // Start or stop Bluetooth scanning based on game mode
        val scanInterval = 8000L
        LaunchedEffect(gameState.expandedImage) {
            when (gameState.expandedImage) {
                R.drawable.hunt_the_heroes -> {
                    bluetoothViewModel.setGameMode(BluetoothScanViewModel.GameMode.HUNT_THE_HEROES)
                    bluetoothViewModel.startScan(activity)
                }
                R.drawable.guess_the_character -> {
                    bluetoothViewModel.setGameMode(BluetoothScanViewModel.GameMode.GUESS_THE_CHARACTER)
                    bluetoothViewModel.startScan(activity)
                }
                else -> {
                    bluetoothViewModel.setGameMode(BluetoothScanViewModel.GameMode.NONE)
                    bluetoothViewModel.stopScan()
                }
            }
        }

        // "Hunt the Heroes" game mode
        if (gameState.expandedImage == R.drawable.hunt_the_heroes) {
            var showRadar by remember { mutableStateOf(false) } // Whether to show the radar
            var showCharacterReveal by remember { mutableStateOf(false) } // Whether to reveal characters
            var detectedCharacters by remember { mutableStateOf<List<String>>(emptyList()) } // Detected heroes
            var currentGuessHero by remember { mutableStateOf<String?>(null) } // Current hero to guess

            // Filter nearby heroes based on allowed list
            val nearbyHeroes = bluetoothDevices.filter { it.name in allowedHeroes }
            val rssiValues = remember(bluetoothDevices) {
                allowedHeroes.associateWith { heroName ->
                    nearbyHeroes.find { it.name == heroName }?.rssi?.toInt()
                }
            }

            // Manage radar and scanning for "Hunt the Heroes"
            LaunchedEffect(gameState.expandedImage) {
                if (gameState.expandedImage == R.drawable.hunt_the_heroes) {
                    bluetoothViewModel.setGameMode(BluetoothScanViewModel.GameMode.HUNT_THE_HEROES)
                    delay(500) // Brief delay before showing radar
                    showRadar = true
                    bluetoothViewModel.startScan(activity)
                } else {
                    showRadar = false
                    showCharacterReveal = false
                    bluetoothViewModel.setGameMode(BluetoothScanViewModel.GameMode.NONE)
                }
            }

            // Update detected characters based on nearby heroes
            LaunchedEffect(nearbyHeroes) {
                if (nearbyHeroes.isNotEmpty() && gameState.expandedImage == R.drawable.hunt_the_heroes) {
                    println("=== HUNT THE HEROES DEBUG ===")
                    nearbyHeroes.forEach { device ->
                        val rssiValue = device.rssi.toInt()
                        println("Device: ${device.name}, RSSI: $rssiValue, Will Blink: ${rssiValue >= -50}")
                    }
                    // Filter heroes with strong signal (RSSI >= -50)
                    detectedCharacters = nearbyHeroes.filter { it.rssi.toInt() >= -50 }.map { it.name }
                    println("Characters that will blink: $detectedCharacters")
                    if (detectedCharacters.isNotEmpty()) {
                        currentGuessHero = detectedCharacters.random() // Select a random hero to guess
                    }
                } else {
                    showCharacterReveal = false
                    detectedCharacters = emptyList()
                    currentGuessHero = null
                }
            }

            // Map of hero names to their image resources
            val heroImages = remember {
                mapOf(
                    "Iron_Man" to R.drawable.iron_man,
                    "Hulk" to R.drawable.hulk_,
                    "Captain Marvel" to R.drawable.captain_marvel,
                    "Captain America" to R.drawable.captain_america,
                    "Scarlet Witch" to R.drawable.scarlet_witch,
                    "Black Widow" to R.drawable.black_widow,
                    "Wasp" to R.drawable.wasp,
                    "Hela" to R.drawable.hela,
                    "Thor" to R.drawable.thor,
                    "Spider Man" to R.drawable.spider_man,
                    "Default" to R.drawable.search
                )
            }

            // Generate positions for all heroes on the radar
            val allHeroesDeviceList = remember {
                val positions = generateMultiplePositions(allowedHeroes.size, 600f)
                allowedHeroes.mapIndexed { index, heroName ->
                    val imageRes = heroImages[heroName] ?: R.drawable.search
                    Pair(imageRes, positions.getOrElse(index) { Offset.Zero })
                }
            }

            // Display radar with detected characters
            if (showRadar) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(7f)
                        .offset(y = 50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RadarScreenWithAllCharacters(
                        activatedDevices = detectedCharacters,
                        deviceList = allHeroesDeviceList,
                        rssiValues = rssiValues
                    )
                }
            }

            // Display detected heroes at the bottom
            if (detectedCharacters.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp)
                        .zIndex(8f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = translatedText.heroesDetected,
                            color = Color.White,
                            fontFamily = helveticaFont,
                            style = MaterialTheme.typography.body1,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        detectedCharacters.forEach { heroName ->
                            Text(
                                text = "${heroName.replace("_", " ")} (RSSI: ${rssiValues[heroName]} dBm)",
                                color = Color.White,
                                fontFamily = helveticaFont,
                                style = MaterialTheme.typography.body2,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // "Guess the Character" game mode
        if (gameState.expandedImage == R.drawable.guess_the_character) {
            // Find a nearby hero with strong signal
            val nearbyHeroDevice = bluetoothDevices.find {
                it.name in allowedHeroes && it.rssi.toInt() >= -50
            }

            // Update game state based on nearby hero detection
            LaunchedEffect(nearbyHeroDevice) {
                if (nearbyHeroDevice != null) {
                    gameState = gameState.copy(
                        showTTHButton = true,
                        showSearchImage = false,
                        detectedHeroRssi = nearbyHeroDevice.rssi.toInt(),
                        currentHeroToGuess = nearbyHeroDevice.name
                    )
                } else {
                    gameState = gameState.copy(
                        showTTHButton = false,
                        showSearchImage = true,
                        detectedHeroRssi = null,
                        currentHeroToGuess = null
                    )
                }
            }

            // Display animated search button
            if (gameState.showSearchImage) {
                val searchOffset = remember(searchX, searchY) {
                    IntOffset(
                        (searchX + gameState.screenWidth / 2 - with(density) { 60.dp.toPx() }).toInt(),
                        (searchY + gameState.screenHeight / 2 - with(density) { 60.dp.toPx() }).toInt()
                    )
                }
                Box(
                    modifier = Modifier
                        .offset { searchOffset }
                        .zIndex(2f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = "Search Button",
                        modifier = Modifier.size(50.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Display detected hero information
            if (nearbyHeroDevice != null && gameState.showTTHButton) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-100).dp)
                        .zIndex(3f)
                        .alpha(blinkAlpha),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Detected: ${nearbyHeroDevice.name.replace("_", " ")}",
                            style = MaterialTheme.typography.h6,
                            color = textColor,
                            fontFamily = helveticaFont
                        )
                        Text(
                            text = "RSSI: ${nearbyHeroDevice.rssi} dBm",
                            style = MaterialTheme.typography.body2,
                            color = secondaryTextColor,
                            fontFamily = helveticaFont
                        )
                    }
                }
            }

            // Display "Tap to Hunt" button
            if (gameState.showTTHButton) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(5f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ttr),
                        contentDescription = "Tap to Hunt Button",
                        modifier = Modifier
                            .size(80.dp)
                            .clickable {
                                gameState = gameState.copy(showHeroSelectionDialog = true)
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Map of hero names to hints for the guessing game
            val heroHints = remember {
                mapOf(
                    "Scarlet Witch" to "Manipulates reality with chaos magic",
                    "Black Widow" to "Master spy with expert combat skills",
                    "Captain Marvel" to "Flies with cosmic energy powers",
                    "Wasp" to "Shrinks and flies with insect-like wings",
                    "Hela" to "Goddess of Death with necromantic powers",
                    "Hulk" to "Transforms into a green giant with immense strength",
                    "Thor" to "Wields a powerful hammer",
                    "Iron_Man" to "Genius inventor in a high-tech suit",
                    "Spider Man" to "Swings with webs and has spider-like agility",
                    "Captain America" to "Carries an indestructible shield"
                )
            }

            // Generate hero options for the guessing dialog
            val heroOptions = remember(gameState.currentHeroToGuess) {
                if (gameState.currentHeroToGuess != null) {
                    val otherHeroes = allowedHeroes.filter { it != gameState.currentHeroToGuess }.shuffled()
                    val selectedHeroes = listOf(gameState.currentHeroToGuess) + otherHeroes.take(2)
                    selectedHeroes.shuffled()
                } else {
                    emptyList()
                }
            }

            // Hero selection dialog for guessing
            if (gameState.showHeroSelectionDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                        .zIndex(10f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(dialogBackgroundColor, shape = RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = translatedText.whichHeroNearby,
                            fontFamily = helveticaFont,
                            style = MaterialTheme.typography.h6,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = gameState.currentHeroToGuess?.let { heroHints[it] } ?: "No hero detected",
                            fontFamily = helveticaFont,
                            style = MaterialTheme.typography.body2,
                            color = secondaryTextColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            heroOptions.forEach { character ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        // Handle hero selection
                                        val isCorrect = character == gameState.currentHeroToGuess
                                        if (isCorrect) {
                                            gameState = gameState.copy(lastCorrectHero = character)
                                            val updatedCharacters = foundCharacters.toMutableMap()
                                            updatedCharacters[character as String] = (updatedCharacters[character] ?: 0) + 1
                                            foundCharacters = updatedCharacters
                                            gameState = gameState.copy(showGuessResult = true, guessAnimationRunning = true)
                                            playSound(R.raw.yayy) // Play success sound
                                            gameState = gameState.copy(showHeroSelectionDialog = false)
                                        } else {
                                            gameState = gameState.copy(showGuessResult = false, guessAnimationRunning = true)
                                            playSound(R.raw.wrong) // Play failure sound
                                        }
                                    }
                                ) {
                                    val resourceId = when (character) {
                                        "Iron_Man" -> R.drawable.iron_man
                                        "Hulk" -> R.drawable.hulk_
                                        "Captain Marvel" -> R.drawable.captain_marvel
                                        "Captain America" -> R.drawable.captain_america
                                        "Scarlet Witch" -> R.drawable.scarlet_witch
                                        "Black Widow" -> R.drawable.black_widow
                                        "Wasp" -> R.drawable.wasp
                                        "Hela" -> R.drawable.hela
                                        "Thor" -> R.drawable.thor
                                        "Spider Man" -> R.drawable.spider_man
                                        else -> R.drawable.search
                                    }
                                    Image(
                                        painter = painterResource(id = resourceId),
                                        contentDescription = character,
                                        modifier = Modifier.size(80.dp)
                                    )
                                    if (character != null) {
                                        Text(
                                            text = character.replace("_", " "),
                                            style = MaterialTheme.typography.caption,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Display guess result animation
            LaunchedEffect(gameState.showGuessResult, guessResultDismissKey.value) {
                if (gameState.showGuessResult != null) {
                    delay(600) // Show result for 600ms
                    gameState = gameState.copy(guessAnimationRunning = false)
                    if (gameState.showGuessResult == true) {
                        gameState = gameState.copy(
                            scratchCardHeroName = gameState.lastCorrectHero,
                            showScratchCard = true
                        )
                    }
                    gameState = gameState.copy(
                        showGuessResult = null,
                        lastCorrectHero = null
                    )
                }
            }

            // Show guess result dialog
            if (gameState.guessAnimationRunning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                        .zIndex(10f),
                    contentAlignment = Alignment.Center
                ) {
                    if (gameState.showGuessResult == true) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = translatedText.correctGuess,
                                style = MaterialTheme.typography.h4,
                                fontFamily = helveticaFont,
                                color = Color.Green
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${translatedText.youFound} ${gameState.lastCorrectHero?.replace("_", " ")}!",
                                style = MaterialTheme.typography.h6,
                                fontFamily = helveticaFont,
                                color = textColor
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = translatedText.wrongGuess,
                                style = MaterialTheme.typography.h4,
                                fontFamily = helveticaFont,
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = translatedText.tryAgain,
                                style = MaterialTheme.typography.h6,
                                fontFamily = helveticaFont,
                                color = textColor
                            )
                        }
                    }
                }
            }

            // Show scratch card for correct guesses
            if (gameState.showScratchCard && gameState.scratchCardHeroName != null) {
                ScratchCardScreen(
                    heroName = gameState.scratchCardHeroName!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .zIndex(15f)
                        .clickable {
                            gameState = gameState.copy(showScratchCard = false)
                            bluetoothViewModel.startScan(activity)
                        },
                    onScratchCompleted = {}
                )
            }

            // Map of hero names to image resources (repeated for clarity)
            val heroImages = remember {
                mapOf(
                    "Iron_Man" to R.drawable.iron_man,
                    "Hulk" to R.drawable.hulk_,
                    "Captain Marvel" to R.drawable.captain_marvel,
                    "Captain America" to R.drawable.captain_america,
                    "Scarlet Witch" to R.drawable.scarlet_witch,
                    "Black Widow" to R.drawable.black_widow,
                    "Wasp" to R.drawable.wasp,
                    "Hela" to R.drawable.hela,
                    "Thor" to R.drawable.thor,
                    "Spider Man" to R.drawable.spider_man,
                    "Default" to R.drawable.search
                )
            }
        }

        // Bottom bar with game box and sound toggle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
                .zIndex(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Game box showing collected heroes count
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { isGameBoxOpen = true })
                        }
                        .animateContentSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.close_box),
                        contentDescription = "Game Box",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "${foundCharacters.size}/${allowedHeroes.size}",
                        style = MaterialTheme.typography.body2,
                        color = textColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(overlayColor, shape = RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    )
                }
                // Sound toggle button
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { gameState = gameState.copy(isSoundOn = !gameState.isSoundOn) })
                        }
                        .background(overlayColor, RoundedCornerShape(30.dp))
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = if (gameState.isSoundOn) R.drawable.soundon else R.drawable.soundoff),
                        contentDescription = if (gameState.isSoundOn) "Sound On" else "Sound Off",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Heroes collection dialog
        AnimatedVisibility(
            visible = isGameBoxOpen,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.zIndex(20f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor.copy(alpha = 0.9f))
                    .clickable { isGameBoxOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(dialogBackgroundColor, shape = RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .fillMaxWidth(0.9f)
                ) {
                    Text(
                        text = translatedText.heroesCollection,
                        fontFamily = helveticaFont,
                        style = MaterialTheme.typography.h6,
                        color = textColor
                    )
                    Text(
                        text = "${translatedText.collected} ${foundCharacters.size}/${allowedHeroes.size}",
                        fontFamily = helveticaFont,
                        style = MaterialTheme.typography.body1,
                        color = secondaryTextColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(allowedHeroes) { character ->
                            val isCollected = character in foundCharacters
                            val count = foundCharacters[character] ?: 0
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val resourceId = when (character) {
                                    "Iron_Man" -> R.drawable.iron_man
                                    "Hulk" -> R.drawable.hulk_
                                    "Captain Marvel" -> R.drawable.captain_marvel
                                    "Captain America" -> R.drawable.captain_america
                                    "Scarlet Witch" -> R.drawable.scarlet_witch
                                    "Black Widow" -> R.drawable.black_widow
                                    "Wasp" -> R.drawable.wasp
                                    "Hela" -> R.drawable.hela
                                    "Thor" -> R.drawable.thor
                                    "Spider Man" -> R.drawable.spider_man
                                    else -> R.drawable.search
                                }
                                if (isCollected) {
                                    Image(
                                        painter = painterResource(id = resourceId),
                                        contentDescription = character,
                                        modifier = Modifier.size(80.dp)
                                    )
                                } else {
                                    Box(modifier = Modifier.size(80.dp)) {
                                        Image(
                                            painter = painterResource(id = resourceId),
                                            contentDescription = character,
                                            colorFilter = ColorFilter.tint(
                                                Color.Gray.copy(alpha = 0.5f),
                                                blendMode = BlendMode.SrcAtop
                                            ),
                                            modifier = Modifier
                                                .size(80.dp)
                                                .alpha(0.7f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(overlayColor)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isCollected) "×$count" else "?",
                                    style = MaterialTheme.typography.body2,
                                    color = if (isCollected) textColor else secondaryTextColor
                                )
                                Text(
                                    text = character.replace("_", " "),
                                    style = MaterialTheme.typography.caption,
                                    color = if (isCollected) textColor else secondaryTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Data class to hold translated text for the game screen
data class TranslatedGameScreenText(
    val huntTheHeroes: String,
    val guessTheCharacter: String,
    val whichHeroNearby: String,
    val correctGuess: String,
    val youFound: String,
    val wrongGuess: String,
    val tryAgain: String,
    val heroesDetected: String,
    val heroesCollection: String,
    val collected: String
)
package com.example.ble_jetpackcompose

// Import necessary libraries for Compose animations, UI components, media playback, and lifecycle management
import android.app.Activity
import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Utility function to convert Dp to pixels
fun dpToPx(dp: Dp): Float {
    return dp.value
}

// Main composable function for the game screen
@Composable
fun GameActivityScreen(
    activity: Activity, // Activity context for Bluetooth operations
    bluetoothViewModel: BluetoothScanViewModel<Any> = viewModel(
        factory = BluetoothScanViewModelFactory(LocalContext.current.applicationContext as Application)
    ) // ViewModel for Bluetooth scanning
) {
    // Observe theme and language state from managers
    val isDarkMode by ThemeManager.isDarkMode.collectAsState()
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()

    // State to hold translated text, initialized with cached or default values
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

    // Preload translations when language changes
    LaunchedEffect(currentLanguage) {
        val translator = GoogleTranslationService()
        // List of texts to translate
        val textsToTranslate = listOf(
            "Hunt the Heroes", "Guess the Character", "Which hero do you think is nearby?",
            "Correct!", "You found", "Wrong Guess!", "Try again...", "Heroes Detected:",
            "Heroes Collection", "Collected:"
        )
        // Translate texts to the current language
        val translatedList = translator.translateBatch(textsToTranslate, currentLanguage)
        // Update translated text state
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

    // Define theme-based colors
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color.White // Background color
    val textColor = if (isDarkMode) Color.White else Color.Black // Primary text color
    val secondaryTextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF8E8E93) // Secondary text color
    val buttonBackgroundColor = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF007AFF) // Button background
    val dialogBackgroundColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White // Dialog background
    val overlayColor =
        if (isDarkMode) Color.Black.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f) // Overlay color

    // State variables for UI interactions
    var expandedImage by remember { mutableStateOf<Int?>(null) } // Tracks expanded game mode image
    var screenWidth by remember { mutableFloatStateOf(0f) } // Screen width in pixels
    var screenHeight by remember { mutableFloatStateOf(0f) } // Screen height in pixels
    var isSoundOn by remember { mutableStateOf(true) } // Sound toggle state
    var showScratchCard by remember { mutableStateOf(false) } // Show scratch card UI
    var showTTHButton by remember { mutableStateOf(false) } // Show "Hunt the Heroes" button
    var showSearchImage by remember { mutableStateOf(true) } // Show search animation
    var showPopup by remember { mutableStateOf(false) } // Show popup dialog
    var scratchCompleted by remember { mutableStateOf(false) } // Scratch card completion state
    var showHeroSelectionDialog by remember { mutableStateOf(false) } // Show hero selection dialog
    var currentHeroToGuess by remember { mutableStateOf<String?>(null) } // Current hero to guess
    var showGuessResult by remember { mutableStateOf<Boolean?>(null) } // Guess result (correct/incorrect)
    var guessAnimationRunning by remember { mutableStateOf(false) } // Guess animation state

    // List of allowed hero names
    val allowedHeroes = listOf(
        "Scarlet Witch", "Black Widow", "Captain Marvel", "Wasp", "Hela",
        "Hulk", "Thor", "Iron_Man", "Spider Man", "Captain America"
    )
    // Observe Bluetooth devices from ViewModel
    val bluetoothDevices by bluetoothViewModel.devices.collectAsState(initial = emptyList())
    // Track found characters and their counts
    var foundCharacters by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // Get current context and lifecycle
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Initialize background music player
    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.bgmusic).apply {
            isLooping = true // Loop background music
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build()
            )
            setVolume(0.5f, 0.5f) // Set volume to 50%
        }
    }
    // Initialize sound player for correct guesses
    val correctSoundPlayer = remember {
        MediaPlayer.create(context, R.raw.yayy).apply {
            isLooping = false // No looping for sound effect
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build()
            )
            setVolume(1.0f, 1.0f) // Full volume
        }
    }
    // Initialize sound player for wrong guesses
    val wrongSoundPlayer = remember {
        MediaPlayer.create(context, R.raw.wrong).apply {
            isLooping = false // No looping for sound effect
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build()
            )
            setVolume(1.0f, 1.0f) // Full volume
        }
    }

    // Manage media players based on lifecycle events
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                // Pause all media players when the app is paused
                if (mediaPlayer.isPlaying) mediaPlayer.pause()
                if (correctSoundPlayer.isPlaying) correctSoundPlayer.pause()
                if (wrongSoundPlayer.isPlaying) wrongSoundPlayer.pause()
            } else if (event == Lifecycle.Event.ON_RESUME) {
                // Resume background music if sound is on
                if (isSoundOn && !mediaPlayer.isPlaying) mediaPlayer.start()
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                // Release resources and stop Bluetooth scan on destroy
                mediaPlayer.release()
                correctSoundPlayer.release()
                wrongSoundPlayer.release()
                bluetoothViewModel.stopScan()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            // Clean up resources and remove observer
            lifecycle.removeObserver(observer)
            bluetoothViewModel.stopScan()
            mediaPlayer.release()
            correctSoundPlayer.release()
            wrongSoundPlayer.release()
        }
    }

    // Control background music based on sound toggle
    LaunchedEffect(isSoundOn) {
        if (isSoundOn) {
            if (!mediaPlayer.isPlaying) mediaPlayer.start()
        } else {
            if (mediaPlayer.isPlaying) mediaPlayer.pause()
        }
    }

    // Animation for search button rotation
    val infiniteTransition = rememberInfiniteTransition(label = "searchButtonAnimation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "searchButtonRotation"
    )

    // Calculate search button position for circular motion
    val radius = 100f
    val searchX =
        remember { derivedStateOf { radius * cos(Math.toRadians(angle.toDouble())).toFloat() } }
    val searchY =
        remember { derivedStateOf { radius * sin(Math.toRadians(angle.toDouble())).toFloat() } }

    // State for hero animation
    val animateHero by remember { mutableStateOf<String?>(null) }
    val transitionProgress by animateFloatAsState(
        targetValue = if (animateHero != null) 2f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "heroTransition"
    )
    // State for game box visibility
    var isGameBoxOpen by remember { mutableStateOf(false) }
    // Fixed position for game box
    val gameBoxPosition = remember { Offset(40f, screenHeight - 100f) }
    // Track hero animations
    val heroAnimationState = remember { mutableStateListOf<Pair<String, Offset>>() }

    // Particle effects for scratch card completion
    val particles = remember { mutableStateListOf<Triple<Float, Float, Double>>() }
    val centerX = screenWidth / 2
    val centerY = screenHeight / 2

    // Generate particles when scratch card is completed
    LaunchedEffect(scratchCompleted) {
        if (scratchCompleted && screenWidth > 0 && screenHeight > 0) {
            repeat(35) {
                val angle = Random.nextDouble(0.0, 360.0)
                val distance = Random.nextFloat() * 50
                val startX = (centerX + cos(Math.toRadians(angle)) * distance).toFloat()
                val startY = (centerY + sin(Math.toRadians(angle)) * distance).toFloat()
                particles.add(Triple(startX, startY, angle))
            }
        }
    }
    // Animate particles
    LaunchedEffect(particles.isNotEmpty()) {
        if (particles.isNotEmpty()) {
            val particlesToUpdate = particles.size.coerceAtMost(10)
            repeat(5) {
                particles.take(particlesToUpdate).forEachIndexed { index, (x, y, angle) ->
                    val speedX = (cos(Math.toRadians(angle)) * 5).toFloat()
                    val speedY = (sin(Math.toRadians(angle)) * 5).toFloat()
                    if (index < particles.size) {
                        particles[index] = Triple(x + speedX, y + speedY, angle)
                    }
                }
                delay(50)
            }
            particles.clear()
        }
    }

    // Draw particles on canvas
    if (particles.isNotEmpty()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { (x, y, _) ->
                drawCircle(color = Color.Red, radius = 5f, center = Offset(x, y))
            }
        }
    }

    // Main screen layout
    Box(
        modifier = Modifier
            .fillMaxSize() // Fill the entire screen
            .background(backgroundColor) // Apply theme-based background
            .padding(WindowInsets.systemBars.asPaddingValues()) // Respect system bars
            .onGloballyPositioned { coordinates ->
                // Update screen dimensions
                screenWidth = coordinates.size.width.toFloat()
                screenHeight = coordinates.size.height.toFloat()
            }
    ) {
        // Background image
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.1.dp)
                .zIndex(1f), // Ensure content is above background
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

            // Game mode selection rows
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // "Hunt the Heroes" button row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .offset(y = (-30).dp)
                        .zIndex(if (expandedImage == R.drawable.hunt_the_heroes) 2f else 1f), // Higher z-index when expanded
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    var buttonPosition by remember { mutableStateOf(IntOffset.Zero) }
                    // Animated button for "Hunt the Heroes" mode
                    AnimatedImageButton(
                        imageResId = R.drawable.guess_the_character,
                        contentDescription = translatedText.huntTheHeroes,
                        isExpanded = expandedImage == R.drawable.hunt_the_heroes,
                        expandedImageResId = R.drawable.gth,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        currentPosition = buttonPosition,
                        onPositioned = { buttonPosition = it },
                        onClick = {
                            // Toggle expansion and UI states
                            expandedImage =
                                if (expandedImage == R.drawable.hunt_the_heroes) null else R.drawable.hunt_the_heroes
                            showPopup = !showPopup
                            showSearchImage = true
                            showTTHButton = false
                            showScratchCard = false
                        }
                    )
                }

                // "Guess the Character" button row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp)
                        .offset(y = (-30).dp)
                        .zIndex(if (expandedImage == R.drawable.guess_the_character) 2f else 1f), // Higher z-index when expanded
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    var buttonPosition by remember { mutableStateOf(IntOffset.Zero) }
                    // Animated button for "Guess the Character" mode
                    AnimatedImageButton(
                        imageResId = R.drawable.hunt_the_heroes,
                        contentDescription = translatedText.guessTheCharacter,
                        isExpanded = expandedImage == R.drawable.guess_the_character,
                        expandedImageResId = R.drawable.hth,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        currentPosition = buttonPosition,
                        onPositioned = { buttonPosition = it },
                        onClick = {
                            // Toggle expansion and UI states
                            expandedImage =
                                if (expandedImage == R.drawable.guess_the_character) null else R.drawable.guess_the_character
                            showPopup = !showPopup
                            showSearchImage = true
                            showTTHButton = false
                            showScratchCard = false
                        }
                    )
                }
            }
        }

        // Manage Bluetooth scanning based on game mode
        val isScanningActive = remember { mutableStateOf(false) }
        val scanInterval = 3000L

        LaunchedEffect(expandedImage) {
            if (expandedImage == R.drawable.hunt_the_heroes || expandedImage == R.drawable.guess_the_character) {
                // Set game mode in ViewModel
                bluetoothViewModel.setGameMode(
                    when (expandedImage) {
                        R.drawable.hunt_the_heroes -> BluetoothScanViewModel.GameMode.HUNT_THE_HEROES
                        else -> BluetoothScanViewModel.GameMode.GUESS_THE_CHARACTER
                    }
                )
                // Periodic scanning loop
                while (true) {
                    if (!isScanningActive.value) {
                        isScanningActive.value = true
                        bluetoothViewModel.startScan(activity)
                        delay(scanInterval)
                        bluetoothViewModel.stopScan()
                        isScanningActive.value = false
                    }
                    delay(500)
                }
            } else {
                // Reset game mode and stop scanning
                bluetoothViewModel.setGameMode(BluetoothScanViewModel.GameMode.NONE)
                bluetoothViewModel.stopScan()
            }
        }

        // "Hunt the Heroes" game mode logic
        if (expandedImage == R.drawable.hunt_the_heroes) {
            // Find nearby hero device with strong signal and not yet found
            val nearbyHeroDevice = bluetoothDevices.find {
                it.name in allowedHeroes && it.rssi.toInt() in -40..0 && it.name !in foundCharacters.keys
            }

            // Update UI based on nearby hero detection
            LaunchedEffect(nearbyHeroDevice) {
                if (nearbyHeroDevice != null) {
                    showTTHButton = true
                    showSearchImage = false
                } else {
                    showTTHButton = false
                    showSearchImage = true
                }
            }

            // Handle scratch card completion
            LaunchedEffect(scratchCompleted) {
                if (scratchCompleted && nearbyHeroDevice != null) {
                    val heroName = nearbyHeroDevice.name
                    // Add hero to animation state
                    heroAnimationState.add(Pair(heroName, Offset(centerX, centerY)))
                    delay(500)
                    heroAnimationState.clear()
                    // Update found characters
                    val updatedCharacters = foundCharacters.toMutableMap()
                    updatedCharacters[heroName] = (updatedCharacters[heroName] ?: 0) + 1
                    foundCharacters = updatedCharacters
                    // Reset UI states
                    scratchCompleted = false
                    showScratchCard = false
                    showTTHButton = false
                    showSearchImage = true
                    bluetoothViewModel.startScan(activity)
                }
            }

            // Display search animation
            if (showSearchImage) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (searchX.value + screenWidth / 2 - 60.dp.toPx()).toInt(),
                                (searchY.value + screenHeight / 2 - 60.dp.toPx()).toInt()
                            )
                        }
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

            // Display "Hunt the Heroes" button
            if (showTTHButton && !showScratchCard) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(5f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ttr),
                        contentDescription = "TTR Button",
                        modifier = Modifier
                            .size(80.dp)
                            .clickable {
                                showHeroSelectionDialog = true
                                currentHeroToGuess = nearbyHeroDevice?.name
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Hero selection dialog
            if (showHeroSelectionDialog) {
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scrollable row of hero options
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            allowedHeroes.forEach { character ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        // Handle hero selection
                                        val isCorrect = character == currentHeroToGuess
                                        showGuessResult = isCorrect
                                        guessAnimationRunning = true
                                        showHeroSelectionDialog = false
                                        if (isCorrect) correctSoundPlayer.start() else wrongSoundPlayer.start()
                                    }
                                ) {
                                    // Map hero names to resource IDs
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

            // Guess result dialog
            if (guessAnimationRunning) {
                LaunchedEffect(guessAnimationRunning) {
                    delay(3000)
                    guessAnimationRunning = false
                    if (showGuessResult == true) showScratchCard = true else showHeroSelectionDialog = true
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                        .zIndex(10f),
                    contentAlignment = Alignment.Center
                ) {
                    if (showGuessResult == true) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = translatedText.correctGuess,
                                style = MaterialTheme.typography.h4,
                                fontFamily = helveticaFont,
                                color = Color.Green
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${translatedText.youFound} ${
                                    currentHeroToGuess?.replace(
                                        "_",
                                        " "
                                    )
                                }!",
                                style = MaterialTheme.typography.h6,
                                fontFamily = helveticaFont,
                                color = textColor
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = translatedText.wrongGuess,
                                fontFamily = helveticaFont,
                                style = MaterialTheme.typography.h4,
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = translatedText.tryAgain,
                                fontFamily = helveticaFont,
                                style = MaterialTheme.typography.h6,
                                color = textColor
                            )
                        }
                    }
                }
            }

            // Scratch card UI
            if (expandedImage == R.drawable.hunt_the_heroes && showScratchCard && nearbyHeroDevice != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(6f),
                    contentAlignment = Alignment.Center
                ) {
                    ScratchCardScreen(
                        heroName = nearbyHeroDevice.name,
                        modifier = Modifier.offset(y = (-50).dp),
                        onScratchCompleted = { scratchCompleted = true }
                    )
                }
            }

            // Hero reveal animation
            if (scratchCompleted) {
                val offsetY by animateFloatAsState(
                    targetValue = if (scratchCompleted) screenHeight / 2 else 0f,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
                    label = "heroOffsetY"
                )

                Box(
                    modifier = Modifier
                        .offset(y = offsetY.dp)
                        .zIndex(7f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            id = when (nearbyHeroDevice?.name) {
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
                        ),
                        contentDescription = "Revealed Hero",
                        modifier = Modifier
                            .size(150.dp)
                            .graphicsLayer {
                                scaleX = transitionProgress
                                scaleY = transitionProgress
                                alpha = transitionProgress
                            }
                    )
                }

                // Draw particles for reveal animation
                Canvas(modifier = Modifier.fillMaxSize()) {
                    particles.forEach { (x, y) ->
                        drawCircle(
                            color = Color(
                                Random.nextInt(256),
                                Random.nextInt(256),
                                Random.nextInt(256)
                            ),
                            radius = 5f,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // Hero image resources
            val heroImages = produceState<Map<String, Int>>(initialValue = emptyMap()) {
                value = mapOf(
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
            }.value

            // Animated hero movement
            Box(modifier = Modifier.fillMaxSize()) {
                heroAnimationState.forEach { (heroName, _) ->
                    val animatedPosition by animateOffsetAsState(
                        targetValue = gameBoxPosition,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "heroAnimation"
                    )
                    Image(
                        painter = painterResource(
                            id = heroImages[heroName] ?: heroImages["Default"]!!
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    animatedPosition.x.roundToInt(),
                                    animatedPosition.y.roundToInt()
                                )
                            }
                            .size(50.dp),
                        alpha = 1f
                    )
                }
            }
        }

        // "Guess the Character" game mode logic
        if (expandedImage == R.drawable.guess_the_character) {
            var showRadar by remember { mutableStateOf(false) } // Show radar UI
            var showCharacterReveal by remember { mutableStateOf(false) } // Show character reveal
            var detectedCharacters by remember { mutableStateOf<List<String>>(emptyList()) } // Detected heroes

            // Filter nearby heroes
            val nearbyHeroes = bluetoothDevices.filter { it.name in allowedHeroes }
            // Map RSSI values for heroes
            val rssiValues = remember(bluetoothDevices) {
                allowedHeroes.associateWith { heroName ->
                    nearbyHeroes.find { it.name == heroName }?.rssi?.toInt()
                }
            }

            // Initialize radar and scanning
            LaunchedEffect(expandedImage) {
                if (expandedImage == R.drawable.guess_the_character) {
                    bluetoothViewModel.setGameMode(BluetoothScanViewModel.GameMode.GUESS_THE_CHARACTER)
                    delay(100)
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
                if (nearbyHeroes.isNotEmpty() && expandedImage == R.drawable.guess_the_character) {
                    delay(1000)
                    detectedCharacters = nearbyHeroes.filter { it.rssi.toInt() in -40..0 }.map { it.name }
                    showCharacterReveal = true
                } else {
                    showCharacterReveal = false
                    detectedCharacters = emptyList()
                }
            }

            // Hero image resources
            val heroImages = produceState<Map<String, Int>>(initialValue = emptyMap()) {
                value = mapOf(
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
            }.value

            // Generate positions for all heroes on radar
            val allHeroesDeviceList = remember {
                val positions = generateMultiplePositions(allowedHeroes.size, 600f)
                allowedHeroes.mapIndexed { index, heroName ->
                    val imageRes = heroImages[heroName] ?: R.drawable.search
                    Pair(imageRes, positions.getOrElse(index) { Offset.Zero })
                }
            }

            // Display radar UI
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
                        rssiValues = rssiValues // Pass full RSSI map
                    )
                }
            }

            // Display detected heroes
            if (detectedCharacters.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 16.dp)
                        .zIndex(8f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(overlayColor, RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = translatedText.heroesDetected,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        detectedCharacters.forEach { heroName ->
                            Text(
                                text = heroName,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }

        // Bottom bar with game box and sound toggle
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
                .zIndex(1f), // Lower z-index for the button row to stay below dialog
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Game box button
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
                            detectTapGestures(onTap = { isSoundOn = !isSoundOn })
                        }
                        .background(overlayColor, RoundedCornerShape(30.dp))
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(id = if (isSoundOn) R.drawable.soundon else R.drawable.soundoff),
                        contentDescription = if (isSoundOn) "Sound On" else "Sound Off",
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
            modifier = Modifier.zIndex(20f) // Higher z-index to ensure it’s on top
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayColor.copy(alpha = 0.9f)) // Slightly more opaque for clarity
                    .clickable { isGameBoxOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(dialogBackgroundColor, shape = RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .fillMaxWidth(0.9f) // Slightly narrower for better layout
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

                    // Scrollable row of heroes
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp)
                    ) {
                        allowedHeroes.forEach { character ->
                            val isCollected = character in foundCharacters.keys
                            val count = foundCharacters[character] ?: 0

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Map hero names to resource IDs
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

                                // Display collected or uncollected hero
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

                                // Display count or placeholder
                                Text(
                                    text = if (isCollected) "×$count" else "?",
                                    style = MaterialTheme.typography.body2,
                                    color = if (isCollected) textColor else secondaryTextColor
                                )

                                // Hero name
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

// Data class for translatable text
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
package com.blesense.app

// Import necessary Compose libraries for animations, UI components, and state management
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Composable for an animated image button with expansion effects
@Composable
fun AnimatedImageButton(
    imageResId: Int, // Resource ID for the default button image
    contentDescription: String, // Accessibility description
    isExpanded: Boolean, // Whether the button is expanded
    expandedImageResId: Int, // Resource ID for the expanded image
    screenWidth: Float, // Screen width for positioning
    screenHeight: Float, // Screen height for positioning
    currentPosition: IntOffset, // Current position of the button
    onPositioned: (IntOffset) -> Unit, // Callback for position updates
    onClick: () -> Unit // Click handler
) {
    // Create a transition for the expansion state
    val transition = updateTransition(targetState = isExpanded, label = "buttonExpansionTransition")

    // Animate scale using transition
    val scale = transition.animateFloat(
        label = "buttonScaleAnimation",
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy, // Bouncy animation effect
                stiffness = Spring.StiffnessLow // Low stiffness for smooth motion
            )
        }
    ) { expanded ->
        if (expanded) 2f else 1f // Scale up to 2x when expanded
    }

    // Animate alpha using transition
    val alpha = transition.animateFloat(
        label = "buttonAlphaAnimation",
        transitionSpec = { tween(300) } // 300ms animation
    ) { expanded ->
        if (expanded) 1f else 0.9f // Slightly transparent when not expanded
    }

    // Animate the X offset to center the button when expanded
    val offsetX = transition.animateFloat(
        label = "buttonOffsetXAnimation",
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) } // Low stiffness for smooth motion
    ) { expanded ->
        if (expanded) {
            val currentX = currentPosition.x.toFloat()
            val buttonWidthPx = dpToPx(200.dp) // Width of the button in pixels
            val targetX = (screenWidth - buttonWidthPx) / 3.5f // Center horizontally
            targetX - currentX // Calculate offset
        } else 0f
    }

    // Animate the Y offset to center the button when expanded
    val offsetY = transition.animateFloat(
        label = "buttonOffsetYAnimation",
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) } // Low stiffness for smooth motion
    ) { expanded ->
        if (expanded) {
            val currentY = currentPosition.y.toFloat()
            val buttonHeightPx = dpToPx(282.dp) // Height of the button in pixels
            val targetY = (screenHeight - buttonHeightPx) / 2.3f // Center vertically
            targetY - currentY // Calculate offset
        } else 0f
    }

    // Container for the button
    Box {
        Image(
            painter = painterResource(id = if (isExpanded) expandedImageResId else imageResId), // Switch image based on expansion
            contentDescription = contentDescription,
            modifier = Modifier
                .width(200.dp) // Fixed width
                .height(280.dp) // Fixed height
                .onGloballyPositioned { coordinates ->
                    // Update position when not expanded
                    if (!isExpanded) {
                        onPositioned(
                            IntOffset(
                                coordinates.boundsInWindow().left.roundToInt(),
                                coordinates.boundsInWindow().top.roundToInt()
                            )
                        )
                    }
                }
                .graphicsLayer {
                    scaleX = scale.value // Apply animated scale
                    scaleY = scale.value
                    this.alpha = alpha.value // Apply animated alpha
                    translationX = offsetX.value // Apply animated X offset
                    translationY = offsetY.value // Apply animated Y offset
                }
                .clickable(
                    onClick = onClick,
                    indication = null, // Disable ripple effect
                    interactionSource = remember { MutableInteractionSource() } // Remove interaction feedback
                ),
            contentScale = ContentScale.Crop // Crop image to fit
        )
    }
}

// Composable for radar screen displaying all characters
@Composable
fun RadarScreenWithAllCharacters(
    modifier: Modifier = Modifier,
    deviceList: List<Pair<Int, Offset>> = emptyList(), // List of device image IDs and positions
    activatedDevices: List<String> = emptyList(), // List of activated device names
    rssiValues: Map<String, Int?> = emptyMap() // RSSI values for devices
) {
    // Define radar colors
    val radarColor = colorResource(id = R.color.radar_color)
    val centerCircleColor = colorResource(id = R.color.radar_color)

    // List of character image resources and names
    val characters = listOf(
        R.drawable.iron_man to "Iron_Man",
        R.drawable.hulk_ to "Hulk",
        R.drawable.captain_marvel to "Captain Marvel",
        R.drawable.captain_america to "Captain America",
        R.drawable.scarlet_witch to "Scarlet Witch",
        R.drawable.black_widow to "Black Widow",
        R.drawable.wasp to "Wasp",
        R.drawable.hela to "Hela",
        R.drawable.spider_man to "Spider Man",
        R.drawable.thor to "Thor"
    )

    // Generate symmetrical positions for characters
    val positions = remember { generateSymmetricalPositions(characters.size, 330f) } // Increased from 300f

    // Map characters to their positions
    val characterPositions = remember(positions) {
        characters.zip(positions).map { (imageResId, position) ->
            imageResId.first to position
        }
    }

    // Container for radar layout
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        OptimizedRadarLayout(
            radarColor = radarColor,
            centerCircleColor = centerCircleColor,
            deviceList = characterPositions,
            activatedDevices = activatedDevices,
            rssiValues = rssiValues
        )
    }
}

// Helper function to generate symmetrical positions for radar icons
fun generateSymmetricalPositions(count: Int, radius: Float): List<Offset> {
    val positions = mutableListOf<Offset>()
    for (i in 0 until count) {
        val angle = 2 * Math.PI * i / count // Calculate angle for each character
        val x = (radius * cos(angle)).toFloat() // X position
        val y = (radius * sin(angle)).toFloat() // Y position
        positions.add(Offset(x, y))
    }
    return positions
}

// Composable for optimized radar layout
@Composable
fun OptimizedRadarLayout(
    radarColor: Color, // Color for radar lines
    centerCircleColor: Color, // Color for center circle
    deviceList: List<Pair<Int, Offset>> = emptyList(), // List of device image IDs and positions
    activatedDevices: List<String> = emptyList(), // List of activated device names
    rssiValues: Map<String, Int?> = emptyMap() // RSSI values for devices
) {
    // Remember activated devices to avoid recomposition
    val rememberActivatedDevices = remember(activatedDevices) { activatedDevices }
    // Create infinite transition for radar animations
    val infiniteTransition = rememberInfiniteTransition(label = "radar_rotation")
    // Animate radar line rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing), // 4-second rotation
            repeatMode = RepeatMode.Restart
        ), label = "radar_angle"
    )
    // Animate blinking effect for activated devices
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing), // 500ms blink
            repeatMode = RepeatMode.Reverse
        ), label = "blink_alpha"
    )

    // Radar container
    Box(
        modifier = Modifier
            .size(340.dp) // Fixed size, can be adjusted to 350.dp
            .clip(CircleShape) // Clip to circular shape
            .background(Color.Transparent), // Transparent background
        contentAlignment = Alignment.Center
    ) {
        // Draw radar base (circles)
        RadarBase(radarColor, centerCircleColor)
        // Draw rotating radar line
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            rotate(degrees = rotationAngle) {
                // Draw radar line
                drawLine(
                    color = radarColor,
                    start = center,
                    end = center.copy(x = center.x, y = center.y - radius),
                    strokeWidth = 3.dp.toPx()
                )
                // Draw shadow arc
                val shadowColor = Color.Black.copy(alpha = 0.2f)
                drawArc(
                    color = shadowColor,
                    startAngle = 240f,
                    sweepAngle = 30f,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Fill
                )
            }
        }

        // Draw device icons
        deviceList.forEach { (imageResId, position) ->
            val deviceName = getDeviceNameFromResId(imageResId)
            val isActivated = rememberActivatedDevices.contains(deviceName)
            val rssi = rssiValues[deviceName]

            key(imageResId) {
                DeviceIcon(
                    imageResId = imageResId,
                    position = position,
                    isActivated = isActivated,
                    blinkAlpha = if (isActivated) blinkAlpha else 1f,
                    rssi = rssi
                )
            }
        }
    }
}

// Composable for individual device icon on radar
@Composable
fun DeviceIcon(
    imageResId: Int, // Image resource ID
    position: Offset, // Position on radar
    isActivated: Boolean, // Whether the device is activated
    blinkAlpha: Float, // Blinking alpha value
    rssi: Int? // RSSI value
) {
    val localDensity = LocalDensity.current
    val heroSize = 60.dp // Size of hero icon
    // Base modifier for positioning
    val baseModifier = Modifier
        .size(heroSize)
        .offset(
            x = with(localDensity) { position.x.toDp() },
            y = with(localDensity) { position.y.toDp() }
        )

    // Animate RSSI value
    val animatedRssi by animateFloatAsState(
        targetValue = rssi?.toFloat() ?: -100f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "rssiAnimation"
    )

    // Track hero icon's top Y position
    var heroTopY by remember { mutableStateOf(0f) }

    // Container for icon and RSSI
    Box(modifier = baseModifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // RSSI indicator
            RssiIcon(
                rssi = rssi,
                animatedRssi = animatedRssi,
                modifier = Modifier
                    .size(18.dp)
                    .offset {
                        val rssiHeightPx = with(localDensity) { 18.dp.toPx() }
                        IntOffset(0, (heroTopY - rssiHeightPx - 2f).toInt()) // Position above hero
                    }
            )

            // Hero icon container
            Box(
                modifier = Modifier
                    .size(heroSize)
                    .align(Alignment.CenterHorizontally)
                    .onGloballyPositioned { coordinates ->
                        heroTopY = coordinates.positionInParent().y // Update top Y position
                    }
            ) {
                // Highlight background for activated devices
                if (isActivated) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.3f), shape = CircleShape)
                    )
                }
                // Animate scale for activated devices
                val scaleAnimation by animateFloatAsState(
                    targetValue = if (isActivated) 1.2f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "device_scale"
                )
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = blinkAlpha // Apply blinking effect
                            scaleX = scaleAnimation // Apply scale animation
                            scaleY = scaleAnimation
                        }
                )
            }
        }
    }
}

// Composable for RSSI indicator
@Composable
fun RssiIcon(
    rssi: Int?, // RSSI value
    animatedRssi: Float, // Animated RSSI value
    modifier: Modifier = Modifier
) {
    // Container for RSSI circle and text
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Draw red circle background
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.Red,
                radius = size.minDimension / 2,
                center = center
            )
        }
        // Display RSSI value or "N/A"
        Text(
            text = if (rssi != null) "${animatedRssi.roundToInt()}" else "N/A",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// Composable for radar line (unused in OptimizedRadarLayout)
@Composable
fun RotatingRadarLine(radarColor: Color, rotationAngle: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.minDimension / 2

        // Draw rotating line
        rotate(degrees = rotationAngle) {
            drawLine(
                color = radarColor,
                start = center,
                end = center.copy(x = center.x, y = center.y - radius),
                strokeWidth = 3.dp.toPx()
            )

            // Draw radar sector shadow
            val shadowColor = Color.Black.copy(alpha = 0.2f)
            drawArc(
                color = shadowColor,
                startAngle = 240f,
                sweepAngle = 30f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Fill
            )
        }
    }
}

// Composable for radar base circles
@Composable
fun RadarBase(radarColor: Color, centerCircleColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val boxSize = size.minDimension
        val radius = boxSize / 2

        // Define radar circles with different radii and stroke widths
        val circles = listOf(
            radius * 0.33f to 2.dp,
            radius * 0.66f to 4.dp,
            radius to 6.dp
        )

        // Draw radar circles
        circles.forEach { (circleRadius, strokeWidth) ->
            drawCircle(
                color = radarColor,
                radius = circleRadius,
                style = Stroke(width = strokeWidth.toPx())
            )
        }

        // Draw center circle
        drawCircle(
            color = centerCircleColor,
            radius = radius * 0.06f,
            alpha = 1f
        )
    }
}

// Helper function to map resource ID to device name
private fun getDeviceNameFromResId(resId: Int): String {
    return when (resId) {
        R.drawable.iron_man -> "Iron_Man"
        R.drawable.hulk_ -> "Hulk"
        R.drawable.captain_marvel -> "Captain Marvel"
        R.drawable.captain_america -> "Captain America"
        R.drawable.scarlet_witch -> "Scarlet Witch"
        R.drawable.black_widow -> "Black Widow"
        R.drawable.wasp -> "Wasp"
        R.drawable.hela -> "Hela"
        R.drawable.thor -> "Thor"
        R.drawable.spider_man -> "Spider Man"
        else -> ""
    }
}

// Helper function to generate multiple positions for radar icons
fun generateMultiplePositions(count: Int, radius: Float): List<Offset> {
    val positions = mutableListOf<Offset>()

    // Divide items into inner, middle, and outer circles
    val innerCount = count / 3
    val middleCount = count / 3
    val outerCount = count - innerCount - middleCount

    // Generate positions for inner circle
    for (i in 0 until innerCount) {
        val angle = 2 * Math.PI * i / innerCount
        val x = (radius * 0.33f * cos(angle)).toFloat()
        val y = (radius * 0.33f * sin(angle)).toFloat()
        positions.add(Offset(x, y))
    }

    // Generate positions for middle circle
    for (i in 0 until middleCount) {
        val angle = 2 * Math.PI * i / middleCount
        val x = (radius * 0.66f * cos(angle)).toFloat()
        val y = (radius * 0.66f * sin(angle)).toFloat()
        positions.add(Offset(x, y))
    }

    // Generate positions for outer circle
    for (i in 0 until outerCount) {
        val angle = 2 * Math.PI * i / outerCount
        val x = (radius * 0.95f * cos(angle)).toFloat()
        val y = (radius * 0.95f * sin(angle)).toFloat()
        positions.add(Offset(x, y))
    }

    return positions
}

// Composable for scratch card effect
//@Composable
//fun ScratchCardScreen(
//    heroName: String, // Name of the hero to reveal
//    modifier: Modifier = Modifier,
//    onScratchCompleted: () -> Unit = {} // Callback when scratching is complete
//) {
//    // Load overlay image
//    val overlayImage = ImageBitmap.imageResource(id = R.drawable.scratch)
//    // Select base image based on hero name
//    val heroImageResId = remember(heroName) {
//        when (heroName) {
//            "Iron_Man" -> R.drawable.iron_man
//            "Hulk" -> R.drawable.hulk_
//            "Captain Marvel" -> R.drawable.captain_marvel
//            "Captain America" -> R.drawable.captain_america
//            "Scarlet Witch" -> R.drawable.scarlet_witch
//            "Black Widow" -> R.drawable.black_widow
//            "Wasp" -> R.drawable.wasp
//            "Hela" -> R.drawable.hela
//            "Thor" -> R.drawable.thor
//            "Spider Man" -> R.drawable.spider_man
//            else -> R.drawable.inner // Default fallback image
//        }
//    }
//
//    // Load base image
//    val baseImage = ImageBitmap.imageResource(id = heroImageResId)
//    // Track current scratch path
//    val currentPathState = remember { mutableStateOf(DraggedPath(path = Path(), width = 150f)) }
//    // Track percentage of area scratched
//    var scratchedAreaPercentage by remember { mutableFloatStateOf(0f) }
//    // Track whether completion callback has been called
//    var hasCalledCompletion by remember { mutableStateOf(false) }
//    // Canvas size in pixels
//    val canvasSizePx = with(LocalDensity.current) { 300.dp.toPx() }
//
//    // Animation state for hero reveal
//    var showHeroReveal by remember { mutableStateOf(false) }
//    // Animate scale for hero reveal
//    val animatedScale by animateFloatAsState(
//        targetValue = if (showHeroReveal) 1.2f else 1f,
//        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
//        label = "heroRevealScale"
//    )
//
//    // Trigger completion callback when 95% scratched
//    LaunchedEffect(scratchedAreaPercentage) {
//        if (scratchedAreaPercentage >= 95f && !hasCalledCompletion) {
//            hasCalledCompletion = true
//            onScratchCompleted()
//            showHeroReveal = true // Trigger hero reveal animation
//        }
//    }
//
//    // Simulate scratching animation
//    LaunchedEffect(Unit) {
//        val stepSize = canvasSizePx / 2 // Larger step size for efficiency
//        val totalArea = canvasSizePx * canvasSizePx
//        val scratchPath = currentPathState.value.path
//
//        // Create a simple scratch pattern
//        for (y in 0..canvasSizePx.toInt() step stepSize.toInt()) {
//            for (x in 0..canvasSizePx.toInt() step stepSize.toInt()) {
//                scratchPath.addOval(
//                    androidx.compose.ui.geometry.Rect(
//                        center = Offset(x.toFloat(), y.toFloat()),
//                        radius = stepSize * 0.8f
//                    )
//                )
//                scratchedAreaPercentage = ((y * canvasSizePx + x) / totalArea * 100f).coerceAtMost(100f)
//                delay(80) // Delay for smooth animation
//            }
//        }
//    }
//
//    // Scratch card container
//    Box(
//        contentAlignment = Alignment.Center,
//        modifier = modifier
//    ) {
//        // Conditional rendering based on scratch progress
//        if (scratchedAreaPercentage < 95f) {
//            OptimizedScratchCanvas(
//                overlayImage = overlayImage,
//                baseImage = baseImage,
//                currentPath = currentPathState.value.path,
//                modifier = Modifier
//                    .size(300.dp)
//                    .align(Alignment.Center)
//            )
//        } else {
//            // Reveal hero image with animation
//            Image(
//                painter = painterResource(
//                    id = when (heroName) {
//                        "Iron_Man" -> R.drawable.iron_man
//                        "Hulk" -> R.drawable.hulk_
//                        "Captain Marvel" -> R.drawable.captain_marvel
//                        "Captain America" -> R.drawable.captain_america
//                        "Scarlet Witch" -> R.drawable.scarlet_witch
//                        "Black Widow" -> R.drawable.black_widow
//                        "Wasp" -> R.drawable.wasp
//                        "Hela" -> R.drawable.hela
//                        "Thor" -> R.drawable.thor
//                        "Spider Man" -> R.drawable.spider_man
//                        else -> R.drawable.inner
//                    }
//                ),
//                contentDescription = "Revealed Hero",
//                modifier = Modifier
//                    .size(300.dp)
//                    .graphicsLayer {
//                        scaleX = animatedScale // Apply animated scale
//                        scaleY = animatedScale
//                    }
//                    .align(Alignment.Center)
//            )
//        }
//
//        // Completion indicator
//        AnimatedVisibility(
//            visible = scratchedAreaPercentage >= 95f,
//            enter = fadeIn(),
//            exit = fadeOut()
//        ) {
//            Text(
//                text = "Hero collected!",
//                style = MaterialTheme.typography.titleLarge,
//                color = Color.White,
//                modifier = Modifier
//                    .align(Alignment.BottomCenter)
//                    .padding(bottom = 16.dp)
//                    .background(
//                        Color(0x99000000), // Semi-transparent black
//                        shape = RoundedCornerShape(4.dp)
//                    )
//                    .padding(horizontal = 12.dp, vertical = 4.dp)
//            )
//        }
//    }
//}

// Composable for optimized scratch canvas
@Composable
fun OptimizedScratchCanvas(
    overlayImage: ImageBitmap, // Overlay image to scratch
    baseImage: ImageBitmap, // Base image to reveal
    currentPath: Path, // Current scratch path
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clipToBounds() // Clip content to canvas bounds
            .background(Color(0xFFF7DCA7)) // Background color
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw the overlay image
        drawImage(
            image = overlayImage,
            dstSize = IntSize(
                canvasWidth.toInt(),
                canvasHeight.toInt()
            )
        )

        // Clip the base image to the scratched path
        clipPath(path = currentPath) {
            // Scale base image to fit
            val baseImageScaleFactor = 0.8f
            val baseImageWidth = (canvasWidth * baseImageScaleFactor).toInt()
            val baseImageHeight = (canvasHeight * baseImageScaleFactor).toInt()

            // Center the base image
            val baseImageOffsetX = (canvasWidth - baseImageWidth) / 2
            val baseImageOffsetY = (canvasHeight - baseImageHeight) / 2

            drawImage(
                image = baseImage,
                dstSize = IntSize(baseImageWidth, baseImageHeight),
                dstOffset = IntOffset(
                    x = baseImageOffsetX.toInt(),
                    y = baseImageOffsetY.toInt()
                )
            )
        }
    }
}

// Data class for tracking scratch path
data class DraggedPath(
    val path: Path, // Scratch path
    val width: Float = 50f // Default path width
)
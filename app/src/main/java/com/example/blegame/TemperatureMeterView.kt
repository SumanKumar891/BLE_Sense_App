package com.example.blegame

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class TemperatureViewMeter(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thermometerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thermometerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentTemperature = 0.0 // Temperature value (e.g., 0°C to 50°C)

    // Colors for dark and light modes
    private val darkModeColors = ColorScheme(
        backgroundStart = Color.DKGRAY,
        backgroundEnd = Color.BLACK,
        marks = Color.LTGRAY,
        labels = Color.WHITE,
        needle = Color.RED,
        centerCircle = Color.WHITE,
        thermometerBorder = Color.LTGRAY,
        thermometerFill = Color.RED
    )

    private val lightModeColors = ColorScheme(
        backgroundStart = Color.LTGRAY,
        backgroundEnd = Color.WHITE,
        marks = Color.DKGRAY,
        labels = Color.BLACK,
        needle = Color.RED,
        centerCircle = Color.BLACK,
        thermometerBorder = Color.DKGRAY,
        thermometerFill = Color.RED
    )

    private var currentColors = lightModeColors // Default to light mode

    init {
        updateColors()
    }

    private fun updateColors() {
        // Detect current UI mode
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        currentColors = when (uiMode) {
            Configuration.UI_MODE_NIGHT_YES -> darkModeColors
            else -> lightModeColors
        }
    }

    fun setTemperature(temperature: Float) {
        currentTemperature =
            temperature.coerceIn(0.0F, 50.0F).toDouble() // Coerce within a range of 0°C to 50°C
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 3f
        val radius = (width / 4f) * 1.1f

        // Draw gradient background
        val gradient = android.graphics.RadialGradient(
            centerX, centerY, radius,
            intArrayOf(currentColors.backgroundStart, currentColors.backgroundEnd),
            null, android.graphics.Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = gradient
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // Draw temperature marks and labels
        markPaint.color = currentColors.marks
        markPaint.strokeWidth = 5f
        labelPaint.color = currentColors.labels
        labelPaint.textSize = radius / 5f

        for (i in 0..50 step 5) { // Marks for every 5°C
            if (i == 50) continue // Skip drawing for 50°C

            val angle = 360.0 * (i / 50.0) - 90.0 // Map temperature to angle and offset to start at the top
            val angleRad = Math.toRadians(angle)

            val startX = centerX + radius * 0.9f * cos(angleRad).toFloat()
            val startY = centerY + radius * 0.9f * sin(angleRad).toFloat()
            val endX = centerX + radius * cos(angleRad).toFloat()
            val endY = centerY + radius * sin(angleRad).toFloat()

            // Draw mark
            canvas.drawLine(startX, startY, endX, endY, markPaint)

            // Draw label (without "°C")
            val text = "$i" // Just the number
            val textX = centerX + radius * 1.1f * cos(angleRad).toFloat() - labelPaint.measureText(text) / 2f
            val textY = centerY + radius * 1.1f * sin(angleRad).toFloat() + labelPaint.textSize / 2f
            canvas.drawText(text, textX, textY, labelPaint)
        }


        // Draw needle
        needlePaint.color = currentColors.needle
        needlePaint.style = Paint.Style.FILL

        val needleLength = radius * 0.7f
        val needleWidth = radius * 0.05f
        val temperatureAngle = 360.0 * (currentTemperature / 50.0) - 90.0 // Convert temperature to angle
        val angleRad = Math.toRadians(temperatureAngle)

        val needleEndX = centerX + needleLength * cos(angleRad).toFloat()
        val needleEndY = centerY + needleLength * sin(angleRad).toFloat()
        val baseLeftX = centerX - needleWidth * sin(angleRad).toFloat()
        val baseLeftY = centerY + needleWidth * cos(angleRad).toFloat()
        val baseRightX = centerX + needleWidth * sin(angleRad).toFloat()
        val baseRightY = centerY - needleWidth * cos(angleRad).toFloat()

        val needlePath = android.graphics.Path().apply {
            moveTo(needleEndX, needleEndY)
            lineTo(baseLeftX, baseLeftY)
            lineTo(baseRightX, baseRightY)
            close()
        }

        // Add glow effect to needle
        needlePaint.setShadowLayer(10f, 0f, 0f, currentColors.needle)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        canvas.drawPath(needlePath, needlePaint)

        // Draw center circle
        needlePaint.color = currentColors.centerCircle
        canvas.drawCircle(centerX, centerY, radius * 0.08f, needlePaint)

        // Draw thermometer below the gauge (horizontal)
        val thermometerWidth = width * 0.7f
        val thermometerHeight = height / 20f
        val thermometerLeft = (width - thermometerWidth) / 2f
        val thermometerTop = centerY + radius + 120f
        val thermometerRight = thermometerLeft + thermometerWidth
        val thermometerBottom = thermometerTop + thermometerHeight
        val thermometerRect = RectF(thermometerLeft, thermometerTop, thermometerRight, thermometerBottom)

        thermometerPaint.color = Color.BLACK // Set to black

        thermometerPaint.style = Paint.Style.STROKE
        thermometerPaint.strokeWidth = 8f
        canvas.drawRoundRect(thermometerRect, thermometerHeight / 2f, thermometerHeight / 2f, thermometerPaint)

        // Draw thermometer fill
        val fillWidth = thermometerWidth * (currentTemperature / 50.0).toFloat()
        val fillRect = RectF(
            thermometerLeft,
            thermometerTop + thermometerHeight / 4f,
            thermometerLeft + fillWidth,
            thermometerBottom - thermometerHeight / 4f
        )
        // Change color based on temperature
        thermometerFillPaint.color = if (currentTemperature <= 25) {
            Color.rgb(0, 100, 0)
        } else {
            currentColors.thermometerFill // This will be red
        }
        thermometerFillPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(fillRect, thermometerHeight / 4f, thermometerHeight / 4f, thermometerFillPaint)

        // Draw thermometer labels and marks (horizontal)
        // Draw thermometer labels and marks (horizontal)
        labelPaint.color = currentColors.labels
        labelPaint.textSize = thermometerHeight / 1.5f

        val boundaryToLabelGap = 50f // Adjust this value to increase the gap between the thermometer boundary and the labels

        for (i in 0..50 step 5) {
            val labelX = thermometerLeft + (i / 50.0).toFloat() * thermometerWidth
            val labelY = thermometerBottom + boundaryToLabelGap // Increased gap from the thermometer boundary

            val text = "$i°"

            // Draw marks above the labels
            val markYStart = thermometerBottom + 10f // Start slightly below the thermometer
            val markYEnd = markYStart + 10f // Adjust the length of the hyphen mark
            canvas.drawLine(labelX, markYStart, labelX, markYEnd, markPaint)


            canvas.drawText(text, labelX - labelPaint.measureText(text) / 2f, labelY, labelPaint)
        }

    }

    // Helper class for storing color schemes
    private data class ColorScheme(
        val backgroundStart: Int,
        val backgroundEnd: Int,
        val marks: Int,
        val labels: Int,
        val needle: Int,
        val centerCircle: Int,
        val thermometerBorder: Int,
        val thermometerFill: Int
    )
}

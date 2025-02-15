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

    enum class SensorType {
        SHT40,
        SPEED_DISTANCE
    }

    // Remove the manual sensor type initialization
    private lateinit var sensorType: SensorType
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentValue = 0.0f

    // Sensor configurations
    private val sensorConfig = mapOf(
        SensorType.SHT40 to SensorConfig(
            maxValue = 50.0f,
            stepSize = 5f,
            minValue = 0f,
            unit = ""
        ),
        SensorType.SPEED_DISTANCE to SensorConfig(
            maxValue = 10.0f,
            stepSize = 1f,
            minValue = 0f,
            unit = ""
        )
    )

    init {
        // Initialize with a default sensor type
        updateSensorTypeByDevice("SHT40")
        updateColors()
    }

    // Add back the getCurrentConfig function
    private fun getCurrentConfig(): SensorConfig {
        return sensorConfig[sensorType] ?: sensorConfig[SensorType.SHT40]!!
    }

    fun updateSensorTypeByDevice(deviceType: String) {
        sensorType = when (deviceType.uppercase()) {
            "SHT40" -> SensorType.SHT40
            "SPEED_DISTANCE" -> SensorType.SPEED_DISTANCE
            else -> SensorType.SHT40 // Default to SHT40 if the device type is unknown
        }
        currentValue = getCurrentConfig().minValue // Reset the value for the new sensor
        invalidate() // Redraw the view with the new sensor configuration
    }

    private val darkModeColors = ColorScheme(
        backgroundStart = Color.DKGRAY,
        backgroundEnd = Color.BLACK,
        marks = Color.LTGRAY,
        labels = Color.WHITE,
        needle = Color.RED,
        centerCircle = Color.WHITE,
        progressBorder = Color.LTGRAY,
        progressFill = Color.RED
    )

    private val lightModeColors = ColorScheme(
        backgroundStart = Color.LTGRAY,
        backgroundEnd = Color.WHITE,
        marks = Color.DKGRAY,
        labels = Color.BLACK,
        needle = Color.RED,
        centerCircle = Color.BLACK,
        progressBorder = Color.DKGRAY,
        progressFill = Color.RED
    )

    private var currentColors = lightModeColors

    private fun updateColors() {
        val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        currentColors = when (uiMode) {
            Configuration.UI_MODE_NIGHT_YES -> darkModeColors
            else -> lightModeColors
        }
    }

    fun setTemperature(value: Float) {
        val config = getCurrentConfig()
        currentValue = value.coerceIn(config.minValue, config.maxValue)
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val config = getCurrentConfig()

        val centerX = width / 2f
        val centerY = height / 3f
        val radius = (width / 4f) * 1.1f

        drawBackground(canvas, centerX, centerY, radius)
        drawScale(canvas, centerX, centerY, radius, config)
        drawNeedle(canvas, centerX, centerY, radius,
            (currentValue - config.minValue) / (config.maxValue - config.minValue))
        drawProgressBar(canvas, centerX, centerY, radius, config)
    }

    private fun drawBackground(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val gradient = android.graphics.RadialGradient(
            centerX, centerY, radius,
            intArrayOf(currentColors.backgroundStart, currentColors.backgroundEnd),
            null, android.graphics.Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = gradient
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
    }

    private fun drawScale(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, config: SensorConfig) {
        markPaint.color = currentColors.marks
        markPaint.strokeWidth = 5f
        labelPaint.color = currentColors.labels
        labelPaint.textSize = radius / 5f

        val steps = ((config.maxValue - config.minValue) / config.stepSize).toInt()

        for (i in 0..steps) {
            val value = config.minValue + (i * config.stepSize)
            val progress = (value - config.minValue) / (config.maxValue - config.minValue)
            val angle = 270.0 * progress - 45.0
            val angleRad = Math.toRadians(angle)

            // Draw mark
            val startX = centerX + radius * 0.9f * cos(angleRad).toFloat()
            val startY = centerY + radius * 0.9f * sin(angleRad).toFloat()
            val endX = centerX + radius * cos(angleRad).toFloat()
            val endY = centerY + radius * sin(angleRad).toFloat()
            canvas.drawLine(startX, startY, endX, endY, markPaint)

            // Draw label
            val labelRadius = radius * 1.2f
            val text = value.toInt().toString()
            val textX = centerX + labelRadius * cos(angleRad).toFloat() - labelPaint.measureText(text) / 2f
            val textY = centerY + labelRadius * sin(angleRad).toFloat() + labelPaint.textSize / 2f
            canvas.drawText(text, textX, textY, labelPaint)
        }

        // Draw unit label
        labelPaint.textSize = radius / 6f
        val unitText = config.unit
        val unitX = centerX - labelPaint.measureText(unitText) / 2f
        val unitY = centerY + radius * 0.5f
        canvas.drawText(unitText, unitX, unitY, labelPaint)
    }

    private fun drawNeedle(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, progress: Float) {
        needlePaint.color = currentColors.needle
        needlePaint.style = Paint.Style.FILL

        val needleLength = radius * 0.7f
        val needleWidth = radius * 0.05f
        val valueAngle = 270.0 * progress - 45.0
        val angleRad = Math.toRadians(valueAngle)

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

        needlePaint.setShadowLayer(10f, 0f, 0f, currentColors.needle)
        canvas.drawPath(needlePath, needlePaint)

        needlePaint.color = currentColors.centerCircle
        canvas.drawCircle(centerX, centerY, radius * 0.08f, needlePaint)
    }

    private fun drawProgressBar(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, config: SensorConfig) {
        val barWidth = width * 0.7f
        val barHeight = height / 20f
        val barLeft = (width - barWidth) / 2f
        val barTop = centerY + radius + 120f
        val barRight = barLeft + barWidth
        val barBottom = barTop + barHeight
        val barRect = RectF(barLeft, barTop, barRight, barBottom)

        progressPaint.color = currentColors.progressBorder
        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeWidth = 8f
        canvas.drawRoundRect(barRect, barHeight / 2f, barHeight / 2f, progressPaint)

        val progress = (currentValue - config.minValue) / (config.maxValue - config.minValue)
        val fillWidth = barWidth * progress
        val fillRect = RectF(
            barLeft,
            barTop + barHeight / 4f,
            barLeft + fillWidth,
            barBottom - barHeight / 4f
        )

        progressFillPaint.color = when (sensorType) {
            SensorType.SHT40 -> when {
                currentValue <= 25f -> Color.GREEN
                currentValue <= 35f -> Color.YELLOW
                else -> Color.RED
            }
            SensorType.SPEED_DISTANCE -> Color.BLUE
        }

        progressFillPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(fillRect, barHeight / 4f, barHeight / 4f, progressFillPaint)

        drawProgressLabels(canvas, barLeft, barRight, barBottom, config)
    }

    private fun drawProgressLabels(canvas: Canvas, barLeft: Float, barRight: Float, barBottom: Float, config: SensorConfig) {
        labelPaint.textSize = height / 40f
        val steps = ((config.maxValue - config.minValue) / config.stepSize).toInt()

        for (i in 0..steps) {
            val value = config.minValue + (i * config.stepSize)
            val x = barLeft + (barRight - barLeft) * ((value - config.minValue) / (config.maxValue - config.minValue))
            val text = value.toInt().toString()
            canvas.drawText(text, x - labelPaint.measureText(text) / 2f, barBottom + 40f, labelPaint)
        }
    }

    private data class ColorScheme(
        val backgroundStart: Int,
        val backgroundEnd: Int,
        val marks: Int,
        val labels: Int,
        val needle: Int,
        val centerCircle: Int,
        val progressBorder: Int,
        val progressFill: Int
    )

    private data class SensorConfig(
        val maxValue: Float,
        val stepSize: Float,
        val minValue: Float,
        val unit: String
    )
}
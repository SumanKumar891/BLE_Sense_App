package com.example.blegame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class LuxViewBulb(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var currentLux = 0.0f // Current lux value
    private val bulbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        bulbPaint.color = Color.YELLOW
        bulbPaint.style = Paint.Style.FILL

        glowPaint.color = Color.YELLOW
        glowPaint.alpha = 50 // Initial transparency for glow
        glowPaint.style = Paint.Style.FILL
    }

    fun setLuxValue(lux: Float) {
        currentLux = lux.coerceIn(0.0f, 10000.0f) // Assuming a lux range of 0 to 10,000
        glowPaint.alpha = (min(255, (lux / 10000.0f * 255).toInt())) // Map lux to alpha for glow
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val bulbRadius = min(width, height) / 6f

        // Draw glow
        canvas.drawCircle(centerX, centerY, bulbRadius * 2, glowPaint)

        // Draw bulb
        canvas.drawCircle(centerX, centerY, bulbRadius, bulbPaint)
    }
}

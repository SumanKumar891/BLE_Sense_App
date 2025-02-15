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

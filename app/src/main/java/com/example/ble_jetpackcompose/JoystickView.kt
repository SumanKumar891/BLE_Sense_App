//package com.example.ble_jetpackcompose
//
//
//import android.animation.ValueAnimator
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.BlurMaskFilter
//import android.graphics.Canvas
//import android.graphics.Paint
//import android.graphics.Typeface
//import android.util.AttributeSet
//import android.util.Log
//import android.view.GestureDetector
//import android.view.MotionEvent
//import android.view.View
//import android.view.animation.OvershootInterpolator
//import kotlin.math.atan2
//import kotlin.math.hypot
//
//class JoystickView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0
//) : View(context, attrs, defStyleAttr) {
//
//    private var outerCircleRadius = 0f
//    private var innerCircleRadius = 0f
//    private var centerX = 0f
//    private var centerY = 0f
//    private var thumbX = 0f
//    private var thumbY = 0f
//    private var isDraggingThumb = false
//    private var isDraggingJoystick = false
//
//    private val outerPaint = Paint().apply {
//        style = Paint.Style.FILL
//        color = 0x80000000.toInt() // 50% transparent black
//    }
//
//    private val innerPaint = Paint().apply {
//        color = 0xFFFFFFFF.toInt() // Solid white
//        style = Paint.Style.FILL
//    }
//
//    private val shadowPaint = Paint().apply {
//        color = 0x40000000 // 25% transparent black
//        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
//    }
//
//    private val markerPaint = Paint().apply {
//        color = 0xB3FFFFFF.toInt() // 70% transparent white
//        textSize = 36f
//        textAlign = Paint.Align.CENTER
//        typeface = Typeface.DEFAULT_BOLD
//    }
//
//    private var onDirectionChangeListener: ((direction: String, joystickPosition: Pair<Float, Float>) -> Unit)? = null
//
//    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
//        override fun onDoubleTap(e: MotionEvent): Boolean {
//            resetJoystickPositionByDoubleTap()
//            return true
//        }
//    })
//
//    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
//        super.onSizeChanged(w, h, oldw, oldh)
//        val smallerSide = w.coerceAtMost(h)
//        outerCircleRadius = smallerSide * 0.45f
//        innerCircleRadius = outerCircleRadius * 0.4f
//        centerX = w / 2f
//        centerY = h / 2f
//        thumbX = centerX
//        thumbY = centerY
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        gestureDetector.onTouchEvent(event)
//
//        when (event.action) {
//            MotionEvent.ACTION_DOWN -> {
//                if (isTouchInsideThumb(event.x, event.y)) {
//                    isDraggingThumb = true
//                    updateThumbPosition(event.x, event.y)
//                }
//                return true
//            }
//
//            MotionEvent.ACTION_MOVE -> {
//                if (isDraggingThumb) {
//                    updateThumbPosition(event.x, event.y)
//                }
//                return true
//            }
//
//            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                if (isDraggingThumb) {
//                    isDraggingThumb = false
//                    resetThumbPosition()
//                }
//                return true
//            }
//        }
//
//        return super.onTouchEvent(event)
//    }
//
////    private fun isTouchInsideBase(x: Float, y: Float): Boolean {
////        return hypot(x - centerX, y - centerY) <= outerCircleRadius
////    }
//
//    override fun onDraw(canvas: Canvas) {
//        canvas.drawCircle(centerX, centerY, outerCircleRadius, outerPaint)
//        canvas.drawText("↑", centerX, centerY - outerCircleRadius * 0.75f, markerPaint)
//        canvas.drawText("↓", centerX, centerY + outerCircleRadius * 0.75f, markerPaint)
//        canvas.drawText("←", centerX - outerCircleRadius * 0.75f, centerY, markerPaint)
//        canvas.drawText("→", centerX + outerCircleRadius * 0.75f, centerY, markerPaint)
//
//        canvas.drawCircle(thumbX, thumbY, innerCircleRadius, innerPaint)
//        canvas.drawCircle(thumbX, thumbY, innerCircleRadius * 1.1f, shadowPaint)
//    }
//
//    private fun isTouchInsideThumb(x: Float, y: Float): Boolean {
//        return hypot(x - thumbX, y - thumbY) <= innerCircleRadius
//    }
//
//    private fun updateThumbPosition(x: Float, y: Float) {
//        val dx = x - centerX
//        val dy = y - centerY
//        val distance = hypot(dx, dy)
//
//        thumbX = if (distance > outerCircleRadius) centerX + dx * (outerCircleRadius / distance) else x
//        thumbY = if (distance > outerCircleRadius) centerY + dy * (outerCircleRadius / distance) else y
//
//        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
//        val direction = when (angle) {
//            in -45f..45f -> "R"
//            in 45f..135f -> "D"
//            in -135f..-45f -> "U"
//            else -> "L"
//        }
//
//        onDirectionChangeListener?.invoke(direction, getJoystickPosition())
//        invalidate()
//    }
//
//    private fun resetThumbPosition() {
//        thumbX = centerX
//        thumbY = centerY
//        ValueAnimator.ofFloat(1.2f, 1f).apply {
//            duration = 300
//            interpolator = OvershootInterpolator(3f)
//            addUpdateListener {
//                innerCircleRadius = outerCircleRadius * 0.4f * (it.animatedValue as Float)
//                invalidate()
//            }
//        }.start()
//        onDirectionChangeListener?.invoke("C", getJoystickPosition())
//    }
//
//    private fun resetJoystickPositionByDoubleTap() {
//        centerX = width / 2f
//        centerY = height / 2f
//        resetThumbPosition()
//        invalidate()
//        Log.d("Joystick", "Joystick reset by double tap")
//    }
//
//    private fun moveJoystick(rawX: Float, rawY: Float) {
//        val location = IntArray(2)
//        getLocationOnScreen(location)
//        val viewX = rawX - location[0]
//        val viewY = rawY - location[1]
//
//        centerX = viewX
//        centerY = viewY
//        thumbX = centerX
//        thumbY = centerY
//        Log.d("Joystick", "Updated Joystick Position: ($centerX, $centerY)")
//        invalidate()
//        bringToFront()
//    }
//
//    private fun getJoystickPosition(): Pair<Float, Float> {
//        return Pair(thumbX, thumbY)
//    }
//
//    fun setOnDirectionChangeListener(listener: (direction: String, joystickPosition: Pair<Float, Float>) -> Unit) {
//        this.onDirectionChangeListener = listener
//    }
//}

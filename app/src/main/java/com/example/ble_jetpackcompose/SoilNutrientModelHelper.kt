package com.example.ble_jetpackcompose

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SoilNutrientModelHelper(context: Context) {
    private val interpreter: Interpreter

    init {
        val modelBytes = context.assets.open("soil_model.tflite").readBytes()
        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size).apply {
            order(ByteOrder.nativeOrder())
            put(modelBytes)
            rewind()
        }
        interpreter = Interpreter(modelBuffer)
        Log.d("SoilModel", "✅ Model loaded successfully")
    }

    data class NutrientPrediction(
        val organicCarbon: Float,
        val nitrogen: Float,
        val phosphorus: Float,
        val potassium: Float
    )

    fun predictNutrients(reflectanceValues: List<Float>): NutrientPrediction? {
        if (reflectanceValues.size != 18) {
            Log.w("SoilModel", "⚠️ Invalid reflectance size: ${reflectanceValues.size}")
            return null
        }

        val cleaned = reflectanceValues.mapIndexed { idx, v ->
            if (v <= 0f) {
                Log.d("SoilModel", "Reflectance[$idx]=$v → replaced with 0.01f")
                0.01f
            } else v
        }
        Log.d("SoilModel", "Cleaned reflectance: $cleaned")

        val scaled = cleaned.map { it * 100f }
        Log.d("SoilModel", "Scaled reflectance (×100): $scaled")

        val interpolated = interpolateReflectance400to1000_1201(scaled)
        Log.d("SoilModel", "✅ Interpolated size: ${interpolated.size}")

        val safeInterpolated = interpolated.mapIndexed { idx, v ->
            if (v <= 0f) {
                Log.d("SoilModel", "Interpolated[$idx]=$v → replaced with 1f")
                1f
            } else v
        }

        Log.d("SoilModel", "✅ Final interpolated (first 10): ${safeInterpolated.take(10)}")

        // ✅ NEW: Log every interpolated value with wavelength (400–1000 nm in steps of 0.5)
        val sb = StringBuilder()
        safeInterpolated.forEachIndexed { idx, value ->
            val nm = 400f + idx * 0.5f
            sb.append("nm=${"%.1f".format(nm)} → ${"%.6f".format(value)}, ")
            if (idx % 30 == 29) { // print every 30 values to avoid log cutoff
                Log.d("InterpolatedFull", sb.toString())
                sb.clear()
            }
        }
        if (sb.isNotEmpty()) {
            Log.d("InterpolatedFull", sb.toString())
        }

        val inputArray = arrayOf(safeInterpolated.toFloatArray())
        val outputArray = Array(1) { FloatArray(4) }

        interpreter.run(inputArray, outputArray)
        val preds = outputArray[0]

        Log.d("SoilModel", "✅ Model output: OC=${preds[0]}, N=${preds[1]}, P=${preds[2]}, K=${preds[3]}")

        return NutrientPrediction(
            organicCarbon = preds[0],
            nitrogen = preds[1],
            phosphorus = preds[2],
            potassium = preds[3]
        )
    }



    private fun interpolateReflectance400to1000_1201(values: List<Float>): List<Float> {
        val originalWavelengths = floatArrayOf(
            410f, 435f, 460f, 485f, 510f, 535f, 560f, 585f, 610f,
            645f, 680f, 705f, 730f, 760f, 810f, 860f, 900f, 940f
        )
        val result = mutableListOf<Float>()

        // Produce exactly 1201 points from 400 to 1000 nm inclusive
        for (i in 0 until 1201) {
            val nm = 400f + i * 0.5f
            val idx = originalWavelengths.indexOfLast { it <= nm }
            val y = when {
                idx == -1 -> values.first()
                idx >= originalWavelengths.lastIndex -> values.last()
                else -> {
                    val x0 = originalWavelengths[idx]
                    val x1 = originalWavelengths[idx + 1]
                    val y0 = values[idx]
                    val y1 = values[idx + 1]
                    y0 + ((nm - x0) / (x1 - x0)) * (y1 - y0)
                }
            }
            result.add(y)
        }
        return result
    }

    fun close() = interpreter.close()
}
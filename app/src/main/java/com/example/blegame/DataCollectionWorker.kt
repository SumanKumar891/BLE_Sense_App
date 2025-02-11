package com.example.blegame

import android.content.Context
import androidx.work.WorkerParameters
import java.io.File

class DataCollectionWorker(context: Context, workerParams: WorkerParameters) :
    androidx.work.Worker(context, workerParams) {
    override fun doWork(): Result {
        // Simulate sensor data collection
        val timestamp = System.currentTimeMillis()
        val temperature = (20..30).random().toFloat()
        val humidity = (40..60).random().toFloat()
        val deviceId = "Sensor_01"

        // Save data locally
        saveReadingLocally(timestamp, temperature, humidity, deviceId)

        return Result.success()
    }

    private fun saveReadingLocally(
        timestamp: Long,
        temperature: Float,
        humidity: Float,
        deviceId: String
    ) {
        val file = File(applicationContext.getExternalFilesDir(null), "SensorData.csv")

        val isNewFile = !file.exists()
        file.appendText(
            if (isNewFile) {
                "Timestamp,Device ID,Temperature (°C),Humidity (%)\n"
            } else {
                ""
            }
        )

        file.appendText("$timestamp,$deviceId,$temperature,$humidity\n")
    }
}

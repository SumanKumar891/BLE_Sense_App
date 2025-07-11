package com.example.ble_jetpackcompose


// Update with your actual package

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class ReflectanceUploadWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val MAX_RETRIES = 3
        private const val TAG = "ReflectanceUploadWorker"

        fun createInputData(
            deviceAddress: String,
            deviceId: String,
            reflectanceValues: List<Float>,
            rawData: String
        ): Data {
            return Data.Builder()
                .putString("deviceAddress", deviceAddress)
                .putString("deviceId", deviceId)
                .putFloatArray("reflectanceValues", reflectanceValues.toFloatArray())
                .putString("rawData", rawData)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        return try {
            // 1. Extract and validate input data
            val deviceAddress = inputData.getString("deviceAddress")
                ?: return logAndFail("Missing deviceAddress")

            val deviceId = inputData.getString("deviceId")
                ?: return logAndFail("Missing deviceId")

            val reflectanceArray = inputData.getFloatArray("reflectanceValues")
                ?: return logAndFail("Missing reflectanceValues")

            val userId = Firebase.auth.currentUser?.uid
                ?: return logAndFail("User not authenticated")

            val rawData = inputData.getString("rawData").orEmpty()

            // 2. Prepare Firestore document
            val documentData = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "deviceId" to deviceId,
                "deviceAddress" to deviceAddress,
                "reflectanceValues" to reflectanceArray.toList(),
                "rawData" to rawData,
                "userId" to userId,
                "uploadAttempt" to runAttemptCount + 1,
                "uploadSource" to "Worker"
            )

            // 3. Upload to Firestore
            Firebase.firestore.collection("reflectance_readings")
                .add(documentData)
                .await()

            Log.d(TAG, "Successfully uploaded reflectance data")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed (attempt ${runAttemptCount + 1})", e)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun logAndFail(message: String): Result {
        Log.w(TAG, message)
        return Result.failure()
    }
}
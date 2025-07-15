package com.example.ble_jetpackcompose

// Import necessary Android and Firebase libraries for WorkManager and Firestore operations
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

// Worker class for uploading reflectance data to Firestore
class ReflectanceUploadWorker(
    context: Context, // Application context
    workerParams: WorkerParameters // Worker parameters
) : CoroutineWorker(context, workerParams) {

    // Companion object for constants and utility methods
    companion object {
        private const val MAX_RETRIES = 3 // Maximum number of retry attempts
        private const val TAG = "ReflectanceUploadWorker" // Log tag for debugging

        // Creates input data for the worker
        fun createInputData(
            deviceAddress: String, // Bluetooth device address
            deviceId: String, // Device identifier
            reflectanceValues: List<Float>, // Reflectance sensor values
            rawData: String // Raw sensor data
        ): Data {
            return Data.Builder()
                .putString("deviceAddress", deviceAddress)
                .putString("deviceId", deviceId)
                .putFloatArray("reflectanceValues", reflectanceValues.toFloatArray())
                .putString("rawData", rawData)
                .build()
        }
    }

    // Main worker function to perform the upload task
    override suspend fun doWork(): Result {
        return try {
            // 1. Extract and validate input data
            val deviceAddress = inputData.getString("deviceAddress")
                ?: return logAndFail("Missing deviceAddress") // Fail if deviceAddress is missing

            val deviceId = inputData.getString("deviceId")
                ?: return logAndFail("Missing deviceId") // Fail if deviceId is missing

            val reflectanceArray = inputData.getFloatArray("reflectanceValues")
                ?: return logAndFail("Missing reflectanceValues") // Fail if reflectanceValues are missing

            val userId = Firebase.auth.currentUser?.uid
                ?: return logAndFail("User not authenticated") // Fail if user is not authenticated

            val rawData = inputData.getString("rawData").orEmpty() // Get rawData, default to empty string if null

            // 2. Prepare Firestore document
            val documentData = hashMapOf(
                "timestamp" to System.currentTimeMillis(), // Current timestamp
                "deviceId" to deviceId, // Device identifier
                "deviceAddress" to deviceAddress, // Device address
                "reflectanceValues" to reflectanceArray.toList(), // Reflectance values as list
                "rawData" to rawData, // Raw sensor data
                "userId" to userId, // Authenticated user ID
                "uploadAttempt" to runAttemptCount + 1, // Current attempt number
                "uploadSource" to "Worker" // Source of upload
            )

            // 3. Upload to Firestore
            Firebase.firestore.collection("reflectance_readings")
                .add(documentData)
                .await() // Wait for Firestore upload to complete

            Log.d(TAG, "Successfully uploaded reflectance data")
            Result.success() // Return success on successful upload
        } catch (e: Exception) {
            // Handle any exceptions during upload
            Log.e(TAG, "Upload failed (attempt ${runAttemptCount + 1})", e)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry() // Retry if within max retry limit
            } else {
                Result.failure() // Fail if max retries exceeded
            }
        }
    }

    // Helper function to log failure and return Result.failure
    private fun logAndFail(message: String): Result {
        Log.w(TAG, message)
        return Result.failure()
    }
}
package com.example.blegame

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule WorkManager tasks
            val dataCollectionWorkRequest =
                PeriodicWorkRequestBuilder<DataCollectionWorker>(30, TimeUnit.MINUTES)
                    .build()
            WorkManager.getInstance(context).enqueue(dataCollectionWorkRequest)
        }
    }
}

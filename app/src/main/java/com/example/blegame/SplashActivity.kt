package com.example.blegame

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen) // Ensure you have an XML layout file for the splash screen

        // Delay for 3 seconds (3000 milliseconds) before navigating to the main activity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close the splash screen so the user cannot return to it
        }, 2000) // 3000 milliseconds = 3 seconds
    }
}

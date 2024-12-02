package com.merge.awadh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class test6 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test6)

        val button=findViewById<Button>(R.id.playButton2)
        button.setOnClickListener{
            val Intent= Intent(this, test7::class.java)
            startActivity(Intent)
        }
    }
}
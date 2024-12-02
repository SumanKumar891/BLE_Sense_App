package com.merge.awadh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

import com.merge.awadh.test3

class test2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test2)
        val button=findViewById<Button>(R.id.button2)
        button.setOnClickListener{
            val Intent= Intent(this, test3::class.java)
            startActivity(Intent)
        }
        val button2=findViewById<Button>(R.id.multiUserPlay)
        button2.setOnClickListener{
            val Intent= Intent(this, test6::class.java)
            startActivity(Intent)
        }
    }
}
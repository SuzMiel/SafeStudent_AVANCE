package com.example.safestudent

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSafeStudent = findViewById<LinearLayout>(R.id.btnSafeStudent)

        btnSafeStudent.setOnClickListener {
            val intent = Intent(this, AlertaActivity::class.java)
            startActivity(intent)
        }
    }
}
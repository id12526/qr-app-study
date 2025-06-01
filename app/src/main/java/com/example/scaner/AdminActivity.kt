package com.example.scaner

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import com.example.scaner.databinding.ActivityAdminBinding

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = resources.getColor(android.R.color.white, theme)
        window.navigationBarColor = resources.getColor(android.R.color.white, theme)

        window.decorView.systemUiVisibility = (
                window.decorView.systemUiVisibility or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        binding.mainUpload.setOnClickListener {
            val intent = Intent(this@AdminActivity, UploadActivity::class.java)
            startActivity(intent)
        }
        binding.mainUpdate.setOnClickListener {
            val intent = Intent(this@AdminActivity, UpdateActivity::class.java)
            startActivity(intent)
        }
        binding.mainDelete.setOnClickListener{
            val intent = Intent(this@AdminActivity, DeleteActivity::class.java)
            startActivity(intent)
        }
        binding.mainGenerate.setOnClickListener{
            val intent = Intent(this@AdminActivity, GenerationActivity::class.java)
            startActivity(intent)
        }
    }
}
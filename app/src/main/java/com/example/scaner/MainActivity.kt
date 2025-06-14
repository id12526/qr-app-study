package com.example.scaner

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.example.scaner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var currentUid: String
    private var avatarUrl: String? = null
    private var role: String? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)

        window.statusBarColor = resources.getColor(R.color.gradient_1, theme)
        window.navigationBarColor = resources.getColor(android.R.color.white, theme)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        binding.backButton.setOnClickListener {
            clearUserData()
            val intent = Intent(this@MainActivity, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        currentUid = intent.getStringExtra("UID") ?: ""
        role = intent.getStringExtra("ROLE")
        avatarUrl = intent.getStringExtra("AVATAR_URL")

        if (role != "admin") {
            binding.mainAdmin.visibility = View.GONE
        }

        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.circle_background)
                .error(R.drawable.error_avatar)
                .circleCrop()
                .into(binding.avatarImage)
        } else {
            Glide.with(this)
                .load(R.drawable.default_avatar)
                .circleCrop()
                .into(binding.avatarImage)
        }

        binding.mainCategory.setOnClickListener {
            val intent = Intent(this@MainActivity, CategoryActivity::class.java)
            startActivity(intent)
        }

        binding.mainOpenScanner.setOnClickListener {
            val intent = Intent(this@MainActivity, ScannerActivity::class.java)
            startActivity(intent)
        }

        binding.mainAdmin.setOnClickListener {
            val intent = Intent(this@MainActivity, AdminActivity::class.java)
            startActivity(intent)
        }

        binding.avatarImage.setOnClickListener {
            val intent = Intent(this@MainActivity, ProfileActivity::class.java)
            intent.putExtra("UID", currentUid)
            intent.putExtra("AVATAR_URL", avatarUrl)
            startActivity(intent)
        }
    }

    private fun clearUserData() {
        val editor = sharedPreferences.edit()
        editor.remove("LOGIN")
        editor.remove("PASSWORD")
        editor.apply()
    }
}
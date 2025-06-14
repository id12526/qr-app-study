package com.example.scaner

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.scaner.databinding.ActivityCategoryBinding

class CategoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = resources.getColor(R.color.gradient_1, theme)
        window.navigationBarColor = resources.getColor(android.R.color.white, theme)

        window.decorView.systemUiVisibility = (
                window.decorView.systemUiVisibility or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.screenCategory.setOnClickListener {
            navigateToListActivity("Мониторы")
        }

        binding.keyboardCategory.setOnClickListener {
            navigateToListActivity("Клавиатуры")
        }

        binding.mouseCategory.setOnClickListener {
            navigateToListActivity("Мыши")
        }

        binding.unitCategory.setOnClickListener {
            navigateToListActivity("Системные блоки")
        }

        binding.chairCategory.setOnClickListener {
            navigateToListActivity("Стулья")
        }

        binding.tableCategory.setOnClickListener {
            navigateToListActivity("Столы")
        }

        binding.cableCategory.setOnClickListener {
            navigateToListActivity("Удлинители")
        }
    }

    private fun navigateToListActivity(categoryName: String) {
        val intent = Intent(this@CategoryActivity, ListActivity::class.java)
        intent.putExtra("CATEGORY_NAME", categoryName)
        startActivity(intent)
    }
}
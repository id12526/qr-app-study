package com.example.scaner

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scaner.databinding.ActivityDeleteBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class DeleteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeleteBinding
    private lateinit var databaseReference: DatabaseReference
    private var selectedCategory: String = "Мониторы"
    private var selectedObjectKey: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeleteBinding.inflate(layoutInflater)
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

        val categories = listOf("Мониторы", "Клавиатуры", "Мыши", "Системные блоки", "Стулья", "Столы", "Удлинители")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter
        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedCategory = categories[position]
                loadObjectsForCategory(selectedCategory)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.objectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedObjectKey = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.deleteButton.setOnClickListener {
            if (selectedObjectKey.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, выберите объект для удаления", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            databaseReference = FirebaseDatabase.getInstance().getReference("Products/$selectedCategory/$selectedObjectKey")
            databaseReference.removeValue().addOnSuccessListener {
                Toast.makeText(this, "Объект удален", Toast.LENGTH_SHORT).show()
                loadObjectsForCategory(selectedCategory)
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Ошибка удаления: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun loadObjectsForCategory(category: String) {
        val objects = mutableListOf<String>()
        FirebaseDatabase.getInstance().getReference("Products/$category")
            .get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    for (childSnapshot in snapshot.children) {
                        objects.add(childSnapshot.key.toString())
                    }
                    val objectAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, objects)
                    objectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.objectSpinner.adapter = objectAdapter
                } else {
                    Toast.makeText(this, "Нет объектов в этой категории", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки объектов: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

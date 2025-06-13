package com.example.scaner

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scaner.databinding.ActivityUploadBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UploadActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUploadBinding
    private lateinit var databaseReference: DatabaseReference
    private var selectedCategory: String = "Мониторы"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = resources.getColor(R.color.gradient_1, theme)
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

        val rootLayout = findViewById<View>(R.id.root_layout)
        rootLayout.setOnClickListener {
            currentFocus?.let { view ->
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }

        val defaultMinLines = 1
        val defaultMaxLines = 1
        val expandedMaxLines = 5
        val editText = findViewById<EditText>(R.id.uploadDesc)

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                editText.minLines = expandedMaxLines
                editText.maxLines = expandedMaxLines
            } else {
                editText.minLines = defaultMinLines
                editText.maxLines = defaultMaxLines
            }
        }

        val categories = listOf("Мониторы", "Клавиатуры", "Мыши", "Системные блоки", "Стулья", "Столы", "Удлинители")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter

        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedCategory = categories[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.saveButton.setOnClickListener {
            val name = binding.uploadName.text.toString()
            val desc = binding.uploadDesc.text.toString()
            val id = binding.uploadId.text.toString()
            val qr = binding.uploadQr.text.toString()

            if (name.isEmpty() || desc.isEmpty() || id.isEmpty() || qr.isEmpty()) {
                Toast.makeText(this, "Пожалуйста заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            databaseReference = FirebaseDatabase.getInstance().getReference("Products/$selectedCategory")

            databaseReference.child(name).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Toast.makeText(this, "Такой объект уже имеется", Toast.LENGTH_SHORT).show()
                } else {
                    val productData = ProductData(name, desc, id, qr)
                    databaseReference.child(name).setValue(productData).addOnSuccessListener {
                        binding.uploadName.text.clear()
                        binding.uploadDesc.text.clear()
                        binding.uploadId.text.clear()
                        binding.uploadQr.text.clear()
                        Toast.makeText(this, "Успешно сохранено в $selectedCategory", Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Ошибка сохранения данных: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Ошибка проверки данных: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

    }
}

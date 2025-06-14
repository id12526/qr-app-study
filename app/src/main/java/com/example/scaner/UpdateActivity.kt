package com.example.scaner

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scaner.databinding.ActivityUpdateBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UpdateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUpdateBinding
    private lateinit var databaseReference: DatabaseReference
    private var selectedCategory: String = "Мониторы"
    private var selectedObjectKey: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
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
                loadObjectData(selectedObjectKey)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.saveButton.setOnClickListener {
            val newName = binding.uploadName.text.toString().trim()
            val desc = binding.uploadDesc.text.toString().trim()
            val id = binding.uploadId.text.toString().trim()
            val qr = binding.uploadQr.text.toString().trim()
            if (newName.isEmpty() || desc.isEmpty() || id.isEmpty() || qr.isEmpty()) {
                Toast.makeText(this, "Пожалуйста заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isKeyChanged = selectedObjectKey != newName
            if (isKeyChanged) {
                val newRef = FirebaseDatabase.getInstance().getReference("Products/$selectedCategory/$newName")
                val oldRef = FirebaseDatabase.getInstance().getReference("Products/$selectedCategory/$selectedObjectKey")
                val productData = ProductData(newName, desc, id, qr)
                newRef.setValue(productData).addOnSuccessListener {
                    oldRef.removeValue().addOnSuccessListener {
                        Toast.makeText(this, "Данные успешно обновлены с новым именем", Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Ошибка удаления старой записи: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Ошибка обновления данных: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                val currentRef = FirebaseDatabase.getInstance().getReference("Products/$selectedCategory/$selectedObjectKey")
                val productData = ProductData(newName, desc, id, qr)
                currentRef.setValue(productData).addOnSuccessListener {
                    Toast.makeText(this, "Данные успешно обновлены", Toast.LENGTH_SHORT).show()
                    finish()
                }.addOnFailureListener {
                    Toast.makeText(this, "Ошибка обновления данных: ${it.message}", Toast.LENGTH_SHORT).show()
                }
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
                }
            }
    }

    private fun loadObjectData(objectKey: String) {
        FirebaseDatabase.getInstance().getReference("Products/$selectedCategory/$objectKey")
            .get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val productData = snapshot.getValue(ProductData::class.java)
                    if (productData != null) {
                        binding.uploadName.setText(productData.name)
                        binding.uploadDesc.setText(productData.desc)
                        binding.uploadId.setText(productData.id)
                        binding.uploadQr.setText(productData.qr)
                    }
                } else {
                    Toast.makeText(this, "Объект не найден", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Ошибка загрузки данных: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

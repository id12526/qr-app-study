package com.example.scaner

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scaner.databinding.ActivityListBinding
import com.google.firebase.database.*

class ListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var productList: MutableList<String>
    private lateinit var fullProductList: MutableList<String>

    private var categoryName: String = ""
    private lateinit var searchEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка цветов статус-бара и навигационной панели
        window.statusBarColor = resources.getColor(android.R.color.white, theme)
        window.navigationBarColor = resources.getColor(android.R.color.white, theme)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        // Настройка кнопки "Назад"
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        // Получаем название категории из Intent
        categoryName = intent.getStringExtra("CATEGORY_NAME") ?: ""
        binding.categoryTitle.text = categoryName

        // Инициализация списков
        productList = mutableListOf()
        fullProductList = mutableListOf()

        // Инициализация адаптера
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, productList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)

                // Добавляем номер перед названием элемента
                textView.text = "${position + 1}. ${productList[position]}"

                return view
            }
        }

        binding.listView.adapter = adapter

        // Подключение к Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("Products/$categoryName")

        // Загрузка данных
        loadProducts()

        // Настройка поиска
        searchEditText = findViewById(R.id.searchEditText)
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val query = charSequence.toString().trim()
                if (query.isNotEmpty()) {
                    filterList(query)
                } else {
                    resetFilter()
                }
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        // Обработка клика на элемент списка
        binding.listView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = productList[position]
            navigateToDetailActivity(selectedItem)
        }
    }

    private fun loadProducts() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    productList.clear()
                    fullProductList.clear()
                    for (childSnapshot in snapshot.children) {
                        val productName = childSnapshot.key.toString()
                        productList.add(productName)
                        fullProductList.add(productName)
                    }
                    (binding.listView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
                    showListView()
                } else {
                    Toast.makeText(this@ListActivity, "Нет данных для этой категории", Toast.LENGTH_SHORT).show()
                    showNoResultsMessage() // Показать сообщение, если данных нет
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ListActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterList(query: String) {
        val filteredList = fullProductList.filter { it.contains(query, ignoreCase = true) }
        if (filteredList.isNotEmpty()) {
            productList.clear()
            productList.addAll(filteredList)
            (binding.listView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            showListView()
        } else {
            showNoResultsMessage()
        }
    }

    private fun resetFilter() {
        productList.clear()
        productList.addAll(fullProductList)
        (binding.listView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        showListView()
    }

    private fun showListView() {
        binding.listView.visibility = View.VISIBLE
        binding.noResultsTextView.visibility = View.GONE
    }

    private fun showNoResultsMessage() {
        binding.listView.visibility = View.GONE
        binding.noResultsTextView.visibility = View.VISIBLE
    }

    private fun navigateToDetailActivity(productNameWithNumber: String) {
        // Убираем номер из строки перед передачей в следующую активити
        val productName = productNameWithNumber.substringAfter(". ").trim()

        val intent = Intent(this@ListActivity, ListDetailActivity::class.java)
        intent.putExtra("CATEGORY_NAME", categoryName)
        intent.putExtra("ITEM_NAME", productName)
        startActivity(intent)
    }
}
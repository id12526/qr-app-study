package com.example.scaner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        window.statusBarColor = resources.getColor(R.color.gradient_1, theme)
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

        setupFocusChange(findViewById(R.id.searchEditText), R.drawable.ic_search_gradient, R.drawable.ic_search)

        val rootLayout = findViewById<View>(R.id.root_layout)
        val listView = findViewById<ListView>(R.id.listView)

        rootLayout.setOnClickListener {
            // Снимаем фокус с EditText
            currentFocus?.let { view ->
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }

        listView.setOnTouchListener { _, _ ->
            // Передаем клик на root_layout
            rootLayout.performClick()
            false // Возвращаем false, чтобы ListView продолжал обрабатывать свои события
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

    private fun setupFocusChange(editText: EditText, focusedDrawableRes: Int, defaultDrawableRes: Int) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            // Получаем текущие compoundDrawables
            val currentDrawables = editText.compoundDrawables

            // Определяем новую левую иконку в зависимости от фокуса
            val newLeftDrawable = ContextCompat.getDrawable(this, if (hasFocus) focusedDrawableRes else defaultDrawableRes)?.mutate()

            // Устанавливаем новые compoundDrawables, сохраняя правую иконку (глаз)
            editText.setCompoundDrawablesWithIntrinsicBounds(
                newLeftDrawable, // Левая иконка
                currentDrawables[1], // Верхняя иконка (если есть)
                currentDrawables[2], // Правая иконка (глаз)
                currentDrawables[3]  // Нижняя иконка (если есть)
            )
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
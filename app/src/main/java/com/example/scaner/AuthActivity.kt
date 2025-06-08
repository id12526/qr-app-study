package com.example.scaner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.method.PasswordTransformationMethod
import android.text.method.HideReturnsTransformationMethod
import android.content.SharedPreferences
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.scaner.databinding.ActivityAuthBinding
import com.google.firebase.database.*

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка цветов статус-бара и навигационной панели
        window.statusBarColor = resources.getColor(android.R.color.white, theme)
        window.navigationBarColor = resources.getColor(android.R.color.white, theme)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        val rootLayout = findViewById<View>(R.id.root_layout)

        rootLayout.setOnClickListener {
            // Снимаем фокус с EditText
            currentFocus?.let { view ->
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)
        val rememberMeCheckBox = findViewById<CheckBox>(R.id.remember_me)

        rememberMeCheckBox.buttonTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.dark_accent)
        )

        // Проверка сохраненных данных
        val savedLogin = sharedPreferences.getString("LOGIN", "")
        val savedPassword = sharedPreferences.getString("PASSWORD", "")
        if (savedLogin != null && savedPassword != null) {
            binding.login.setText(savedLogin)
            binding.pass.setText(savedPassword)
            binding.rememberMe.isChecked = true
        }

        // Настройка EditText для пароля
        val passwordInput = findViewById<EditText>(R.id.pass)
        var isPasswordVisible = false

        // Добавляем обработчик нажатия на иконку глазка
        passwordInput.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                // Проверяем, было ли нажатие на иконку глазка
                if (event.rawX >= passwordInput.right - passwordInput.compoundDrawables[2].bounds.width()) {
                    isPasswordVisible = !isPasswordVisible
                    updatePasswordVisibility(passwordInput, isPasswordVisible)
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }

        // Настройка TextView "Зарегистрируйтесь"
        val registerText = findViewById<TextView>(R.id.register_link)
        registerText.setOnClickListener {
            val intent = Intent(this@AuthActivity, RegActivity::class.java)
            startActivity(intent)
        }

        // Логика авторизации
        binding.signInButton.setOnClickListener {
            val loginInput = binding.login.text.toString()
            val passInput = binding.pass.text.toString()

            if (loginInput.isNotEmpty() && passInput.isNotEmpty()) {
                signIn(loginInput, passInput)
            } else {
                Toast.makeText(this, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Метод для обновления видимости пароля
    private fun updatePasswordVisibility(editText: EditText, isVisible: Boolean) {
        if (isVisible) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            editText.setCompoundDrawablesWithIntrinsicBounds(
                editText.compoundDrawables[0], // Левая иконка (например, замок)
                null,
                getDrawable(R.drawable.ic_eye_on), // Правая иконка (открытый глаз)
                null
            )
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            editText.setCompoundDrawablesWithIntrinsicBounds(
                editText.compoundDrawables[0], // Левая иконка (например, замок)
                null,
                getDrawable(R.drawable.ic_eye_off), // Правая иконка (закрытый глаз)
                null
            )
        }
        // Перемещаем курсор в конец текста
        editText.setSelection(editText.text.length)
    }

    private fun signIn(login: String, passInput: String) {
        databaseReference = FirebaseDatabase.getInstance().getReference("Authorization")
        databaseReference.orderByChild("login").equalTo(login).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val uid = userSnapshot.key // Получаем UID пользователя
                        val password = userSnapshot.child("password").value
                        val role = userSnapshot.child("role").value
                        val avatarUrl = userSnapshot.child("avatarUrl").value

                        if (password.toString() == passInput) {
                            // Сохранение данных, если отмечено "Запомнить меня"
                            if (binding.rememberMe.isChecked) {
                                saveUserData(login, passInput)
                            } else {
                                clearUserData()
                            }

                            // Переход на MainActivity с данными пользователя
                            val intent = Intent(this@AuthActivity, MainActivity::class.java).apply {
                                putExtra("ROLE", role.toString())
                                putExtra("AVATAR_URL", avatarUrl?.toString())
                                putExtra("UID", uid) // Передаем UID вместо логина
                            }
                            startActivity(intent)
                            finish()
                            Toast.makeText(this@AuthActivity, "Вы успешно авторизовались", Toast.LENGTH_SHORT).show()
                            return
                        } else {
                            Toast.makeText(this@AuthActivity, "Неверный пароль", Toast.LENGTH_SHORT).show()
                            return
                        }
                    }
                } else {
                    Toast.makeText(this@AuthActivity, "Неверный логин", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AuthActivity, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveUserData(login: String, password: String) {
        val editor = sharedPreferences.edit()
        editor.putString("LOGIN", login)
        editor.putString("PASSWORD", password)
        editor.apply()
    }

    private fun clearUserData() {
        val editor = sharedPreferences.edit()
        editor.remove("LOGIN")
        editor.remove("PASSWORD")
        editor.apply()
    }
}
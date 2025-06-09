package com.example.scaner

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegActivity : AppCompatActivity() {
    private lateinit var databaseReference: DatabaseReference

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reg)

        databaseReference = FirebaseDatabase.getInstance().getReference("Authorization")

        // Настройка цветов статус-бара и навигационной панели
        window.statusBarColor = resources.getColor(R.color.gradient_2, theme)
        window.navigationBarColor = resources.getColor(R.color.gradient_1, theme)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        // Настройка кнопки "Назад"
        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        val rootLayout = findViewById<View>(R.id.root_layout)
        rootLayout.setOnClickListener {
            // Снимаем фокус с EditText
            currentFocus?.let { view ->
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }

        setupFocusChange(findViewById(R.id.reg_login), R.drawable.ic_person_gradient, R.drawable.ic_person)
        setupFocusChange(findViewById(R.id.reg_pass), R.drawable.ic_password_gradient, R.drawable.ic_password)
        setupFocusChange(findViewById(R.id.reg_firstName), R.drawable.ic_contact_gradient, R.drawable.ic_contact)
        setupFocusChange(findViewById(R.id.reg_lastName), R.drawable.ic_contact_gradient, R.drawable.ic_contact)
        setupFocusChange(findViewById(R.id.reg_middleName), R.drawable.ic_contact_gradient, R.drawable.ic_contact)

        // Логика регистрации
        val regLogin = findViewById<EditText>(R.id.reg_login)
        val regPass = findViewById<EditText>(R.id.reg_pass)
        val regFirstName = findViewById<EditText>(R.id.reg_firstName)
        val regLastName = findViewById<EditText>(R.id.reg_lastName)
        val regMiddleName = findViewById<EditText>(R.id.reg_middleName)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val login = regLogin.text.toString().trim()
            val password = regPass.text.toString().trim()
            val firstName = regFirstName.text.toString().trim()
            val lastName = regLastName.text.toString().trim()
            val middleName = regMiddleName.text.toString().trim()

            if (login.isNotEmpty() && password.isNotEmpty() && firstName.isNotEmpty() && lastName.isNotEmpty()) {
                registerUser(login, password, firstName, lastName, middleName)
            } else {
                Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
            }
        }

        regPass.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                if (password.isNotEmpty()) {
                    validatePassword(password)
                } else {
                    // Если поле пустое, убираем правую иконку
                    regPass.setCompoundDrawablesWithIntrinsicBounds(
                        regPass.compoundDrawables[0], // Левая иконка
                        null,
                        null, // Убираем правую иконку
                        null
                    )
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        regLogin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val login = s.toString().trim()
                if (login.isNotEmpty()) {
                    checkLoginAvailability(login)
                } else {
                    // Если поле пустое, устанавливаем ic_null
                    regLogin.setCompoundDrawablesWithIntrinsicBounds(
                        regLogin.compoundDrawables[0], // Левая иконка
                        null,
                        ContextCompat.getDrawable(this@RegActivity, R.drawable.ic_null), // Правая иконка
                        null
                    )
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private val handler = Handler(Looper.getMainLooper())
    private var loginCheckRunnable: Runnable? = null

    private fun checkLoginAvailability(login: String) {
        // Если логин пустой, устанавливаем ic_null
        if (login.isEmpty()) {
            val regLogin = findViewById<EditText>(R.id.reg_login)
            regLogin.setCompoundDrawablesWithIntrinsicBounds(
                regLogin.compoundDrawables[0], // Левая иконка
                null,
                ContextCompat.getDrawable(this@RegActivity, R.drawable.ic_null), // Правая иконка
                null
            )
            return
        }

        // Отменяем предыдущий запланированный запрос
        loginCheckRunnable?.let { handler.removeCallbacks(it) }

        // Создаем новый запрос с задержкой
        loginCheckRunnable = Runnable {
            databaseReference.orderByChild("login").equalTo(login).get()
                .addOnSuccessListener { snapshot ->
                    val drawableRes = if (snapshot.exists()) R.drawable.ic_check_wrong else R.drawable.ic_check_done
                    val drawable = ContextCompat.getDrawable(this@RegActivity, drawableRes)?.mutate()

                    runOnUiThread {
                        val regLogin = findViewById<EditText>(R.id.reg_login)
                        val currentDrawables = regLogin.compoundDrawables
                        if (currentDrawables != null && regLogin.text.toString().trim() == login) {
                            regLogin.setCompoundDrawablesWithIntrinsicBounds(
                                currentDrawables[0], // Левая иконка
                                null,
                                drawable, // Правая иконка
                                null
                            )
                        }
                    }
                }
                .addOnFailureListener {
                    runOnUiThread {
                        Toast.makeText(this@RegActivity, "Ошибка проверки логина", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Запускаем запрос через 300 мс
        handler.postDelayed(loginCheckRunnable!!, 300)
    }

    private fun validatePassword(password: String) {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        val isLengthValid = password.length >= 8

        val isValid = hasUppercase && hasDigit && hasSpecialChar && isLengthValid
        val drawableRes = if (isValid) R.drawable.ic_check_done else R.drawable.ic_check_wrong
        val drawable = ContextCompat.getDrawable(this@RegActivity, drawableRes)?.mutate()
        val regPass = findViewById<EditText>(R.id.reg_pass)

        val currentDrawables = regPass.compoundDrawables
        if (currentDrawables != null) {
            regPass.setCompoundDrawablesWithIntrinsicBounds(
                currentDrawables[0], // Левая иконка
                null,
                drawable, // Правая иконка
                null
            )

            // Показываем подсказку, если иконка ic_check_wrong
            if (drawable?.constantState == ContextCompat.getDrawable(this@RegActivity, R.drawable.ic_check_wrong)?.constantState) {
                TooltipCompat.setTooltipText(regPass, "Пароль должен состоять из 8 символов и включать в себя цифры, буквы, один спецсимвол и одну букву с заглавной")
                regPass.performLongClick() // Активируем подсказку
            }
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
                currentDrawables[2], // Правая иконка (галочка)
                currentDrawables[3]  // Нижняя иконка (если есть)
            )
        }
    }

    private fun registerUser(login: String, password: String, firstName: String, lastName: String, middleName: String) {
        // Проверяем пароль перед регистрацией
        if (!isPasswordValid(password)) {
            Toast.makeText(this, "Пароль не соответствует требованиям", Toast.LENGTH_SHORT).show()
            return
        }

        // Шаг 1: Получаем все записи для определения максимального UID и проверки пропусков
        databaseReference.get().addOnSuccessListener { snapshot ->
            val existingUIDs = mutableListOf<Long>() // Список для хранения существующих UID

            // Проходим по всем записям и извлекаем UID
            for (userSnapshot in snapshot.children) {
                val uidKey = userSnapshot.key ?: continue
                val uidNumber = uidKey.replace("uid", "").toLongOrNull() ?: continue
                existingUIDs.add(uidNumber)
            }

            // Сортируем UID по возрастанию
            existingUIDs.sort()

            // Находим первый пропущенный UID
            var newUIDNumber = 1L // Начинаем с 1
            for (uid in existingUIDs) {
                if (uid != newUIDNumber) {
                    break // Пропуск найден, выходим из цикла
                }
                newUIDNumber++
            }

            // Генерируем новый UID
            val newUID = "uid$newUIDNumber"

            // Создаем объект с данными пользователя
            val userData = mapOf(
                "login" to login,
                "password" to password,
                "role" to "user", // Новый пользователь по умолчанию имеет роль "user"
                "avatarUrl" to "", // URL аватарки пока пустой
                "firstName" to firstName,
                "lastName" to lastName,
                "middleName" to middleName
            )

            // Проверяем, существует ли пользователь с таким логином
            databaseReference.orderByChild("login").equalTo(login).get().addOnSuccessListener { loginSnapshot ->
                if (loginSnapshot.exists()) {
                    Toast.makeText(this, "Пользователь с таким логином уже существует", Toast.LENGTH_SHORT).show()
                } else {
                    // Сохраняем данные нового пользователя с новым UID
                    databaseReference.child(newUID.toString()).setValue(userData).addOnSuccessListener {
                        Toast.makeText(this, "Пользователь успешно зарегистрирован", Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Ошибка регистрации: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Ошибка проверки данных: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Ошибка загрузки данных: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        val isLengthValid = password.length >= 8
        return hasUppercase && hasDigit && hasSpecialChar && isLengthValid
    }
}
package com.example.scaner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegActivity : AppCompatActivity() {
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reg)

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
    }

    private fun registerUser(login: String, password: String, firstName: String, lastName: String, middleName: String) {
        databaseReference = FirebaseDatabase.getInstance().getReference("Authorization")

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
}
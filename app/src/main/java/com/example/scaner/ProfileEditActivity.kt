package com.example.scaner

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.scaner.databinding.ActivityProfileEditBinding
import com.google.firebase.database.*

class ProfileEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var currentUid: String
    private var avatarUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка цветов статус-бара и навигационной панели
        window.statusBarColor = resources.getColor(R.color.gradient_1, theme)
        window.navigationBarColor = resources.getColor(android.R.color.white, theme)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        // Получение данных из Intent
        currentUid = intent.getStringExtra("UID") ?: ""
        avatarUrl = intent.getStringExtra("AVATAR_URL")

        // Загрузка данных пользователя
        loadUserData()

        // Настройка кнопки "Назад"
        binding.backButton.setOnClickListener {
            finish()
        }

        // Скрытие клавиатуры при нажатии вне полей ввода
        val rootLayout = findViewById<View>(R.id.root_layout)
        rootLayout.setOnClickListener {
            currentFocus?.let { view ->
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }

        // Загрузка аватарки
        loadAvatar()

        // Изменение аватарки
        binding.changeAvatarButton.setOnClickListener {
            showEditDialog("URL аватарки") { newAvatarUrl ->
                updateAvatar(newAvatarUrl)
            }
        }

        // Проверка логина на доступность
        binding.editLogin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val login = s.toString().trim()
                if (login.isNotEmpty()) {
                    checkLoginAvailability(login)
                } else {
                    setLoginValidationIcon(false)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Проверка пароля на соответствие требованиям
        binding.editPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                validatePassword(password)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Сохранение изменений
        binding.saveButton.setOnClickListener {
            val newLogin = binding.editLogin.text.toString().trim()
            val newPassword = binding.editPassword.text.toString().trim()
            val firstName = binding.editFirstName.text.toString().trim()
            val lastName = binding.editLastName.text.toString().trim()
            val middleName = binding.editMiddleName.text.toString().trim()

            if (newLogin.isNotEmpty() && newPassword.isNotEmpty()) {
                if (!isPasswordValid(newPassword)) {
                    Toast.makeText(this, "Пароль не соответствует требованиям", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val updatedData = mapOf(
                    "login" to newLogin,
                    "password" to newPassword,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "middleName" to middleName
                )

                updateUserData(updatedData)
            } else {
                Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserData() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Authorization")
        databaseReference.child(currentUid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val login = snapshot.child("login").value.toString()
                val password = snapshot.child("password").value.toString()
                val firstName = snapshot.child("firstName").value.toString()
                val lastName = snapshot.child("lastName").value.toString()
                val middleName = snapshot.child("middleName").value.toString()

                // Заполняем поля данными пользователя
                binding.editLogin.setText(login)
                binding.editPassword.setText(password)
                binding.editFirstName.setText(firstName)
                binding.editLastName.setText(lastName)
                binding.editMiddleName.setText(middleName)

                Log.d("ProfileEditActivity", "Данные пользователя загружены")
            } else {
                Log.e("ProfileEditActivity", "Ошибка загрузки данных пользователя")
            }
        }.addOnFailureListener {
            Log.e("ProfileEditActivity", "Ошибка загрузки данных: ${it.message}")
        }
    }

    private fun loadAvatar() {
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
    }

    private fun showEditDialog(field: String, onSave: (String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Изменить $field")
        val input = androidx.appcompat.widget.AppCompatEditText(this)
        builder.setView(input)
        builder.setPositiveButton("Сохранить") { _, _ ->
            val newValue = input.text.toString()
            if (newValue.isNotEmpty()) {
                onSave(newValue)
            }
        }
        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun updateUserData(updatedData: Map<String, String>) {
        databaseReference.child(currentUid).updateChildren(updatedData)
            .addOnSuccessListener {
                Log.d("ProfileEditActivity", "Данные успешно обновлены")
                Toast.makeText(this, "Профиль успешно обновлен", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Log.e("ProfileEditActivity", "Ошибка при обновлении данных: ${it.message}")
                Toast.makeText(this, "Ошибка при обновлении профиля", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAvatar(newAvatarUrl: String) {
        databaseReference.child(currentUid).child("avatarUrl").setValue(newAvatarUrl)
            .addOnSuccessListener {
                Log.d("ProfileEditActivity", "Аватарка успешно обновлена: $newAvatarUrl")
                avatarUrl = newAvatarUrl
                loadAvatar()
                Toast.makeText(this, "Аватарка успешно изменена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Log.e("ProfileEditActivity", "Ошибка при обновлении аватарки: ${it.message}")
                Toast.makeText(this, "Ошибка при изменении аватарки", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkLoginAvailability(login: String) {
        if (login.isEmpty()) {
            setLoginValidationIcon(false)
            return
        }

        val handler = Handler(Looper.getMainLooper())
        val loginCheckRunnable = Runnable {
            databaseReference.orderByChild("login").equalTo(login).get()
                .addOnSuccessListener { snapshot ->
                    val isAvailable = !snapshot.exists() || snapshot.children.firstOrNull()?.key == currentUid
                    setLoginValidationIcon(isAvailable)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Ошибка проверки логина", Toast.LENGTH_SHORT).show()
                }
        }

        handler.postDelayed(loginCheckRunnable, 300)
    }

    private fun setLoginValidationIcon(isValid: Boolean) {
        val drawableRes = if (isValid) R.drawable.ic_check_done else R.drawable.ic_check_wrong
        val drawable = ContextCompat.getDrawable(this, drawableRes)?.mutate()
        binding.editLogin.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            drawable,
            null
        )
    }

    private fun validatePassword(password: String) {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        val isLengthValid = password.length >= 8
        val isValid = hasUppercase && hasDigit && hasSpecialChar && isLengthValid

        val drawableRes = if (isValid) R.drawable.ic_check_done else R.drawable.ic_check_wrong
        val drawable = ContextCompat.getDrawable(this, drawableRes)?.mutate()
        binding.editPassword.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            drawable,
            null
        )
    }

    private fun isPasswordValid(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        val isLengthValid = password.length >= 8
        return hasUppercase && hasDigit && hasSpecialChar && isLengthValid
    }
}
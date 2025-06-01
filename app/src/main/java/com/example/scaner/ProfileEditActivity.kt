package com.example.scaner

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.scaner.databinding.ActivityProfileEditBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var currentUid: String // UID пользователя вместо логина
    private var avatarUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка цветов статус-бара и навигационной панели
        window.statusBarColor = resources.getColor(android.R.color.white, theme)
        window.navigationBarColor = resources.getColor(android.R.color.white, theme)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        // Получаем данные из Intent
        currentUid = intent.getStringExtra("UID") ?: ""
        avatarUrl = intent.getStringExtra("AVATAR_URL")
        Log.d("ProfileEditActivity", "Текущий UID: $currentUid")
        Log.d("ProfileEditActivity", "URL аватарки: $avatarUrl")

        // Настройка верхнего бара
        binding.backButton.setOnClickListener {
            finish()
        }

        // Загрузка аватарки
        loadAvatar()

        // Обработка нажатия на кнопку "Изменить фото"
        binding.changeAvatarButton.setOnClickListener {
            showEditDialog("URL аватарки") { newAvatarUrl ->
                updateAvatar(newAvatarUrl)
            }
        }

        // Обработка нажатия на кнопку "Сохранить"
        binding.saveButton.setOnClickListener {
            val newLogin = binding.editLogin.text.toString().trim()
            val newPassword = binding.editPassword.text.toString().trim()
            val firstName = binding.editFirstName.text.toString().trim()
            val lastName = binding.editLastName.text.toString().trim()
            val middleName = binding.editMiddleName.text.toString().trim()

            if (newLogin.isNotEmpty() && newPassword.isNotEmpty()) {
                updateUserData(
                    mapOf(
                        "login" to newLogin,
                        "password" to newPassword,
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "middleName" to middleName
                    )
                )
            } else {
                Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
            }
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

    private fun loadAvatar() {
        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.circle_background) // Заполнитель
                .error(R.drawable.error_avatar) // Изображение при ошибке
                .circleCrop()
                .into(binding.avatarImage)
        } else {
            // Если URL пустой, устанавливаем изображение по умолчанию
            Glide.with(this)
                .load(R.drawable.default_avatar)
                .circleCrop()
                .into(binding.avatarImage)
        }
    }

    private fun updateUserData(updatedData: Map<String, String>) {
        databaseReference = FirebaseDatabase.getInstance().getReference("Authorization")

        // Обновляем данные пользователя в базе данных
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
        databaseReference = FirebaseDatabase.getInstance().getReference("Authorization")

        // Обновляем URL аватарки в базе данных
        databaseReference.child(currentUid).child("avatarUrl").setValue(newAvatarUrl)
            .addOnSuccessListener {
                Log.d("ProfileEditActivity", "Аватарка успешно обновлена: $newAvatarUrl")
                avatarUrl = newAvatarUrl // Обновляем URL аватарки
                loadAvatar() // Перезагружаем аватарку
                Toast.makeText(this, "Аватарка успешно изменена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Log.e("ProfileEditActivity", "Ошибка при обновлении аватарки: ${it.message}")
                Toast.makeText(this, "Ошибка при изменении аватарки", Toast.LENGTH_SHORT).show()
            }
    }
}
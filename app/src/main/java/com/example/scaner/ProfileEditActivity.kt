package com.example.scaner

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.scaner.databinding.ActivityProfileEditBinding
import com.example.scaner.databinding.DialogEditTextBinding
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

        window.statusBarColor = resources.getColor(R.color.gradient_1, theme)
        window.navigationBarColor = resources.getColor(android.R.color.white, theme)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        currentUid = intent.getStringExtra("UID") ?: ""
        avatarUrl = intent.getStringExtra("AVATAR_URL")

        loadUserData()

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.editLogin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val login = s.toString().trim()
                if (login.isNotEmpty()) {
                    checkLoginAvailability(login)
                } else {
                    clearLoginValidationIconWithDelay()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.editPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                if (password.isNotEmpty()) {
                    validatePassword(password)
                } else {
                    clearPasswordValidationIconWithDelay()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

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

        binding.changeAvatarButton.setOnClickListener {
            showEditDialog("URL аватарки") { newAvatarUrl ->
                updateAvatar(newAvatarUrl)
            }
        }
        loadAvatar()
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
        val dialogBinding = DialogEditTextBinding.inflate(layoutInflater)
        dialogBinding.dialogTitle.text = "Изменить $field"
        dialogBinding.dialogInput.hint = "Введите $field"
        val builder = AlertDialog.Builder(this, R.style.CustomDialogTheme)
        builder.setView(dialogBinding.root)
        builder.setPositiveButton("Сохранить") { _, _ ->
            val newValue = dialogBinding.dialogInput.text.toString()
            if (newValue.isNotEmpty()) {
                onSave(newValue)
            }
        }
        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
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

    private val handler = Handler(Looper.getMainLooper())
    private var loginCheckRunnable: Runnable? = null
    private fun checkLoginAvailability(login: String) {
        if (login.isEmpty()) {
            clearLoginValidationIconWithDelay()
            return
        }
        loginCheckRunnable?.let { handler.removeCallbacks(it) }
        loginCheckRunnable = Runnable {
            databaseReference.orderByChild("login").equalTo(login).get()
                .addOnSuccessListener { snapshot ->
                    val isAvailable = !snapshot.exists() || snapshot.children.firstOrNull()?.key == currentUid
                    setLoginValidationIcon(isAvailable)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Ошибка проверки логина", Toast.LENGTH_SHORT).show()
                }
        }
        handler.postDelayed(loginCheckRunnable!!, 300)
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
        if (!isValid) {
            TooltipCompat.setTooltipText(
                binding.editLogin,
                "Логин уже используется другим пользователем"
            )
        } else {
            TooltipCompat.setTooltipText(binding.editLogin, null)
        }
    }

    private fun clearLoginValidationIconWithDelay() {
        loginCheckRunnable?.let { handler.removeCallbacks(it) }
        loginCheckRunnable = Runnable {
            binding.editLogin.setCompoundDrawablesWithIntrinsicBounds(
                binding.editLogin.compoundDrawables[0],
                null,
                null,
                null
            )
            TooltipCompat.setTooltipText(binding.editLogin, null)
        }
        handler.postDelayed(loginCheckRunnable!!, 300)
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
        if (!isValid) {
            TooltipCompat.setTooltipText(
                binding.editPassword,
                "Пароль должен состоять из 8 символов и включать в себя цифры, буквы, один спецсимвол и одну букву с заглавной"
            )
        } else {
            TooltipCompat.setTooltipText(binding.editPassword, null)
        }
    }

    private fun clearPasswordValidationIconWithDelay() {
        handler.postDelayed({
            binding.editPassword.setCompoundDrawablesWithIntrinsicBounds(
                binding.editPassword.compoundDrawables[0],
                null,
                null,
                null
            )
            TooltipCompat.setTooltipText(binding.editPassword, null)
        }, 300)
    }

    private fun isPasswordValid(password: String): Boolean {
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        val isLengthValid = password.length >= 8
        return hasUppercase && hasDigit && hasSpecialChar && isLengthValid
    }
}
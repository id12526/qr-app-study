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
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import com.example.scaner.databinding.ActivityRegBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegActivity : AppCompatActivity() {
    private lateinit var databaseReference: DatabaseReference
    private lateinit var binding: ActivityRegBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseReference = FirebaseDatabase.getInstance().getReference("Authorization")

        window.statusBarColor = resources.getColor(R.color.gradient_2, theme)
        window.navigationBarColor = resources.getColor(R.color.gradient_1, theme)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.rootLayout.setOnClickListener {
            currentFocus?.let { view ->
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }

        setupFocusChange(binding.regLogin, R.drawable.ic_person_gradient, R.drawable.ic_person)
        setupFocusChange(binding.regPass, R.drawable.ic_password_gradient, R.drawable.ic_password)
        setupFocusChange(binding.regFirstName, R.drawable.ic_contact_gradient, R.drawable.ic_contact)
        setupFocusChange(binding.regLastName, R.drawable.ic_contact_gradient, R.drawable.ic_contact)
        setupFocusChange(binding.regMiddleName, R.drawable.ic_contact_gradient, R.drawable.ic_contact)

        binding.registerButton.setOnClickListener {
            val login = binding.regLogin.text.toString().trim()
            val password = binding.regPass.text.toString().trim()
            val firstName = binding.regFirstName.text.toString().trim()
            val lastName = binding.regLastName.text.toString().trim()
            val middleName = binding.regMiddleName.text.toString().trim()
            if (login.isNotEmpty() && password.isNotEmpty() && firstName.isNotEmpty() && lastName.isNotEmpty()) {
                registerUser(login, password, firstName, lastName, middleName)
            } else {
                Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
            }
        }

        binding.regPass.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                if (password.isNotEmpty()) {
                    validatePassword(password)
                } else {
                    binding.regPass.setCompoundDrawablesWithIntrinsicBounds(
                        binding.regPass.compoundDrawables[0],
                        null,
                        null,
                        null
                    )
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.regLogin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val login = s.toString().trim()
                if (login.isNotEmpty()) {
                    checkLoginAvailability(login)
                } else {
                    binding.regLogin.setCompoundDrawablesWithIntrinsicBounds(
                        binding.regLogin.compoundDrawables[0],
                        null,
                        ContextCompat.getDrawable(this@RegActivity, R.drawable.ic_null),
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
        if (login.isEmpty()) {
            binding.regLogin.setCompoundDrawablesWithIntrinsicBounds(
                binding.regLogin.compoundDrawables[0], // Левая иконка
                null,
                ContextCompat.getDrawable(this@RegActivity, R.drawable.ic_null), // Правая иконка
                null
            )
            return
        }
        loginCheckRunnable?.let { handler.removeCallbacks(it) }
        loginCheckRunnable = Runnable {
            databaseReference.orderByChild("login").equalTo(login).get()
                .addOnSuccessListener { snapshot ->
                    val drawableRes = if (snapshot.exists()) R.drawable.ic_check_wrong else R.drawable.ic_check_done
                    val drawable = ContextCompat.getDrawable(this@RegActivity, drawableRes)?.mutate()
                    runOnUiThread {
                        val currentDrawables = binding.regLogin.compoundDrawables
                        if (currentDrawables != null && binding.regLogin.text.toString().trim() == login) {
                            binding.regLogin.setCompoundDrawablesWithIntrinsicBounds(
                                currentDrawables[0],
                                null,
                                drawable,
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
        val currentDrawables = binding.regPass.compoundDrawables
        if (currentDrawables != null) {
            binding.regPass.setCompoundDrawablesWithIntrinsicBounds(
                currentDrawables[0],
                null,
                drawable,
                null
            )
            if (drawable?.constantState == ContextCompat.getDrawable(this@RegActivity, R.drawable.ic_check_wrong)?.constantState) {
                TooltipCompat.setTooltipText(binding.regPass, "Пароль должен состоять из 8 символов и включать в себя цифры, буквы, один спецсимвол и одну букву с заглавной")
                binding.regPass.performLongClick()
            }
        }
    }

    private fun setupFocusChange(editText: EditText, focusedDrawableRes: Int, defaultDrawableRes: Int) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            val currentDrawables = editText.compoundDrawables
            val newLeftDrawable = ContextCompat.getDrawable(this, if (hasFocus) focusedDrawableRes else defaultDrawableRes)?.mutate()
            editText.setCompoundDrawablesWithIntrinsicBounds(
                newLeftDrawable,
                currentDrawables[1],
                currentDrawables[2],
                currentDrawables[3]
            )
        }
    }

    private fun registerUser(login: String, password: String, firstName: String, lastName: String, middleName: String) {
        if (!isPasswordValid(password)) {
            Toast.makeText(this, "Пароль не соответствует требованиям", Toast.LENGTH_SHORT).show()
            return
        }
        databaseReference.get().addOnSuccessListener { snapshot ->
            val existingUIDs = mutableListOf<Long>()
            for (userSnapshot in snapshot.children) {
                val uidKey = userSnapshot.key ?: continue
                val uidNumber = uidKey.replace("uid", "").toLongOrNull() ?: continue
                existingUIDs.add(uidNumber)
            }
            existingUIDs.sort()
            var newUIDNumber = 1L
            for (uid in existingUIDs) {
                if (uid != newUIDNumber) {
                    break
                }
                newUIDNumber++
            }
            val newUID = "uid$newUIDNumber"
            val userData = mapOf(
                "login" to login,
                "password" to password,
                "role" to "user",
                "avatarUrl" to "",
                "firstName" to firstName,
                "lastName" to lastName,
                "middleName" to middleName
            )
            databaseReference.orderByChild("login").equalTo(login).get().addOnSuccessListener { loginSnapshot ->
                if (loginSnapshot.exists()) {
                    Toast.makeText(this, "Пользователь с таким логином уже существует", Toast.LENGTH_SHORT).show()
                } else {
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
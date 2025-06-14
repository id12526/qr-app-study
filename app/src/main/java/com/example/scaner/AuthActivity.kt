package com.example.scaner

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.method.PasswordTransformationMethod
import android.text.method.HideReturnsTransformationMethod
import android.content.SharedPreferences
import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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

        window.statusBarColor = resources.getColor(R.color.gradient_2, theme)
        window.navigationBarColor = resources.getColor(R.color.gradient_1, theme)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        binding.rootLayout.setOnClickListener {
            currentFocus?.let { view ->
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                view.clearFocus()
            }
        }

        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)

        binding.rememberMe.buttonTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.gradient_1)
        )

        val savedLogin = sharedPreferences.getString("LOGIN", "")
        val savedPassword = sharedPreferences.getString("PASSWORD", "")
        if (savedLogin != null && savedPassword != null) {
            binding.login.setText(savedLogin)
            binding.pass.setText(savedPassword)
            binding.rememberMe.isChecked = true
        }

        setupFocusChange(binding.login, R.drawable.ic_person_gradient, R.drawable.ic_person)
        setupFocusChange(binding.pass, R.drawable.ic_password_gradient, R.drawable.ic_password)

        var isPasswordVisible = false
        binding.pass.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                if (event.rawX >= binding.pass.right - binding.pass.compoundDrawables[2].bounds.width()) {
                    isPasswordVisible = !isPasswordVisible
                    updatePasswordVisibility(binding.pass, isPasswordVisible)
                    return@setOnTouchListener true
                }
            }
            return@setOnTouchListener false
        }

        binding.registerLink.setOnClickListener {
            val intent = Intent(this@AuthActivity, RegActivity::class.java)
            startActivity(intent)
        }

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

    private fun updatePasswordVisibility(editText: EditText, isVisible: Boolean) {
        if (isVisible) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            editText.setCompoundDrawablesWithIntrinsicBounds(
                editText.compoundDrawables[0],
                null,
                getDrawable(R.drawable.ic_eye_on),
                null
            )
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            editText.setCompoundDrawablesWithIntrinsicBounds(
                editText.compoundDrawables[0],
                null,
                getDrawable(R.drawable.ic_eye_off),
                null
            )
        }
        editText.setSelection(editText.text.length)
    }

    private fun signIn(login: String, passInput: String) {
        databaseReference = FirebaseDatabase.getInstance().getReference("Authorization")
        databaseReference.orderByChild("login").equalTo(login).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val uid = userSnapshot.key
                        val password = userSnapshot.child("password").value
                        val role = userSnapshot.child("role").value
                        val avatarUrl = userSnapshot.child("avatarUrl").value
                        if (password.toString() == passInput) {
                            if (binding.rememberMe.isChecked) {
                                saveUserData(login, passInput)
                            } else {
                                clearUserData()
                            }
                            val intent = Intent(this@AuthActivity, MainActivity::class.java).apply {
                                putExtra("ROLE", role.toString())
                                putExtra("AVATAR_URL", avatarUrl?.toString())
                                putExtra("UID", uid)
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
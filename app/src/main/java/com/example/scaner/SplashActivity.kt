package com.example.scaner

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class SplashActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        window.statusBarColor = resources.getColor(android.R.color.white, theme)
        window.navigationBarColor = resources.getColor(android.R.color.white, theme)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)

        // Проверка сохраненных данных
        val savedLogin = sharedPreferences.getString("LOGIN", "")
        val savedPassword = sharedPreferences.getString("PASSWORD", "")

        // Задержка для демонстрации загрузки
        Handler(Looper.getMainLooper()).postDelayed({
            if (!savedLogin.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                // Если данные сохранены, проверяем их в базе данных
                checkSavedCredentials(savedLogin, savedPassword)
            } else {
                // Если данных нет, переходим на экран авторизации
                navigateToAuthActivity()
            }
        }, 1000) // delay
    }

    private fun checkSavedCredentials(login: String, password: String) {
        databaseReference = FirebaseDatabase.getInstance().getReference("Authorization")
        databaseReference.orderByChild("login").equalTo(login).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val uid = userSnapshot.key
                        val storedPassword = userSnapshot.child("password").value
                        val role = userSnapshot.child("role").value
                        val avatarUrl = userSnapshot.child("avatarUrl").value

                        if (storedPassword.toString() == password) {
                            // Переход на MainActivity с данными пользователя
                            val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                                putExtra("ROLE", role.toString())
                                putExtra("AVATAR_URL", avatarUrl?.toString())
                                putExtra("UID", uid) // Передаем UID вместо логина
                            }
                            startActivity(intent)
                            finish()
                            return
                        }
                    }
                }
                // Если данные неверны, переходим на экран авторизации
                navigateToAuthActivity()
            }

            override fun onCancelled(error: DatabaseError) {
                // Если произошла ошибка, переходим на экран авторизации
                navigateToAuthActivity()
            }
        })
    }

    private fun navigateToAuthActivity() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }
}
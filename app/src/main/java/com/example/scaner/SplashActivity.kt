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

        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE)

        val savedLogin = sharedPreferences.getString("LOGIN", "")
        val savedPassword = sharedPreferences.getString("PASSWORD", "")

        Handler(Looper.getMainLooper()).postDelayed({
            if (!savedLogin.isNullOrEmpty() && !savedPassword.isNullOrEmpty()) {
                checkSavedCredentials(savedLogin, savedPassword)
            } else {
                navigateToAuthActivity()
            }
        }, 1000)
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
                            val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                                putExtra("ROLE", role.toString())
                                putExtra("AVATAR_URL", avatarUrl?.toString())
                                putExtra("UID", uid)
                            }
                            startActivity(intent)
                            finish()
                            return
                        }
                    }
                }
                navigateToAuthActivity()
            }
            override fun onCancelled(error: DatabaseError) {
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
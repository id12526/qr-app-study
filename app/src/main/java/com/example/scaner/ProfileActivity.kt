package com.example.scaner

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.example.scaner.databinding.ActivityProfileBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var currentUid: String
    private var avatarUrl: String? = null
    private var firstName: String? = null
    private var lastName: String? = null
    private var middleName: String? = null
    private var login: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)


        window.statusBarColor = resources.getColor(R.color.gradient_1, theme)
        window.navigationBarColor = resources.getColor(android.R.color.white, theme)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )


        currentUid = intent.getStringExtra("UID") ?: ""
        Log.d("ProfileActivity", "Текущий UID: $currentUid")


        binding.backButton.setOnClickListener {
            finish()
        }


        loadUserData()


        binding.editProfileButton.setOnClickListener {
            val intent = Intent(this, ProfileEditActivity::class.java).apply {
                putExtra("UID", currentUid)
                putExtra("AVATAR_URL", avatarUrl)
            }
            startActivity(intent)
        }


        binding.deleteAccountButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun loadUserData() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Authorization/$currentUid")
        databaseReference.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                firstName = snapshot.child("firstName").getValue(String::class.java)
                lastName = snapshot.child("lastName").getValue(String::class.java)
                middleName = snapshot.child("middleName").getValue(String::class.java)
                login = snapshot.child("login").getValue(String::class.java)
                avatarUrl = snapshot.child("avatarUrl").getValue(String::class.java)

                binding.userName.text = "$lastName $firstName ${middleName ?: ""}".trim()
                binding.userLogin.text = "@$login"
                loadAvatar()
            } else {
                Log.e("ProfileActivity", "Данные пользователя не найдены")
            }
        }.addOnFailureListener {
            Log.e("ProfileActivity", "Ошибка загрузки данных пользователя: ${it.message}")
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

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Подтвердите удаление")
            .setMessage("Вы уверены, что хотите удалить свой аккаунт? Это действие нельзя отменить.")
            .setPositiveButton("Да, удалить мой аккаунт") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteAccount() {
        databaseReference.removeValue().addOnSuccessListener {
            Log.d("ProfileActivity", "Аккаунт успешно удален")
            finish() 
        }.addOnFailureListener {
            Log.e("ProfileActivity", "Ошибка при удалении аккаунта: ${it.message}")
        }
    }
}
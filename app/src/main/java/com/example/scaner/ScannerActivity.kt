package com.example.scaner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.scaner.databinding.CustomScannerLayoutBinding
import com.google.firebase.database.FirebaseDatabase
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult

class ScannerActivity : AppCompatActivity() {
    private lateinit var binding: CustomScannerLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = CustomScannerLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }

        window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )

        binding.backButton.setOnClickListener {
            finish()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            initScanner()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            initScanner()
        } else {
            Toast.makeText(this, "Камера недоступна", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initScanner() {
        binding.barcodeScannerView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                val qrCode = result?.text ?: return

                val databaseReference = FirebaseDatabase.getInstance().getReference("Products")
                databaseReference.get().addOnSuccessListener { snapshot ->
                    var productFound = false

                    for (category in snapshot.children) { // Категории
                        for (productSnapshot in category.children) { // Продукты внутри категории
                            val qr = productSnapshot.child("qr").value?.toString()
                            if (qr == qrCode) {
                                val itemName = productSnapshot.child("name").value?.toString()

                                val intent = Intent(this@ScannerActivity, ListDetailActivity::class.java)
                                intent.putExtra("ITEM_NAME", itemName) // Передаем ITEM_NAME
                                intent.putExtra("CATEGORY_NAME", category.key) // Передаем CATEGORY_NAME (ключ категории)
                                startActivity(intent)

                                productFound = true
                                break
                            }
                        }
                        if (productFound) break
                    }
                    if (!productFound) {
                        Toast.makeText(this@ScannerActivity, "Неверный код", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this@ScannerActivity, "Ошибка загрузки данных: ${it.message}", Toast.LENGTH_SHORT).show()
                }

                binding.barcodeScannerView.pause()
            }
            override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        binding.barcodeScannerView.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScannerView.pause()
    }
}

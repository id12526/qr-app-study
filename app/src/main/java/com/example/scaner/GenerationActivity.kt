package com.example.scaner

import android.content.ContentValues
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.*
import com.example.scaner.databinding.ActivityGenerationBinding
import java.io.File
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import java.io.FileInputStream

class GenerationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGenerationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGenerationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = resources.getColor(android.R.color.white, theme)
        window.navigationBarColor = resources.getColor(android.R.color.white, theme)

        window.decorView.systemUiVisibility = (
                window.decorView.systemUiVisibility or
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                )

        val backButton = findViewById<ImageButton>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        val generateButton = findViewById<Button>(R.id.generateButton)
        generateButton.setOnClickListener {
            generateQRCode()
        }

        binding.saveButton.setOnClickListener {
            val qrCodeBitmap = binding.qrCodeImageView.drawable?.toBitmap()
            if (qrCodeBitmap != null) {
                saveQRCodeToGallery(qrCodeBitmap)
            } else {
                Toast.makeText(this, "QR-код не найден", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateQRCode() {
        // Получаем текст из EditText
        val qrText = binding.qrText.text.toString().trim()

        if (qrText.isEmpty()) {
            Toast.makeText(this, "Введите текст для QR-кода", Toast.LENGTH_SHORT).show()
            return
        }

        // Генерация QR-кода
        val qrCodeBitmap = generateQRCodeBitmap(qrText, 500, 500)
        if (qrCodeBitmap != null) {
            // Отображаем QR-код в ImageView
            binding.qrCodeImageView.setImageBitmap(qrCodeBitmap)
            binding.qrCodeImageView.visibility = View.VISIBLE

            binding.saveButton.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "Ошибка при генерации QR-кода", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateQRCodeBitmap(text: String, width: Int, height: Int): Bitmap? {
        val hints = Hashtable<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = 1 // Уменьшаем отступы

        try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
            )

            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            return bmp
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun saveQRCodeToGallery(bitmap: Bitmap) {
        val fileName = "QRCode_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            uri?.let {
                val outputStream = resolver.openOutputStream(it)
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                outputStream?.close()
                Toast.makeText(this, "QR-код успешно сохранен", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка при сохранении QR-кода", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val SAVE_QR_CODE_REQUEST_CODE = 1001
        private var tempFilePath: String? = null // Путь к временному файлу
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SAVE_QR_CODE_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    // Копируем временный файл в выбранное место
                    val inputStream = FileInputStream(File(tempFilePath))
                    val outputStream = contentResolver.openOutputStream(uri)

                    inputStream.copyTo(outputStream!!)
                    inputStream.close()
                    outputStream.close()

                    Toast.makeText(this, "QR-код успешно сохранен", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Ошибка при сохранении QR-кода", Toast.LENGTH_SHORT).show()
                } finally {
                    // Удаляем временный файл
                    File(tempFilePath).delete()
                    tempFilePath = null
                }
            }
        }
    }
}
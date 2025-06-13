package com.example.scaner

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.scaner.databinding.ActivityListDetailBinding
import com.google.firebase.database.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.*

class ListDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListDetailBinding
    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = resources.getColor(R.color.gradient_1, theme)
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

        val itemName = intent.getStringExtra("ITEM_NAME")
        val categoryName = intent.getStringExtra("CATEGORY_NAME")

        if (itemName == null || categoryName == null) {
            Toast.makeText(this, "Ошибка передачи данных", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("Products/$categoryName/$itemName")

        loadItemDetails()

        binding.showQrButton.setOnClickListener {
            showQRCodeDialog()
        }

        binding.exportPdfButton.setOnClickListener {
            generateAndSavePDF()
        }
    }

    private fun loadItemDetails() {
        databaseReference.addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: "Нет названия"
                        val description = snapshot.child("desc").getValue(String::class.java) ?: "Нет описания"
                        val id = snapshot.child("id").getValue(String::class.java) ?: "Нет номера"
                        val qr = snapshot.child("qr").getValue(String::class.java) ?: "Нет кода"
                        binding.itemName.text = name
                        binding.itemDescription.text = description
                        binding.itemId.text = "Серийный номер: $id"
                        binding.itemQr.text = "QR-код: $qr"
                    } else {
                        Toast.makeText(this@ListDetailActivity, "Объект не найден", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ListDetailActivity, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showQRCodeDialog() {
        val qrText = binding.itemQr.text.toString().replace("QR-код: ", "")
        if (qrText.isEmpty()) {
            Toast.makeText(this, "QR-код не найден", Toast.LENGTH_SHORT).show()
            return
        }

        val qrCodeBitmap = generateQRCodeBitmap(qrText, 500, 500)
        if (qrCodeBitmap != null) {
            val dialogView = ImageView(this).apply {
                setImageBitmap(qrCodeBitmap)
                adjustViewBounds = true
                setPadding(16, 16, 16, 16)
            }

            AlertDialog.Builder(this)
                .setTitle("QR-код")
                .setView(dialogView)
                .setPositiveButton("Закрыть") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            Toast.makeText(this, "Ошибка при генерации QR-кода", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateQRCodeBitmap(text: String, width: Int, height: Int): Bitmap? {
        val hints = Hashtable<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = 1

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

    private fun generateAndSavePDF() {
        val itemName = binding.itemName.text.toString()
        val itemDescription = binding.itemDescription.text.toString()
        val itemId = binding.itemId.text.toString()
        val itemQr = binding.itemQr.text.toString()

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 размер (в пикселях)
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = android.graphics.Paint().apply {
            textSize = 16f
            color = android.graphics.Color.BLACK
        }

        var yPosition = 50f
        val pageWidth = 595f
        val margin = 50f

        fun drawMultilineText(text: String) {
            val maxWidth = pageWidth - 2 * margin
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = ""

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = testLine
                } else {
                    lines.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine)
            }

            for (line in lines) {
                canvas.drawText(line, margin, yPosition, paint)
                yPosition += 30f
            }
        }

        canvas.drawText("Название:", margin, yPosition, paint)
        yPosition += 30f
        drawMultilineText(itemName)

        yPosition += 30f
        canvas.drawText("Описание:", margin, yPosition, paint)
        yPosition += 30f
        drawMultilineText(itemDescription)

        yPosition += 30f
        canvas.drawText(itemId, margin, yPosition, paint)

        yPosition += 30f
        canvas.drawText(itemQr, margin, yPosition, paint)

        pdfDocument.finishPage(page)

        val fileName = "ItemDetails_${System.currentTimeMillis()}.pdf"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }

        val resolver = contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        try {
            uri?.let {
                val outputStream = resolver.openOutputStream(it)
                pdfDocument.writeTo(outputStream)
                outputStream?.close()
                Toast.makeText(this, "PDF успешно сохранен", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Ошибка при сохранении PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
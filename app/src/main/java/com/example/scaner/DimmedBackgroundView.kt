package com.example.scaner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class DimmedBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dimmedPaint = Paint().apply {
        color = 0x90000000.toInt()
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint().apply {
        color = Color.TRANSPARENT
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimmedPaint)
        val overlayWidth = 775f
        val overlayHeight = 775f
        val left = (width - overlayWidth) / 2
        val top = (height - overlayHeight) / 2
        val right = left + overlayWidth
        val bottom = top + overlayHeight
        val cornerRadius = 24f
        val path = Path()
        path.addRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.drawPath(path, clearPaint)
        canvas.restoreToCount(layer)
    }
}
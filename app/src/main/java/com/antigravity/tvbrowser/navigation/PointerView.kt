package com.antigravity.tvbrowser.navigation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class PointerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var pointerX: Float = 400f
        private set
    var pointerY: Float = 300f
        private set

    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5722")
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80FF5722")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val pointerPath = Path()
    private var isClicking: Boolean = false
    var isPointerVisible: Boolean = true
        set(value) {
            field = value
            visibility = if (value) VISIBLE else GONE
            invalidate()
        }

    fun updatePosition(x: Float, y: Float) {
        pointerX = x.coerceIn(0f, width.toFloat())
        pointerY = y.coerceIn(0f, height.toFloat())
        invalidate()
    }

    fun setClickingState(clicking: Boolean) {
        isClicking = clicking
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isPointerVisible) return

        canvas.save()
        canvas.translate(pointerX, pointerY)

        // Draw Click Ripple Ring when active
        if (isClicking) {
            canvas.drawCircle(0f, 0f, 24f, ringPaint)
        }

        // Draw Cursor Arrow Path
        pointerPath.reset()
        pointerPath.moveTo(0f, 0f)
        pointerPath.lineTo(0f, 32f)
        pointerPath.lineTo(8f, 24f)
        pointerPath.lineTo(16f, 32f)
        pointerPath.lineTo(20f, 28f)
        pointerPath.lineTo(12f, 20f)
        pointerPath.lineTo(22f, 20f)
        pointerPath.close()

        canvas.drawPath(pointerPath, pointerPaint)
        canvas.drawPath(pointerPath, strokePaint)

        canvas.restore()
    }
}

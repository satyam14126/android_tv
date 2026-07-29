package com.antigravity.tvbrowser.navigation

import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.webkit.WebView

class VirtualCursorController(
    private val pointerView: PointerView,
    private val webView: WebView
) {

    private val focusSnapper = MagneticFocusSnapper(webView)

    var isCursorModeEnabled: Boolean = true
        set(value) {
            field = value
            pointerView.isPointerVisible = value
        }

    private var moveSpeed = 16f
    private val maxSpeed = 48f
    private var accelStep = 2f

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (!isCursorModeEnabled) return false

        val keyCode = event.keyCode
        val action = event.action

        if (action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    moveCursor(0f, -moveSpeed)
                    accelerate()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    moveCursor(0f, moveSpeed)
                    accelerate()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    moveCursor(-moveSpeed, 0f)
                    accelerate()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    moveCursor(moveSpeed, 0f)
                    accelerate()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    pointerView.setClickingState(true)
                    performClickAtCursor()
                    return true
                }
                KeyEvent.KEYCODE_PAGE_UP -> {
                    webView.scrollBy(0, -400)
                    return true
                }
                KeyEvent.KEYCODE_PAGE_DOWN -> {
                    webView.scrollBy(0, 400)
                    return true
                }
            }
        } else if (action == KeyEvent.ACTION_UP) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    resetAcceleration()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    pointerView.setClickingState(false)
                    return true
                }
            }
        }

        return false
    }

    private fun moveCursor(dx: Float, dy: Float) {
        val newX = pointerView.pointerX + dx
        val newY = pointerView.pointerY + dy

        pointerView.updatePosition(newX, newY)

        // Web View edge auto-scrolling
        if (newY <= 20f) {
            webView.scrollBy(0, -100)
        } else if (newY >= pointerView.height - 20f) {
            webView.scrollBy(0, 100)
        }

        // Check magnetic element snapping
        focusSnapper.checkMagneticSnap(newX, newY, object : MagneticFocusSnapper.SnapCallback {
            override fun onSnap(targetX: Float, targetY: Float) {
                pointerView.updatePosition(targetX, targetY)
            }
        })
    }

    private fun accelerate() {
        if (moveSpeed < maxSpeed) {
            moveSpeed += accelStep
        }
    }

    private fun resetAcceleration() {
        moveSpeed = 16f
    }

    fun performClickAtCursor() {
        val x = pointerView.pointerX
        val y = pointerView.pointerY
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()

        val downEvent = MotionEvent.obtain(
            downTime, eventTime, MotionEvent.ACTION_DOWN,
            x, y, 0
        )
        val upEvent = MotionEvent.obtain(
            downTime, eventTime + 100, MotionEvent.ACTION_UP,
            x, y, 0
        )

        webView.dispatchTouchEvent(downEvent)
        webView.dispatchTouchEvent(upEvent)

        downEvent.recycle()
        upEvent.recycle()
    }
}

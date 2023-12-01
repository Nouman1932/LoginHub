package com.example.tnc

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.appcompat.widget.AppCompatEditText

class PasswordToggleEditText : AppCompatEditText {
    private var passwordVisible = false
    private var toggleDrawable: Drawable? = null
    private var cursorStart: Int = 0
    private var cursorEnd: Int = 0

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        toggleDrawable = compoundDrawablesRelative[2] // Assuming the drawable is at the right

        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP && toggleDrawable != null) {
                if (event.rawX >= (right - paddingRight - toggleDrawable!!.intrinsicWidth)) {
                    togglePasswordVisibility()
                    performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }
        ViewCompat.setBackground(this, ResourcesCompat.getDrawable(resources, R.drawable.edittext_border_selector, null))
        updateToggleIcon()
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
        cursorStart = selectionStart
        cursorEnd = selectionEnd
        transformationMethod =
            if (passwordVisible) null else PasswordTransformationMethod.getInstance()
        updateToggleIcon()
        restoreCursorPosition()
    }

    private fun restoreCursorPosition() {
        setSelection(cursorStart, cursorEnd)
    }

    private fun updateToggleIcon() {
        toggleDrawable?.let {
            val drawable = if (passwordVisible) {
                AppCompatResources.getDrawable(context, R.drawable.baseline_visibility_24)
            } else {
                AppCompatResources.getDrawable(context, R.drawable.baseline_disabled_visible_24)
            }
            drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
        }
    }
}

package com.example.tnc

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatEditText

class PasswordToggleEditText : AppCompatEditText {
    private var passwordVisible = false
    private var toggleDrawable: Drawable? = null

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
                if (event.x >= (right - paddingRight - toggleDrawable!!.intrinsicWidth)) {
                    togglePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }
        updateToggleIcon()
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
        transformationMethod =
            if (passwordVisible) null else PasswordTransformationMethod.getInstance()
        updateToggleIcon()
    }

    private fun updateToggleIcon() {
        toggleDrawable?.let {
            val drawable = if (passwordVisible) {
                resources.getDrawable(R.drawable.baseline_visibility_24, null)
            } else {
                resources.getDrawable(R.drawable.baseline_disabled_visible_24, null)
            }
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null)
        }
    }
}
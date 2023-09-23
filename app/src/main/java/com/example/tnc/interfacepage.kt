package com.example.tnc

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText

class interfacepage : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interfacepage)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        emailEditText.addTextChangedListener(TextValidationWatcher(emailEditText) { text ->
            val isValid = isValidEmail(text)
            isValid // Return the validation result as a Boolean
        })
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        passwordEditText.addTextChangedListener(TextValidationWatcher(passwordEditText) { text ->
            text.length >= 6
        })
    }
    fun isValidEmail(email: String): Boolean {
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }

}
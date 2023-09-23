package com.example.tnc
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class TextValidationWatcher(private val editText: EditText, private val validationFunction: (String) -> Boolean) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(editable: Editable?) {
        val text = editable.toString().trim()
        if (validationFunction(text)) {
            editText.setBackgroundResource(R.drawable.edittext_border_selector)
        } else {
            editText.setBackgroundResource(R.drawable.edittext_border_error)
        }
    }
}

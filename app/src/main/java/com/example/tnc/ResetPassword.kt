package com.example.tnc

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth

class ResetPassword : AppCompatActivity() {


    private lateinit var authHelper: AuthHelper
    private lateinit var emailEditText: EditText
    private lateinit var resetButton: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var builder: AlertDialog.Builder
    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)
        authHelper = AuthHelper(this)

        val backToLoginTextView: TextView = findViewById(R.id.backToLogin)
        backToLoginTextView.setOnClickListener {
            // Handle the click event, navigate back to the login screen
            backToLogin()
        }


        emailEditText = findViewById(R.id.emailEditText)
        resetButton = findViewById(R.id.resetButton)
        firebaseAuth = FirebaseAuth.getInstance()
        builder = AlertDialog.Builder(this, R.style.MyAlertDialog)

        val googleSignInButton = findViewById<Button>(R.id.rpgoogleSignInButton)
        val fbLoginButton = findViewById<Button>(R.id.rpfbLoginBtn)


        googleSignInButton.setOnClickListener {
            authHelper.signInWithGoogle()
        }

        fbLoginButton.setOnClickListener {
            authHelper.signInWithFacebook()
        }

        //Loading
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Sending Link")
        progressDialog.setMessage("Please wait while we send the link...")
        progressDialog.setCancelable(false) // Prevent dismissing by tapping outside

        resetButton.setOnClickListener {
            resetEmail()
        }

    }
    fun backToLogin(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
    fun backToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
    fun goToSignUp(view: View) {
        val intent = Intent(this, SignUp::class.java)
        startActivity(intent)
    }
    private fun resetEmail() {
        val email = emailEditText.text.toString().trim()
        if (email.isEmpty()) {
                builder.setTitle("Please enter your email address.")
                builder.setPositiveButton("OK", null)
                builder.show()
        }
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            builder.setTitle("Please enter a valid email address.")
            builder.setPositiveButton("OK", null)
            builder.show()
        } else {
            progressDialog.show()
            firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Password reset email sent successfully
                        progressDialog.dismiss()
                        builder.setTitle("Please check your email for the reset link.")
                        builder.setPositiveButton("OK", null)
                        builder.show()
                        emailEditText.text.clear()
                    } else {
                        progressDialog.dismiss()
                        // Password reset email sending failed
                        builder.setTitle("Please register your email first!")
                        builder.setPositiveButton("OK", null)
                        builder.show()
                    }
                }
        }
    }
} // End
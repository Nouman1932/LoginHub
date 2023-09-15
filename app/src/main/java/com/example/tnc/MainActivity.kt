package com.example.tnc

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {

    private lateinit var authHelper: AuthHelper
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var progressDialog: ProgressDialog
    private lateinit var builder: AlertDialog.Builder
    private lateinit var auth: FirebaseAuth



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        authHelper = AuthHelper(this)

        builder = AlertDialog.Builder(this, R.style.MyAlertDialog)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Logging In")
        progressDialog.setMessage("Please wait...")
        progressDialog.setCancelable(false)

        auth = FirebaseAuth.getInstance()

        val loginButton = findViewById<Button>(R.id.loginButton)
        val googleSignInButton = findViewById<Button>(R.id.googleSignInButton)
        val fbLoginButton = findViewById<Button>(R.id.fbLoginBtn)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signIn(email, password)
            } else {
                builder.setTitle("Please enter both your email and password to continue.")
                builder.setPositiveButton("OK", null)
                builder.show()
            }
        }

        googleSignInButton.setOnClickListener {
            authHelper.signInWithGoogle()
        }

        fbLoginButton.setOnClickListener {
            authHelper.signInWithFacebook()
        }

        // Check if the user is already logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            navigateToProfilePage()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        authHelper.onActivityResult(requestCode, resultCode, data)
    }

    private fun signIn(email: String, password: String) {
        progressDialog.show()
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    progressDialog.dismiss()
                    navigateToProfilePage()
                } else {
                    progressDialog.dismiss()
                    builder.setTitle("User Authentication failed..")
//                    builder.setMessage("There was an error in login. Please try again.")
                    builder.setPositiveButton("OK", null)
                    builder.show()
//                    Toast.makeText(this, "User Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToProfilePage() {
        // Create an Intent object
        val intent = Intent(this, HomeActivity::class.java)
        // Start the WelcomeFragment
        startActivity(intent)
    }
    fun goToReset(view: View) {
        val intent = Intent(this, ResetPassword ::class.java)
        startActivity(intent)
    }
    fun goToSignUp(view: View) {
        val intent = Intent(this, SignUp::class.java)
        startActivity(intent)
    }


}
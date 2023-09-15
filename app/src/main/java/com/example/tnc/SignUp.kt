// Signup Data

package com.example.tnc

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SignUp : AppCompatActivity(){
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var fnameEditText: EditText
    private lateinit var lnameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var registerBtn: Button
    private lateinit var profileImageView: ShapeableImageView
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        //Loading
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Uploading")
        progressDialog.setMessage("Please wait...")
        progressDialog.setCancelable(false) // Prevent dismissing by tapping outside

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        fnameEditText = findViewById(R.id.firstNameEditText)
        lnameEditText = findViewById(R.id.lastNameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        phoneEditText = findViewById(R.id.phoneEditText)

        profileImageView = findViewById(R.id.profileImageView)
        profileImageView.setOnClickListener {
            selectImage()
        }
        registerBtn = findViewById(R.id.buttonRegister)
        registerBtn.setOnClickListener {
            saveUserData()
        }
    }//End oncreate
    private fun saveUserData() {
        val fName = fnameEditText.text.toString().trim()
        val lName = lnameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()
        // Check Email format
        if (fName.isEmpty()) {
            Toast.makeText(this, "Please enter your First name", Toast.LENGTH_SHORT).show()
            return
        }
        if (lName.isEmpty()) {
            Toast.makeText(this, "Please enter your Last name", Toast.LENGTH_SHORT).show()
            return
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "Please enter your PhoneNo", Toast.LENGTH_SHORT).show()
            return
        }
        if (!phone.matches(Regex("^\\d{11}$"))) {
            // Generate a toast message for an invalid phone number format
            Toast.makeText(this, "Please enter a valid 11-digit phone number", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your Password", Toast.LENGTH_SHORT).show()
            return
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your Email", Toast.LENGTH_SHORT).show()
            return
        }
        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please enter your ConfirmPassword", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isEmailValid(email)) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT)
                .show()
            passwordEditText.requestFocus()
            return
        }
        // check Match Password
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            passwordEditText.requestFocus()
            return
        }

        // Check if email already exists in Firestore
        checkEmailExists(email)

        // Create a unique filename for the image
        val imageFileName = "${auth.currentUser?.uid}_${System.currentTimeMillis()}.jpg"
        // Get a reference to the profile image storage location
        val profileImageRef = storage.reference.child("profile_images/$imageFileName")
        // Upload the selected image
        val selectedImageUri = profileImageView.tag as Uri? // Tag holds the selected image URI
        progressDialog.show()

        if (selectedImageUri != null) {
            val uploadTask = profileImageRef.putFile(selectedImageUri)
            uploadTask.addOnSuccessListener { taskSnapshot ->
                // Image upload successful, get the download URL
                profileImageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()

                    // Continue with user registration and profile data storage
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                // Registration successful
                                progressDialog.dismiss()
                                Toast.makeText(
                                    this,
                                    "Account Created successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val userID = FirebaseAuth.getInstance().currentUser?.uid
                                val documentReference = firestore.collection("UserProfileData")
                                    .document(userID.toString())
                                val user = mutableMapOf<String, Any>()
                                user["firstname"] = fName
                                user["lastname"] = lName
                                user["email"] = email
                                user["phone"] = phone
                                user["profile_image_url"] = imageUrl // Store the image URL
                                documentReference.set(user).addOnSuccessListener {
                                    Log.d(ContentValues.TAG, "User profile is created for $userID")
                                }.addOnFailureListener {
                                    Log.d(
                                        ContentValues.TAG,
                                        "Failed to Created"
                                    )
                                }
                                navigateToLoginPage()
                            } else {
                                progressDialog.dismiss()
                                // Registration failed
                                val errorMessage = task.exception?.message ?: "Registration failed"
                                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
            uploadTask.addOnFailureListener { exception: Exception ->
                progressDialog.dismiss()
                // Handle image upload failure
                Toast.makeText(this, "Failed to upload profile image", Toast.LENGTH_SHORT).show()

            }
        } else {
            progressDialog.dismiss()
            // Handle the case where no image was selected
            Toast.makeText(this, "Please select a profile image", Toast.LENGTH_SHORT).show()

        }
    }
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                profileImageView.setImageURI(uri)
                profileImageView.tag = uri // Store the selected image URI as a tag
            }
        }
    }
    // ... (isEmailValid, selectImage, resultLauncher, clearFields, backToLogin)
    private fun isEmailValid(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }
    private fun navigateToLoginPage() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close the SignUp activity to prevent going back to it.
    }
    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"  // Set the MIME type to restrict to images only
        resultLauncher.launch(intent)
    }// End selectImage
    private fun clearFields() {
        fnameEditText.text.clear()
        lnameEditText.text.clear()
        emailEditText.text.clear()
        passwordEditText.text.clear()
        confirmPasswordEditText.text.clear()
        phoneEditText.text.clear()
    }
    private fun checkEmailExists(email: String) {
        firestore.collection("UserProfileData")
            .whereEqualTo("email", email)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (!task.result.isEmpty) {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    progressDialog.dismiss()
                    // Error occurred while checking email existence
                    Toast.makeText(this, "Error checking email existence", Toast.LENGTH_SHORT).show()
                }
            }
    }
    fun backToLogin(view: View) {
        val intent = Intent(this, MainActivity ::class.java)
        startActivity(intent)
    }

}//end

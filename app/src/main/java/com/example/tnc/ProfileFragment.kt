// Profile Faragment 

package com.example.tnc

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.facebook.AccessToken
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.appevents.AppEventsLogger
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.json.JSONObject

class ProfileFragment : Fragment(){
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firestore: FirebaseFirestore

    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var passwordEditText: EditText

    private var progressDialog: ProgressDialog? = null
    private lateinit var profileImageView: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null
    private var isEditing = false

    private val enabledColor by lazy { resources.getColor(R.color.colorEnabledButton) }
    private val disabledColor by lazy { resources.getColor(R.color.colorDisabledButton) }

    private lateinit var editProfileButton: Button
    private lateinit var saveProfileButton: Button
    private lateinit var changeProfileButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Initialize the ImageViews
        profileImageView = view.findViewById(R.id.profileImageView)
        profileImageView.setOnClickListener {
            selectImage()
        }


        // Initialize butt  ons
        editProfileButton = view.findViewById(R.id.btnEditProfile)
        saveProfileButton = view.findViewById(R.id.btnSaveChanges)
        changeProfileButton = view.findViewById(R.id.btnchange)

        // Set initial colors for buttons
        editProfileButton.setBackgroundColor(enabledColor)
        saveProfileButton.setBackgroundColor(disabledColor)

        // Initialize EditText fields
        firstNameEditText = view.findViewById(R.id.firstNameEditText)
        lastNameEditText = view.findViewById(R.id.lastNameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        phoneEditText = view.findViewById(R.id.phoneEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // For Google Signout
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        setEditTextsEditable(false)
        editProfileButton.setOnClickListener {
//            isEditing = !isEditing
            setEditMode(!isEditing)
        }
        saveProfileButton.setOnClickListener {
            updateUserData()
        }
        checkAndFetchUserInfo()

        // Change Password
        changeProfileButton.setOnClickListener {
            val newPassword = passwordEditText.text.toString()
            // Check if the new password is empty
            if (newPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Check if the new password is at least 6 characters long
            if (newPassword.length < 6) {
                Toast.makeText(requireContext(), "The new password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showConfirmationDialog(newPassword)
        }
        return view
    }  // end on create
    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"  // Set the MIME type to restrict to images only
        resultLauncher.launch(intent)
    }

    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                // Store the selected image URI
                selectedImageUri = uri
                profileImageView.setImageURI(uri)
            }
        }
    }

    private fun checkAndFetchUserInfo() {
        // Check if the user is logged in with Firebase
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        if (firebaseUser != null) {
            // User is logged in with Firebase
            if (firebaseUser.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
                // User logged in with Google, fetch and display Google user info
                fetchAndDisplayUserInfoGoogle()
            } else if (firebaseUser.providerData.any { it.providerId == FacebookAuthProvider.PROVIDER_ID }) {
                // User logged in with Facebook, fetch and display Facebook user info
                fetchAndDisplayUserInfoFacebook()
            } else {
                // User logged in with email/password, fetch and display Firebase user info
                fetchAndDisplayUserInfoFirebase()
            }
        } else {
            // User is not logged in, handle this case as needed
            Log.e(ContentValues.TAG, "User is not logged in.")
        }
    }
    //------ Fetch --!
    private fun fetchAndDisplayUserInfoFirebase(){
        firestore = FirebaseFirestore.getInstance()
        val userID = FirebaseAuth.getInstance().currentUser?.uid
        val documentReference= firestore.collection("UserProfileData").document(userID.toString())
        documentReference.get().addOnSuccessListener {
            if (it != null) {
                firstNameEditText.setText(it.data?.get("firstname")?.toString())
                lastNameEditText.setText(it.data?.get("lastname")?.toString())
                phoneEditText.setText(it.data?.get("phone")?.toString())
                emailEditText.setText(it.data?.get("email")?.toString())


                // Get the profile image URL from the Firestore document
                val profileImageUrl = it.data?.get("profile_image_url")?.toString()
                // If the profile image URL is not null, load the image into the ImageView
                if (profileImageUrl != null) {
                    Picasso.get().load(profileImageUrl).into(profileImageView)
                }

            }
        }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Fetching Failed", Toast.LENGTH_SHORT).show()
            }
    }
    //-----------------------------------------------
    // Fetch Facebook User Info
    private fun fetchAndDisplayUserInfoFacebook() {
        val request = GraphRequest.newMeRequest(
            AccessToken.getCurrentAccessToken(),
            object : GraphRequest.GraphJSONObjectCallback {
                override fun onCompleted(graphObject: JSONObject?, response: GraphResponse?) {
                    if (graphObject != null) {
                        val name = graphObject.optString("name")
                        val email = graphObject.optString("email")
                        val accessToken = AccessToken.getCurrentAccessToken()
                        val profilePictureUrl = accessToken?.userId?.let { "https://graph.facebook.com/$it/picture?type=large" }

                        val nameParts = name.split(" ")
                        val firstName = nameParts.getOrElse(0) { "" }
                        val lastName = nameParts.getOrNull(1) ?: ""

                        // Set the first name and last name in your EditText fields
                        firstNameEditText.setText(firstName)
                        lastNameEditText.setText(lastName)
                        emailEditText.setText(email)

                        // Load and display the profile picture
                        if (profilePictureUrl != null) {
                            Picasso.get().load(profilePictureUrl)
                                .error(R.drawable.profilepic) // Set error image resource
                                .into(profileImageView, object : Callback {
                                    override fun onSuccess() {
                                        // Image loaded successfully
                                    }
                                    override fun onError(e: Exception?) {
                                        // Handle error when loading image
                                        Log.e("Picasso", "Error loading image: $e")
                                        // You can show an error message to the user here
                                    }
                                })
                        } else {
                            profileImageView.setImageResource(R.drawable.profilepic)
                        }
                    }
                }
            })

        // Set the email in the email EditText field
        phoneEditText.visibility = View.GONE
        passwordEditText.visibility = View.GONE
        editProfileButton.visibility = View.GONE
        saveProfileButton.visibility = View.GONE
        changeProfileButton.visibility = View.GONE

        val parameters = Bundle()
        parameters.putString("fields", "name,email") // Specify the fields you want to fetch
        request.parameters = parameters
        request.executeAsync()
    }
    //-----------------------------------------------
    //Fetch Google Case
    private fun fetchAndDisplayUserInfoGoogle() {
        val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(requireContext())

        if (googleSignInAccount != null) {
            val displayName = googleSignInAccount.displayName
            val email = googleSignInAccount.email
            val photoUrl = googleSignInAccount.photoUrl // Get the profile image URL


            // Load and display the profile image if the URL is not null
            if (photoUrl != null) {
                Picasso.get().load(photoUrl).into(profileImageView)// Use Picasso to load the image
                // Glide can also be used similarly if you prefer it
            } else {
                // If the URL is null, you can provide a default image or handle it as needed
                profileImageView.setImageResource(R.drawable.profilepic)
            }
            // Set the first name and last name in the EditText fields
            val nameParts = displayName?.split(" ")
            if (nameParts?.size == 2) {
                firstNameEditText.setText(nameParts[0])
                lastNameEditText.setText(nameParts[1])
            }

            // Set the email in the email EditText field
            emailEditText.setText(email)

            phoneEditText.visibility = View.GONE
            passwordEditText.visibility = View.GONE
            editProfileButton.visibility = View.GONE
            saveProfileButton.visibility = View.GONE
            changeProfileButton.visibility = View.GONE
        } else {
            Log.e(ContentValues.TAG, "User is not signed in with Google.")
        }
    }
    private fun setEditTextsEditable(editable: Boolean) {
        firstNameEditText.isEnabled = editable
        lastNameEditText.isEnabled = editable
        emailEditText.isEnabled = editable
        phoneEditText.isEnabled = editable
        profileImageView.isEnabled = editable
    }
    private fun animateButtonColor(button: Button, fromColor: Int, toColor: Int) {
        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        colorAnimation.duration = 300 // Adjust the duration as needed
        colorAnimation.addUpdateListener { animator ->
            button.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnimation.start()
    }
    private fun setEditMode(editMode: Boolean) {
        // Enable or disable EditText fields based on editMode
        setEditTextsEditable(editMode)

        // Enable or disable the Save button based on editMode
        saveProfileButton.isEnabled = editMode

        // Animate button colors
        val fromColor = if (editMode) enabledColor else disabledColor
        val toColor = if (editMode) disabledColor else enabledColor

        animateButtonColor(editProfileButton, fromColor, toColor)
        animateButtonColor(saveProfileButton, toColor, fromColor)
    }
    private fun updateUserData() {
        // Get the current user
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser
        // Get the user's ID
        val userId = firebaseUser?.uid ?: ""

        // Get the values of the EditText fields
        val firstName = firstNameEditText.text.toString()
        val lastName = lastNameEditText.text.toString()
        val email = emailEditText.text.toString()
        val phone = phoneEditText.text.toString()
//        val profileImageView = view.findViewById<ImageView>(R.id.profileImageView)

        // Check if the email field is empty
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an email address", Toast.LENGTH_SHORT).show()
            return
        }
        // Check if the email format is valid
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }
        if (firstName.isEmpty()) {
            Toast.makeText(requireContext(),  "Please enter your first name", Toast.LENGTH_SHORT).show()
            return
        }
        // Check if the last name field is empty
        if (lastName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your last name", Toast.LENGTH_SHORT).show()
            return
        }
        // Check if the phone number field is empty
        if (phone.isEmpty()) {
            Toast.makeText(requireContext(),  "Please enter your phone number", Toast.LENGTH_SHORT).show()
            return
        }
        // Check if the profile picture is empty
        if (profileImageView.drawable == null) {
            Toast.makeText(requireContext(),  "Please select a profile picture", Toast.LENGTH_SHORT).show()
            return
        }

        // Update the user's email in Firebase Authentication

        // Check if a new image was selected
        showProgressDialog("Updating data. Please wait a moment...", "Please wait...", false)
        if (selectedImageUri != null) {
            // Upload the new profile image to Firebase Storage
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("profileImages/$userId.jpg")

            // Get the URI of the selected image
            val imageUri = selectedImageUri ?: Uri.parse("")

            imageRef.putFile(imageUri).addOnSuccessListener { uploadTask ->
                if (uploadTask.task.isSuccessful) {
                    // Image uploaded successfully, get the download URL
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()

                        // Update the user's email in Firebase Authentication
                        firebaseUser?.updateEmail(email)?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Email updated successfully
                                dismissProgressDialog()
                                Toast.makeText(requireContext(),  "Your information has been updated.", Toast.LENGTH_SHORT).show()
                                setEditMode(false)

                                // Update the user's data in Firebase Firestore
                                val userData = mapOf(
                                    "firstname" to firstName,
                                    "lastname" to lastName,
                                    "phone" to phone,
                                    "email" to email,
                                    "profile_image_url" to imageUrl // Update the profile image URL
                                )
                                firestore.collection("UserProfileData").document(userId).update(userData)
                            } else {
                                dismissProgressDialog()
                                // An error occurred
                                Toast.makeText(requireContext(),  "An error occurred updating email", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    dismissProgressDialog()
                    // An error occurred uploading the image
                    Toast.makeText(requireContext(), "An error occurred uploading the image", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // If no new image was selected, update other user data without the profile image URL
            // Update the user's email in Firebase Authentication
            firebaseUser?.updateEmail(email)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Email updated successfully
                    dismissProgressDialog()
                    Toast.makeText(requireContext(),  "Your information has been updated.", Toast.LENGTH_SHORT).show()
                    setEditMode(false)

                    // Update the user's data in Firebase Firestore (excluding profileImageUrl)
                    val userData = mapOf(
                        "firstname" to firstName,
                        "lastname" to lastName,
                        "phone" to phone,
                        "email" to email
                    )
                    firestore.collection("UserProfileData").document(userId).update(userData)
                } else {
                    dismissProgressDialog()
                    // An error occurred
                    Toast.makeText(requireContext(),  "Oops! Something went wrong while updating. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showConfirmationDialog(newPassword: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Password Change")
            .setMessage("Are you sure you want to change your password?")
            .setPositiveButton("Yes") { _, _ ->
                showProgressDialog("Updating Password. Please wait a moment...", "Please wait...", false)
                FirebaseAuth.getInstance().currentUser?.updatePassword(newPassword)
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            dismissProgressDialog()
                            Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show()
                            passwordEditText.setText("")
                        } else {
                            dismissProgressDialog()
                            Toast.makeText(requireContext(), "Error updating password: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    fun showProgressDialog(title: String, message: String, cancelable: Boolean) {
        progressDialog?.dismiss() // Dismiss any existing ProgressDialog
        progressDialog = ProgressDialog(requireContext())
        progressDialog?.setTitle(title)
        progressDialog?.setMessage(message)
        progressDialog?.setCancelable(cancelable)
        progressDialog?.show()
    }
    fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}//end
//Auth Helper
package com.example.tnc

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.example.tnc.databinding.ActivityMainBinding
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class AuthHelper(private val activity: Activity) {
    private val auth = FirebaseAuth.getInstance()
    private val callbackManager = CallbackManager.Factory.create()
    private val RC_SIGN_IN = 9001 // Request code for Google Sign-In
    private var progressDialog: ProgressDialog
    private var builder = AlertDialog.Builder(activity, R.style.MyAlertDialog)

    private val firestore = FirebaseFirestore.getInstance()
    private val userProfileCollection = firestore.collection("UserProfileData")

    init {
        progressDialog = ProgressDialog(activity)
        progressDialog.setTitle("Logging In")
        progressDialog.setMessage("Please wait...")
        progressDialog.setCancelable(false)
        builder = AlertDialog.Builder(activity)
    }

    fun signInWithGoogle() {
        progressDialog.show()
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(activity, googleSignInOptions)
        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    fun signInWithFacebook() {
        progressDialog.show()
        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired
        if (!isLoggedIn) {
            val loginManager = LoginManager.getInstance()
            loginManager.logInWithReadPermissions(activity, listOf("email", "public_profile"))
            loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    progressDialog.dismiss()
                    builder.setTitle("Facebook login canceled.")
                    builder.setPositiveButton("OK", null)
                    builder.show()
                }

                override fun onError(error: FacebookException) {
                    progressDialog.dismiss()
                    builder.setTitle("Encountered an issue with Facebook login.")
                    builder.setPositiveButton("OK", null)
                    builder.show()
                }
            })
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignInResult(task)
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        progressDialog.show()
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                firebaseAuthWithGoogle(account)
            } else {
                progressDialog.dismiss()
                builder.setTitle("Google Sign-In failed.")
                builder.setPositiveButton("OK", null)
                builder.show()
            }
        } catch (e: ApiException) {
            progressDialog.dismiss()
            builder.setTitle("Cancelled Google Sign-In.")
            builder.setPositiveButton("OK", null)
            builder.show()
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        progressDialog.show()
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    progressDialog.dismiss()
                    val user = auth.currentUser
                    val name = acct.displayName.toString()
                    val email = acct.email.toString()
                    val profilePicUrl = acct.photoUrl.toString()
                    if (user != null) {
                        saveUserDataToFirestore(
                            user.uid, name,
                            email,
                            profilePicUrl
                        )
                    }
                }  else {
                    progressDialog.dismiss()
                    builder.setTitle("Authentication with Firebase encountered an error.")
                    //                    builder.setMessage("There was an error in login. Please try again.")
                    builder.setPositiveButton("OK", null)
                    builder.show()
                }
            }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential: AuthCredential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val authResult = task.result
                    val facebookUser = authResult.additionalUserInfo
                    val email = facebookUser?.profile?.get("email").toString()
                    if (user != null) {
                        saveUserDataToFirestore(
                            user.uid,
                            user.displayName ?: "",
                            email,
                            user.photoUrl?.toString() ?: ""
                        )
                    }
                } else {
                    progressDialog.dismiss()
                    builder.setTitle("Authentication with Firebase encountered an error.")
                    builder.setPositiveButton("OK", null)
                    builder.show()
                }
            }
    }


    private fun saveUserDataToFirestore(userId: String, fullName: String, email: String, profileImageUrl: String) {
        val userDocument = userProfileCollection.document(userId)

        userDocument.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // User data already exists, no need to save it again
                    progressDialog.dismiss()
                    navigateToProfilePage()
                } else {
                    // User data doesn't exist, save it
                    val userData = HashMap<String, Any>()
                    userData["FullName"] = fullName
                    userData["email"] = email
                    userData["profile_image_url"] = profileImageUrl

                    userDocument.set(userData)
                        .addOnSuccessListener {
                            progressDialog.dismiss()
                            navigateToProfilePage()
                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss()
                            builder.setTitle("Error Saving User Data to Firestore")
                            builder.setMessage("Failed to save user data: ${e.message}")
                            builder.setPositiveButton("OK", null)
                            builder.show()
                        }
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                builder.setTitle("Error Checking User Data in Firestore")
                builder.setMessage("Failed to check user data: ${e.message}")
                builder.setPositiveButton("OK", null)
                builder.show()
            }
    }

    fun navigateToProfilePage() {
        val intent = Intent(activity, HomeActivity::class.java)
        activity.startActivity(intent)
        // If you want to close the current activity (optional)
        if (activity is Activity) {
            activity.finish()
        }
    }
}
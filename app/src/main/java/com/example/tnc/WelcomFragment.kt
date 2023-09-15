package com.example.tnc

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.json.JSONObject

class WelcomFragment : Fragment() {

    private lateinit var profileImageView: ImageView
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_welcom, container, false)

        // Inflate the layout for this fragment
        profileImageView = view.findViewById(R.id.profileImageView)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        checkAndFetchUserInfo()

        return view
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
                        val accessToken = AccessToken.getCurrentAccessToken()
                        val profilePictureUrl = accessToken?.userId?.let { "https://graph.facebook.com/$it/picture?type=large" }

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
            val photoUrl = googleSignInAccount.photoUrl
            // Load and display the profile image if the URL is not null
            if (photoUrl != null) {
                Picasso.get().load(photoUrl).into(profileImageView)// Use Picasso to load the image
                // Glide can also be used similarly if you prefer it
            } else {
                // If the URL is null, you can provide a default image or handle it as needed
                profileImageView.setImageResource(R.drawable.profilepic)
            }
        } else {
            Log.e(ContentValues.TAG, "User is not signed in with Google.")
        }
    }

}
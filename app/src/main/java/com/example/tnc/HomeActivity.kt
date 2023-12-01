
package com.example.tnc

import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.util.Locale.filter


class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var builder: AlertDialog.Builder
    private lateinit var progressDialog: ProgressDialog
    private var documentListener: ListenerRegistration? = null

    // Menu Bar
    lateinit var toolbar: Toolbar
    lateinit var drawerLayout: DrawerLayout
    lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()
        builder = AlertDialog.Builder(this, R.style.MyAlertDialog)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Deleting User Data")
        progressDialog.setMessage("Please wait...")
        progressDialog.setCancelable(false)

        //Menu Bar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, 0, 0
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)


        setToolbarTitle("Home")
        val fragment= supportFragmentManager.beginTransaction()
        fragment.replace(R.id.fragment_container,WelcomFragment()).commit()

        //---------------------------------------End Menu Bar ------------------------

        auth = FirebaseAuth.getInstance()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Fetch and display user information
        checkAndFetchUserInfo()
    }// end oncreate
    fun setToolbarTitle(title: String){
        supportActionBar?.title=title
    }
    override fun onBackPressed() {
        // Start an intent to go to the mobile home screen
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        startActivity(homeIntent)
    }
    //------ Fetch --!
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
            }else {
                // User logged in with email/password, fetch and display Firebase user info
                fetchAndDisplayUserInfoFirebase()
            }
        } else {
            // User is not logged in, handle this case as needed
            Log.e(ContentValues.TAG, "User is not logged in.")
        }
    }
    //------ Fetch --!
    private fun fetchAndDisplayUserInfoFirebase() {
        firestore = FirebaseFirestore.getInstance()
        val userID = FirebaseAuth.getInstance().currentUser?.uid
        val documentReference = firestore.collection("UserProfileData").document(userID.toString())

        documentListener = documentReference.addSnapshotListener { documentSnapshot, _ ->
            if (documentSnapshot != null && documentSnapshot.exists()) {
                // Check if activity is still alive
                if (!isFinishing && !isDestroyed) {
                    val userNameTextView = findViewById<TextView>(R.id.userNameTextView)
                    val userEmailTextView = findViewById<TextView>(R.id.userEmailTextView)
                    val rMenuprofileImageView = findViewById<ImageView>(R.id.rMenuprofileImageView)

                    // Check if views are not null
                    userNameTextView?.text = documentSnapshot.getString("FullName")
                    userEmailTextView?.text = documentSnapshot.getString("email")

                    val profileImageUrl = documentSnapshot.getString("profile_image_url")
                    if (profileImageUrl != null) {
                        // Use Picasso only if ImageView is not null
                        rMenuprofileImageView?.let {
                            Picasso.get().load(profileImageUrl).into(it)
                        }
                    }
                }
            }
        }
    }
    //-----------------------------------------------
    // Fetch Facebook User Info
    private fun fetchAndDisplayUserInfoFacebook() {

        val headerView = navView.getHeaderView(0)
        val userNameTextView = headerView.findViewById<TextView>(R.id.userNameTextView)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.userEmailTextView)
        val rMenuprofileImageView = headerView.findViewById<ImageView>(R.id.rMenuprofileImageView)


        val request = GraphRequest.newMeRequest(
            AccessToken.getCurrentAccessToken(),
            object : GraphRequest.GraphJSONObjectCallback {
                override fun onCompleted(graphObject: JSONObject?, response: GraphResponse?) {
                    if (graphObject != null) {
                        val name = graphObject.optString("name")
                        val email = graphObject.optString("email")
                        val accessToken = AccessToken.getCurrentAccessToken()
                        val profilePictureUrl = accessToken?.userId?.let { "https://graph.facebook.com/$it/picture?type=large" }

                        // Now you can use this data as needed
                        // Set the name and email in your EditText fields
                        userNameTextView.setText(name)
                        userEmailTextView.setText(email)

                        // Load and display the profile picture
                        if (profilePictureUrl != null) {
                            Picasso.get().load(profilePictureUrl)
                                .error(R.drawable.profilepic) // Set error image resource
                                .into(rMenuprofileImageView, object : Callback {
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
                            rMenuprofileImageView.setImageResource(R.drawable.profilepic)
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
        val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this)
        val headerView = navView.getHeaderView(0)

        val userNameTextView = headerView.findViewById<TextView>(R.id.userNameTextView)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.userEmailTextView)
        val rMenuprofileImageView = headerView.findViewById<ImageView>(R.id.rMenuprofileImageView)// Assuming you have an ImageView for the profile image
        if (googleSignInAccount != null) {
            val displayName = googleSignInAccount.displayName
            val email = googleSignInAccount.email
            val photoUrl = googleSignInAccount.photoUrl // Get the profile image URL

            userNameTextView.text = displayName
            userEmailTextView.text = email
            // Load and display the profile image if the URL is not null
            if (photoUrl != null) {
                Picasso.get().load(photoUrl).into(rMenuprofileImageView)
                // Glide can also be used similarly if you prefer it
            } else {
                // If the URL is null, you can provide a default image or handle it as needed
                rMenuprofileImageView.setImageResource(R.drawable.profilepic)
            }
        } else {
            Log.e(ContentValues.TAG, "User is not signed in with Google.")
        }
    }
    //-----------------------------------------------
    private fun signOut() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Logout")
        alertDialog.setMessage("Are you sure you want to logout?")
        alertDialog.setPositiveButton("Yes") { _, _ ->
            // User clicked "Yes," perform logout
            auth.signOut()
            // Sign out from Google Sign-In
            googleSignInClient.signOut().addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    navigateToLogin()
                } else {
                    // Handle sign-out failure
                    Toast.makeText(this, "Google Sign-Out failed", Toast.LENGTH_SHORT).show()
                }
            }
            // Sign out from Facebook (if the user logged in with Facebook)
            val loginManager = LoginManager.getInstance()
            loginManager.logOut()
            // Navigate to the login page
            navigateToLogin()
        }
        alertDialog.setNegativeButton("No", null) // User clicked "No," do nothing
        alertDialog.show()
    }
    private fun navigateToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Close the profile activity to prevent returning to it.
    }
    //------------------------   Menu Bar -----------------------------------------
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                setToolbarTitle("Profile")
                val fragment = ProfileFragment()
                val fragmentManager = supportFragmentManager
                fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
            }
            R.id.nav_eventAdd -> {
                setToolbarTitle("Event")
                val fragment = FragmentEvent()
                val fragmentManager = supportFragmentManager
                fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
            }
            R.id.nav_home -> {
                setToolbarTitle("Home")
                val fragment = WelcomFragment()
                val fragmentManager = supportFragmentManager
                fragmentManager.beginTransaction().replace(R.id.fragment_container, fragment).commit()
            }
            R.id.nav_logout -> {
                signOut()
            }
            R.id.nav_deleteAccount -> {
                    DeleteAccount()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    //---------------------------- End -------------------------------------------
    fun DeleteAccount() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Delete")
        alertDialog.setMessage("Are you sure you want to Delete your Account?")
        alertDialog.setPositiveButton("Yes") { _, _ ->
        // Get the current user.
        val user = Firebase.auth.currentUser
        // Delete the user from Firebase Authentication.
        user?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // The user has been deleted from Firebase Authentication.
                // Delete the user's data from Firestore.
                val db = FirebaseFirestore.getInstance()
                val userRef = db.collection("UserProfileData").document(user!!.uid)
                userRef.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // The user's data has been deleted from Firestore.
                        Toast.makeText(this, "Deleting Successfully", Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    } else {
                        // An error occurred while deleting the user's data from Firestore.
                        Toast.makeText(this, "An error occurred while deleting the user's data from Firestore.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // An error occurred while deleting the user from Firebase Authentication.
                Toast.makeText(this, "An error occurred while deleting the user from Firebase Authentication", Toast.LENGTH_SHORT).show()
            }
        }
        }
        alertDialog.setNegativeButton("No", null) // User clicked "No," do nothing
        alertDialog.show()
    }



}// end

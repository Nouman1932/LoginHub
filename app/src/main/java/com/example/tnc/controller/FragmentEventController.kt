package com.example.tnc.controller

import android.net.Uri
import com.example.tnc.UserAdapter
import com.example.tnc.model.UserModel
import com.example.tnc.view.FragmentEventView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage

class FragmentEventController(
    private val view: FragmentEventView,
    private val model: UserModel,
    private val userAdapter: UserAdapter,


    ) {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val storageReference = storage.reference
    private var userSnapshotListener: ListenerRegistration? = null

    fun removeUserSnapshotListener() {
        userSnapshotListener?.remove()
    }
    interface DeleteUserCallback {
        fun onDeleteSuccess()
        fun onDeleteFailure(message: String)
        fun onDeleteComplete()
    }
    fun deleteUserFromFirestore(user: UserModel, callback: DeleteUserCallback) {
        db.collection("Users").document(user.userId)
            .delete()
            .addOnSuccessListener {
                // User deleted successfully
                callback.onDeleteSuccess()
            }
            .addOnFailureListener { exception ->
                // Failed to delete user
                callback.onDeleteFailure(exception.message ?: "Unknown error")
            }
            .addOnCompleteListener {
                // Regardless of success or failure, notify that deletion is complete
                callback.onDeleteComplete()
            }
    }

    fun onAddUserButtonClicked() {
        view.showAddUserDialog()
    }

    fun onProfileImageClicked() {
        view.selectImage()
    }
    interface UserListCallback {
        fun onUserListRetrieved(userList: List<UserModel>)
    }

    fun updateUserData(fullName: String, profilePictureUri: String, date: Timestamp, comment: String) {
        model.fullName = fullName
        model.profilePictureUri = profilePictureUri
        model.date = date
        model.comment = comment
    }

    fun onAddUserButtonClick() {
        view.showLoadingIndicator()
        view.updateUserData()
        val fullName = model.fullName
        val date = model.date ?: Timestamp.now()
        val comment = model.comment
        val imageUri = model.profilePictureUri

        if (imageUri != null) {
            val stringImageUri = imageUri.toString() // Convert Uri to String
            saveUserDataToFirestoreWithImage(fullName, date, comment, stringImageUri)
        } else {
            view.showToast("Image URI is null. Please select an image.")
            view.hideLoadingIndicator()
        }
    }

    private fun saveUserDataToFirestoreWithImage(fullName: String, date: Timestamp, comment: String, stringImageUri: String) {
        // Generate a unique filename for the user's image
        val imageFileName = "${System.currentTimeMillis()}_${model.userId}"
        val storageRef = storage.reference.child("profile_images/$imageFileName")

        storageRef.putFile(Uri.parse(stringImageUri))
            .addOnSuccessListener { taskSnapshot ->
                val downloadUrlTask = taskSnapshot.storage.downloadUrl
                downloadUrlTask.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()

                    val user = hashMapOf(
                        "fullName" to fullName,
                        "profilePictureUri" to downloadUrl,
                        "date" to date,
                        "comment" to comment
                    )

                    db.collection("Users")
                        .add(user)
                        .addOnSuccessListener { documentReference ->
                            model.userId = documentReference.id  // Save the document ID
                            view.showToast("User data added to Firestore successfully!")
                            view.resetFieldsAndFocusOnFullName()
                            view.hideLoadingIndicator()
                            view.hideAddUserDialog()
                        }
                        .addOnFailureListener {
                            view.hideLoadingIndicator()
                            view.showToast("Failed to add user data to Firestore!")
                        }
                        .addOnCompleteListener {
                            view.hideLoadingIndicator()
                        }
                }
            }
            .addOnFailureListener {
                view.showToast("Failed to upload image to Firebase Storage!")
                view.hideLoadingIndicator()
            }
    }


    fun getAllUsersFromFirestore(callback: UserListCallback) {
            val userList = mutableListOf<UserModel>()

            // Remove existing listener if any
            userSnapshotListener?.remove()

            // Set up a real-time listener
            userSnapshotListener = db.collection("Users")
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        view.showToast("Error getting real-time updates: $exception")
                        return@addSnapshotListener
                    }

                    userList.clear()

                    if (snapshot != null) {
                        for (document in snapshot.documents) {
                            val user = document.toObject(UserModel::class.java)
                            user?.userId = document.id
                            user?.let { userList.add(it) }
                        }
                    }

                    // Update the RecyclerView with the fetched user list
                    userAdapter.updateUserList(userList)

                    // Invoke the callback with the retrieved user list
                    callback.onUserListRetrieved(userList)
                }
        }
}






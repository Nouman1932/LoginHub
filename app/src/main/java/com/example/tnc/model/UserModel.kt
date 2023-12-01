package com.example.tnc.model

import android.net.Uri
import com.google.firebase.Timestamp

data class UserModel(
    var userId: String = "",
    var fullName: String = "",
    var profilePictureUri: String? = null,  // Change the type to String
    var date: Timestamp? = null,
    var comment: String = ""
)
package com.example.tnc.view

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.example.tnc.model.UserModel

interface FragmentEventView {
    fun showAddUserDialog()
    fun selectImage()

    fun updateProfilePicture(uri: Uri)
    fun deleteUserFromFirestore(user: UserModel)
    val resultLauncher: ActivityResultLauncher<Intent>
    fun showToast(message: String)
    fun updateUserData()
    fun updateUserList(userList: List<UserModel>)
    fun resetFieldsAndFocusOnFullName()
    fun showLoadingIndicator()
    fun hideLoadingIndicator()
    fun hideAddUserDialog()
}
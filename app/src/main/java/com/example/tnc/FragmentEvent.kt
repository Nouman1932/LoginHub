package com.example.tnc

import android.app.Activity
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tnc.controller.FragmentEventController
import com.example.tnc.model.UserModel
import com.example.tnc.view.FragmentEventView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import java.util.Date
import java.util.Locale
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
class FragmentEvent : Fragment(), FragmentEventView {
    private lateinit var controller: FragmentEventController
    private lateinit var dialog: BottomSheetDialog
    private lateinit var dateTextView: TextView
    private lateinit var profileImageView: ShapeableImageView
    private lateinit var fullNameEditText: EditText
    private lateinit var commentEditText: EditText
    private lateinit var btnAddUser: Button

    // For show List
    private lateinit var userRecyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter

    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_event, container, false)

        // Initialize RecyclerView
        userRecyclerView = view.findViewById(R.id.userRecyclerView)
        // Pass onDeleteClickListener to UserAdapter constructor
        userAdapter = UserAdapter(emptyList()) { user ->
            controller.deleteUserFromFirestore(user, object : FragmentEventController.DeleteUserCallback {
                override fun onDeleteSuccess() {
                    showToast("User deleted successfully!")
                }

                override fun onDeleteFailure(message: String) {
                    showToast("Failed to delete user: $message")
                }

                override fun onDeleteComplete() {
                    hideLoadingIndicator()
                }
            })
        }

        userRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        userRecyclerView.adapter = userAdapter

        // Initialize your ProgressDialog
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setTitle("Uploading")
        progressDialog.setMessage("Please wait...")
        progressDialog.setCancelable(false)

        val openAddUserButton = view.findViewById<FloatingActionButton>(R.id.openAddUserButton)
        controller = FragmentEventController(this, UserModel(), userAdapter)

        openAddUserButton.setOnClickListener {
            controller.onAddUserButtonClicked()
        }
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Call this to fetch users when the fragment is created
        controller.getAllUsersFromFirestore(object : FragmentEventController.UserListCallback {
            override fun onUserListRetrieved(userList: List<UserModel>) {
                // Update the RecyclerView with the fetched user list
                userAdapter.updateUserList(userList)
            }
        })
    }
        override fun showAddUserDialog() {
            dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
            val dialogView = layoutInflater.inflate(R.layout.activity_event_add_user, null)
            dialog.setContentView(dialogView)

            profileImageView = dialogView.findViewById(R.id.profileImageView)
            dateTextView = dialogView.findViewById(R.id.dateTextView)
            fullNameEditText = dialogView.findViewById(R.id.fullNameEditText)
            commentEditText = dialogView.findViewById(R.id.commentEditText)
            btnAddUser = dialogView.findViewById(R.id.btnAddUser)



            dialog.behavior.peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)

            profileImageView.setOnClickListener {
                controller.onProfileImageClicked()
            }

            dateTextView.setOnClickListener {
                showDatePicker()
            }

            btnAddUser.setOnClickListener {
                if (validateFields()) {
                    controller.onAddUserButtonClick()

                }
            }

            dialog.show()
        }
    override fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    private fun validateFields(): Boolean {
        val fullName = fullNameEditText.text.toString()
        val date = dateTextView.text.toString()
        val comment = commentEditText.text.toString()

        if (fullName.isBlank() || date.isBlank() || comment.isBlank() || profileImageView.tag == null) {
            showToast("Please fill in all the fields")
            return false
        }

        return true
    }
    override fun updateProfilePicture(uri: Uri) {
        profileImageView.setImageURI(uri)
        profileImageView.tag = uri  // Set the tag here
    }

        private fun showDatePicker() {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = "$selectedYear-${selectedMonth + 1}-$selectedDay"
                    updateDate(selectedDate)
                },
                year,
                month,
                day
            )

            datePickerDialog.show()
        }

    private fun updateDate(selectedDate: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.parse(selectedDate)

        // Create a new Timestamp
        val timestamp = if (date != null) {
            Timestamp(Date(date.time))
        } else {
            Timestamp.now()
        }

        dateTextView.tag = timestamp
        dateTextView.text = selectedDate
    }
        override val resultLauncher: ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    data?.data?.let { uri ->
                        profileImageView.setImageURI(uri)
                        profileImageView.tag = uri
                    }
                }
            }

        override fun selectImage() {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            resultLauncher.launch(intent)
        }

    override fun updateUserData() {
        val fullName = fullNameEditText.text.toString()
        val imageUri = profileImageView.tag as Uri
        val date = dateTextView.tag as Timestamp
        val comment = commentEditText.text.toString()

        // Convert Uri to String
        val imageUriString = imageUri.toString()

        controller.updateUserData(fullName, imageUriString, date, comment)
    }


    override fun updateUserList(userList: List<UserModel>) {
        userAdapter.updateUserList(userList)
    }
    private fun resetFields() {
        fullNameEditText.text.clear()
        commentEditText.text.clear()
        dateTextView.text = ""
        // Reset your profile image, assuming you have a default image
        // profileImageView.setImageResource(R.drawable.default_profile_image)
        profileImageView.setImageURI(null)
        profileImageView.tag = null
    }
    override fun resetFieldsAndFocusOnFullName() {
        resetFields()
        fullNameEditText.requestFocus()
    }
    override fun showLoadingIndicator() {
        // Show the ProgressDialog
        progressDialog.show()
    }

    override fun hideLoadingIndicator() {
        // Hide the ProgressDialog
        progressDialog.dismiss()
    }
    override fun deleteUserFromFirestore(user: UserModel) {
        // Show loading indicator if needed
        showLoadingIndicator()

        // Delete the user from Firestore
        controller.deleteUserFromFirestore(user, object : FragmentEventController.DeleteUserCallback {
            override fun onDeleteSuccess() {
                showToast("User deleted successfully!")
            }

            override fun onDeleteFailure(message: String) {
                showToast("Failed to delete user: $message")
            }

            override fun onDeleteComplete() {
                // Hide loading indicator if needed
                hideLoadingIndicator()
            }
        })
    }
    override fun hideAddUserDialog() {
        dialog.dismiss()
    }
    override fun onDestroy() {
        super.onDestroy()
        // Remove the listener to avoid memory leaks
        controller.removeUserSnapshotListener()
    }
}
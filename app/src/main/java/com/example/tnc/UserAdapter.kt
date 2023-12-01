package com.example.tnc

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tnc.model.UserModel
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class UserAdapter(private var userList: List<UserModel>, private val onDeleteClickListener: (UserModel) -> Unit) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ImageView = itemView.findViewById(R.id.cardProfileImageView)
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val commentTextView: TextView = itemView.findViewById(R.id.commentTextView)
        val removeButton: Button = itemView.findViewById(R.id.deleteUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.event_profile_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]

        // Load profile picture using Picasso
        if (user.profilePictureUri != null) {
            Picasso.get().load(user.profilePictureUri)
                .error(R.drawable.defaultprofile) // Set error image resource
                .into(holder.profileImageView, object : Callback {
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
            holder.profileImageView.setImageResource(R.drawable.defaultprofile)
        }

        holder.nameTextView.text = user.fullName
        val date = user.date?.toDate()
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        holder.dateTextView.text = formattedDate

        holder.commentTextView.text = user.comment
        holder.removeButton.setOnClickListener {
            onDeleteClickListener.invoke(user)
        }

        // You can add click listeners or other operations here
    }

    override fun getItemCount(): Int {
        return userList.size
    }
    fun updateUserList(newList: List<UserModel>) {
        userList = newList
        notifyDataSetChanged()
    }
}
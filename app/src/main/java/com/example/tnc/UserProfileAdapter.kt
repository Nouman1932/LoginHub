package com.example.tnc
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import android.widget.Filter
import android.widget.Filterable


class UserProfileAdapter(private val userList: List<UserProfile>) :
    RecyclerView.Adapter<UserProfileAdapter.UserProfileViewHolder>(), Filterable {


    private var filteredUserList: List<UserProfile> = userList
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserProfileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_profile_item, parent, false)
        return UserProfileViewHolder(view)
    }
    override fun onBindViewHolder(holder: UserProfileViewHolder, position: Int) {
        val user = filteredUserList[position]
        holder.nameTextView.text = user.name
        holder.emailTextView.text = user.email

        // Load and display the profile picture using Glide
        if (!user.profilePicUrl.isNullOrBlank()) {
            Picasso.get().load(user.profilePicUrl).into(holder.profilePicImageView)
        }
    }
    override fun getItemCount(): Int {
        return filteredUserList.size
    }
    inner class UserProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val emailTextView: TextView = itemView.findViewById(R.id.emailTextView)
        val profilePicImageView: ImageView = itemView.findViewById(R.id.cardProfileImageView)
        // Add other views for user fields here
    }
    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint.toString().toLowerCase()
                val filteredList = mutableListOf<UserProfile>()

                for (user in userList) {
                    val name = user.name?.toLowerCase() ?: ""
                    val email = user.email?.toLowerCase() ?: ""

                    if (name.contains(query) || email.contains(query)) {
                        filteredList.add(user)
                    }
                }

                val results = FilterResults()
                results.values = filteredList
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredUserList = results?.values as? List<UserProfile> ?: emptyList()
                Log.d("Filter", "Filtered list size: ${filteredUserList.size}")
                notifyDataSetChanged()
            }
        }
    }



}

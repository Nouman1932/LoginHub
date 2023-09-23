package com.example.tnc

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import com.google.firebase.firestore.FirebaseFirestore

class WelcomFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val userProfileList = mutableListOf<UserProfile>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserProfileAdapter
    private lateinit var searchView: SearchView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_welcom, container, false)

        recyclerView = view.findViewById(R.id.userRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = UserProfileAdapter(userProfileList) // Should this be userProfileList?
        recyclerView.adapter = adapter

        // Initialize and set up the SearchView
        searchView = view.findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d("SearchView", "Query changed: $newText")
                adapter.filter.filter(newText)
                return true
            }
        })

        fetchDataFromFirestore()
        return view
    }

    private fun fetchDataFromFirestore() {
        db.collection("UserProfileData")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val fullName = document.getString("FullName")
                    val email = document.getString("email")
                    val profileImageUrl = document.getString("profile_image_url")

                    val userProfile = UserProfile(fullName, email, profileImageUrl)
                    userProfileList.add(userProfile)
                }
                adapter.notifyDataSetChanged()
            }
    }
}

package com.example.bitapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val idKaryawan: String = "",
    val email: String = ""
)

class HomeViewModel : ViewModel() {

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> get() = _userProfile

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun fetchUserProfile() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val profile = UserProfile(
                        firstName = document.getString("firstName") ?: "",
                        lastName = document.getString("lastName") ?: "",
                        idKaryawan = document.getString("idKaryawan") ?: "",
                        email = document.getString("email") ?: ""
                    )
                    _userProfile.value = profile
                }
            }
            .addOnFailureListener { it.printStackTrace() }
    }
}

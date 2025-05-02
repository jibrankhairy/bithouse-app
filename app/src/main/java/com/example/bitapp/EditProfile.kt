package com.example.bitapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfile : AppCompatActivity() {

    private lateinit var firstNameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var idKaryawanInput: EditText
    private lateinit var saveButton: Button
    private lateinit var emailText: EditText


    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        firstNameInput = findViewById(R.id.editFirstName)
        lastNameInput = findViewById(R.id.editLastName)
        idKaryawanInput = findViewById(R.id.editIdKaryawan)
        saveButton = findViewById(R.id.saveButton)
        emailText = findViewById(R.id.emailText)

        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        if (uid != null) {
            emailText.setText(currentUser?.email)
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        firstNameInput.setText(document.getString("firstName"))
                        lastNameInput.setText(document.getString("lastName"))
                        idKaryawanInput.setText(document.getString("idKaryawan"))
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                }

            saveButton.setOnClickListener {
                val newFirstName = firstNameInput.text.toString().trim()
                val newLastName = lastNameInput.text.toString().trim()
                val newIdKaryawan = idKaryawanInput.text.toString().trim()

                val updates = mapOf(
                    "firstName" to newFirstName,
                    "lastName" to newLastName,
                    "idKaryawan" to newIdKaryawan
                )

                db.collection("users").document(uid).update(updates)
                    .addOnSuccessListener {
                        val sharedPref = getSharedPreferences("user_pref", MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("firstName", newFirstName)
                            putString("lastName", newLastName)
                            putString("idKaryawan", newIdKaryawan)
                            apply()
                        }

                        Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        finish() // kembali ke halaman sebelumnya
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal memperbarui data", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}

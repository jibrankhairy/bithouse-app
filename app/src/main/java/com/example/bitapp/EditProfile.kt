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
        backButton.setOnClickListener { finish() }

        if (uid != null) {
            emailText.setText(currentUser.email)

            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        firstNameInput.setText(document.getString("firstName") ?: "")
                        lastNameInput.setText(document.getString("lastName") ?: "")
                        val idKaryawan = document.getLong("idKaryawan")?.toInt() ?: 0
                        idKaryawanInput.setText(idKaryawan.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                }

            saveButton.setOnClickListener {
                val newFirstName = firstNameInput.text.toString().trim()
                val newLastName = lastNameInput.text.toString().trim()
                val newIdKaryawanStr = idKaryawanInput.text.toString().trim()

                if (newFirstName.isEmpty() || newLastName.isEmpty() || newIdKaryawanStr.isEmpty()) {
                    Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newIdKaryawan = try {
                    newIdKaryawanStr.toInt()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "ID Karyawan harus berupa angka", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val sharedPref = getSharedPreferences("user_pref", MODE_PRIVATE)
                val oldIdKaryawan = sharedPref.getInt("idKaryawan", 0)

                val updates = mapOf(
                    "firstName" to newFirstName,
                    "lastName" to newLastName,
                    "idKaryawan" to newIdKaryawan // ✅ pastikan number, bukan string
                )

                db.collection("users").document(uid).update(updates)
                    .addOnSuccessListener {
                        db.collection("fingerprints").document(uid).update(
                            mapOf(
                                "idKaryawan" to newIdKaryawan,
                                "firstName" to newFirstName,
                                "lastName" to newLastName
                            )
                        ).addOnFailureListener {
                            Toast.makeText(this, "Gagal update idKaryawan di fingerprints", Toast.LENGTH_SHORT).show()
                        }

                        if (oldIdKaryawan != newIdKaryawan) {
                            val oldRef = db.collection("absensi").document(oldIdKaryawan.toString()).collection("tanggal")
                            val newRef = db.collection("absensi").document(newIdKaryawan.toString()).collection("tanggal")

                            oldRef.get().addOnSuccessListener { snapshot ->
                                for (doc in snapshot.documents) {
                                    val data = doc.data
                                    if (data != null) {
                                        newRef.document(doc.id).set(data)
                                    }
                                }
                                for (doc in snapshot.documents) {
                                    oldRef.document(doc.id).delete()
                                }
                                db.collection("absensi").document(oldIdKaryawan.toString()).delete()
                            }
                        }

                        with(sharedPref.edit()) {
                            putString("firstName", newFirstName)
                            putString("lastName", newLastName)
                            putInt("idKaryawan", newIdKaryawan) // ✅ simpan sebagai Int
                            apply()
                        }

                        Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        finish()
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

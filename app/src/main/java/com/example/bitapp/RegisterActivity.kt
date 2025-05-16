package com.example.bitapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etIdKaryawan: EditText
    private lateinit var etEmail: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()


        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etIdKaryawan = findViewById(R.id.etIdKaryawan)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvToLogin = findViewById(R.id.tvToLogin)

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val idKaryawanText = etIdKaryawan.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || idKaryawanText.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val idKaryawan = idKaryawanText.toIntOrNull()
            if (idKaryawan == null) {
                Toast.makeText(this, "ID Karyawan harus berupa angka", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: ""
                        val user = hashMapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "idKaryawan" to idKaryawan,
                            "email" to email
                        )

                        db.collection("users").document(uid).set(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registrasi berhasil", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal simpan data: ${e.message}", Toast.LENGTH_LONG).show()
                            }

                } else {
                        Toast.makeText(this, "Registrasi gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvToLogin.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

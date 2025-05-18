package com.example.bitapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.snackbar.Snackbar
import android.graphics.Color
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent


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

        btnRegister.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                }
            }
            false
        }

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val idKaryawanStr  = etIdKaryawan.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || idKaryawanStr.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Snackbar.make(findViewById(android.R.id.content), "All fields must be filled in", Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.btnRegister)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.red))
                    .setTextColor(Color.WHITE)
                    .show()
                return@setOnClickListener
            }

            val idKaryawan = idKaryawanStr.toIntOrNull()
            if (idKaryawan == null) {
                Snackbar.make(findViewById(android.R.id.content), "Employee ID must be numeric", Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.btnRegister)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.red))
                    .setTextColor(Color.WHITE)
                    .show()
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
                                val snackbar = Snackbar.make(findViewById(android.R.id.content), "Registrasi Successfull!", Snackbar.LENGTH_LONG)
                                    .setAnchorView(R.id.btnRegister)
                                    .setBackgroundTint(ContextCompat.getColor(this, R.color.secondary))
                                    .setTextColor(Color.WHITE)

                                snackbar.show()

                                Handler(Looper.getMainLooper()).postDelayed({
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }, 1500)
                            }
                            .addOnFailureListener { e ->
                                Snackbar.make(findViewById(android.R.id.content), "Failed to save data: ${e.message}", Snackbar.LENGTH_LONG)
                                    .setAnchorView(R.id.btnRegister)
                                    .setBackgroundTint(ContextCompat.getColor(this, R.color.red))
                                    .setTextColor(Color.WHITE)
                                    .show()
                            }
                    } else {
                        Snackbar.make(findViewById(android.R.id.content), "Registrasi Failed: ${task.exception?.message}", Snackbar.LENGTH_LONG)
                            .setAnchorView(R.id.btnRegister)
                            .setBackgroundTint(ContextCompat.getColor(this, R.color.red))
                            .setTextColor(Color.WHITE)
                            .show()
                    }
                }
        }

        val tvPasswordWarning = findViewById<TextView>(R.id.tvPasswordWarning)
        val tvConfirmPasswordWarning = findViewById<TextView>(R.id.tvConfirmPasswordWarning)

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                if (password.length in 1..5) {
                    tvPasswordWarning.text = "Password must be at least 6 characters"
                    tvPasswordWarning.visibility = TextView.VISIBLE
                } else {
                    tvPasswordWarning.visibility = TextView.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = etPassword.text.toString()
                val confirmPassword = s.toString()

                if (confirmPassword != password) {
                    tvConfirmPasswordWarning.text = "Password does not match"
                    tvConfirmPasswordWarning.visibility = TextView.VISIBLE
                } else {
                    tvConfirmPasswordWarning.visibility = TextView.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        tvToLogin.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

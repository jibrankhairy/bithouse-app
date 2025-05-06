package com.example.bitapp

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvToRegister = findViewById<TextView>(R.id.tvToRegister)
        val rootView = findViewById<LinearLayout>(R.id.main) // Gunakan root layout sebagai anchor Snackbar

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                showSnackbar(rootView, "Email dan password tidak boleh kosong", R.color.red)
            } else {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showSnackbar(rootView, "Login berhasil", R.color.secondary)
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            showSnackbar(
                                rootView,
                                "Login gagal: ${task.exception?.message}",
                                R.color.red
                            )
                        }
                    }
            }
        }

        tvToRegister.setOnClickListener {
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showSnackbar(view: LinearLayout, message: String, colorResId: Int) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, colorResId))
            .setTextColor(ContextCompat.getColor(this, android.R.color.white))
            .show()
    }
}

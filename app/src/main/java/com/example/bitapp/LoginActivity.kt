package com.example.bitapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var loadingDialog: android.app.AlertDialog? = null

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
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvToRegister = findViewById<TextView>(R.id.tvToRegister)
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
        val snackbarAnchor = findViewById<View>(R.id.snackbarAnchor)
        val checkRemember = findViewById<CheckBox>(R.id.checkRemember)

        btnLogin.setOnTouchListener { v, event ->
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

        var isPasswordVisible = false
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_eye)
            } else {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
            etPassword.setSelection(etPassword.text.length)
        }

        val sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val isRemembered = sharedPreferences.getBoolean("rememberMe", false)
        if (isRemembered) {
            val savedEmail = sharedPreferences.getString("email", "")
            val savedPassword = sharedPreferences.getString("password", "")
            etEmail.setText(savedEmail)
            etPassword.setText(savedPassword)
            checkRemember.isChecked = true
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(snackbarAnchor, "Email and password cannot be empty", Snackbar.LENGTH_LONG)
                    .setAnchorView(snackbarAnchor)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.red))
                    .setTextColor(Color.WHITE)
                    .show()
            } else {
                showLoadingDialog()

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        hideLoadingDialog()

                        if (task.isSuccessful) {
                            val user = FirebaseAuth.getInstance().currentUser
                            if (user != null && user.isEmailVerified) {
                                val uid = user.uid
                                getSharedPreferences("MyPrefs", MODE_PRIVATE).edit()
                                    .putString("uid", uid).apply()

                                val loginPrefs = getSharedPreferences("loginPrefs", MODE_PRIVATE)
                                if (checkRemember.isChecked) {
                                    loginPrefs.edit()
                                        .putBoolean("rememberMe", true)
                                        .putString("email", email)
                                        .putString("password", password)
                                        .apply()
                                } else {
                                    loginPrefs.edit().clear().apply()
                                }

                                Snackbar.make(snackbarAnchor, "Login Successful!", Snackbar.LENGTH_LONG)
                                    .setAnchorView(snackbarAnchor)
                                    .setBackgroundTint(ContextCompat.getColor(this, R.color.secondary))
                                    .setTextColor(Color.WHITE)
                                    .show()

                                Handler(Looper.getMainLooper()).postDelayed({
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }, 1000)
                            } else {
                                Snackbar.make(snackbarAnchor, "Please verify your email before logging in", Snackbar.LENGTH_LONG)
                                    .setAnchorView(snackbarAnchor)
                                    .setBackgroundTint(ContextCompat.getColor(this, R.color.red))
                                    .setTextColor(Color.WHITE)
                                    .show()
                                auth.signOut()
                            }
                        } else {
                            Snackbar.make(snackbarAnchor, "Login Failed! Incorrect email or password", Snackbar.LENGTH_LONG)
                                .setAnchorView(snackbarAnchor)
                                .setBackgroundTint(ContextCompat.getColor(this, R.color.red))
                                .setTextColor(Color.WHITE)
                                .show()
                        }
                    }
            }
        }

        tvToRegister.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
            finish()
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this@LoginActivity, ForgotPassword::class.java))
            finish()
        }
    }

    private fun showLoadingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        loadingDialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
    }
}

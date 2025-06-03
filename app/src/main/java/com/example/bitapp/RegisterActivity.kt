package com.example.bitapp

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false
    private var loadingDialog: Dialog? = null

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
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)

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

        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye)
            } else {
                etConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off)
            }
            etConfirmPassword.setSelection(etConfirmPassword.text.length)
        }

        btnRegister.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }
            false
        }

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val idKaryawanStr = etIdKaryawan.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || idKaryawanStr.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showSnackbar("All fields must be filled in", R.color.red)
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                showSnackbar("Passwords do not match", R.color.red)
                return@setOnClickListener
            }

            val idKaryawan = idKaryawanStr.toIntOrNull()
            if (idKaryawan == null) {
                showSnackbar("Employee ID must be numeric", R.color.red)
                return@setOnClickListener
            }

            showLoadingDialog()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        firebaseUser?.sendEmailVerification()
                            ?.addOnCompleteListener { verifyTask ->
                                if (verifyTask.isSuccessful) {
                                    val uid = firebaseUser.uid
                                    val user = hashMapOf(
                                        "firstName" to firstName,
                                        "lastName" to lastName,
                                        "idKaryawan" to idKaryawan,
                                        "email" to email
                                    )

                                    db.collection("users").document(uid).set(user)
                                        .addOnSuccessListener {
                                            hideLoadingDialog()
                                            showSnackbar("Registration successful. Email verification sent!", R.color.secondary)
                                            Handler(Looper.getMainLooper()).postDelayed({
                                                startActivity(Intent(this, LoginActivity::class.java))
                                                finish()
                                            }, 2000)
                                        }
                                        .addOnFailureListener { e ->
                                            hideLoadingDialog()
                                            showSnackbar("Failed to save data: ${e.message}", R.color.red)
                                        }
                                } else {
                                    hideLoadingDialog()
                                    showSnackbar("Failed to send email verification: ${verifyTask.exception?.message}", R.color.red)
                                }
                            }
                    } else {
                        hideLoadingDialog()
                        showSnackbar("Registration failed: ${task.exception?.message}", R.color.red)
                    }
                }
        }

        val tvPasswordWarning = findViewById<TextView>(R.id.tvPasswordWarning)
        val tvConfirmPasswordWarning = findViewById<TextView>(R.id.tvConfirmPasswordWarning)

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().length in 1..5) {
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
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun showLoadingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        loadingDialog = Dialog(this).apply {
            setContentView(dialogView)
            setCancelable(false)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            show()
        }
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun showSnackbar(message: String, colorRes: Int) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.btnRegister)
            .setBackgroundTint(ContextCompat.getColor(this, colorRes))
            .setTextColor(Color.WHITE)
            .show()
    }
}

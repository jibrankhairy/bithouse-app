package com.example.bitapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var etNewPassword: EditText
    private lateinit var etNewConfirmPassword: EditText
    private lateinit var btnContinue: Button
    private lateinit var btnCancel: Button
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var rootLayout: View

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private lateinit var uid: String
    private var loadingDialog: android.app.AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        uid = intent.getStringExtra("uid") ?: ""

        etNewPassword = findViewById(R.id.etNewPassword)
        etNewConfirmPassword = findViewById(R.id.etNewConfirmPassword)
        btnContinue = findViewById(R.id.btnContinue)
        btnCancel = findViewById(R.id.btnCancel)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        rootLayout = findViewById(android.R.id.content)

        btnContinue.setOnClickListener {
            val password = etNewPassword.text.toString().trim()
            val confirmPassword = etNewConfirmPassword.text.toString().trim()

            if (password.length < 6) {
                showSnackbar("Password minimal 6 karakter")
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                showSnackbar("Password tidak cocok")
                return@setOnClickListener
            }

            resetPassword(uid, password)
        }

        btnCancel.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etNewPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_eye)
            } else {
                etNewPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
            etNewPassword.setSelection(etNewPassword.text.length)
        }

        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                etNewConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye)
            } else {
                etNewConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off)
            }
            etNewConfirmPassword.setSelection(etNewConfirmPassword.text.length)
        }

        val tvPasswordWarning = findViewById<TextView>(R.id.tvPasswordWarning)
        val tvConfirmPasswordWarning = findViewById<TextView>(R.id.tvConfirmPasswordWarning)

        etNewPassword.addTextChangedListener(object : TextWatcher {
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

        etNewConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = etNewPassword.text.toString()
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
    }

    private fun showSnackbar(message: String, isSuccess: Boolean = false) {
        val colorRes = if (isSuccess) R.color.secondary else R.color.red

        val snackbar = Snackbar.make(rootLayout, message, Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.btnContinue)
            .setBackgroundTint(ContextCompat.getColor(this, colorRes))
            .setTextColor(Color.WHITE)

        snackbar.show()
    }

    private fun showLoadingDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_loading, null)
        loadingDialog = android.app.AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()
        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
    }

    private fun resetPassword(uid: String, newPassword: String) {
        val url = "http://192.168.1.4:8080/reset-password"

        val jsonBody = JSONObject().apply {
            put("uid", uid)
            put("newPassword", newPassword)
        }

        showLoadingDialog()

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { _ ->
                hideLoadingDialog()

                val snackbar = Snackbar.make(rootLayout, "Password changed successfully", Snackbar.LENGTH_LONG)
                    .setAnchorView(R.id.btnContinue)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.secondary))
                    .setTextColor(Color.WHITE)

                snackbar.addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                })

                snackbar.show()
            },
            { error ->
                hideLoadingDialog()
                showSnackbar("Failed to change password", isSuccess = false)
            }
        )

        Volley.newRequestQueue(this).add(request)
    }
}

package com.example.bitapp

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ForgotPassword : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var loadingDialog: Dialog
    private lateinit var btnSubmit: Button
    private lateinit var snackbarAnchor: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val etEmail = findViewById<EditText>(R.id.etEmailUser)
        btnSubmit = findViewById(R.id.btnSubmit)
        snackbarAnchor = findViewById(R.id.snackbarAnchor)

        setupLoadingDialog()

        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()
            if (email.isEmpty()) {
                Snackbar.make(snackbarAnchor, "Email cannot be empty", Snackbar.LENGTH_LONG)
                    .setAnchorView(snackbarAnchor)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.red))
                    .setTextColor(Color.WHITE)
                    .show()
            } else {
                kirimOtp(email)
            }
        }

        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.backToLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupLoadingDialog() {
        loadingDialog = Dialog(this)
        loadingDialog.setContentView(R.layout.dialog_loading)
        loadingDialog.setCancelable(false)
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showLoading() {
        btnSubmit.isEnabled = false
        loadingDialog.show()
    }

    private fun hideLoading() {
        btnSubmit.isEnabled = true
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    private fun kirimOtp(email: String) {
        showLoading()

        val json = JSONObject().apply {
            put("email", email)
        }

        val mediaType = "application/json".toMediaType()
        val requestBody = json.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.4:8080/request-otp")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    hideLoading()
                    Snackbar.make(snackbarAnchor, "Failed to send OTP", Snackbar.LENGTH_LONG)
                        .setAnchorView(snackbarAnchor)
                        .setBackgroundTint(ContextCompat.getColor(this@ForgotPassword, R.color.red))
                        .setTextColor(Color.WHITE)
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    hideLoading()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val jsonResponse = JSONObject(responseBody ?: "{}")
                        val uid = jsonResponse.optJSONObject("data")?.optString("uid", "") ?: ""

                        if (uid.isNotEmpty()) {
                            Snackbar.make(snackbarAnchor, "OTP successfully sent", Snackbar.LENGTH_LONG)
                                .setAnchorView(snackbarAnchor)
                                .setBackgroundTint(ContextCompat.getColor(this@ForgotPassword, R.color.secondary))
                                .setTextColor(Color.WHITE)
                                .show()

                            android.os.Handler().postDelayed({
                                val intent = Intent(this@ForgotPassword, VerifyOtpActivity::class.java)
                                intent.putExtra("uid", uid)
                                intent.putExtra("email", email)
                                startActivity(intent)
                            }, 2000)
                        } else {
                            Toast.makeText(this@ForgotPassword, "UID tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Snackbar.make(snackbarAnchor, "Email not found", Snackbar.LENGTH_LONG)
                            .setAnchorView(snackbarAnchor)
                            .setBackgroundTint(ContextCompat.getColor(this@ForgotPassword, R.color.red))
                            .setTextColor(Color.WHITE)
                            .show()
                    }
                }
            }
        })
    }
}

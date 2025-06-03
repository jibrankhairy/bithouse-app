package com.example.bitapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import android.text.Editable
import android.text.TextWatcher

class VerifyOtpActivity : AppCompatActivity() {

    private lateinit var otpInputs: List<EditText>
    private lateinit var btnContinue: Button
    private lateinit var tvResendCode: TextView
    private lateinit var tvTimerOtp: TextView
    private lateinit var uid: String
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var snackbarAnchor: View
    private var loadingDialog: AlertDialog? = null

    private val resendDelayMillis = 5 * 60 * 1000L // 5 menit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)

        snackbarAnchor = findViewById(R.id.snackbarAnchor)
        tvTimerOtp = findViewById(R.id.tvTimerOtp)
        tvResendCode = findViewById(R.id.tvResendCode)
        btnContinue = findViewById(R.id.btnContinue)

        uid = intent.getStringExtra("uid") ?: ""

        otpInputs = listOf(
            findViewById(R.id.otp1),
            findViewById(R.id.otp2),
            findViewById(R.id.otp3),
            findViewById(R.id.otp4),
            findViewById(R.id.otp5),
            findViewById(R.id.otp6)
        )

        setupOtpInputAutoMove()

        btnContinue.setOnClickListener {
            val otpCode = otpInputs.joinToString("") { it.text.toString().trim() }

            if (otpCode.length != 6) {
                Snackbar.make(snackbarAnchor, "Fill in all OTP columns", Snackbar.LENGTH_LONG)
                    .setAnchorView(snackbarAnchor)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.red))
                    .setTextColor(Color.WHITE)
                    .show()
                return@setOnClickListener
            }

            showLoadingDialog()
            verifyOtp(uid, otpCode)
        }

        tvResendCode.setOnClickListener {
            if (tvResendCode.isEnabled) {
                showLoadingDialog()
                resendOtp(uid)
            }
        }

        startResendCountdown()
    }

    private fun setupOtpInputAutoMove() {
        for (i in otpInputs.indices) {
            otpInputs[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < otpInputs.size - 1) {
                        otpInputs[i + 1].requestFocus()
                    }
                }
            })
        }
    }

    private fun showLoadingDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        loadingDialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()
        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun verifyOtp(uid: String, otp: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "http://192.168.1.4:8080/verify-otp"

        val jsonBody = JSONObject().apply {
            put("uid", uid)
            put("otp", otp)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { _ ->
                dismissLoadingDialog()
                Snackbar.make(snackbarAnchor, "OTP verified successfully", Snackbar.LENGTH_LONG)
                    .setAnchorView(snackbarAnchor)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.secondary))
                    .setTextColor(Color.WHITE)
                    .show()

                android.os.Handler().postDelayed({
                    val intent = Intent(this, ResetPasswordActivity::class.java)
                    intent.putExtra("uid", uid)
                    startActivity(intent)
                    finish()
                }, 2000)
            },
            { error ->
                dismissLoadingDialog()
                val errorMsg = error.networkResponse?.data?.let {
                    JSONObject(String(it)).optString("message", "Invalid OTP")
                } ?: "Terjadi kesalahan"
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    private fun resendOtp(uid: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "http://192.168.1.4:8080/resend-otp"

        val jsonBody = JSONObject().apply {
            put("uid", uid)
        }

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { _ ->
                dismissLoadingDialog()
                Snackbar.make(snackbarAnchor, "OTP successfully sent", Snackbar.LENGTH_LONG)
                    .setAnchorView(snackbarAnchor)
                    .setBackgroundTint(ContextCompat.getColor(this, R.color.secondary))
                    .setTextColor(Color.WHITE)
                    .show()

                if (::countDownTimer.isInitialized) countDownTimer.cancel()
                startResendCountdown()
            },
            { error ->
                dismissLoadingDialog()
                val errorMsg = error.networkResponse?.data?.let {
                    JSONObject(String(it)).optString("message", "Gagal mengirim ulang OTP")
                } ?: "Terjadi kesalahan"
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    private fun startResendCountdown() {
        tvResendCode.isEnabled = false
        tvResendCode.setTextColor(Color.GRAY)

        countDownTimer = object : CountDownTimer(resendDelayMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val secs = seconds % 60
                tvTimerOtp.text = String.format("Resend available in %02d:%02d", minutes, secs)
            }

            override fun onFinish() {
                tvResendCode.isEnabled = true
                tvResendCode.setTextColor(ContextCompat.getColor(this@VerifyOtpActivity, R.color.secondary))
                tvTimerOtp.text = ""
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
        dismissLoadingDialog()
    }
}

package com.example.bitapp

import FingerprintAdapter
import FingerprintItem
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class FingerprintsProfile : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FingerprintAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var uid: String? = null
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerprints_profile)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        uid = sharedPref.getString("uid", null)

        if (uid == null) {
            Toast.makeText(this, "UID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        recyclerView = findViewById(R.id.recyclerViewFingerprints)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FingerprintAdapter(emptyList()) { fingerprint ->
            showDeleteDialog(fingerprint.key)
        }
        recyclerView.adapter = adapter

        loadFingerprints()
    }

    private fun loadFingerprints() {
        uid?.let { currentUid ->
            firestore.collection("fingerprints").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    val data = doc.data ?: return@addOnSuccessListener
                    val fingerprints = data
                        .filterKeys { it.startsWith("id_fingerprint_") }
                        .map { FingerprintItem(it.key, it.value.toString()) }
                    adapter.updateData(fingerprints)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal ambil data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showDeleteDialog(fingerprintKey: String) {
        val fingerDataKey = fingerprintKey.replace("id_fingerprint_", "finger_data_")
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_fingerprint, null)
        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val btnYes = dialogView.findViewById<Button>(R.id.btn_yes)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)

        btnYes.setOnClickListener {
            dialog.dismiss()

            val lottieView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
            val lottieDialog = android.app.AlertDialog.Builder(this)
                .setView(lottieView)
                .setCancelable(false)
                .create()
            lottieDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            lottieDialog.show()

            uid?.let { currentUid ->
                android.os.Handler(mainLooper).postDelayed({
                    firestore.collection("fingerprints").document(currentUid)
                        .update(
                            mapOf(
                                fingerprintKey to FieldValue.delete(),
                                fingerDataKey to FieldValue.delete()
                            )
                        )
                        .addOnSuccessListener {
                            loadFingerprints()
                            lottieDialog.dismiss()
                            val snackbarAnchor = findViewById<View>(R.id.snackbarAnchor)
                            Snackbar.make(snackbarAnchor, "Fingerprint deleted", Snackbar.LENGTH_LONG)
                                .setAnchorView(snackbarAnchor)
                                .setBackgroundTint(ContextCompat.getColor(this, R.color.secondary))
                                .setTextColor(Color.WHITE)
                                .show()

                            val fingerprintId = fingerprintKey.removePrefix("id_fingerprint_").toIntOrNull()
                            if (fingerprintId != null) {
                                val json = """
                        {
                            "fingerprintId": $fingerprintId
                        }
                    """.trimIndent()

                                val mediaType = "application/json; charset=utf-8".toMediaType()
                                val body = json.toRequestBody(mediaType)

                                val request = Request.Builder()
                                    .url("http://192.168.201.148/delete/start")
                                    .post(body)
                                    .build()

                                client.newCall(request).enqueue(object : Callback {
                                    override fun onFailure(call: Call, e: IOException) {
                                        runOnUiThread {
                                            Toast.makeText(
                                                this@FingerprintsProfile,
                                                "Gagal kirim ke ESP32",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                    override fun onResponse(call: Call, response: Response) {
                                        val respText = response.body?.string()
                                        runOnUiThread {
                                            Toast.makeText(
                                                this@FingerprintsProfile,
                                                "ESP32: $respText",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                })
                            }
                        }
                        .addOnFailureListener {
                            lottieDialog.dismiss()
                            Toast.makeText(this, "Gagal hapus fingerprint", Toast.LENGTH_SHORT).show()
                        }
                }, 3000) // ⏱️ Delay 3 detik
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
}

package com.example.bitapp

import FingerprintAdapter
import FingerprintItem
import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        // Ambil UID dari SharedPreferences
        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        uid = sharedPref.getString("uid", null)

        if (uid == null) {
            Toast.makeText(this, "UID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        recyclerView = findViewById(R.id.recyclerViewFingerprints)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FingerprintAdapter(emptyList()) { fingerprint ->
            deleteFingerprint(fingerprint.key)
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

    private fun deleteFingerprint(fingerprintKey: String) {
        val fingerDataKey = fingerprintKey.replace("id_fingerprint_", "finger_data_")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirmation")
            .setMessage("Do you want to delete this fingerprint?")
            .setPositiveButton("Yes") { _, _ ->
                uid?.let { currentUid ->
                    firestore.collection("fingerprints").document(currentUid)
                        .update(
                            mapOf(
                                fingerprintKey to FieldValue.delete(),
                                fingerDataKey to FieldValue.delete()
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, "Fingerprint deleted", Toast.LENGTH_SHORT).show()
                            loadFingerprints()

                            // Kirim request ke ESP32 untuk hapus fingerprint dari sensor
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
                                    .url("http://192.168.9.148/delete/start") // Ganti IP sesuai ESP32 kamu
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
                            Toast.makeText(this, "Gagal hapus fingerprint", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}

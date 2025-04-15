package com.example.bitapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    private val client = OkHttpClient()
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val tvWelcome: TextView = findViewById(R.id.tvWelcome)
        val btnLogout: Button = findViewById(R.id.btnLogout)
        val btnRegisterFingerprint: Button = findViewById(R.id.btnRegisterFingerprint)

        val user = auth.currentUser
        tvWelcome.text = "Selamat datang, ${user?.email}"

        btnRegisterFingerprint.setOnClickListener {
            registerFingerprint(user?.uid)
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerFingerprint(userId: String?) {
        if (userId == null) {
            Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = firestore.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val userData = document.data

                val json = """
                    {
                        "uid": "$userId"
                    }
                """.trimIndent()

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("http://192.168.1.11:3000/api/register-fingerprint") // ← ganti IP sesuai laptop/ESP bro
                    .post(body)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Gagal kirim ke ESP32: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val responseText = response.body?.string()
                        runOnUiThread {
                            if (response.isSuccessful) {
                                Toast.makeText(this@MainActivity, "Proses register fingerprint dimulai", Toast.LENGTH_SHORT).show()
                                saveFingerprintDataToRealtimeDb(userId, userData)
                            } else {
                                Toast.makeText(this@MainActivity, "ESP32 error: $responseText", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                })

            } else {
                Toast.makeText(this, "Data user tidak ditemukan di Firestore", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal ambil data user", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFingerprintDataToRealtimeDb(uid: String, userData: Map<String, Any>?) {
        if (userData == null) return

        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("fingerprints").child(uid)

        val fingerprintData = HashMap<String, Any>()
        fingerprintData.putAll(userData)
        fingerprintData["uid"] = uid
        fingerprintData["fingerprintId"] = "pending" // nanti diupdate sama ESP32 kalau udah scan

        ref.setValue(fingerprintData).addOnSuccessListener {
            Toast.makeText(this, "Data berhasil disimpan ke Realtime Database", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal simpan data ke Realtime Database", Toast.LENGTH_SHORT).show()
        }
    }
}

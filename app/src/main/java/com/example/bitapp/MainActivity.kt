package com.example.bitapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
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
                val idKaryawan = userData?.get("idKaryawan")?.toString() ?: ""
                val firstName = userData?.get("firstName")?.toString() ?: ""
                val lastName = userData?.get("lastName")?.toString() ?: ""

                // Step 1: Trigger ESP32 untuk scan jari
                val espRequest = Request.Builder()
                    .url("http://192.168.1.8/start-scan") // GANTI IP INI sesuai IP ESP32 kamu
                    .build()

                client.newCall(espRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "ESP32 tidak bisa dihubungi", Toast.LENGTH_SHORT).show()
                        }
                        Log.e("ESP32", "Gagal hubungi ESP32: ${e.message}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val idFingerprint = response.body?.string()?.trim()
                        Log.d("ESP32", "Respons ESP32: $idFingerprint")

                        if (idFingerprint.isNullOrEmpty()) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "ESP32 tidak mengembalikan ID fingerprint", Toast.LENGTH_SHORT).show()
                            }
                            return
                        }

                        // Step 2: Kirim data lengkap ke server Node.js
                        val json = """
                            {
                                "uid": "$userId",
                                "id_karyawan": "$idKaryawan",
                                "firstName": "$firstName",
                                "lastName": "$lastName",
                                "id_fingerprint": $idFingerprint
                            }
                        """.trimIndent()

                        val mediaType = "application/json; charset=utf-8".toMediaType()
                        val body = json.toRequestBody(mediaType)

                        val backendRequest = Request.Builder()
                            .url("http://192.168.1.7:3000/register-fingerprint") // GANTI IP INI ke IP server Node.js
                            .post(body)
                            .build()

                        client.newCall(backendRequest).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                runOnUiThread {
                                    Toast.makeText(this@MainActivity, "Gagal kirim ke server: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                                Log.e("BACKEND", "Gagal kirim ke backend: ${e.message}")
                            }

                            override fun onResponse(call: Call, response: Response) {
                                val responseText = response.body?.string()
                                runOnUiThread {
                                    if (response.isSuccessful) {
                                        Toast.makeText(this@MainActivity, "Fingerprint berhasil diregistrasi", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Gagal register fingerprint: $responseText", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })
                    }
                })

            } else {
                Toast.makeText(this, "Data user tidak ditemukan di Firestore", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal ambil data user", Toast.LENGTH_SHORT).show()
        }
    }
}

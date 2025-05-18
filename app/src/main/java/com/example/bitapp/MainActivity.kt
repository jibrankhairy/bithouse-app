package com.example.bitapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        bottomNavigationView = findViewById(R.id.bottom_navigation_menu)
        loadFragment(HomeFragment())

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }

        val fab_fingerprint = findViewById<CardView>(R.id.fab_fingerprint)
        val user = auth.currentUser

        fab_fingerprint.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage("Apakah Anda ingin registrasi fingerprint?")
                .setPositiveButton("Ya") { _, _ ->
                    sendUserDataToEsp32(user?.uid)
                }
                .setNegativeButton("Batal", null)
                .show()
        }

    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun sendUserDataToEsp32(uid: String?) {
        if (uid == null) {
            Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val userRef = firestore.collection("users").document(uid)

        userRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val idKaryawanStr = document.getString("idKaryawan") ?: ""
                val firstName = document.getString("firstName") ?: ""
                val lastName = document.getString("lastName") ?: ""

                val idKaryawan = idKaryawanStr.toIntOrNull()
                if (idKaryawan == null) {
                    Toast.makeText(this, "ID Karyawan tidak valid", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val json = """
                {
                    "uid": "$uid",
                    "idKaryawan": $idKaryawan,
                    "firstName": "$firstName",
                    "lastName": "$lastName"
                }
            """.trimIndent()

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("http://192.168.53.148/register/start") // GANTI IP sesuai ESP32 kamu
                    .post(body)
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Gagal hubungi ESP32: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Log.e("ESP32", "Error: ${e.message}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val resp = response.body?.string()
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "ESP32: $resp",
                                Toast.LENGTH_SHORT
                            ).show()
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
}

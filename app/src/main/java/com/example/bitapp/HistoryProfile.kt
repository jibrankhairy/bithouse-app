package com.example.bitapp

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context


class HistoryProfile : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "HistoryProfile"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_profile)

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener { finish() }

        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        historyRecyclerView.layoutManager = LinearLayoutManager(this)

        loadHistory()
    }

    private fun loadHistory() {
        val sharedPref = getSharedPreferences("user_pref", Context.MODE_PRIVATE)
        val idKaryawan = sharedPref.getInt("idKaryawan", -1)

        db.collection("absensi").document(idKaryawan.toString())
            .collection("tanggal")
            .get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<AbsensiHistory>()

                for (document in result) {
                    val checkIn = document.get("check_in") as? Map<*, *>
                    val waktuMasuk = checkIn?.get("waktu") as? String ?: "-"
                    val waktuKeluar = document.get("check_out") as? String ?: "-"

                    list.add(
                        AbsensiHistory(
                            tanggal = document.id,
                            waktuMasuk = waktuMasuk,
                            waktuKeluar = waktuKeluar
                        )
                    )
                }

                historyRecyclerView.adapter = HistoryAdapter(list)
                Log.d(TAG, "Berhasil load ${list.size} data")
            }
            .addOnFailureListener {
                Log.e(TAG, "Gagal ambil history", it)
            }
    }
}

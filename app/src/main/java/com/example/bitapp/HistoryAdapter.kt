package com.example.bitapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private val historyList: List<AbsensiHistory>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tanggalText: TextView = view.findViewById(R.id.textTanggal)
        val waktuMasuk: TextView = view.findViewById(R.id.textWaktuMasuk)
        val waktuKeluar: TextView = view.findViewById(R.id.textWaktuKeluar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = historyList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]
        holder.tanggalText.text = "Date: ${item.tanggal}"
        holder.waktuMasuk.text = "Check in: ${item.waktuMasuk}"
        holder.waktuKeluar.text = "Check out: ${item.waktuKeluar}"
    }
}

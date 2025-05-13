import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bitapp.R

class FingerprintAdapter(
    private var fingerprintList: List<FingerprintItem>,
    private val onDeleteClick: (FingerprintItem) -> Unit
) : RecyclerView.Adapter<FingerprintAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fingerprintIdText: TextView = view.findViewById(R.id.fingerprintIdText)
        val deleteButton: ImageView = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fingerprint, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = fingerprintList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fingerprint = fingerprintList[position]
        holder.fingerprintIdText.text = fingerprint.key

        holder.deleteButton.setOnClickListener {
            onDeleteClick(fingerprint)
        }
    }

    fun updateData(newList: List<FingerprintItem>) {
        fingerprintList = newList
        notifyDataSetChanged()
    }
}

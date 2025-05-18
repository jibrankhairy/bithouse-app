package com.example.bitapp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class Notification : AppCompatActivity() {
    private lateinit var backButton: ImageView
    private lateinit var notifContainer: LinearLayout
    private lateinit var defaultLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        backButton = findViewById(R.id.backButton)
        notifContainer = findViewById(R.id.notifContainer)
        defaultLayout = findViewById(R.id.defaultLayout)

        backButton.setOnClickListener {
            finish()
        }

        val sharedPref = getSharedPreferences("notif_pref", MODE_PRIVATE)
        val notifCheckIn = sharedPref.getString("notifCheckIn", "")
        val notifCheckOut = sharedPref.getString("notifCheckOut", "")

        notifContainer.removeAllViews()

        if (notifCheckIn.isNullOrEmpty() && notifCheckOut.isNullOrEmpty()) {
            defaultLayout.visibility = View.VISIBLE
            notifContainer.visibility = View.GONE
        } else {
            defaultLayout.visibility = View.GONE
            notifContainer.visibility = View.VISIBLE

            if (!notifCheckOut.isNullOrEmpty()) {
                notifContainer.addView(createNotifCard("Check out berhasil", notifCheckOut))
            }

            if (!notifCheckIn.isNullOrEmpty()) {
                notifContainer.addView(createNotifCard("Check in berhasil", notifCheckIn))
            }
        }
    }

    private fun createNotifCard(title: String, message: String): View {
        val inflater = layoutInflater
        val card = inflater.inflate(R.layout.notif_card_layout, null)

        val notifIcon = card.findViewById<ImageView>(R.id.notifIcon)
        val notifTitle = card.findViewById<TextView>(R.id.notifTitle)
        val notifTime = card.findViewById<TextView>(R.id.notifTime)

        notifTitle.text = title
        notifTime.text = message

        if (title.contains("Check in", ignoreCase = true)) {
            notifIcon.setImageResource(R.drawable.ic_check_in)
        } else if (title.contains("Check out", ignoreCase = true)) {
            notifIcon.setImageResource(R.drawable.ic_check_out_2)
        }

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, 24)
        card.layoutParams = layoutParams

        return card
    }


}


package com.example.bitapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private lateinit var usernameText: TextView
    private lateinit var employeeIdText: TextView
    private lateinit var emailText: TextView
    private lateinit var logoutCard: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_fragment_profile, container, false)

        val cardEditProfile = view.findViewById<CardView>(R.id.editProfile)
        val cardFingerprintsProfile = view.findViewById<CardView>(R.id.fingerprintsProfile)
        val cardHistoryProfile = view.findViewById<CardView>(R.id.historyProfile)

        cardEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfile::class.java))
        }

        cardFingerprintsProfile.setOnClickListener {
            startActivity(Intent(requireContext(), FingerprintsProfile::class.java))
        }

        cardHistoryProfile.setOnClickListener {
            startActivity(Intent(requireContext(), HistoryProfile::class.java))
        }

        usernameText = view.findViewById(R.id.usernameText)
        employeeIdText = view.findViewById(R.id.employeeIdText)
        emailText = view.findViewById(R.id.emailText)
        logoutCard = view.findViewById(R.id.logoutProfile)

        val sharedPref = requireContext().getSharedPreferences("user_pref", Context.MODE_PRIVATE)
        val firstName = sharedPref.getString("firstName", "")
        val lastName = sharedPref.getString("lastName", "")
        val idKaryawan = sharedPref.getInt("idKaryawan", 0)
        val email = sharedPref.getString("email", "")

        usernameText.text = "$firstName $lastName"
        employeeIdText.text = "ID: $idKaryawan"
        emailText.text = email

        logoutCard.setOnClickListener {
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_logout, null)
            val alertDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create()

            val btnYes = dialogView.findViewById<Button>(R.id.btn_yes)
            val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)

            btnYes.setOnClickListener {
                alertDialog.dismiss()
                sharedPref.edit().clear().apply()
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }

            btnCancel.setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            alertDialog.show()
        }

        return view
    }
}

package com.example.bitapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class HomeFragment : Fragment() {

    private lateinit var usernameText: TextView
    private lateinit var employeeIdText: TextView
    private lateinit var checkInText: TextView
    private lateinit var checkOutText: TextView
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_fragment_home, container, false)

        val notificationIcon = view.findViewById<FrameLayout>(R.id.notificationIcon)
        val calendarIcon = view.findViewById<FrameLayout>(R.id.calendarIcon)

        notificationIcon.setOnClickListener {
            val intent = Intent(requireContext(), Notification::class.java)
            startActivity(intent)
        }

        calendarIcon.setOnClickListener {
            val intent = Intent(requireContext(), Calendar::class.java)
            startActivity(intent)
        }

        usernameText = view.findViewById(R.id.usernameText)
        employeeIdText = view.findViewById(R.id.employeeIdText)
        checkInText = view.findViewById(R.id.CheckInText)
        checkOutText = view.findViewById(R.id.CheckOutText)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        viewModel.userProfile.observe(viewLifecycleOwner, Observer { profile ->
            usernameText.text = "${profile.firstName} ${profile.lastName}"
            employeeIdText.text = "ID: ${profile.idKaryawan}"

            val sharedPref = requireContext().getSharedPreferences("user_pref", android.content.Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("firstName", profile.firstName)
                putString("lastName", profile.lastName)
                putString("idKaryawan", profile.idKaryawan)
                putString("email", profile.email)
                apply()
            }

            fetchAbsensi(profile.idKaryawan)
        })

        viewModel.fetchUserProfile()

        return view
    }

    private fun fetchAbsensi(idKaryawan: String) {
        val url = "http://192.168.1.3:3001/absensi/$idKaryawan"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("HomeFragment", "Response body: $responseBody")

                responseBody?.let { body ->
                    val json = JSONObject(body)
                    val checkInRaw = json.optString("check_in", "")
                    val checkOutRaw = json.optString("check_out", "")

                    val checkInTime = extractTime(checkInRaw)
                    val checkOutTime = extractTime(checkOutRaw)

                    activity?.runOnUiThread {
                        checkInText.text = checkInTime ?: "—"
                        checkOutText.text = checkOutTime ?: "—"
                    }
                }
            }
        })
    }

    private fun extractTime(datetime: String): String? {
        return try {
            val timePart = datetime.split(",").getOrNull(1)?.trim() // "09.58.43"
            timePart?.split(".")?.take(2)?.joinToString(":") // jadi "09:58"
        } catch (e: Exception) {
            null
        }
    }
}

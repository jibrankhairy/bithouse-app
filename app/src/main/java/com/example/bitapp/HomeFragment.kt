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
    private lateinit var statusInText: TextView
    private lateinit var totalHoursText: TextView
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_fragment_home, container, false)

        val notificationIcon = view.findViewById<FrameLayout>(R.id.notificationIcon)
        val calendarIcon = view.findViewById<FrameLayout>(R.id.calendarIcon)

        notificationIcon.setOnClickListener {
            startActivity(Intent(requireContext(), Notification::class.java))
        }

        calendarIcon.setOnClickListener {
            startActivity(Intent(requireContext(), Calendar::class.java))
        }

        usernameText = view.findViewById(R.id.usernameText)
        employeeIdText = view.findViewById(R.id.employeeIdText)
        statusInText = view.findViewById(R.id.StatusInText)
        checkInText = view.findViewById(R.id.CheckInText)
        checkOutText = view.findViewById(R.id.CheckOutText)
        totalHoursText = view.findViewById(R.id.TotalHoursText)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        viewModel.userProfile.observe(viewLifecycleOwner, Observer { profile ->
            usernameText.text = "${profile.firstName} ${profile.lastName}"
            employeeIdText.text = "ID: ${profile.idKaryawan}"

            val sharedPref = requireContext().getSharedPreferences("user_pref", android.content.Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("firstName", profile.firstName)
                putString("lastName", profile.lastName)
                putInt("idKaryawan", profile.idKaryawan)
                putString("email", profile.email)
                apply()
            }

            fetchAbsensi(profile.idKaryawan)
        })

        viewModel.fetchUserProfile()

        return view
    }

    private fun fetchAbsensi(idKaryawan: Int) {
        val url = "https://api-bit-2-429534243481.asia-southeast2.run.app/absensi/$idKaryawan"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    checkInText.text = "Gagal memuat"
                    checkOutText.text = "Gagal memuat"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("HomeFragment", "Response body: $responseBody")

                responseBody?.let { body ->
                    val json = JSONObject(body)
                    val data = json.optJSONObject("data")

                    val checkInObj = data?.optJSONObject("check_in")
                    val checkInRaw = checkInObj?.optString("waktu", "") ?: ""
                    val statusInRaw = checkInObj?.optString("status", "") ?: ""

                    val rawOut = data?.opt("check_out")
                    val checkOutRaw = if (rawOut != null && rawOut != JSONObject.NULL) rawOut.toString() else ""

                    activity?.runOnUiThread {
                        checkInText.text = if (checkInRaw.isNotEmpty()) formatJam(checkInRaw) else "-"
                        statusInText.text = if (statusInRaw.isNotEmpty()) statusInRaw else "Not yet checked in"
                        checkOutText.text = if (checkOutRaw.isNotEmpty()) formatJam(checkOutRaw) else "-"

                        val sharedPref = requireContext().getSharedPreferences("notif_pref", android.content.Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("notifCheckIn", if (checkInRaw.isNotEmpty()) "Check in was successful at ${formatJam(checkInRaw)} WIB" else "")
                            putString("notifCheckOut", if (checkOutRaw.isNotEmpty()) "Check out was successful at ${formatJam(checkOutRaw)} WIB" else "")
                            apply()
                        }

                        if (checkInRaw.isNotEmpty() && checkOutRaw.isNotEmpty()) {
                            try {
                                val inParts = checkInRaw.split(".")
                                val outParts = checkOutRaw.split(".")

                                if (inParts.size >= 2 && outParts.size >= 2) {
                                    val jamIn = inParts[0].toInt()
                                    val menitIn = inParts[1].toInt()
                                    val jamOut = outParts[0].toInt()
                                    val menitOut = outParts[1].toInt()

                                    val totalMenitIn = jamIn * 60 + menitIn
                                    val totalMenitOut = jamOut * 60 + menitOut
                                    val selisihMenit = totalMenitOut - totalMenitIn

                                    totalHoursText.text = when {
                                        selisihMenit < 0 -> "-"
                                        selisihMenit < 60 -> "$selisihMenit min"
                                        selisihMenit % 60 == 0 -> "${selisihMenit / 60} h"
                                        else -> "${selisihMenit / 60} h ${selisihMenit % 60} min"
                                    }
                                } else {
                                    totalHoursText.text = "-"
                                }
                            } catch (e: Exception) {
                                Log.e("HomeFragment", "Error parsing jam: $e")
                                totalHoursText.text = "-"
                            }
                        } else {
                            totalHoursText.text = "-"
                        }
                    }
                }
            }
        })
    }

    private fun formatJam(jam: String): String {
        return try {
            val parts = jam.split(".")
            if (parts.size >= 2) "${parts[0]}:${parts[1]}" else jam
        } catch (e: Exception) {
            "—"
        }
    }
}


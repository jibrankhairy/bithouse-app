package com.example.bitapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar


class HomeFragment : Fragment() {

    private lateinit var usernameText: TextView
    private lateinit var employeeIdText: TextView
    private lateinit var checkInText: TextView
    private lateinit var checkOutText: TextView
    private lateinit var statusInText: TextView
    private lateinit var totalHoursText: TextView
    private lateinit var breakTimeText: TextView
    private lateinit var viewModel: HomeViewModel

    private val handler = Handler(Looper.getMainLooper())
    private var breakRunnable: Runnable? = null
    private val BREAK_START_HOUR = 12
    private val BREAK_DURATION_MINUTES = 30

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
        breakTimeText = view.findViewById(R.id.BreakTimeText)

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

        checkBreakReminder()

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

    private fun checkBreakReminder() {
        val prefs = requireContext().getSharedPreferences("break_pref", android.content.Context.MODE_PRIVATE)
        val now = Calendar.getInstance()

        val breakStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, BREAK_START_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val breakEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, BREAK_START_HOUR)
            set(Calendar.MINUTE, BREAK_DURATION_MINUTES)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val savedTimestamp = prefs.getLong("breakStartTime", -1L)

        if (savedTimestamp == -1L && now.timeInMillis >= breakStart.timeInMillis && now.timeInMillis < breakEnd.timeInMillis) {
            prefs.edit().putLong("breakStartTime", breakStart.timeInMillis).apply()
            showBreakStartPopup()
            startBreakCountdown(breakStart.timeInMillis)
        } else if (savedTimestamp != -1L) {
            if (now.timeInMillis in savedTimestamp until savedTimestamp + BREAK_DURATION_MINUTES * 60 * 1000) {
                startBreakCountdown(savedTimestamp)
            } else if (now.timeInMillis >= savedTimestamp + BREAK_DURATION_MINUTES * 60 * 1000) {
                showBreakEndPopup()
                prefs.edit().remove("breakStartTime").apply()
            }
        }
    }

    private fun startBreakCountdown(startTimeMillis: Long) {
        breakRunnable?.let { handler.removeCallbacks(it) }

        breakRunnable = object : Runnable {
            override fun run() {
                val now = System.currentTimeMillis()
                val endTime = startTimeMillis + BREAK_DURATION_MINUTES * 60 * 1000
                val remaining = endTime - now

                if (remaining > 0) {
                    val minutes = (remaining / 1000) / 60
                    val seconds = (remaining / 1000) % 60
                    breakTimeText.text = String.format("%02d:%02d remaining", minutes, seconds)
                    handler.postDelayed(this, 1000)
                } else {
                    showBreakEndPopup()
                    breakTimeText.text = "Break selesai"
                    requireContext().getSharedPreferences("break_pref", android.content.Context.MODE_PRIVATE)
                        .edit().remove("breakStartTime").apply()
                }
            }
        }

        handler.post(breakRunnable!!)
    }

    private fun showBreakStartPopup() {
        Toast.makeText(requireContext(), "Waktunya istirahat!", Toast.LENGTH_LONG).show()
    }

    private fun showBreakEndPopup() {
        Toast.makeText(requireContext(), "Waktu istirahat selesai!", Toast.LENGTH_LONG).show()
    }
}

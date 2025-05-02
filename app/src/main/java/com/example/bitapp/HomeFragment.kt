package com.example.bitapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class HomeFragment : Fragment() {

    private lateinit var usernameText: TextView
    private lateinit var employeeIdText: TextView
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_fragment_home, container, false)

        usernameText = view.findViewById(R.id.usernameText)
        employeeIdText = view.findViewById(R.id.employeeIdText)

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
        })

        viewModel.fetchUserProfile()

        return view
    }
}

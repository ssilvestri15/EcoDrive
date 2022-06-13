package com.silvered.ecodrive.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.silvered.ecodrive.R
import com.silvered.ecodrive.databinding.ActivityMainBinding
import com.silvered.ecodrive.fragments.AnalyticsFragment
import com.silvered.ecodrive.fragments.HomeFragment
import com.silvered.ecodrive.fragments.ProfileFragment


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = Firebase.database.reference

        database.child("form").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                val enabled = snapshot.child("enabled").getValue(Boolean::class.java)

                if (enabled != null && enabled) {

                    val linkFinal = if (isGamified())
                        snapshot.child("linkG").getValue(String::class.java)
                    else
                        snapshot.child("linkNonG").getValue(String::class.java)

                    binding.btnQuestPre.visibility = View.GONE
                    binding.btnQuestFinal.visibility = View.GONE

                    if (isURLValid(linkFinal).not()) {
                        binding.cardQuest.visibility = View.GONE
                        return
                    }

                    Firebase.auth.currentUser?.let {
                        database.child("users").child(it.uid)
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {

                                    val final = snapshot.child("linkFinalCompleted").getValue(Boolean::class.java)

                                    if (final != null && final) {
                                        binding.cardQuest.visibility = View.GONE
                                        binding.btnQuestPre.visibility = View.GONE
                                        binding.btnQuestFinal.visibility = View.GONE
                                        return
                                    }

                                    setBtnForm(binding.btnQuestFinal, final, linkFinal, "FINAL")
                                    binding.cardQuest.visibility = View.VISIBLE
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    binding.cardQuest.visibility = View.GONE
                                }

                            })
                    }

                    return
                }

                binding.cardQuest.visibility = View.GONE

            }

            override fun onCancelled(error: DatabaseError) {
                binding.cardQuest.visibility = View.GONE
            }

        })

        val isGamified = isGamified()

        val homeFragment = HomeFragment()
        val profileFragment = ProfileFragment()
        val analyticsFragment = AnalyticsFragment()

        binding.bottomNavigation.menu.getItem(0).isChecked = true
        setFragment(homeFragment)

        if (!isGamified)
            binding.bottomNavigation.menu.findItem(R.id.page_1).isVisible = false

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.page_1 -> {
                    setFragment(analyticsFragment)
                    true
                }
                R.id.page_2 -> {
                    setFragment(homeFragment)
                    true
                }
                R.id.page_3 -> {
                    setFragment(profileFragment)
                    true
                }
                else -> false
            }

        }


    }

    private fun isGamified(): Boolean {
        val sharedPreferences = getSharedPreferences("info", MODE_PRIVATE)
        return sharedPreferences.getBoolean("isGamified", false)
    }

    fun setFragment(fr: Fragment) {
        val frag = supportFragmentManager.beginTransaction()
        frag.replace(R.id.fragment_container, fr)
        frag.commit()
    }

    private fun isURLValid(url: String?): Boolean {

        if (url == null)
            return false

        if (url.trim().isEmpty())
            return false

        if (url.contains("http", ignoreCase = true).not())
            return false

        return true
    }

    private fun setBtnForm(btn: MaterialButton, userAlreadyComplete: Boolean?, link: String?, type: String) {
        when (userAlreadyComplete) {

            true -> btn.visibility = View.GONE
            else -> {

                link?.let { url ->

                    when (isURLValid(url)) {
                        false -> btn.visibility = View.GONE
                        true -> {
                            btn.visibility = View.VISIBLE
                            btn.setOnClickListener {
                                val intent = Intent(this@MainActivity, FormActivity::class.java)
                                intent.putExtra("URL", url)
                                intent.putExtra("TYPE", type)
                                startActivity(intent)
                            }
                        }
                    }

                }

            }
        }
    }

}
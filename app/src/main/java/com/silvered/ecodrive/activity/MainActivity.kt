package com.silvered.ecodrive.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.silvered.ecodrive.fragments.AnalyticsFragment
import com.silvered.ecodrive.fragments.HomeFragment
import com.silvered.ecodrive.fragments.ProfileFragment
import com.silvered.ecodrive.R
import com.silvered.ecodrive.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val homeFragment = HomeFragment()
        val profileFragment = ProfileFragment()
        val analyticsFragment = AnalyticsFragment()

        binding.bottomNavigation.menu.getItem(0).isChecked = true
        setFragment(homeFragment)


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

    fun setFragment(fr: Fragment) {
        val frag = supportFragmentManager.beginTransaction()
        frag.replace(R.id.fragment_container, fr)
        frag.commit()
    }

}
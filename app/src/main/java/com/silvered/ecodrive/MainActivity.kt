package com.silvered.ecodrive

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.silvered.ecodrive.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.menu.getItem(1).isChecked = true
        setFragment(HomeFragment())

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.page_1 -> {
                    setFragment(AnalyticsFragment())
                    true
                }
                R.id.page_2 -> {
                    setFragment(HomeFragment())
                    true
                }
                R.id.page_3 -> {
                    setFragment(ProfileFragment())
                    true
                }
                else -> false
            }

        }

    }

    private fun setFragment(fr: Fragment) {
        val frag = supportFragmentManager.beginTransaction()
        frag.replace(R.id.fragment_container, fr)
        frag.commit()
    }

}
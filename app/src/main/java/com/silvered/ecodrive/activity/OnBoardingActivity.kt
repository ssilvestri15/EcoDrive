package com.silvered.ecodrive.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.silvered.ecodrive.R
import com.silvered.ecodrive.databinding.ActivityCountryBinding
import com.silvered.ecodrive.databinding.ActivityOnBoardingBinding
import com.silvered.ecodrive.fragments.OnBoardFirstFragment

class OnBoardingActivity : FragmentActivity() {

    private lateinit var binding: ActivityOnBoardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnBoardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pager.adapter = ViewPagerAdapter(this)

    }

    fun endPage() {
        val sharedPreferences = getSharedPreferences("info", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isFirstTime",false).commit()
        val intent = Intent(this@OnBoardingActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun goNext() {
        if (binding.pager.currentItem in 0..2)
            binding.pager.currentItem = binding.pager.currentItem + 1
    }

    override fun onBackPressed() {

        if (binding.pager.currentItem == 0)
            super.onBackPressed()
         else
            binding.pager.currentItem = binding.pager.currentItem - 1

    }

    private inner class ViewPagerAdapter(fragment: FragmentActivity): FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int {
            return 3
        }

        override fun createFragment(position: Int): Fragment {
            return when(position) {
                0 -> OnBoardFirstFragment.newInstance(R.layout.fragment_on_board_first)
                1 -> OnBoardFirstFragment.newInstance(R.layout.fragment_on_board_second)
                2 -> OnBoardFirstFragment.newInstance(R.layout.fragment_on_board_third)
                else -> OnBoardFirstFragment.newInstance(R.layout.fragment_on_board_first)
            }

        }

    }
}
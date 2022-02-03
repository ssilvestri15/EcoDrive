package com.silvered.ecodrive.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.silvered.ecodrive.databinding.ActivitySettingsBinding
import com.silvered.ecodrive.fragments.HomeFragment
import com.silvered.ecodrive.util.helpers.HomeHelper
import com.silvered.ecodrive.util.helpers.RankHelper


class SettingsActivity : AppCompatActivity() {


    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("info", MODE_PRIVATE)
        sharedPreferencesEditor = sharedPreferences.edit()

        val isCarlaActive = sharedPreferences.getBoolean("carla",false)

        binding.carlaSimulatorSwitch.isChecked = isCarlaActive

        binding.carlaSimulatorSwitch.setOnCheckedChangeListener { _, value ->
            sharedPreferencesEditor.putBoolean("carla",value).commit()
        }

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            HomeHelper.resetAll()
            RankHelper.resetAll()
            sharedPreferencesEditor.clear().commit()
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent)
            finish()
        }
    }
}
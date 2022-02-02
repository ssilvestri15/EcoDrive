package com.silvered.ecodrive

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.silvered.ecodrive.databinding.ActivityCountryBinding
import com.silvered.ecodrive.util.CountryHelper
import java.util.*
import kotlin.collections.ArrayList

class CountryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCountryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isLogin = intent.getBooleanExtra("isLogin", false)

        if (!isLogin) {
            binding.backbtnNaz.visibility = View.VISIBLE
            binding.backbtnNaz.setOnClickListener {
                onBackPressed()
            }
        }

        binding.nazList.setText(Locale.getDefault().displayCountry)
        setRegion(Locale.getDefault().getDisplayCountry(Locale.ENGLISH))

        val map: HashMap<String, String> = HashMap()

        val locales: Array<Locale> = Locale.getAvailableLocales()
        val countries = ArrayList<String>()
        for (locale in locales) {
            val country: String = locale.displayCountry
            if (country.trim { it <= ' ' }.isNotEmpty() && !countries.contains(country)) {
                countries.add(country)
                map[country] = locale.getDisplayCountry(Locale.ENGLISH)
            }
        }

        countries.sort()


        val arrayAdapter = ArrayAdapter(this, R.layout.list_item, countries)
        binding.nazList.setAdapter(arrayAdapter)
        binding.nazList.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val selected = arrayAdapter.getItem(position).toString()
                setRegion(selected)
            }

        binding.continueBtnNaz.setOnClickListener {

            val choosedNaz = binding.nazList.text.toString()
            val en_choosedNaz = map[choosedNaz]

            val choosedReg = binding.regList.text.toString()

            val sharedPref = getSharedPreferences("info",MODE_PRIVATE).edit()
            sharedPref.putString("nazione", en_choosedNaz).commit()
            sharedPref.putString("regione", choosedReg).commit()
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                FirebaseDatabase.getInstance().reference.child("users/${currentUser.uid}/nazione")
                    .setValue(en_choosedNaz)
                FirebaseDatabase.getInstance().reference.child("users/${currentUser.uid}/regione")
                    .setValue(choosedReg)
            }

            if (isLogin) {
                startActivity(Intent(this@CountryActivity, MainActivity::class.java))
            }

            finish()

        }


    }

    private fun setRegion(displayCountry: String) {
        CountryHelper.getRegioni(displayCountry, object: CountryHelper.CountryInterface {
            override fun onListReceived(list: ArrayList<String>) {

                if (binding.progressCountry.visibility == View.VISIBLE) {
                    binding.progressCountry.visibility = View.GONE
                    binding.textInputLayout2.visibility = View.VISIBLE
                    binding.continueBtnNaz.visibility = View.VISIBLE
                }

                list.sort()
                val newlist = list.distinct().toList() as ArrayList<String>
                binding.regList.setText(newlist[0])
                val adapter = ArrayAdapter(this@CountryActivity, R.layout.list_item, newlist)
                binding.regList.setAdapter(adapter)
            }
        })
    }
}
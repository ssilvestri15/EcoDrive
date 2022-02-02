package com.silvered.ecodrive

import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.google.android.material.color.MaterialColors
import com.silvered.ecodrive.databinding.ActivityRoutesCarlaBinding

class RoutesActivityCarla : AppCompatActivity() {

    private lateinit var binding: ActivityRoutesCarlaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutesCarlaBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val peso = intent.getFloatExtra(RoutesActivity.PESO_MEDIO, 0f)
        val km = intent.getFloatExtra(RoutesActivity.KM_PERCORSI, 0f)
        val vel = intent.getFloatExtra(RoutesActivity.VEL_MEDIA, 0f)
        val punteggio = intent.getFloatExtra(RoutesActivity.PUNTEGGIO, 0f)
        val data = intent.getStringExtra(RoutesActivity.DATA)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        val colorPrimary = MaterialColors.getColor(
            this,
            android.R.attr.colorPrimary,
            ContextCompat.getColor(this, R.color.pink)
        )

        binding.layoutResoconto.dataResocoto.text = data
        binding.layoutResoconto.kmTvEndSession.text = km.toInt().toString()
        binding.layoutResoconto.punteggioTvEndSession.text = punteggio.toInt().toString()
        binding.layoutResoconto.velTvEndSession.text = vel.toInt().toString()

        when (peso) {
            in 0f..0.33f -> {
                ImageViewCompat.setImageTintList(binding.layoutResoconto.foglia1, ColorStateList.valueOf(colorPrimary))
            }
            in 0.34f..0.66f -> {
                ImageViewCompat.setImageTintList(binding.layoutResoconto.foglia1, ColorStateList.valueOf(colorPrimary))
                ImageViewCompat.setImageTintList(binding.layoutResoconto.foglia2, ColorStateList.valueOf(colorPrimary))
            }
            else -> {
                ImageViewCompat.setImageTintList(binding.layoutResoconto.foglia1, ColorStateList.valueOf(colorPrimary))
                ImageViewCompat.setImageTintList(binding.layoutResoconto.foglia2, ColorStateList.valueOf(colorPrimary))
                ImageViewCompat.setImageTintList(binding.layoutResoconto.foglia3, ColorStateList.valueOf(colorPrimary))
            }
        }

    }
}
package com.silvered.ecodrive

import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.silvered.ecodrive.databinding.ActivityWinnerBinding
import kotlin.random.Random.Default.nextInt

class WinnerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWinnerBinding

    private class Product(val name: String, val imageID: Int)

    private val productListLevel2 = arrayListOf(Product("Spazzolino da denti in bamboo!", R.drawable.spazzolino))
    private val productListLevel3 = arrayListOf(Product("Borsa di stoffa!", R.drawable.shopper))
    private val productListLevel4 = arrayListOf(Product("Semi di una pianta a sorpresa!", R.drawable.seed))
    private val productListLevel5 = arrayListOf(Product("Bottoglia in alluminio!", R.drawable.bottle))
    private val productListLevel6 = arrayListOf(Product("Bonsai!", R.drawable.bonsai))

    companion object {
        val LEVEL = "level"
        val NAME  = "fullname"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWinnerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val level = intent.getIntExtra(LEVEL,0)
        var fullname = intent.getStringExtra(NAME)

        if (level < 2) //Errore
            finish()

        if (fullname == null)
            fullname = ""

        if (fullname.trim().isNotEmpty()) {
            fullname = fullname.split(" ")[0]
            binding.titleTV.text = "Congratulazioni $fullname!\nHai vinto un premio!"
        }

        var product = Product("Spazzolino da denti in bamboo!", R.drawable.spazzolino)

        when (level) {
            2 -> {product = productListLevel2[nextInt(0,productListLevel2.size-1)]}
            3 -> {product = productListLevel2[nextInt(0,productListLevel3.size-1)]}
            4 -> {product = productListLevel2[nextInt(0,productListLevel4.size-1)]}
            5 -> {product = productListLevel2[nextInt(0,productListLevel5.size-1)]}
            6 -> {product = productListLevel2[nextInt(0,productListLevel6.size-1)]}
        }

        binding.imageView.setImageDrawable(ContextCompat.getDrawable(this@WinnerActivity,product.imageID))
        binding.nomeProdottoTv.text = product.name

        binding.grazieBtn.setOnClickListener {
            onBackPressed()
        }

    }

    override fun onBackPressed() {
        finish()
    }


}
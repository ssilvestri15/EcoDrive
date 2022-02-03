package com.silvered.ecodrive.util.helpers

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

object CountryHelper {

    private interface CountryListInterface {

        @GET("countriesToCities.json")
        fun getCountryList(): Call<Map<String,ArrayList<String>>>

        companion object {

            var BASE_URL = "https://raw.githubusercontent.com/David-Haim-zz/CountriesToCitiesJSON/master/"

            fun create() : CountryListInterface {

                val retrofit = Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(BASE_URL)
                    .build()
                return retrofit.create(CountryListInterface::class.java)

            }
        }
    }

    interface CountryInterface {
        fun onListReceived(list: ArrayList<String>)
    }

    private var countryList: Map<String,ArrayList<String>>? = null

    private val regioniList = arrayListOf(
        "Abruzzo",
        "Basilicata",
        "Calabria",
        "Campania",
        "Emilia-Romagna",
        "Friuli Venezia Giulia",
        "Lazio",
        "Liguria",
        "Lombardia",
        "Marche",
        "Molise",
        "Piemonte",
        "Puglia",
        "Sardegna",
        "Sicilia",
        "Toscana",
        "Trentino-Alto Adige",
        "Umbria",
        "Valle d'Aosta",
        "Veneto"
    )

    private fun getRegioniItalia(): ArrayList<String> {
        return regioniList
    }


    fun getRegioni(nazione: String, listener: CountryInterface) {

        if (nazione == "Italy") {
            listener.onListReceived(getRegioniItalia())
            return
        }

        if (countryList != null) {
            listener.onListReceived(countryList!![nazione]!!)
            return
        }

        val apiInterface = CountryListInterface.create().getCountryList()

        apiInterface.enqueue( object : Callback<Map<String,ArrayList<String>>> {

            override fun onResponse(
                call: Call<Map<String, ArrayList<String>>>,
                response: Response<Map<String, ArrayList<String>>>
            ) {
                if(response.body() != null) {
                    countryList = response.body()!!
                    listener.onListReceived(countryList!![nazione]!!)
                }
            }

            override fun onFailure(call: Call<Map<String, ArrayList<String>>>, t: Throwable) {
            }
        })

    }

}


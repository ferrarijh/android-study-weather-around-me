package com.jonathan.practice.weatheraroundme

import android.content.Context
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel: ViewModel(){
    private val weatherService = Repository.weatherService

    val location = MutableLiveData<Location>()
    val locationLatLng = Transformations.map(location){ LatLng(it.latitude, it.longitude) }

    val weather = MutableLiveData<JsonObject>()
    val weatherIcon = Transformations.map(weather){it.getAsJsonArray("weather")[0].asJsonObject["icon"].asString}
    val weatherDesc = Transformations.map(weather){it.getAsJsonArray("weather")[0].asJsonObject["main"].asString}
    val weatherCountry = Transformations.map(weather){
        val res = it["sys"].asJsonObject["country"]?.asString
        Log.d("", "country: $res")
        res}
    val weatherLocation = Transformations.map(weather){ it["name"].asString}
    val weatherTemperature = Transformations.map(weather){it.getAsJsonObject("main")}
    val weatherWind = Transformations.map(weather){it["wind"].asJsonObject["speed"].asString + " m/s"}

    val isMyLocationEnabled = MutableLiveData<Boolean>()

    fun getWeather(latLng: LatLng, context: Context) {
        val (lat, lng) = arrayOf(String.format("%.2f", latLng.latitude), String.format("%.2f", latLng.longitude))
        weatherService
            .getWeather(lat, lng, Key.VALUE)
            .enqueue(object: Callback<JsonObject>{
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if(response.isSuccessful)
                        weather.value = response.body()
                    else {
                        Toast.makeText(
                            context,
                            "FAILED: code-${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("", "FAILED: code-${response.code()}")
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(context, "FAILED(onFailure): ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.d("", "FAILED(onFailure): ${t.message}")
                }
            })
    }

    init{
        Log.d("", "MainViewModel initialized")
    }
}
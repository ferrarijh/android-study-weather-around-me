package com.jonathan.practice.weatheraroundme

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.Call
import retrofit2.http.Path
import retrofit2.http.Query

interface WeatherService {
    @GET("data/2.5/weather")
    fun getWeather(
        @Query("lat")lat: String,
        @Query("lon")lon: String,
        @Query("appid")key: String
    ): Call<JsonObject>

}
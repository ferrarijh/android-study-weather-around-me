# Weather Around Me - android application
* Shows weather on my location(gps/network) or locations set manually on map.
* Used: Retrofit2, MapView(Google Map), FusedLocationProviderClient, ViewModel + LiveData
* Server: openweathermap api

## Demo
<div>
    <img src="https://github.com/ferrarijh/android-study-weather-around-me/blob/master/demo/weather-around-me.gif"/>
</div>

### Furthermore...
* Under sdk 21 the weather api doesn't work since CA of openweathermap api is not listed in trusted CAs.
(base URL for retrofit starts with "https" in the app)
* MapView(google map) in fragment dies on rotation - probably not within activity?
* Null check!!
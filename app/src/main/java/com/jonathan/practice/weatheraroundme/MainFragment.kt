package com.jonathan.practice.weatheraroundme

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.layout_collapsing.*
import java.util.*

class MainFragment: Fragment(), OnMapReadyCallback{

    companion object{
        const val REQUEST_GPS = 1994
    }

    private val parent by lazy{requireActivity() as AppCompatActivity}
    private val fab by lazy {parent.findViewById<FloatingActionButton>(R.id.fab)}
    private val mViewModel by lazy{ ViewModelProvider(this).get(MainViewModel::class.java) }

    private val fusedLocationClient by lazy{ LocationServices.getFusedLocationProviderClient(parent)}
    private lateinit var locationCallback : LocationCallback
    private lateinit var locationRequest: LocationRequest
//    private lateinit var locationListener: LocationListener
//    private val locationManager by lazy{parent.getSystemService(Context.LOCATION_SERVICE) as LocationManager}
//    private lateinit var locationListener: LocationListener
    private val mapView by lazy{layout_fragment_main.findViewById(R.id.map) as MapView}
    private var gMap: GoogleMap? = null

    private lateinit var pinPersonBitmap: Bitmap
    private lateinit var pinBitmap: Bitmap

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_main, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setMap(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()

        setFAB()
        setPinBitmap()
        //setLocationManager()
        setLocationClient()
        setPermission()
        setToolbar()
        setWeatherObservers()

        mViewModel.locationLatLng.observe(viewLifecycleOwner){
            getWeather()
            setPinTo(it)
            if(mViewModel.isMyLocationEnabled.value == true)
                moveCameraTo(it)
        }
    }

    private fun setWeatherObservers(){
        mViewModel.apply{
            weatherCountry.observe(viewLifecycleOwner){
                parent.tv_country.text = Locale("", it).displayCountry
            }
            weatherIcon.observe(viewLifecycleOwner){
                val url = "https://openweathermap.org/img/wn/$it@2x.png"
                Glide.with(parent).load(url).override(200).into(parent.iv_weather)
            }
            weatherTemperature.observe(viewLifecycleOwner){
                val temp = (it["temp"].asDouble - 273.15).toInt().toString() + "°C"
                parent.tv_temperature.text = temp
                val tFeels = (it["feels_like"].asDouble-273.15).toInt().toString()
                val tMin = (it["temp_min"].asDouble-273.15).toInt().toString()
                val tMax = (it["temp_max"].asDouble-273.15).toInt().toString()
                val tempDesc = "$tMin° / $tMax°\nFeels like $tFeels°"
                parent.tv_temp_desc.text = tempDesc
            }
            weatherDesc.observe(viewLifecycleOwner){
                parent.tv_weather_desc.text = it
            }
            weatherWind.observe(viewLifecycleOwner){
                parent.tv_wind.text = it
            }
        }
    }

    private fun setToolbar(){
        mViewModel.weatherLocation.observe(viewLifecycleOwner){
            parent.ctl.title = "Weather in $it is..."
            val tvLoc = "$it, "
            parent.tv_location.text = tvLoc
        }
    }

    private fun setLocationClient(){
        locationRequest = LocationRequest()
            .setInterval(2000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        locationCallback = object: LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                val latestLoc = p0?.run{
                    locations[this.locations.size-1]
                }
                mViewModel.location.value = latestLoc
            }
        }
    }

    private fun setFAB(){
        fab.setOnClickListener{
            mViewModel.isMyLocationEnabled.value = !mViewModel.isMyLocationEnabled.value!!
        }
        mViewModel.isMyLocationEnabled.observe(viewLifecycleOwner){
            if(it){
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                fab.setImageDrawable(ContextCompat.getDrawable(parent, R.drawable.ic_my_location))
                toast("내 위치 찾기를 시작합니다")
            }else {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                fab.setImageDrawable(ContextCompat.getDrawable(parent, R.drawable.ic_my_location_grey))
                toast("수동 위치 설정 시작 - 길게 눌러 핀을 꽂으세요")
            }
        }
    }

    private fun setPermission(){
        if (!permissionStatus()){
            val accessLoc = Manifest.permission.ACCESS_FINE_LOCATION
            val permissions = arrayOf(accessLoc)
            requestPermissions(permissions, REQUEST_GPS)
        }else if(mViewModel.isMyLocationEnabled.value == null)
            mViewModel.isMyLocationEnabled.value = true
    }

//    private fun setLocationManager(){
//
//        locationListener = object: LocationListener{
//            override fun onLocationChanged(location: Location?) {
//                mViewModel.location.value = location
//                Log.d("", "location changed to: $location")
//            }
//            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
//            override fun onProviderEnabled(provider: String?) {}
//            override fun onProviderDisabled(provider: String?) {}
//        }
//
//    }

    @SuppressLint("MissingPermission")
    fun requestLocationUpdates(){
        fusedLocationClient.lastLocation
            .addOnSuccessListener {
                mViewModel.location.value = it
            }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun setMap(savedInstanceState: Bundle?){
        mapView.onCreate(savedInstanceState)
        mapView.onResume()
        mapView.getMapAsync(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == REQUEST_GPS && grantResults.isNotEmpty())
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                mViewModel.isMyLocationEnabled.value = true
            else {
                toast("위치 검색을 허용해야 앱을 사용할 수 있어요")
                parent.finish()
            }
    }

    private fun permissionStatus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            parent.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else
            true
    }

    override fun onResume() {
        super.onResume()

        if(mViewModel.isMyLocationEnabled.value == true)
            requestLocationUpdates()
    }

    override fun onPause() {
        super.onPause()

        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onMapReady(p0: GoogleMap?) {
        gMap = p0

        val hyehwa = LatLng(37.582, 127.0)
        gMap?.apply{
            val latLng = mViewModel.locationLatLng.value
            if(latLng != null){
                setPinTo(latLng)
                moveCameraTo(latLng)
            }else
                mViewModel.location.value = Location("").apply{
                    longitude = hyehwa.longitude
                    latitude = hyehwa.latitude
                }

            setOnMapLongClickListener {
                if(!mViewModel.isMyLocationEnabled.value!!) {
                    mViewModel.location.value = Location("").apply{
                        longitude = it.longitude
                        latitude = it.latitude
                    }
                }
            }
        }
    }

    private fun setPinTo(latLng: LatLng){
        val icon = if(mViewModel.isMyLocationEnabled.value!!) pinPersonBitmap else pinBitmap
        val markerOptions = MarkerOptions().position(latLng)
            .icon(BitmapDescriptorFactory.fromBitmap(icon))
        gMap?.apply{
            clear()
            addMarker(markerOptions)
        }
    }

    private fun moveCameraTo(latLng: LatLng){
        gMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
    }

    private fun getWeather(){
        mViewModel.locationLatLng.value?.let {
            mViewModel.getWeather(it, requireContext())
        }
    }

    private fun toast(str: String){
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show()
    }

    private fun setPinBitmap(){
        val bmPerson = BitmapFactory.decodeResource(requireContext().resources, R.mipmap.ic_pin_person_foreground)
        val bmPin = BitmapFactory.decodeResource(requireContext().resources, R.mipmap.ic_pin_foreground)
        pinPersonBitmap = bmPerson.scale(70, 70)
        pinBitmap = bmPin.scale(70, 70)
    }

    //for getting debug key
//    private fun getSignature(context: Context): String? {
//        val pm = context.packageManager
//        try {
//            val packageInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
//            for (i in packageInfo.signatures.indices) {
//                val signature: Signature = packageInfo.signatures[i]
//                try {
//                    val md: MessageDigest = MessageDigest.getInstance("SHA")
//                    md.update(signature.toByteArray())
//                    return Base64.encodeToString(md.digest(), Base64.NO_WRAP)
//                } catch (e: NoSuchAlgorithmException) {
//                    e.printStackTrace()
//                }
//            }
//        } catch (e: PackageManager.NameNotFoundException) {
//            e.printStackTrace()
//        }
//        return null
//    }
}
package com.example.map_location_prac

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.health.connect.datatypes.ExerciseRoute
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.map_location_prac.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private var providers: String = "Providers: "
    private val MyLocationManager by lazy {
        getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private lateinit var googleMap: GoogleMap

    //위치 서비스가 gps를 사용해서 위치를 확인
    lateinit var fusedLocationClient: FusedLocationProviderClient

    //위치 값 요청에 대한 갱신 정보를 받는 변수
    lateinit var locationCallback: LocationCallback
    lateinit var locationPermission: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setGoogleMap()
    }

    override fun onMapReady(p0: GoogleMap) {
        val seoul = LatLng(37.566610, 126.978403)

        googleMap = p0
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap.run {
            markerGoogleMap(seoul, "서울시청", "Tel:01-120")
        }
        moveGoogleMap(seoul)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        updateLocation()
    }

    private fun setGoogleMap() {
        locationPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            if (results.all { it.value }) {
                (supportFragmentManager.findFragmentById(com.example.map_location_prac.R.id.mapView) as com.google.android.gms.maps.SupportMapFragment)!!.getMapAsync(
                    this
                )
            } else{
                Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

        locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    private fun moveGoogleMap(latLng: LatLng) {
        val position = CameraPosition.Builder()
            .target(latLng)
            .zoom(18f)
            .build()
        googleMap?.moveCamera((CameraUpdateFactory.newCameraPosition(position)))
    }

    private fun markerGoogleMap(latLng: LatLng, title: String, snippet: String) {
        val markerOptions = MarkerOptions()

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_marker)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, false)

        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap))
        markerOptions.position(latLng)
        markerOptions.title(title)
        markerOptions.snippet(snippet)
        googleMap?.addMarker(markerOptions)
    }

    private fun updateLocation() { // google map implementation으로 사용
        val locationRequest = LocationRequest.create().apply {
            interval = 1000 // 1초
            fastestInterval = 500 // 0.5초
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult?.let {
                    for (location in it.locations) {
                        Log.d("위치정보", "위도: ${location.latitude} 경도: ${location.longitude}")
                        setLastLocation(location)
                    }
                }
            }
        }

        //권한 처리
        if(ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED){
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()!!)
    }

    private fun setLastLocation(lastLocation: Location){
        val LATLNG = LatLng(lastLocation.latitude, lastLocation.longitude)

        markerGoogleMap(LATLNG, "내 위치", "")
        moveGoogleMap(LATLNG)
    }


    private fun setClickGoogleMpa() {
        googleMap?.setOnMapClickListener { latLng ->
            Log.d("map_test", "click : ${latLng.latitude} , ${latLng.longitude}")
        }

        googleMap?.setOnMapLongClickListener { latLng ->
            Log.d("map_test", "long click : ${latLng.latitude} , ${latLng.longitude}")
        }

        googleMap?.setOnCameraIdleListener { //지도 화면 변경 이벤트
            val position = googleMap!!.cameraPosition
            val zoom = position.zoom
            val latitude = position.target.latitude
            val longitude = position.target.longitude
            Log.d("map_test", "User change : $zoom $latitude , $longitude")
        }

        googleMap?.setOnMarkerClickListener { marker -> //마커 클릭 이벤트, 드래그도 존재
            true
        }

        googleMap?.setOnInfoWindowClickListener { marker -> // 정보 창 클릭 이벤트

        }
    }

    private fun getAllProviders() {
        val AllProviders = MyLocationManager.allProviders
        providers = "All Providers: "
        for (provider in AllProviders)
            providers += "$provider, "
    }

    private fun getEnabledProviders() {
        val enabledProviders = MyLocationManager.getProviders(true)
        providers = "Enabled Providers: "
        for (provider in enabledProviders)
            providers += "$provider, "
    }

    private fun getLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val location: Location? =
                MyLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            location?.let {
                val latitude = location.latitude
                val longitude = location.longitude
                val accuracy = location.accuracy
                val time = location.time
                Log.d("map_test", "$latitude, $location, $accuracy, $time")
            }
        }
    }

    private fun getLocationContinuously() {
        val listener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d(
                    "map_test,",
                    "${location.latitude}, ${location.longitude}, ${location.accuracy}"
                )
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            MyLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10_000L,
                10f,
                listener
            )
            // (.. 생략 ..) //
            MyLocationManager.removeUpdates(listener)
        }
    }


    private fun getLocationWithGoogle() {
        val connectionCallback = object : GoogleApiClient.ConnectionCallbacks {
            override fun onConnected(p0: Bundle?) {
                // 위치 제공자를 사용할 수 있을 때
                // 위치 획득
            }

            override fun onConnectionSuspended(p0: Int) {
                // 위치 제공자를 사용할 수 없을 때
            }
        }

        val onConnectionFailCallback = object : GoogleApiClient.OnConnectionFailedListener {
            override fun onConnectionFailed(p0: ConnectionResult) {
                // 사용할 수 있는 위치 제공자가 없을 때
            }
        }

        val apiClient = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(connectionCallback)
            .addOnConnectionFailedListener(onConnectionFailCallback)
            .build()

        apiClient.connect()
    }

}
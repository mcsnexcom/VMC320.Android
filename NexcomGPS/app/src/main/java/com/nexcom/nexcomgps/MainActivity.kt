package com.nexcom.nexcomgps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.GnssStatus
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationManager
    private lateinit var satelliteView: SatelliteView
    private lateinit var gpsStatusTextView: TextView
    private lateinit var satelliteCountTextView: TextView
    private lateinit var gpsTimeTextView: TextView
    private lateinit var gpsCoordinatesTextView: TextView
    private lateinit var satelliteInfoLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext))
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.map)
        satelliteView = findViewById(R.id.satellite_view)
        gpsStatusTextView = findViewById(R.id.gps_status)
        satelliteCountTextView = findViewById(R.id.satellite_count)
        gpsTimeTextView = findViewById(R.id.gps_time)
        gpsCoordinatesTextView = findViewById(R.id.gps_coordinates)
        satelliteInfoLayout = findViewById(R.id.satellite_info)

        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0)
        mapView.controller.setCenter(GeoPoint(25.003398, 121.504543))

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            initializeLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeLocation()
        }
    }

    private fun initializeLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, locationListener)
        locationManager.registerGnssStatusCallback(gnssStatusCallback, Handler(Looper.getMainLooper()))

        // Show initial GPS status
        updateGpsStatus()
    }

    private val locationListener = LocationListener { location ->
        val lat = location.latitude
        val lon = location.longitude
        val time = location.time

        runOnUiThread {
            satelliteView.updateCoordinates(lat, lon, time)
            gpsCoordinatesTextView.text = "Coordinates: $lat, $lon"
            gpsTimeTextView.text = "GPS Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(time))}"
            updateGpsStatus()

            // Update map center to current location
            mapView.controller.setCenter(GeoPoint(lat, lon))
        }
    }

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            runOnUiThread {
                if (status.satelliteCount > 0) {
                    satelliteView.updateSatellites(status)
                    satelliteCountTextView.text = "Satellites: ${status.satelliteCount}"
                    updateSatelliteInfo(status)
                }
            }
        }
    }

    private fun updateSatelliteInfo(status: GnssStatus) {
        satelliteInfoLayout.removeAllViews()

        for (i in 0 until status.satelliteCount) {
            val satelliteText = TextView(this).apply {
                text = "Sat $i: Az=${status.getAzimuthDegrees(i)}, El=${status.getElevationDegrees(i)}, Used=${status.usedInFix(i)}"
                textSize = 15f
            }
            satelliteInfoLayout.addView(satelliteText)
        }
    }

    private fun updateGpsStatus() {
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        gpsStatusTextView.text = if (isGpsEnabled) "GPS Status: Active" else "GPS Status: Disabled"
    }
}

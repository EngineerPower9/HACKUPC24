package com.example.prova4

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class SecondActivity : AppCompatActivity() {
    private lateinit var gnssDataLayout: LinearLayout
    private lateinit var locationManager: LocationManager
    private lateinit var gnssMeasurementsListener: GnssMeasurementsEvent.Callback
    private val maxTextViews = 15

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        gnssDataLayout = findViewById(R.id.gnssDataLayout)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager


        gnssMeasurementsListener = object : GnssMeasurementsEvent.Callback() {
            override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                val formattedMeasurements = print_connected_satelites(event)
                runOnUiThread {

                    formattedMeasurements.forEach { pair ->
                        val textView = TextView(this@SecondActivity)
                        if (gnssDataLayout.childCount == maxTextViews) {
                            gnssDataLayout.removeViewAt(0) // Remove oldest TextView
                        }
                        textView.text = pair.first
                        textView.setBackgroundColor(pair.second)
                        gnssDataLayout.addView(textView)
                    }
                }
            }

            override fun onStatusChanged(status: Int) {
                // Handle status changes if needed
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
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                123
            )
            return
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            locationManager.registerGnssMeasurementsCallback(gnssMeasurementsListener)
        }
    }



    private fun print_connected_satelites(event: GnssMeasurementsEvent): List<Pair<String, Int>> {
        val constellationColorMap = mapOf(
            GnssStatus.CONSTELLATION_GPS to Color.rgb(255,153,153),
            GnssStatus.CONSTELLATION_GLONASS to Color.rgb(153,204,255),
            GnssStatus.CONSTELLATION_BEIDOU to Color.rgb(0,193,102),
            GnssStatus.CONSTELLATION_GALILEO to Color.YELLOW,
            GnssStatus.CONSTELLATION_QZSS to Color.CYAN,
            GnssStatus.CONSTELLATION_SBAS to Color.MAGENTA,
            GnssStatus.CONSTELLATION_UNKNOWN to Color.WHITE
        )

        val filteredMeasurements = event.measurements.filter { measurement ->
            measurement.constellationType != GnssStatus.CONSTELLATION_UNKNOWN
        }

        val formattedMeasurements = filteredMeasurements.map { measurement ->
            val constellationType = measurement.constellationType
            val constellationName = when (constellationType) {
                GnssStatus.CONSTELLATION_GPS -> "GPS"
                GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
                GnssStatus.CONSTELLATION_BEIDOU -> "BEIDOU"
                GnssStatus.CONSTELLATION_GALILEO -> "GALILEO"
                GnssStatus.CONSTELLATION_QZSS -> "QZSS"
                GnssStatus.CONSTELLATION_SBAS -> "SBAS"
                else -> "UNKNOWN"
            }
            val bgColor = constellationColorMap[constellationType] ?: Color.WHITE
            Pair("Satelite ID: ${measurement.svid} \t - Constellation: $constellationName", bgColor)
        }

        return formattedMeasurements
    }




}
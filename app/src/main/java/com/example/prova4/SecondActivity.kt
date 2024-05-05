package com.example.prova4

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.GnssMeasurement
import android.location.GnssMeasurementsEvent
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


class SecondActivity : AppCompatActivity() {
    private lateinit var gnssDataLayout: LinearLayout
    private lateinit var gnssPlotView: GnssPlotView_SVID_CN
    private lateinit var skyPlotView: SkyPlotView

    private lateinit var locationManager: LocationManager
    private lateinit var gnssMeasurementsListener: GnssMeasurementsEvent.Callback

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var coordinatesTextView: TextView
    private lateinit var altitudeTextView: TextView

    private val maxTextViews = 15

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        gnssDataLayout = findViewById(R.id.gnssDataLayout)
        gnssPlotView = findViewById(R.id.gnssPlotView)
        skyPlotView = findViewById(R.id.skyPlotView)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        coordinatesTextView = findViewById(R.id.coordinatesTextView)
        altitudeTextView = findViewById(R.id.altitudeTextView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        gnssMeasurementsListener = object : GnssMeasurementsEvent.Callback() {
            override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                // We select the data for all the diferent views
                val formattedMeasurements = print_connected_satelites(event)
                val dataForPlot = prepareDataForPlot(event)

                // This is for the SkyMap (we should change)
                val satellitePositions = mapOf(
                    1 to Pair(45f, 30f), // Example position for SvId 1 (azimuth: 45 degrees, elevation: 30 degrees)
                    2 to Pair(90f, 60f),
                    3 to Pair(175f, 20f),
                    // Add more satellite positions as needed
                )

                runOnUiThread {

                    // This code detects and updates the interactions
                    formattedMeasurements.forEach { pair ->
                        val textView = TextView(this@SecondActivity)
                        if (gnssDataLayout.childCount == maxTextViews) {
                            gnssDataLayout.removeViewAt(0) // Remove oldest TextView
                        }
                        textView.text = pair.first
                        textView.setBackgroundColor(pair.second)
                        gnssDataLayout.addView(textView)
                    }

                    // This shows the Tables and Sky Plots
                    gnssPlotView.setPlots(dataForPlot.first, dataForPlot.second)
                    skyPlotView.setSatellitePositions(satellitePositions)

                }
            }
        }

        // En cas de no disposar dels permisos
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
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

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                coordinatesTextView.text = formatCoordinates(location.latitude, location.longitude).toString()
                altitudeTextView.text = "Altitude: " + location.altitude.toInt().toString() + " m"
            }
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

    private fun prepareDataForPlot(event: GnssMeasurementsEvent) : Pair<Map<Int, Float>, List<Int>> {
        val formattedMeasurements =  event.measurements.map { measurement ->
            Pair(measurement.svid, measurement.cn0DbHz.toFloat())
        }
        val constelacionTypes =  event.measurements.map { measurement ->
            measurement.constellationType
        }
        return Pair(formattedMeasurements.toMap(),constelacionTypes)
    }

/*
    private fun prepareDataForSkyView(event : GnssMeasurementsEvent, m : GnssMeasurement, l : Location) :  Map<Int, Pair<Float, Float>> {
        event.measurements.map { measurement ->
            val range = measurement.pseudorangeRateMetersPerSecond * measurement.timeOffsetNanos
            val angle = m.xSatellitePosition

        }



        return
    }
*/
    private fun formatCoordinates(latitude: Double, longitude: Double): Pair<String, String> {
        val latDegrees = latitude.toInt()
        val latMinutes = ((latitude - latDegrees) * 60).toInt()
        val latSeconds = ((latitude - latDegrees - (latMinutes.toDouble() / 60)) * 3600).toInt()
        val latDirection = if (latitude >= 0) "N" else "S"

        val lonDegrees = longitude.toInt()
        val lonMinutes = ((longitude - lonDegrees) * 60).toInt()
        val lonSeconds = ((longitude - lonDegrees - (lonMinutes.toDouble() / 60)) * 3600).toInt()
        val lonDirection = if (longitude >= 0) "E" else "W"

        val latString = "$latDegrees°$latMinutes'$latSeconds\"$latDirection"
        val lonString = "$lonDegrees°$lonMinutes'$lonSeconds\"$lonDirection"

        return Pair(latString, lonString)
    }
}
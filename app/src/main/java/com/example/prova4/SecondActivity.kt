package com.example.prova4

import android.Manifest
import android.content.pm.PackageManager
import android.location.GnssMeasurementsEvent
import android.location.LocationManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class SecondActivity : AppCompatActivity() {
    private lateinit var gnssDataTextView: TextView
    private lateinit var locationManager: LocationManager
    private lateinit var gnssMeasurementsListener: GnssMeasurementsEvent.Callback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        gnssDataTextView = findViewById(R.id.gnssDataTextView)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        gnssMeasurementsListener = object : GnssMeasurementsEvent.Callback() {
            override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                val gnssData = event.measurements.map { measurement ->
                    "--------------------\nSvId: ${measurement.svid}, Cn0DbHz: ${measurement.cn0DbHz}, PseudorangeRateMetersPerSecond: ${measurement.pseudorangeRateMetersPerSecond}\nConstelation Type: ${measurement.constellationType}\n"
                }.joinToString("\n")
                runOnUiThread {
                    gnssDataTextView.append(gnssData)
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
}
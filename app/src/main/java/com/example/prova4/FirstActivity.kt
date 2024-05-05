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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

import android.location.GnssMeasurement
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices


interface InfluxDBService { // Database stuff
    @POST("write?db=UABDB")
    fun writeData(@Body data: String): Call<Void>
}

class FirstActivity : AppCompatActivity() {
    // View layouts
    private lateinit var gnssDataLayout: LinearLayout
    private lateinit var gnssPlotView: GnssPlotView_SVID_CN
    private lateinit var skyPlotView: SkyPlotView

    // location and GNSS
    private lateinit var locationManager: LocationManager
    private lateinit var gnssMeasurementsListener: GnssMeasurementsEvent.Callback

    //  Other
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var coordinatesTextView: TextView
    private lateinit var altitudeTextView: TextView

    // database
    private lateinit var influxDBService: InfluxDBService

    private val maxTextViews = 15

    //coses de demanar a la DB
    fun createRetrofitService(): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request()
                val authenticatedRequest = request.newBuilder()
                    .header("Authorization", Credentials.basic("geriigarcia", "20501aed2466b193"))
                    .build()
                chain.proceed(authenticatedRequest)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("http://84.247.188.251:8086/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)

        // create influx DB
        influxDBService = createRetrofitService().create(InfluxDBService::class.java)

        // Layouts and Views
        gnssDataLayout = findViewById(R.id.gnssDataLayout)
        gnssPlotView = findViewById(R.id.gnssPlotView)
        skyPlotView = findViewById(R.id.skyPlotView)

        // Location
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Texts
        coordinatesTextView = findViewById(R.id.coordinatesTextView)
        altitudeTextView = findViewById(R.id.altitudeTextView)

        // The following function will execute each time we recieve data from the satelites
        gnssMeasurementsListener = object : GnssMeasurementsEvent.Callback() {
            override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                // We select the data for all the diferent views
                val formattedMeasurements = print_connected_satelites(event)
                val dataForPlot = prepareDataForPlot(event)

                // This is for the SkyMap (we should change)
                val satellitePositions = mapOf(
                    1 to Pair(
                        45f,
                        30f
                    ), // Example position for SvId 1 (azimuth: 45 degrees, elevation: 30 degrees)
                    2 to Pair(90f, 60f),
                    3 to Pair(175f, 20f),
                    // Add more satellite positions as needed
                )


                runOnUiThread {

                    // This code detects and updates the interactions
                    formattedMeasurements.forEach { pair ->
                        val textView = TextView(this@FirstActivity)
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

        // We check the permisions
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

        // Here we show the latitude, longitude and height
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                coordinatesTextView.text = formatCoordinates(location.latitude, location.longitude).toString()
                altitudeTextView.text = "Altitude: " + location.altitude.toInt().toString() + " m"
            }
        }

    }


    // DB functions
    private fun sendDataToInfluxDB(data: String) {
        influxDBService.writeData(data).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@FirstActivity, "Data sent successfully to InfluxDB", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@FirstActivity, "Failed to send data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@FirstActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


    // The next functions returns a list of satelite data
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

            // This next bit sends data to the server
            val data = "Raw " +
                    "AccumulatedDeltaRangeMeters=${measurement.accumulatedDeltaRangeMeters}," +
                    "AccumulatedDeltaRangeState=${measurement.accumulatedDeltaRangeState}," +
                    "AccumulatedDeltaRangeUncertaintyMeters=${measurement.accumulatedDeltaRangeUncertaintyMeters}," +
                    "BasebandCn0DbHz=${measurement.basebandCn0DbHz}," +
                    "BiasNanos=${measurement.satelliteInterSignalBiasNanos}," +
                    "BiasUncertaintyNanos=${measurement.satelliteInterSignalBiasUncertaintyNanos}," +
                    "CarrierCycles=${measurement.carrierCycles}," +
                    "CarrierFrequencyHz=${measurement.carrierFrequencyHz}," +
                    "CarrierPhase=${measurement.carrierPhase}," +
                    "CarrierPhaseUncertainty=${measurement.carrierPhaseUncertainty}," +
                    "Cn0DbHz=${measurement.cn0DbHz}," +
                    "ConstellationType=${measurement.constellationType}," +
                    "FullBiasNanos=${measurement.fullInterSignalBiasNanos}," +
                    "FullInterSignalBiasNanos=${measurement.fullInterSignalBiasNanos}," +
                    "FullInterSignalBiasUncertaintyNanos=${measurement.fullInterSignalBiasUncertaintyNanos}," +
                    "MultipathIndicator=${measurement.multipathIndicator}," +
                    "PseudorangeRateMetersPerSecond=${measurement.pseudorangeRateMetersPerSecond}," +
                    "PseudorangeRateUncertaintyMetersPerSecond=${measurement.pseudorangeRateUncertaintyMetersPerSecond}," +
                    "ReceivedSvTimeNanos=${measurement.receivedSvTimeNanos}," +
                    "ReceivedSvTimeUncertaintyNanos=${measurement.receivedSvTimeUncertaintyNanos}," +
                    "SatelliteInterSignalBiasNanos=${measurement.satelliteInterSignalBiasNanos}," +
                    "SatelliteInterSignalBiasUncertaintyNanos=${measurement.satelliteInterSignalBiasUncertaintyNanos}," +
                    "SnrInDb=${measurement.snrInDb}," +
                    "State=${measurement.state}," +
                    "Svid=${measurement.svid}," +
                    "TimeOffsetNanos=${measurement.timeOffsetNanos}"

            sendDataToInfluxDB(data)

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



/*
*
*
*
*
*
*
*
*
* package com.example.prova4;

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


interface InfluxDBService {
    @POST("write?db=UABDB")
    fun writeData(@Body data: String): Call<Void>
}

class MainActivity : AppCompatActivity() {
    private lateinit var influxDBService: InfluxDBService

    fun createRetrofitService(): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request()
                val authenticatedRequest = request.newBuilder()
                    .header("Authorization", Credentials.basic("geriigarcia", "20501aed2466b193"))
                    .build()
                chain.proceed(authenticatedRequest)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("http://84.247.188.251:8086/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        influxDBService = createRetrofitService().create(InfluxDBService::class.java)

        findViewById<Button>(R.id.button1).setOnClickListener {
            val data = buildDataLine()
            sendDataToInfluxDB(data)
        }

        findViewById<Button>(R.id.button3).setOnClickListener {
            startActivity(Intent(this, FirstActivity::class.java))
        }
    }

    private fun buildDataLine(): String {
        return "Raw AccumulatedDeltaRangeMeters=1584286.5679329718,AccumulatedDeltaRangeState=16.0"
    }

    private fun sendDataToInfluxDB(data: String) {
        influxDBService.writeData(data).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Data sent successfully to InfluxDB", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Failed to send data", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
*
* {'time': '2023-09-17T16:00:00Z', 'AccumulatedDeltaRangeMeters': 1584286.5679329718, 'AccumulatedDeltaRangeState': 16.0, 'AccumulatedDeltaRangeUncertaintyMeters': 0.004900000058114529, 'AgcDb': 4.0, 'BasebandCn0DbHz': 22.0, 'BiasNanos': 0.0, 'BiasUncertaintyNanos': 25.89488454489842, 'CarrierCycles': 0.0, 'CarrierFrequencyHz': 1602562560.0, 'CarrierPhase': 0.0, 'CarrierPhaseUncertainty': 0.0, 'ChipsetElapsedRealtimeNanos': 0, 'Cn0DbHz': 22.0, 'CodeType': 'C', 'ConstellationType': 3, 'DriftNanosPerSecond': 767.8170268204456, 'DriftUncertaintyNanosPerSecond': 0.0, 'FullBiasNanos': -1.378986809421e+18, 'FullInterSignalBiasNanos': 0.0, 'FullInterSignalBiasUncertaintyNanos': 0.0, 'HardwareClockDiscontinuityCount': 0, 'LeapSecond': 18, 'MultipathIndicator': 0.0, 'PseudorangeRateMetersPerSecond': -396.34449584646296, 'PseudorangeRateUncertaintyMetersPerSecond': 0.14999, 'ReceivedSvTimeNanos': 68399930290231, 'ReceivedSvTimeUncertaintyNanos': 396.0, 'SatelliteInterSignalBiasNanos': 51.7427469398681, 'SatelliteInterSignalBiasUncertaintyNanos': 66.7128, 'SnrInDb': 1.7700035039091215, 'State': 32995, 'Svid': 1, 'SvidTag': '1', 'TimeNanos': 14808580000000, 'TimeOffsetNanos': 0.0, 'TimeUncertaintyNanos': 25.89488454489842, 'utcTimeMillis': 1694966400000},

*
* [truncated]Raw AccumulatedDeltaRangeMeters=6620.74237064615,AccumulatedDeltaRangeState=17,AccumulatedDeltaRangeUncertaintyMeters=0.00271145859733223,BasebandCn0DbHz=0.0,BiasNanos=0.0,BiasUncertaintyNanos=0.0,CarrierCycles=-92233720368547
*
*
*
*
*
*
*
*
*
*
*
*
*
*
*
*
* */
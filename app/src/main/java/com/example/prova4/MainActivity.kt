package com.example.prova4;

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
